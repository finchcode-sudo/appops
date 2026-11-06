package dev.izumi.appopsnext.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.izumi.appopsnext.R
import dev.izumi.appopsnext.appops.command.AppOpMode
import dev.izumi.appopsnext.appops.model.AppOpModeChangePhase
import dev.izumi.appopsnext.appops.model.AppOpsRestorationStatus
import dev.izumi.appopsnext.presentation.app_detail.AppOpDisplayCatalog
import dev.izumi.appopsnext.presentation.app_detail.AppOpModeChangeRequest
import dev.izumi.appopsnext.presentation.app_detail.AppOpModeChangeUiState
import dev.izumi.appopsnext.presentation.history.HistoryPermissionIconCatalog

/** 带返回箭头和可选副标题的顶栏。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShellTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                    )
                }
            }
        },
        actions = actions,
    )
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, hint: String) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(hint) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@StringRes
fun modeLabelRes(mode: AppOpMode): Int = when (mode) {
    AppOpMode.ALLOW -> R.string.app_op_mode_allow
    AppOpMode.IGNORE -> R.string.app_op_mode_ignore
    AppOpMode.DENY -> R.string.app_op_mode_deny
    AppOpMode.DEFAULT -> R.string.app_op_mode_default
    AppOpMode.FOREGROUND -> R.string.app_op_mode_foreground
}

/** 权限的本地化名称；目录里没有的就显示系统名。 */
@Composable
fun opLabel(operationName: String): String {
    val res = AppOpDisplayCatalog.labelResOf(operationName)
    return if (res != null) stringResource(res) else operationName
}

@Composable
fun PermIcon(operationName: String, size: Dp = 24.dp) {
    Icon(
        painter = painterResource(HistoryPermissionIconCatalog.visualFor(operationName).iconRes),
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 当前模式按钮，点开选新模式。 */
@Composable
fun ModeDropdown(rawMode: String, onPick: (AppOpMode) -> Unit) {
    val current = AppOpMode.fromShellValue(rawMode)
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }, enabled = current != null) {
            Text(
                text = if (current != null) {
                    stringResource(R.string.app_detail_mode_button, stringResource(modeLabelRes(current)))
                } else {
                    rawMode
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            AppOpMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(modeLabelRes(mode))) },
                    onClick = {
                        open = false
                        if (mode != current) onPick(mode)
                    },
                )
            }
        }
    }
}

// ---- 改模式的确认 / 进度 / 结果弹窗 ----

@Composable
fun ModeChangeDialogs(
    state: AppOpModeChangeUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onTryForeground: () -> Unit,
    onDenyNoticeDismiss: (Boolean) -> Unit,
    onFlagNoticeDismiss: (Boolean) -> Unit,
) {
    when (state) {
        AppOpModeChangeUiState.Idle -> Unit
        is AppOpModeChangeUiState.Confirming -> ConfirmDialog(state.request, onConfirm, onDismiss)
        is AppOpModeChangeUiState.Applying -> AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(R.string.app_detail_mode_item_applying)) },
            text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
        )
        is AppOpModeChangeUiState.DenyFallbackApplied -> NoticeDialog(
            title = stringResource(R.string.app_detail_mode_deny_fallback_applied_title),
            message = stringResource(R.string.app_detail_mode_deny_fallback_applied),
            checkboxLabel = stringResource(R.string.app_detail_mode_deny_fallback_dont_show),
            onClose = onDenyNoticeDismiss,
        )
        is AppOpModeChangeUiState.PermissionFlagApplied -> {
            val base = stringResource(R.string.app_detail_mode_permission_flag_applied)
            val deny = stringResource(R.string.app_detail_mode_permission_flag_deny)
            val settings = stringResource(R.string.app_detail_mode_permission_flag_settings)
            NoticeDialog(
                title = stringResource(R.string.app_detail_mode_permission_flag_title),
                message = if (state.denyFallbackAttempted) "$deny\n\n$base\n$settings" else "$base\n$settings",
                checkboxLabel = stringResource(R.string.app_detail_mode_permission_flag_dont_show),
                onClose = onFlagNoticeDismiss,
            )
        }
        is AppOpModeChangeUiState.Failure -> FailureDialog(state, onDismiss, onTryForeground)
    }
}

