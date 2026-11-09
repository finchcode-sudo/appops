package dev.izumi.appopsnext.shizuku

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Which way the privileged backend is started: through Shizuku or through root (`su`). */
object WorkModeStore {
    private const val PREFS = "app_ops_ui"
    private const val KEY = "work_mode"
    private const val ROOT = "root"

    private val rootFlow = MutableStateFlow(false)

    @Volatile
    private var loaded = false

    private fun ensure(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            rootFlow.value = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null) == ROOT
            loaded = true
        }
    }

    fun root(context: Context): StateFlow<Boolean> {
        ensure(context)
        return rootFlow
    }

    fun isRoot(context: Context): Boolean {
        ensure(context)
        return rootFlow.value
    }

    fun setRoot(context: Context, root: Boolean) {
        ensure(context)
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, if (root) ROOT else "shizuku")
            .apply()
        rootFlow.value = root
    }
}
