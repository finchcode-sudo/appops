package dev.izumi.appopsnext.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.izumi.appopsnext.R
import dev.izumi.appopsnext.appops.command.AppOpMode
import dev.izumi.appopsnext.apps.model.InstalledApp
import dev.izumi.appopsnext.presentation.app_detail.AppDetailUiState
import dev.izumi.appopsnext.presentation.app_detail.AppDetailViewModel
import dev.izumi.appopsnext.presentation.app_detail.AppOpDisplayCatalog

/** 应用详情：列出这个应用的全部权限，点右侧按钮改模式。 */
@Composable
fun AppDetailPage(vm: AppDetailViewModel, onBack: () -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val change by vm.modeChangeState.collectAsStateWithLifecycle()
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler {
        if (searching) {
            searching = false
            vm.updateSearchQuery("")
        } else {
            onBack()
        }
    }

    val state = ui
    val app: InstalledApp? = when (state) {
        is AppDetailUiState.Ready -> state.app
        is AppDetailUiState.Loading -> state.app
        is AppDetailUiState.WaitingForBackend -> state.app
        is AppDetailUiState.Failure -> state.app
        AppDetailUiState.Idle -> null
    }

    Scaffold(
        topBar = {
            ShellTopBar(
                title = app?.label.orEmpty(),
                subtitle = app?.packageName,
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        if (searching) vm.updateSearchQuery("")
                        searching = !searching
                    }) {
                        Icon(
                            if (searching) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = stringResource(R.string.shell_search),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (searching) {
                SearchField(
                    value = query,
                    onValueChange = vm::updateSearchQuery,
                    hint = stringResource(R.string.app_detail_search_label),
                )
            }
            when (state) {
                is AppDetailUiState.Ready -> {
                    val ops = remember(state.snapshot, query) {
                        AppOpDisplayCatalog.build(
                            entries = state.snapshot.entries,
                            query = query,
                            labelResolver = { context.getString(it) },
                        )
                    }
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(ops, key = { it.operationName }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PermIcon(item.operationName)
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    val labelRes = item.labelRes
                                    Text(
                                        if (labelRes != null) stringResource(labelRes) else item.operationName,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    if (labelRes != null) {
                                        Text(
                                            item.operationName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                ModeDropdown(
                                    rawMode = item.mode,
                                    onPick = { picked ->
                                        vm.requestModeChange(
                                            item.operationName,
                                            item.scope,
                                            AppOpMode.fromShellValue(item.mode) ?: AppOpMode.DEFAULT,
                                            picked,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                is AppDetailUiState.Failure -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.permission_load_failed),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = vm::refresh) { Text(stringResource(R.string.action_retry)) }
                }
                else -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    ModeChangeDialogs(
        state = change,
        onConfirm = { vm.confirmModeChange() },
        onDismiss = vm::dismissModeChange,
        onTryForeground = vm::requestForegroundAlternative,
        onDenyNoticeDismiss = vm::dismissDenyFallbackNotice,
        onFlagNoticeDismiss = vm::dismissPermissionFlagNotice,
    )
}
