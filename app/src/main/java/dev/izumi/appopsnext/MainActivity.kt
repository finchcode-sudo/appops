package dev.izumi.appopsnext

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.izumi.appopsnext.development.DevelopmentWindowPolicy
import dev.izumi.appopsnext.monitor.AppOpsMonitorService
import dev.izumi.appopsnext.presentation.AppOpsApp
import dev.izumi.appopsnext.presentation.AppOpsTheme
import dev.izumi.appopsnext.presentation.app_list.AppListViewModel
import dev.izumi.appopsnext.presentation.diagnostics.DiagnosticsViewModel
import dev.izumi.appopsnext.presentation.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val diagnosticsViewModel: DiagnosticsViewModel by viewModels()
    private val appListViewModel: AppListViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private var modeChosen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DevelopmentWindowPolicy.apply(window)
        modeChosen = getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_MODE_CHOSEN, false)
        restoreBackgroundMonitor()

        setContent {
            val diagnostics by diagnosticsViewModel.uiState.collectAsStateWithLifecycle()
            val appList by appListViewModel.uiState.collectAsStateWithLifecycle()
            val settings by settingsViewModel.uiState.collectAsStateWithLifecycle()

            AppOpsTheme {
                AppOpsApp(
                    diagnostics = diagnostics,
                    appList = appList,
                    hideSystemApps = settings.hideSystemApps,
                    modeChosen = modeChosen,
                    onModeChosenChange = ::setModeChosen,
                    onHideSystemAppsChange = settingsViewModel::setHideSystemApps,
                    onShizukuAction = diagnosticsViewModel::performShizukuAction,
                    onRetryService = diagnosticsViewModel::retryPrivilegedService,
                    onSearchQueryChange = appListViewModel::updateSearchQuery,
                )
            }
        }
    }

    private fun setModeChosen(chosen: Boolean) {
        modeChosen = chosen
        getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MODE_CHOSEN, chosen)
            .apply()
    }

    /**
     * A force stop tears the service down and stops the system from restarting
     * it, so the switch would read as on while nothing was being watched. This
     * brings the service back the next time the user opens the app.
     */
    private fun restoreBackgroundMonitor() {
        val application = application as AppOpsNextApplication
        lifecycleScope.launch {
            val enabled = runCatching {
                application.userSettingsRepository.settings.first().backgroundMonitor
            }.getOrDefault(false)
            if (enabled && !application.appOpsMonitorController.isRunning) {
                runCatching {
                    AppOpsMonitorService.start(this@MainActivity)
                }.onFailure { error ->
                    application.diagnosticLogRepository.error(
                        source = "Monitor",
                        message = "Unable to restore the background monitor.",
                        error = error,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as AppOpsNextApplication).apply {
            newAppPolicyCoordinator.onAppForeground()
            guardController.onAppForeground()
            historyArchiveRecorder.onAppForeground()
        }
        appListViewModel.refreshAfterResume()
    }

    private companion object {
        const val UI_PREFS = "app_ops_ui"
        const val KEY_MODE_CHOSEN = "mode_chosen"
    }
}
