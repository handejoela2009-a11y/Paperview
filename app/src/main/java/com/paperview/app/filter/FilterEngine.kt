package com.paperview.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
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

    private val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, _ ->
        runCatching { scope.launch { repository.setActive(false) } }
        stopSelf()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + exceptionHandler)
    private var transitionJob: Job? = null

    private lateinit var repository: PreferencesRepository
    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var currentAppearance: FilterEngine.OverlayAppearance? = null

    private lateinit var lightSensorManager: LightSensorManager
    private val autoAdaptation = AutoAdaptationManager()

    override fun onCreate() {
        super.onCreate()
        try {
            repository = PreferencesRepository(this)
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            lightSensorManager = LightSensorManager(this)

            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification())

            if (!hasOverlayPermission(this)) {
                scope.launch { repository.setActive(false) }
                stopSelf()
                return
            }

            addOverlayView()
            lightSensorManager.start()
            observeSettings()
        } catch (t: Throwable) {
            runCatching { if (::repository.isInitialized) scope.launch { repository.setActive(false) } }
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISABLE) {
            scope.launch { repository.setActive(false) }
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        lightSensorManager.stop()
        removeOverlayView()
        scope.cancel()
        super.onDestroy()
    }

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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        params.gravity = Gravity.TOP or Gravity.START

        val view = View(this).apply {
            background = ColorDrawable(0x00000000)
        }
        overlayView = view
        runCatching { windowManager.addView(view, params) }
            .onFailure { stopSelf() }
    }

    private fun removeOverlayView() {
        overlayView?.let { runCatching { windowManager.removeView(it) } }
        overlayView = null
    }

    private fun renderAppearance(appearance: FilterEngine.OverlayAppearance) {
        val view = overlayView ?: return
        val combinedColor = FilterEngine.compose(appearance)
        view.background = ColorDrawable(combinedColor)
        view.foreground = null
        currentAppearance = appearance
    }

    private fun observeSettings() {
        scope.launch {
            combine(
                repository.currentSettings,
                lightSensorManager.stableLux,
            ) { settings, lux -> settings to lux }
                .collectLatest { (settings, lux) ->
                    val target = if (settings.autoAdaptationEnabled) {
                        val timeAdjusted = autoAdaptation.presetForTime(LocalTime.now())
                        val base = if (settings.id == "personalizado") settings else timeAdjusted
                        autoAdaptation.adjustForAmbientLight(base, lux)
                    } else {
                        settings
                    }
                    transitionTo(FilterEngine.toAppearance(target), settings.transitionSpeedMs)
                }
        }
    }

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
