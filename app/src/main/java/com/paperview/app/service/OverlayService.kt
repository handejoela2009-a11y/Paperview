package com.paperview.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.paperview.app.MainActivity
import com.paperview.app.R
import com.paperview.app.data.PreferencesRepository
import com.paperview.app.filter.FilterEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.max

/**
 * Servicio en primer plano responsable de:
 *  1. Comprobar (nunca asumir) que Android concedió el permiso de overlay.
 *  2. Dibujar una única View transparente a pantalla completa, tintada según
 *     FilterEngine, por encima de todo lo demás (TYPE_APPLICATION_OVERLAY).
 *  3. Aplicar transiciones progresivas (nunca saltos bruscos) cuando cambian
 *     los ajustes, la hora del día o la luz ambiental.
 *  4. Mostrar una notificación de estado con acción rápida de desactivar.
 *
 * Este servicio NO lee, captura ni analiza el contenido que hay debajo del
 * overlay: la View es transparente a los toques (FLAG_NOT_TOUCHABLE /
 * FLAG_NOT_FOCUSABLE) y no usa ningún servicio de accesibilidad.
 */
class OverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "paperview_status"
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISABLE = "com.paperview.app.action.DISABLE"

        fun hasOverlayPermission(context: Context): Boolean =
            Settings.canDrawOverlays(context)

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var transitionJob: Job? = null

    private lateinit var repository: PreferencesRepository
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var currentAppearance: FilterEngine.OverlayAppearance? = null

    private lateinit var lightSensorManager: LightSensorManager
    private val autoAdaptation = AutoAdaptationManager()

    override fun onCreate() {
        super.onCreate()
        repository = PreferencesRepository(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lightSensorManager = LightSensorManager(this)

        if (!hasOverlayPermission(this)) {
            // Honestidad técnica (sección 32): si no hay permiso, no fingimos
            // que el filtro está activo. Se detiene el servicio y se deja que
            // la UI informe al usuario para que lo conceda desde ajustes.
            stopSelf()
            return
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        addOverlayView()
        lightSensorManager.start()
        observeSettings()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISABLE) {
            scope.launch { repository.setActive(false) }
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lightSensorManager.stop()
        removeOverlayView()
        scope.cancel()
        super.onDestroy()
    }

    // --- Overlay window -----------------------------------------------------

    private fun addOverlayView() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // No consume toques ni foco: la app de debajo se sigue usando con
            // total normalidad (requisito de la sección 15).
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START

        val view = View(this).apply {
            background = ColorDrawable(0x00000000)
        }
        overlayView = view
        runCatching { windowManager.addView(view, params) }
            .onFailure { stopSelf() } // dispositivo/OEM restringe el overlay: no fingir que funciona
    }

    private fun removeOverlayView() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
    }

    private fun renderAppearance(appearance: FilterEngine.OverlayAppearance) {
        val view = overlayView ?: return
        val drawable = ColorDrawable(appearance.tintColor).apply {
            alpha = appearance.tintAlpha
            setColorFilter(appearance.tintColor, PorterDuff.Mode.MULTIPLY)
        }
        // Se compone tinte + oscurecimiento como dos capas dentro del mismo
        // background usando un LayerDrawable simplificado vía alpha combinado,
        // ya que la vista es única y transparente.
        view.background = drawable
        view.alpha = 1f
        // El oscurecimiento puro se aplica subiendo el alfa efectivo del negro
        // mezclado sobre el tinte: se aproxima combinando ambos alfas.
        val combined = ColorDrawable(appearance.dimColor)
        combined.alpha = appearance.dimAlpha
        view.foreground = combined
        currentAppearance = appearance
    }

    // --- Reacciona a ajustes, hora y luz ambiental --------------------------

    private fun observeSettings() {
        scope.launch {
            combine(
                repository.currentSettings,
                lightSensorManager.stableLux,
            ) { settings, lux -> settings to lux }
                .collectLatest { (settings, lux) ->
                    val target = if (settings.autoAdaptationEnabled) {
                        val timeAdjusted = autoAdaptation.presetForTime(LocalTime.now())
                        // Se combinan los sliders manuales del usuario con el
                        // preset horario solo si el usuario no personalizó,
                        // para no pisar una configuración manual explícita.
                        val base = if (settings.id == "personalizado") settings else timeAdjusted
                        autoAdaptation.adjustForAmbientLight(base, lux)
                    } else {
                        settings
                    }
                    transitionTo(FilterEngine.toAppearance(target), settings.transitionSpeedMs)
                }
        }
    }

    /** Nunca aplica un cambio de golpe: interpola durante `durationMs`
     *  (secciones 10 y 12 — "evitar cambios bruscos"). */
    private fun transitionTo(target: FilterEngine.OverlayAppearance, durationMs: Long) {
        val start = currentAppearance ?: run {
            renderAppearance(target)
            return
        }
        transitionJob?.cancel()
        transitionJob = scope.launch {
            val steps = max(1, (durationMs / 50L).toInt())
            for (i in 1..steps) {
                val fraction = i / steps.toFloat()
                renderAppearance(FilterEngine.lerp(start, target, fraction))
                kotlinx.coroutines.delay(50L)
            }
        }
    }

    // --- Notificación de estado ---------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val disableIntent = PendingIntent.getService(
            this, 0,
            Intent(this, OverlayService::class.java).setAction(ACTION_DISABLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_paper)
            .setContentTitle(getString(R.string.notification_active_title))
            .setContentText(getString(R.string.notification_active_text))
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_action_disable), disableIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
