package dev.izumi.appopsnext.clipboard

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import dev.izumi.appopsnext.AppOpsNextApplication
import dev.izumi.appopsnext.R
import dev.izumi.appopsnext.appops.command.AppOpMode
import dev.izumi.appopsnext.appops.model.AppOpIdentifier
import dev.izumi.appopsnext.appops.model.AppOpModeChangeResult
import dev.izumi.appopsnext.appops.model.AppOpScope
import dev.izumi.appopsnext.monitor.AppOpAccessEvent
import dev.izumi.appopsnext.monitor.AppOpAccessKind
import dev.izumi.appopsnext.monitor.AppOpCodes
import dev.izumi.appopsnext.monitor.AppOpsMonitorClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** Shows a prompt over other apps whenever an app reads the clipboard. */
class ClipboardMonitorService : Service() {
    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = AppOpsMonitorClient()
    private var overlay: View? = null
    private val dismiss = Runnable { removeOverlay() }

    /** package -> mode to put back after a temporary allow. */
    private val pendingRestore = HashMap<String, AppOpMode>()

    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val operation = AppOpIdentifier(OPERATION, OPERATION)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        scope.launch { register() }
        return START_STICKY
    }

    private fun register() {
        val code = AppOpCodes.codeOf(OPERATION) ?: return
        runCatching {
            client.register(intArrayOf(code), { event -> onAccess(event) }, { })
        }
    }

    private fun onAccess(event: AppOpAccessEvent) {
        if (event.kind != AppOpAccessKind.NOTED) return
        val pkg = event.packageName
        if (pkg == packageName) return
        if (pkg in prefs.getStringSet(KEY_EXCLUDED, emptySet()).orEmpty()) return
        val pm = packageManager
        val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
        if (prefs.getBoolean(KEY_EXCLUDE_SYSTEM, false) &&
            info != null && info.flags and ApplicationInfo.FLAG_SYSTEM != 0
        ) return
        val label = info?.let { pm.getApplicationLabel(it).toString() } ?: pkg
        main.post { show(pkg, label, event.allowed) }
    }

    private fun show(pkg: String, label: String, allowed: Boolean) {
        if (!Settings.canDrawOverlays(this)) return
        removeOverlay()
        val seconds = prefs.getInt(KEY_SECONDS, 5)
        val atTop = prefs.getBoolean(KEY_AT_TOP, false)
        val dp = resources.displayMetrics.density
        val dark = (resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (10 * dp).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (dark) "#1B1B1F" else "#F2F3FF"))
                setStroke((2 * dp).toInt(), Color.parseColor(if (dark) "#A8C7FA" else "#0B57D0"))
                cornerRadius = 24 * dp
            }
        }
        row.addView(
            TextView(this).apply {
                text = getString(
                    if (allowed) R.string.tp_clip_accessed else R.string.tp_clip_denied,
                    label,
                )
                setTextColor(Color.parseColor(if (dark) "#E3E2E6" else "#1B1B1F"))
                textSize = 14f
            },
            LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f),
        )
        if (!allowed) {
            row.addView(
                Button(this).apply {
                    text = getString(R.string.tp_clip_allow)
                    setOnClickListener {
                        isEnabled = false
                        tempAllow(pkg, seconds)
                        main.removeCallbacks(dismiss)
                        main.postDelayed(dismiss, 1000)
                    }
                },
            )
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = (if (atTop) Gravity.TOP else Gravity.BOTTOM) or Gravity.CENTER_HORIZONTAL
            val m = (12 * dp).toInt()
            x = 0
            y = m * 3
            horizontalMargin = 0.03f
        }
        val wm = getSystemService(WindowManager::class.java)
        runCatching { wm.addView(row, params) }.onSuccess {
            overlay = row
            main.postDelayed(dismiss, seconds * 1000L)
        }
    }

    private fun removeOverlay() {
        main.removeCallbacks(dismiss)
        overlay?.let { v ->
            runCatching { getSystemService(WindowManager::class.java).removeView(v) }
        }
        overlay = null
    }

    private fun tempAllow(pkg: String, seconds: Int) {
        val repo = (application as AppOpsNextApplication).appOpsRepository
        scope.launch {
            val result = repo.applyMode(pkg, operation, AppOpScope.PACKAGE, AppOpMode.ALLOW)
            val original = (result as? AppOpModeChangeResult.Success)?.originalMode ?: return@launch
            if (original == AppOpMode.ALLOW) return@launch
            synchronized(pendingRestore) { pendingRestore.putIfAbsent(pkg, original) }
            delay(seconds * 1000L)
            restore(pkg)
        }
    }

    private suspend fun restore(pkg: String) {
        val mode = synchronized(pendingRestore) { pendingRestore.remove(pkg) } ?: return
        val repo = (application as AppOpsNextApplication).appOpsRepository
        repo.applyMode(pkg, operation, AppOpScope.PACKAGE, mode)
    }

    override fun onDestroy() {
        runCatching { client.unregister() }
        main.post { removeOverlay() }
        val left = synchronized(pendingRestore) { pendingRestore.keys.toList() }
        if (left.isNotEmpty()) {
            runBlocking { withTimeoutOrNull(4000) { left.forEach { restore(it) } } }
        }
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL, getString(R.string.tp_clip_notif_channel), NotificationManager.IMPORTANCE_LOW,
            ),
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_monitor)
            .setContentTitle(getString(R.string.tp_clip_notif_title))
            .setOngoing(true)
            .build()
    }

    companion object {
        const val PREFS = "app_ops_ui"
        const val KEY_ENABLED = "clip_enabled"
        const val KEY_AT_TOP = "clip_top"
        const val KEY_SECONDS = "clip_seconds"
        const val KEY_EXCLUDE_SYSTEM = "clip_exclude_system"
        const val KEY_EXCLUDED = "clip_excluded"
        private const val OPERATION = "READ_CLIPBOARD"
        private const val CHANNEL = "clipboard_monitor"
        private const val NOTIFICATION_ID = 7421

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ClipboardMonitorService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ClipboardMonitorService::class.java))
        }
    }
}
