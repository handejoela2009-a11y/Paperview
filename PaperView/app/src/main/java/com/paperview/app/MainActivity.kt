package com.paperview.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.paperview.app.service.OverlayService
import com.paperview.app.ui.screens.CalibrationScreen
import com.paperview.app.ui.screens.MainScreen
import com.paperview.app.ui.theme.PaperViewTheme
import com.paperview.app.viewmodel.PaperViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PaperViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* opcional, ver README */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val state by viewModel.uiState.collectAsState()

            PaperViewTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (!state.isOnboarded) {
                        CalibrationScreen(onSelected = { viewModel.completeOnboarding(it) })
                    } else {
                        MainScreen(
                            state = state,
                            viewModel = viewModel,
                            onRequestOverlayPermission = { requestOverlayPermission() },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // El permiso de overlay se concede desde una pantalla del sistema, así
        // que se reevalúa aquí en vez de asumir que sigue como estaba.
        viewModel.refreshPermissionState()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"),
        )
        startActivity(intent)
    }
}
