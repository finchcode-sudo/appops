package dev.izumi.appopsnext.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import dev.izumi.appopsnext.history.model.AppOpHistoryFailureReason
import dev.izumi.appopsnext.presentation.app_detail.AppOpDisplayCatalog
import dev.izumi.appopsnext.presentation.history.HistoryUiState
import dev.izumi.appopsnext.presentation.history.HistoryViewModel
import dev.izumi.appopsnext.presentation.history.PermissionHistory
import dev.izumi.appopsnext.presentation.history.ResolvedHistoryEvent
import java.text.DateFormat
import java.util.Date

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** 近 24 小时里真正发生的访问：有单条记录时不再叠加按区间汇总的计数。 */
private fun recentEvents(history: PermissionHistory, cutoff: Long): List<ResolvedHistoryEvent> =
    history.events.filter {
        it.event.accessCount > 0 &&
            it.event.accessTimeMillis >= cutoff &&
            (!history.individualRecordsAvailable || !it.event.isAggregated)
    }

private fun failureRes(reason: AppOpHistoryFailureReason): Int = when (reason) {
    AppOpHistoryFailureReason.BACKEND_UNAVAILABLE -> R.string.history_failure_backend
    AppOpHistoryFailureReason.COMMAND_FAILED -> R.string.history_failure_command
    AppOpHistoryFailureReason.COMMAND_TIMED_OUT -> R.string.history_failure_timeout
}

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** 使用情况历史首页：24 小时汇总 + 每个权限一张卡片。 */
@Composable
fun HistoryPage(vm: HistoryViewModel, onOpen: (String) -> Unit) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        vm.setVisible(true)
        onDispose { vm.setVisible(false) }
    }

    val cutoff = remember(ui.lastUpdatedAtMillis, ui.permissions) { System.currentTimeMillis() - DAY_MILLIS }
    val recent = remember(ui.permissions, cutoff) {
        ui.permissions.associate { it.permission.shellOperationName to recentEvents(it, cutoff) }
    }
    val totalAccesses = recent.values.sumOf { list -> list.sumOf { it.event.accessCount } }
    val totalApps = recent.values.flatten().map { it.app.packageName }.distinct().size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "summary") {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.shell_history_window),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    stringResource(R.string.shell_history_summary, totalApps, totalAccesses),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }
        item(key = "status") { HistoryStatus(ui) }
        items(ui.permissions, key = { it.permission.shellOperationName }) { history ->
            val name = history.permission.shellOperationName
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(28.dp))
                    .clickable { onOpen(name) }
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PermIcon(name, size = 28.dp)
                Spacer(Modifier.width(24.dp))
                Text(
                    opLabel(name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                val count = recent[name].orEmpty().sumOf { it.event.accessCount }
                Text(
                    stringResource(R.string.shell_history_count, count),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HistoryStatus(ui: HistoryUiState) {
    val message: Int? = when {
        ui.waitingForBackend -> R.string.history_waiting_for_backend
        ui.failureReason != null -> failureRes(ui.failureReason)
        else -> null
    }
    if (ui.isLoading && ui.permissions.isEmpty()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
        }
    } else if (message != null) {
        Text(
            stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private val LOCATION_OPS = listOf("FINE_LOCATION", "COARSE_LOCATION")

@Composable
private fun relativeTime(millis: Long): String {
    val diff = (System.currentTimeMillis() - millis).coerceAtLeast(0)
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> stringResource(R.string.tp_hist_just_now)
        hours < 1 -> stringResource(R.string.tp_hist_minutes_ago, minutes.toInt())
        days < 1 -> stringResource(R.string.tp_hist_hours_ago, hours.toInt())
        days == 1L -> stringResource(R.string.tp_hist_yesterday)
        days == 2L -> stringResource(R.string.tp_hist_day_before)
        days < 8 -> stringResource(R.string.tp_hist_days_ago, days.toInt())
        else -> java.text.SimpleDateFormat(
            stringResource(R.string.tp_hist_date_pattern),
            java.util.Locale.getDefault(),
        ).format(Date(millis))
    }
}

/** 某个权限的全部历史记录：按应用分组，每个应用列出该权限下各操作的最近一次访问。 */
@Composable
fun HistoryDetailPage(
    vm: HistoryViewModel,
    operationName: String,
    onOpenApp: (dev.izumi.appopsnext.apps.model.InstalledApp) -> Unit,
    onBack: () -> Unit,
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val group = if (operationName in LOCATION_OPS) LOCATION_OPS else listOf(operationName)
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val rows = remember(ui.permissions, operationName) {
        ui.permissions
            .filter { it.permission.shellOperationName in group }
            .flatMap { it.events }
            .groupBy { it.app.packageName to it.app.userId }
            .values
            .map { list ->
                val app = list.first().app
                val perOp = list.groupBy { it.event.operationName }
                    .map { (op, evs) ->
                        val latest = evs.maxBy { it.event.accessTimeMillis }.event
                        val allowed = evs.filter { it.event.accessCount > 0 }.maxByOrNull { it.event.accessTimeMillis }
                        Triple(op, allowed?.event?.accessTimeMillis ?: latest.accessTimeMillis, evs.sumOf { it.event.rejectCount })
                            .let { t -> Pair(t, allowed != null) }
                    }
                    .sortedBy { group.indexOf(it.first.first) }
                Triple(app, perOp, perOp.maxOf { it.first.second })
            }
            .sortedByDescending { it.third }
    }
    val shown = rows.filter {
        query.isBlank() || it.first.label.contains(query, ignoreCase = true) ||
            it.first.packageName.contains(query, ignoreCase = true)
    }

    BackHandler {
        if (searching) {
            searching = false
            query = ""
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            ShellTopBar(
                title = if (operationName in LOCATION_OPS) stringResource(R.string.tp_hist_location) else opLabel(operationName),
                onBack = {
                    if (searching) {
                        searching = false
                        query = ""
                    } else {
                        onBack()
                    }
                },
                actions = {
                    IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.shell_refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (searching) {
                SearchField(value = query, onValueChange = { query = it }, hint = stringResource(R.string.shell_search_hint))
            }
            if (shown.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        stringResource(R.string.history_empty),
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { "${it.first.packageName}:${it.first.userId}" }) { (app, perOp, _) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIcon(app.packageName, app.label, app.userId)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                perOp.forEach { (info, wasAllowed) ->
                                    val (op, time, rejects) = info
                                    val text = if (wasAllowed) {
                                        stringResource(R.string.tp_hist_line_allowed, opLabel(op), relativeTime(time))
                                    } else {
                                        stringResource(R.string.tp_hist_line_denied, opLabel(op), rejects)
                                    }
                                    Text(
                                        text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            IconButton(onClick = { onOpenApp(app) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 选择要在历史页显示哪些权限。 */
@Composable
fun HistoryPermissionsDialog(
    selected: List<String>,
    onApply: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val known = remember { AppOpDisplayCatalog.knownOperations() }
    var chosen by remember(selected) { mutableStateOf(selected.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_manage_permissions)) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(known, key = { it.shellName }) { op ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                chosen = if (op.shellName in chosen) chosen - op.shellName else chosen + op.shellName
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = op.shellName in chosen, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        Text(opLabel(op.shellName), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(known.map { it.shellName }.filter { it in chosen })
            }) { Text(stringResource(R.string.action_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
