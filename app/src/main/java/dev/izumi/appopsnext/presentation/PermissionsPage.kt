package dev.izumi.appopsnext.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import dev.izumi.appopsnext.presentation.app_detail.AppOpDisplayCatalog
import dev.izumi.appopsnext.presentation.app_list.AppListUiState
import dev.izumi.appopsnext.presentation.permissions.PermissionBrowserViewModel

/** 优先放在前面的常用权限（系统名），只保留当前系统目录里有的。 */
private val COMMON_OPERATIONS = listOf(
    "COARSE_LOCATION", "FINE_LOCATION", "CAMERA", "RECORD_AUDIO",
    "READ_EXTERNAL_STORAGE", "READ_CALENDAR", "READ_PHONE_STATE", "READ_SMS",
    "READ_CONTACTS", "READ_CALL_LOG", "READ_CLIPBOARD", "POST_NOTIFICATION",
    "BODY_SENSORS", "ACTIVITY_RECOGNITION", "SYSTEM_ALERT_WINDOW", "RUN_IN_BACKGROUND",
)

/** 权限视图的第一层：常用权限 + 全部权限。 */
@Composable
fun PermissionListPage(query: String, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val known = remember { AppOpDisplayCatalog.knownOperations() }
    val knownNames = remember(known) { known.map { it.shellName }.toSet() }
    val common = remember(knownNames) { COMMON_OPERATIONS.filter { it in knownNames } }

    fun matches(shellName: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (shellName.contains(q, ignoreCase = true)) return true
        val res = AppOpDisplayCatalog.labelResOf(shellName) ?: return false
        return context.getString(res).contains(q, ignoreCase = true)
    }

    val shownCommon = common.filter(::matches)
    val shownAll = known.map { it.shellName }.filter(::matches)

    LazyColumn(Modifier.fillMaxSize()) {
        items(shownCommon, key = { "c:$it" }) { name ->
            PermissionRow(name, onOpen)
        }
        if (shownCommon.isNotEmpty() && shownAll.isNotEmpty()) {
            item(key = "divider") { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        }
        if (shownAll.isNotEmpty()) {
            item(key = "all-title") {
                Text(
                    stringResource(R.string.shell_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
            }
        }
        items(shownAll, key = { "a:$it" }) { name ->
            PermissionRow(name, onOpen)
        }
    }
}

@Composable
private fun PermissionRow(shellName: String, onOpen: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(shellName) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PermIcon(shellName)
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(opLabel(shellName), style = MaterialTheme.typography.bodyLarge)
            if (AppOpDisplayCatalog.labelResOf(shellName) != null) {
                Text(
                    shellName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 选中某个权限后：列出所有应用对它的当前模式，可直接改。 */
@Composable
fun PermissionAppsPage(
    vm: PermissionBrowserViewModel,
    appList: AppListUiState,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val change by vm.change.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    val operation = state.operation ?: return

    BackHandler {
        if (searching) {
            searching = false
            onSearchQueryChange("")
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            ShellTopBar(
                title = opLabel(operation),
                subtitle = operation,
                onBack = onBack,
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.shell_refresh))
                    }
                    IconButton(onClick = {
                        if (searching) onSearchQueryChange("")
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
                    value = appList.searchQuery,
                    onValueChange = onSearchQueryChange,
                    hint = stringResource(R.string.shell_search_hint),
                )
            }
            val ready = if (!state.loading && !state.failed) state.states else null
            when {
                state.loading -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { CircularProgressIndicator() }

                ready == null -> Column(
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

                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(appList.visibleApps, key = { "${it.userId}:${it.packageName}" }) { app ->
                        val effective = ready.effective(app.uid, app.packageName)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app.packageName, app.label, app.userId)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    app.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                )
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                                if (state.runtimeGrants[app.packageName] == false) {
                                    Text(
                                        stringResource(R.string.permission_runtime_denied),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            ModeDropdown(
                                rawMode = effective.rawMode,
                                onPick = { picked -> vm.request(app, picked) },
                            )
                        }
                    }
                }
            }
        }
    }

    ModeChangeDialogs(
        state = change,
        onConfirm = { vm.confirm() },
        onDismiss = vm::dismiss,
        onTryForeground = vm::requestForeground,
        onDenyNoticeDismiss = vm::dismissDenyNotice,
        onFlagNoticeDismiss = vm::dismissFlagNotice,
    )
}
