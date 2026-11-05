package dev.izumi.appopsnext.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.izumi.appopsnext.R
import dev.izumi.appopsnext.guard.GuardMode
import dev.izumi.appopsnext.presentation.experimental.ExperimentalViewModel
import dev.izumi.appopsnext.presentation.experimental.GuardViewModel
import dev.izumi.appopsnext.presentation.settings.BackupUiState
import dev.izumi.appopsnext.presentation.settings.SettingsViewModel
import dev.izumi.appopsnext.presentation.templates.TemplatesViewModel
import dev.izumi.appopsnext.settings.AppLanguage
import dev.izumi.appopsnext.settings.AppTheme

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, summary: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, current: T, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(value); onDismiss() },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = value == current, onClick = { onPick(value); onDismiss() })
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tp_cancel)) } },
    )
}

// ---- 设置 ----

@Composable
internal fun SettingsPage(vm: SettingsViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var langDialog by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    val langLabel = { l: AppLanguage ->
        when (l) {
            AppLanguage.SYSTEM -> R.string.tp_follow_system
            AppLanguage.SIMPLIFIED_CHINESE -> R.string.tp_lang_zh
            AppLanguage.ENGLISH -> R.string.tp_lang_en
        }
    }
    val themeLabel = { t: AppTheme ->
        when (t) {
            AppTheme.SYSTEM -> R.string.tp_follow_system
            AppTheme.LIGHT -> R.string.tp_theme_light
            AppTheme.DARK -> R.string.tp_theme_dark
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionTitle(stringResource(R.string.tp_section_behavior))
        SwitchRow(stringResource(R.string.tp_hide_system), null, state.hideSystemApps) { vm.setHideSystemApps(it) }
        SwitchRow(
            stringResource(R.string.tp_save_history),
            stringResource(R.string.tp_save_history_summary, state.savedHistory.recordCount),
            state.saveIndividualHistory,
        ) { vm.setSaveIndividualHistory(it) }
        SectionTitle(stringResource(R.string.tp_section_appearance))
        ValueRow(stringResource(R.string.tp_language), stringResource(langLabel(state.appLanguage))) { langDialog = true }
        ValueRow(stringResource(R.string.tp_theme), stringResource(themeLabel(state.appTheme))) { themeDialog = true }
    }
    if (langDialog) {
        ChoiceDialog(
            stringResource(R.string.tp_language),
            AppLanguage.entries.map { it to stringResource(langLabel(it)) },
            state.appLanguage,
            { vm.setAppLanguage(it) },
            { langDialog = false },
        )
    }
    if (themeDialog) {
        ChoiceDialog(
            stringResource(R.string.tp_theme),
            AppTheme.entries.map { it to stringResource(themeLabel(it)) },
            state.appTheme,
            { vm.setAppTheme(it) },
            { themeDialog = false },
        )
    }
}

// ---- 自动恢复 ----

@Composable
internal fun AutoRestorePage(vm: GuardViewModel) {
    val mode by vm.mode.collectAsStateWithLifecycle()
    val rules by vm.rules.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val checking by vm.checking.collectAsStateWithLifecycle()
    val modeLabel = { m: GuardMode ->
        when (m) {
            GuardMode.OFF -> R.string.tp_guard_off
            GuardMode.PERIODIC -> R.string.tp_guard_periodic
            GuardMode.REALTIME -> R.string.tp_guard_realtime
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionTitle(stringResource(R.string.tp_guard_mode)) }
        items(GuardMode.entries.toList()) { m ->
            Row(
                Modifier.fillMaxWidth().clickable { vm.setMode(m) }.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = mode == m, onClick = { vm.setMode(m) })
                Text(stringResource(modeLabel(m)), style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Button(
                onClick = { vm.checkNow() },
                enabled = !checking && rules.isNotEmpty(),
                modifier = Modifier.padding(16.dp),
            ) { Text(stringResource(if (checking) R.string.tp_guard_checking else R.string.tp_guard_check_now)) }
        }
        item { SectionTitle(stringResource(R.string.tp_guard_rules, rules.size)) }
        if (rules.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.tp_guard_empty),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(rules, key = { it.id }) { rule ->
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(opLabel(rule.stableOperationName), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        rule.packageName + " · " + stringResource(modeLabelRes(rule.expectedMode)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = rule.autoRestore, onCheckedChange = { vm.setAutoRestore(rule, it) })
                TextButton(onClick = { vm.remove(rule) }) { Text(stringResource(R.string.tp_delete)) }
            }
            HorizontalDivider()
        }
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tp_guard_events, events.size), Modifier.weight(1f))
                TextButton(onClick = { vm.clearEvents() }, enabled = events.isNotEmpty()) {
                    Text(stringResource(R.string.tp_clear))
                }
            }
        }
    }
}

// ---- 备份和恢复 ----

