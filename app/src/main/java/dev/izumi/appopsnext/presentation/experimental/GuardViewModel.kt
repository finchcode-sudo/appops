package dev.izumi.appopsnext.presentation.experimental

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.izumi.appopsnext.AppOpsNextApplication
import dev.izumi.appopsnext.guard.GuardMode
import dev.izumi.appopsnext.guard.GuardRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GuardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = getApplication<AppOpsNextApplication>()
    private val repository = app.guardRepository
    val mode = repository.mode.stateIn(viewModelScope, SharingStarted.Eagerly, GuardMode.OFF)
    val rules = repository.rules.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val events = repository.events.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val mutableChecking = MutableStateFlow(false)
    val checking = mutableChecking.asStateFlow()

    fun setMode(mode: GuardMode) {
        viewModelScope.launch { repository.setMode(mode) }
    }

    fun setAutoRestore(rule: GuardRule, enabled: Boolean) {
        viewModelScope.launch { repository.upsert(rule.copy(autoRestore = enabled)) }
    }

    fun remove(rule: GuardRule) {
        viewModelScope.launch { repository.remove(rule.id) }
    }

    fun checkNow() {
        if (mutableChecking.value) return
        mutableChecking.value = true
        viewModelScope.launch {
            try {
                app.guardController.runCheck("manual")
            } finally {
                mutableChecking.value = false
            }
        }
    }

    fun clearEvents() {
        viewModelScope.launch { repository.clearEvents() }
    }
}