@Composable
private fun ConfirmDialog(
    request: AppOpModeChangeRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_detail_mode_confirm_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.app_detail_mode_confirm_operation, opLabel(request.operationName)))
                Text(stringResource(R.string.app_detail_mode_confirm_package, request.packageName))
                Text(
                    stringResource(
                        R.string.app_detail_mode_transition,
                        stringResource(modeLabelRes(request.originalMode)),
                        stringResource(modeLabelRes(request.requestedMode)),
                    ),
                    fontWeight = FontWeight.Medium,
                )
                val others = request.affectedPackages.filter { it != request.packageName }
                if (others.isNotEmpty()) {
                    Text(stringResource(R.string.app_detail_mode_shared_uid_warning, others.joinToString("\n")))
                }
                if (request.requestedMode == AppOpMode.DENY) {
                    Text(
                        stringResource(R.string.app_detail_mode_deny_fallback_notice),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    stringResource(R.string.app_detail_mode_confirm_warning),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun NoticeDialog(
    title: String,
    message: String,
    checkboxLabel: String,
    onClose: (Boolean) -> Unit,
) {
    var checked by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onClose(false) },
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { checked = !checked }
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = checked, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Text(checkboxLabel, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onClose(checked) }) { Text(stringResource(R.string.action_got_it)) }
        },
    )
}

@StringRes
private fun phaseRes(phase: AppOpModeChangePhase): Int = when (phase) {
    AppOpModeChangePhase.READ_ORIGINAL -> R.string.app_detail_mode_phase_read_original
    AppOpModeChangePhase.CHECK_ORIGINAL -> R.string.app_detail_mode_phase_check_original
    AppOpModeChangePhase.APPLY_REQUESTED -> R.string.app_detail_mode_phase_apply
    AppOpModeChangePhase.VERIFY_REQUESTED -> R.string.app_detail_mode_phase_verify
    AppOpModeChangePhase.RESTORE_ORIGINAL -> R.string.app_detail_mode_phase_restore
    AppOpModeChangePhase.VERIFY_RESTORED -> R.string.app_detail_mode_phase_verify_restored
}

@StringRes
private fun restorationRes(status: AppOpsRestorationStatus): Int = when (status) {
    AppOpsRestorationStatus.NOT_REQUIRED -> R.string.app_detail_mode_restore_not_required
    AppOpsRestorationStatus.SUCCEEDED -> R.string.app_detail_mode_restored
    AppOpsRestorationStatus.FAILED -> R.string.app_detail_mode_restore_failed
}

@Composable
private fun FailureDialog(
    state: AppOpModeChangeUiState.Failure,
    onDismiss: () -> Unit,
    onTryForeground: () -> Unit,
) {
    val result = state.result
    val tryForeground = dev.izumi.appopsnext.presentation.app_detail.ModeChangeAlternativePolicy
        .canTryForeground(state.request, result)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_detail_mode_change_failure)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(
                        R.string.app_detail_mode_change_failure_detail,
                        stringResource(phaseRes(result.phase)),
                        stringResource(restorationRes(result.restorationStatus)),
                    ),
                )
                val observed = result.observedMode
                if (observed != null) {
                    Text(stringResource(R.string.app_detail_mode_observed, stringResource(modeLabelRes(observed))))
                }
                if (state.denyFallbackAttempted) {
                    Text(stringResource(R.string.app_detail_mode_deny_fallback_failed))
                }
                val hint = when {
                    state.request.runtimePermissionDenied -> R.string.tp_runtime_denied_hint
                    state.runtimePermissionManaged -> R.string.app_detail_mode_runtime_managed_hint
                    tryForeground -> R.string.app_detail_mode_allow_foreground_hint
                    else -> R.string.app_detail_mode_change_rejected_hint
                }
                Text(
                    stringResource(hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_dismiss)) } },
        dismissButton = if (tryForeground) {
            @Composable {
                TextButton(onClick = onTryForeground) {
                    Text(stringResource(R.string.app_detail_mode_try_foreground))
                }
            }
        } else null,
    )
}