@Composable
internal fun BackupPage(vm: SettingsViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.openBackup(uri)
    }
    val backup = state.backup
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.tp_backup_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { create.launch("appops-backup.json") }, enabled = backup !is BackupUiState.Working) {
            Text(stringResource(R.string.tp_backup_create))
        }
        Button(onClick = { open.launch(arrayOf("*/*")) }, enabled = backup !is BackupUiState.Working) {
            Text(stringResource(R.string.tp_backup_restore))
        }
        when (backup) {
            is BackupUiState.Working -> Text(stringResource(R.string.tp_working))
            is BackupUiState.Finished -> Text(backup.outcome.name)
            is BackupUiState.Rejected -> Text(stringResource(R.string.tp_backup_rejected))
            else -> {}
        }
    }
    if (backup is BackupUiState.Preview) {
        val p = backup.preview
        AlertDialog(
            onDismissRequest = { vm.dismissBackup() },
            title = { Text(stringResource(R.string.tp_backup_restore)) },
            text = {
                Column {
                    Text(stringResource(R.string.tp_backup_templates, p.templates?.added ?: 0))
                    Text(stringResource(R.string.tp_backup_monitor, p.monitorAppCount ?: 0))
                }
            },
            confirmButton = { TextButton(onClick = { vm.confirmBackupImport() }) { Text(stringResource(R.string.tp_confirm)) } },
            dismissButton = { TextButton(onClick = { vm.dismissBackup() }) { Text(stringResource(R.string.tp_cancel)) } },
        )
    }
}

// ---- 新应用行为 ----

@Composable
internal fun NewAppBehaviorPage(vm: TemplatesViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SwitchRow(
            stringResource(R.string.tp_new_auto_apply),
            stringResource(R.string.tp_new_auto_apply_summary),
            state.autoApplyNewAppTemplate,
        ) { vm.setAutoApplyNewAppTemplate(it) }
        SwitchRow(
            stringResource(R.string.tp_new_instant),
            stringResource(R.string.tp_new_instant_summary),
            state.instantNewAppRules,
        ) { vm.setInstantNewAppRules(it) }
    }
}

// ---- 模版 ----

@Composable
internal fun TemplatesPage(vm: TemplatesViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    val selected = state.selectedTemplate
    if (selected != null) {
        androidx.activity.compose.BackHandler { vm.closeEditor() }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(selected.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.closeEditor() }) { Text(stringResource(R.string.tp_done)) }
            }
            HorizontalDivider()
            LazyColumn(Modifier.weight(1f)) {
                if (selected.rules.isEmpty()) {
                    item { Text(stringResource(R.string.tp_tpl_no_rules), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(selected.rules, key = { it.stableOperationName }) { rule ->
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(opLabel(rule.stableOperationName), Modifier.weight(1f))
                        ModeDropdown(rule.mode.shellValue) { vm.setRuleMode(rule.stableOperationName, it) }
                    }
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Button(onClick = { creating = true; name = "" }, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.tp_tpl_create))
            }
            LazyColumn(Modifier.weight(1f)) {
                if (state.templates.isEmpty()) {
                    item { Text(stringResource(R.string.tp_tpl_empty), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(state.templates, key = { it.id }) { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.selectTemplate(t.id) }.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.tp_tpl_rule_count, t.rules.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { vm.deleteTemplate(t.id) }) { Text(stringResource(R.string.tp_delete)) }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    if (creating) {
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(stringResource(R.string.tp_tpl_create)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { vm.createTemplate(name.trim()); creating = false }, enabled = name.isNotBlank()) {
                    Text(stringResource(R.string.tp_confirm))
                }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(stringResource(R.string.tp_cancel)) } },
        )
    }
}

// ---- 使用情况监视器 ----

@Composable
internal fun UsageMonitorPage(vm: ExperimentalViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SwitchRow(
            stringResource(R.string.tp_monitor_enable),
            stringResource(R.string.tp_monitor_summary),
            state.monitorEnabled,
            enabled = !state.monitorBusy,
        ) { vm.setMonitorEnabled(it) }
        state.monitorFailure?.let {
            Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
        }
        SectionTitle(stringResource(R.string.tp_monitor_targets, state.targetCount))
        state.targets.forEach { t ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(t.packageName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    t.operationNames.joinToString(", ") { it },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
        if (state.targets.isEmpty()) {
            Text(stringResource(R.string.tp_monitor_empty), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- 剪贴板监视器（仅界面，后端没有此功能） ----

@Composable
internal fun ClipboardMonitorPage() {
    var enabled by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SwitchRow(stringResource(R.string.tp_clip_enable), null, enabled) { enabled = it }
        SwitchRow(stringResource(R.string.tp_clip_overlay), null, overlay) { overlay = it }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.tp_clip_notice),
            Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
