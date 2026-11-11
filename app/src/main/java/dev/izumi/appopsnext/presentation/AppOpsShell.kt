package dev.izumi.appopsnext.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.Refresh
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.izumi.appopsnext.presentation.app_detail.AppDetailViewModel
import dev.izumi.appopsnext.presentation.history.HistoryViewModel
import dev.izumi.appopsnext.presentation.permissions.PermissionBrowserViewModel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.izumi.appopsnext.AppOpsNextApplication
import dev.izumi.appopsnext.R
import dev.izumi.appopsnext.apps.model.InstalledApp
import dev.izumi.appopsnext.presentation.app_list.AppListUiState
import dev.izumi.appopsnext.presentation.diagnostics.DiagnosticsUiState
import dev.izumi.appopsnext.shizuku.model.PrivilegedServiceState
import dev.izumi.appopsnext.shizuku.model.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---- 主题 ----

@Composable
fun AppOpsTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFFADC6FF))
        else -> lightColorScheme(primary = Color(0xFF0B57D0))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

// ---- 抽屉数据 ----

private class NavEntry(
    val id: Int,
    val icon: ImageVector,
    @StringRes val label: Int,
    val chip: Boolean = false,
)

private val NAV_MAIN = listOf(
    NavEntry(0, Icons.Filled.Apps, R.string.shell_nav_apps_view),
    NavEntry(1, Icons.AutoMirrored.Filled.ViewList, R.string.shell_nav_permissions_view),
    NavEntry(2, Icons.Filled.History, R.string.shell_nav_usage_history, chip = true),
)
private val NAV_TOOLS = listOf(
    NavEntry(3, Icons.Filled.ContentPaste, R.string.shell_nav_clipboard_monitor),
    NavEntry(4, Icons.AutoMirrored.Filled.ManageSearch, R.string.shell_nav_usage_monitor),
    NavEntry(5, Icons.Filled.Description, R.string.shell_nav_templates),
    NavEntry(6, Icons.Filled.Autorenew, R.string.shell_nav_new_app_behavior),
    NavEntry(7, Icons.Filled.SettingsBackupRestore, R.string.shell_nav_backup_restore),
    NavEntry(8, Icons.Filled.RestorePage, R.string.shell_nav_auto_restore),
    NavEntry(9, Icons.Filled.Settings, R.string.shell_nav_settings),
)
private val NAV_TEXT = listOf(
    R.string.shell_nav_help,
    R.string.shell_nav_changelog,
    R.string.shell_nav_about,
)

private val ALL_ENTRIES = NAV_MAIN + NAV_TOOLS
private const val MODE_SHIZUKU = 0
private const val MODE_ROOT = 1
private const val PAGE_APPS = 0
private const val PAGE_PERMISSIONS = 1
private const val PAGE_HISTORY = 2

// ---- 主界面 ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOpsApp(
    diagnostics: DiagnosticsUiState,
    appList: AppListUiState,
    hideSystemApps: Boolean,
    modeChosen: Boolean,
    appDetailVm: AppDetailViewModel,
    permissionVm: PermissionBrowserViewModel,
    historyVm: HistoryViewModel,
    settingsVm: dev.izumi.appopsnext.presentation.settings.SettingsViewModel,
    templatesVm: dev.izumi.appopsnext.presentation.templates.TemplatesViewModel,
    guardVm: dev.izumi.appopsnext.presentation.experimental.GuardViewModel,
    monitorVm: dev.izumi.appopsnext.presentation.experimental.ExperimentalViewModel,
    onModeChosenChange: (Boolean) -> Unit,
    onHideSystemAppsChange: (Boolean) -> Unit,
    onShizukuAction: () -> Unit,
    onRetryService: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectUser: (Int?) -> Unit,
    rootMode: Boolean,
    onWorkModeSelected: (Boolean) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val permissionState by permissionVm.state.collectAsStateWithLifecycle()
    val historyState by historyVm.uiState.collectAsStateWithLifecycle()

    var pickedMode by rememberSaveable {
        mutableIntStateOf(if (!modeChosen) -1 else if (rootMode) MODE_ROOT else MODE_SHIZUKU)
    }
    var page by rememberSaveable { mutableIntStateOf(PAGE_APPS) }
    var modePanel by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var showFramework by rememberSaveable { mutableStateOf(false) }
    var detailOpen by rememberSaveable { mutableStateOf(false) }
    var historyDetail by rememberSaveable { mutableStateOf<String?>(null) }
    var managingHistory by rememberSaveable { mutableStateOf(false) }
    var textPage by rememberSaveable { mutableIntStateOf(-1) }

    // Without a working Shizuku or root connection the three main views show the mode page.
    val connected = diagnostics.privilegedServiceState is PrivilegedServiceState.Connected
    val showModePage = !modeChosen || (page in 0..2 && !connected)
    val searchable = !showModePage && (page == PAGE_APPS || page == PAGE_PERMISSIONS)
    val overlayOpen = detailOpen ||
        (page == PAGE_PERMISSIONS && permissionState.operation != null) ||
        historyDetail != null

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    fun stopSearch() {
        searching = false
        onSearchQueryChange("")
    }

    BackHandler(enabled = searching && !overlayOpen) { stopSearch() }

    Box(Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { modePanel = !modePanel }
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (modeChosen && diagnostics.privilegedServiceState is PrivilegedServiceState.Connected) {
                                val owner = ownerName(appList)
                                if (owner.isNotEmpty()) {
                                    Text(
                                        owner,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    stringResource(
                                        if (rootMode) R.string.shell_mode_root else R.string.shell_mode_shizuku_label,
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Icon(
                            if (modePanel) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    HorizontalDivider()

                    if (modePanel) {
                        if (appList.users.isNotEmpty()) {
                            Text(
                                stringResource(R.string.shell_drawer_users),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp),
                            )
                            val activeId = appList.selectedUserId ?: appList.currentUserId
                            appList.users.forEach { user ->
                                NavigationDrawerItem(
                                    label = {
                                        Text(
                                            if (user.id == 0) stringResource(R.string.shell_owner) else user.name,
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    },
                                    icon = { Icon(Icons.Outlined.AccountCircle, null) },
                                    selected = user.id == activeId,
                                    onClick = {
                                        onSelectUser(user.id.takeIf { it != appList.currentUserId })
                                        modePanel = false
                                        closeDrawer()
                                    },
                                    modifier = Modifier
                                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                                        .height(48.dp),
                                )
                            }
                            HorizontalDivider(Modifier.padding(top = 8.dp))
                        }
                        Text(
                            stringResource(R.string.shell_nav_work_mode),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp),
                        )
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    stringResource(R.string.shell_nav_reselect_mode),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            icon = { Icon(Icons.Filled.Settings, null) },
                            selected = false,
                            onClick = {
                                pickedMode = -1
                                modePanel = false
                                onModeChosenChange(false)
                                closeDrawer()
                            },
                            modifier = Modifier
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                                .height(48.dp),
                        )
                    } else {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            Spacer(Modifier.height(8.dp))
                            NAV_MAIN.forEach { e ->
                                DrawerRow(e, page == e.id) {
                                    if (page != e.id) stopSearch()
                                    page = e.id
                                    closeDrawer()
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            NAV_TOOLS.forEach { e ->
                                DrawerRow(e, page == e.id) {
                                    if (page != e.id) stopSearch()
                                    page = e.id
                                    closeDrawer()
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            NAV_TEXT.forEachIndexed { index, res ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clickable {
                                            textPage = index
                                            closeDrawer()
                                        }
                                        .padding(horizontal = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        stringResource(res),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            if (searching && searchable) {
                                SearchField(
                                    value = appList.searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    hint = stringResource(R.string.shell_search_hint),
                                )
                            } else {
                                Column {
                                    Text(stringResource(R.string.app_name))
                                    if (modeChosen && connected) {
                                        val owner = ownerName(appList)
                                        if (owner.isNotEmpty()) {
                                            Text(
                                                owner,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = null)
                            }
                        },
                        actions = {
                            if (!showModePage && page == PAGE_HISTORY) {
                                IconButton(onClick = historyVm::refresh) {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = stringResource(R.string.shell_refresh),
                                    )
                                }
                            }
                            if (searchable) {
                                if (searching) {
                                    IconButton(onClick = { stopSearch() }) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.shell_close),
                                        )
                                    }
                                } else {
                                    IconButton(onClick = { searching = true }) {
                                        Icon(
                                            Icons.Filled.Search,
                                            contentDescription = stringResource(R.string.shell_search),
                                        )
                                    }
                                }
                            }
                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(
                                        Icons.Filled.Tune,
                                        contentDescription = stringResource(R.string.shell_filter),
                                    )
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    Text(
                                        stringResource(R.string.shell_menu_options),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.shell_menu_show_system)) },
                                        trailingIcon = { Checkbox(checked = !hideSystemApps, onCheckedChange = null) },
                                        onClick = { onHideSystemAppsChange(!hideSystemApps) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.shell_menu_show_framework)) },
                                        trailingIcon = { Checkbox(checked = showFramework, onCheckedChange = null) },
                                        onClick = { showFramework = !showFramework },
                                    )
                                    if (!showModePage && page == PAGE_HISTORY) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.history_manage_permissions)) },
                                            onClick = {
                                                menuOpen = false
                                                managingHistory = true
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if (showModePage) {
                        ModeScreen(
                            picked = pickedMode,
                            onPick = { pickedMode = it },
                            onContinue = {
                                onWorkModeSelected(pickedMode == MODE_ROOT)
                                onModeChosenChange(true)
                                // Start activating: ask Shizuku for permission, or retry the root connection.
                                if (pickedMode == MODE_ROOT) onRetryService() else onShizukuAction()
                            },
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            ConnectionBanner(diagnostics, rootMode, onShizukuAction, onRetryService)
                            when (page) {
                                PAGE_APPS -> AppListPage(appList, showFramework) { app ->
                                    appDetailVm.selectApp(app)
                                    detailOpen = true
                                }
                                PAGE_PERMISSIONS -> PermissionListPage(
                                    query = appList.searchQuery,
                                    onOpen = { permissionVm.select(it) },
                                )
                                PAGE_HISTORY -> HistoryPage(historyVm) { historyDetail = it }
                                3 -> Unit
                                4 -> Unit
                                in 5..9 -> Unit
                                else -> PlaceholderPage(ALL_ENTRIES.firstOrNull { it.id == page }?.label)
                            }
                        }
                    }
                }
            }
        }

        if (modeChosen || page in 3..9) {
            if (detailOpen) {
                Surface(Modifier.fillMaxSize()) {
                    AppDetailPage(appDetailVm) { detailOpen = false }
                }
            } else if (page in 5..9) {
                Surface(Modifier.fillMaxSize()) {
                    BackHandler { page = PAGE_APPS }
                    Column(Modifier.fillMaxSize()) {
                        ShellTopBar(
                            title = stringResource(ALL_ENTRIES.first { it.id == page }.label),
                            onBack = { page = PAGE_APPS },
                        )
                        androidx.compose.runtime.CompositionLocalProvider(LocalRowStart provides 72.dp) {
                            Box(Modifier.weight(1f)) {
                                when (page) {
                                    5 -> TemplatesPage(templatesVm)
                                    6 -> NewAppBehaviorPage(templatesVm)
                                    7 -> BackupPage(settingsVm)
                                    8 -> AutoRestorePage(guardVm)
                                    else -> SettingsPage(settingsVm)
                                }
                            }
                        }
                    }
                }
            } else if (page == 4) {
                Surface(Modifier.fillMaxSize()) {
                    UsageMonitorPage(monitorVm, appList) { page = PAGE_APPS }
                }
            } else if (page == 3) {
                Surface(Modifier.fillMaxSize()) {
                    ClipboardMonitorPage(appList) { page = PAGE_APPS }
                }
            } else if (page == PAGE_PERMISSIONS && permissionState.operation != null) {
                Surface(Modifier.fillMaxSize()) {
                    PermissionAppsPage(
                        vm = permissionVm,
                        appList = appList,
                        onSearchQueryChange = onSearchQueryChange,
                        onBack = { permissionVm.select(null) },
                    )
                }
            } else {
                val opened = historyDetail
                if (opened != null) {
                    Surface(Modifier.fillMaxSize()) {
                        HistoryDetailPage(
                            vm = historyVm,
                            operationName = opened,
                            onOpenApp = { app ->
                                appDetailVm.selectApp(app)
                                detailOpen = true
                            },
                            onBack = { historyDetail = null },
                        )
                    }
                }
            }
        }

        if (textPage >= 0) {
            val ctx = LocalContext.current
            val version = remember {
                runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }
                    .getOrNull().orEmpty()
            }
            val title = NAV_TEXT[textPage]
            val body = when (textPage) {
                0 -> stringResource(R.string.tp_help_body)
                1 -> stringResource(R.string.tp_log_body)
                else -> stringResource(R.string.tp_about_body, version)
            }
            Surface(Modifier.fillMaxSize()) {
                BackHandler { textPage = -1 }
                Column(Modifier.fillMaxSize()) {
                    ShellTopBar(title = stringResource(title), onBack = { textPage = -1 })
                    Text(
                        body,
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }

    if (managingHistory) {
        HistoryPermissionsDialog(
            selected = historyState.permissions.map { it.permission.shellOperationName },
            onApply = {
                historyVm.setPermissions(it)
                managingHistory = false
            },
            onDismiss = { managingHistory = false },
        )
    }
}

/** 顶栏副标题：当前查看的用户；主用户显示“机主”。 */
@Composable
private fun ownerName(appList: AppListUiState): String {
    val selected = appList.selectedUserId
    if (selected == null && appList.currentUserId == 0) return stringResource(R.string.shell_owner)
    val id = selected ?: appList.currentUserId
    return appList.users.firstOrNull { it.id == id }?.name.orEmpty()
}

@Composable
private fun DrawerRow(entry: NavEntry, selected: Boolean, onClick: () -> Unit) {
    val chip: (@Composable () -> Unit)? = if (entry.chip) {
        @Composable {
            Box(
                Modifier
                    .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    stringResource(R.string.shell_nav_try_first),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    } else null
    NavigationDrawerItem(
        label = {
            Text(
                stringResource(entry.label),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        icon = { Icon(entry.icon, contentDescription = null) },
        badge = chip,
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .height(48.dp),
    )
}

// ---- 工作模式选择页 ----

@Composable
private fun ModeScreen(picked: Int, onPick: (Int) -> Unit, onContinue: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 18.dp, vertical = 12.dp)),
    ) {
        Text(
            stringResource(R.string.shell_choose_mode),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        ModeOption(
            title = R.string.shell_mode_shizuku,
            pros = R.string.shell_mode_shizuku_pros,
            cons = R.string.shell_mode_shizuku_cons,
            selected = picked == MODE_SHIZUKU,
            onClick = { onPick(if (picked == MODE_SHIZUKU) -1 else MODE_SHIZUKU) },
        )
        Spacer(Modifier.height(32.dp))
        ModeOption(
            title = R.string.shell_mode_root,
            pros = R.string.shell_mode_root_pros,
            cons = R.string.shell_mode_root_cons,
            selected = picked == MODE_ROOT,
            onClick = { onPick(if (picked == MODE_ROOT) -1 else MODE_ROOT) },
        )

        Spacer(Modifier.height(24.dp))
        FilledTonalButton(
            onClick = onContinue,
            enabled = picked >= 0,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            Spacer(Modifier.size(20.dp))
            Text(
                stringResource(R.string.shell_continue),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeOption(
    @StringRes title: Int,
    @StringRes pros: Int,
    @StringRes cons: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(end = 56.dp)) {
            Text(
                stringResource(title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(pros),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(cons),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 7.dp).size(48.dp),
        )
    }
}

// ---- Shizuku 连接状态条 ----

@Composable
private fun ConnectionBanner(
    diagnostics: DiagnosticsUiState,
    rootMode: Boolean,
    onAuthorize: () -> Unit,
    onRetry: () -> Unit,
) {
    val shizuku = diagnostics.shizukuState
    val service = diagnostics.privilegedServiceState

    var text: Int
    var button: Int? = null
    var action: () -> Unit = {}
    var problem = true
    if (rootMode) {
        when (service) {
            is PrivilegedServiceState.Connected -> {
                text = R.string.shell_status_root_connected
                problem = false
            }
            is PrivilegedServiceState.Connecting -> {
                text = R.string.shell_status_connecting
                problem = false
            }
            else -> {
                text = R.string.shell_status_root_unavailable
                button = R.string.shell_action_retry
                action = onRetry
            }
        }
    } else when (shizuku) {
        ShizukuState.Checking -> {
            text = R.string.shell_status_checking
            problem = false
        }
        is ShizukuState.Unavailable -> {
            text = if (shizuku.isInstalled) R.string.shell_status_not_running
            else R.string.shell_status_not_installed
            button = R.string.shell_action_retry
            action = onAuthorize
        }
        ShizukuState.Unsupported -> text = R.string.shell_status_unsupported
        ShizukuState.PermissionRequired -> {
            text = R.string.shell_status_permission
            button = R.string.shell_action_authorize
            action = onAuthorize
        }
        ShizukuState.PermissionDenied -> {
            text = R.string.shell_status_permission_denied
            button = R.string.shell_action_authorize
            action = onAuthorize
        }
        is ShizukuState.Failure -> {
            text = R.string.shell_status_read_failed
            button = R.string.shell_action_retry
            action = onAuthorize
        }
        is ShizukuState.Ready -> when (service) {
            is PrivilegedServiceState.Connected -> {
                text = R.string.shell_status_connected
                problem = false
            }
            is PrivilegedServiceState.Connecting -> {
                text = R.string.shell_status_connecting
                problem = false
            }
            is PrivilegedServiceState.Failure -> {
                text = R.string.shell_status_backend_failed
                button = R.string.shell_action_retry
                action = onRetry
            }
            PrivilegedServiceState.Disconnected -> {
                text = R.string.shell_status_disconnected
                button = R.string.shell_action_retry
                action = onRetry
            }
        }
    }

    // Only the "connected" bar goes away; problems and the authorize button stay.
    if (text == R.string.shell_status_connected || text == R.string.shell_status_root_connected) return

    Surface(
        color = if (problem) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            button?.let { label ->
                TextButton(onClick = action) { Text(stringResource(label)) }
            }
        }
    }
}

// ---- 应用视图 ----

/** 系统框架应用：UID 低于普通应用起始 UID (10000) 的系统应用。 */
private fun InstalledApp.isFrameworkApp(): Boolean = isSystemApp && uid % 100000 < 10000

@Composable
private fun AppListPage(
    state: AppListUiState,
    showFramework: Boolean,
    onOpen: (InstalledApp) -> Unit,
) {
    val apps = if (showFramework) state.visibleApps
    else state.visibleApps.filterNot { it.isFrameworkApp() }

    when {
        state.isLoading && apps.isEmpty() -> CenterMessage(R.string.shell_apps_loading, progress = true)
        state.loadFailed && apps.isEmpty() -> CenterMessage(R.string.shell_apps_failed)
        apps.isEmpty() -> CenterMessage(R.string.shell_apps_empty)
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(apps, key = { "${it.userId}:${it.packageName}" }) { app ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(app) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(app.packageName, app.label, app.userId)
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                        )
                        Text(
                            app.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    if (app.isSystemApp) {
                        Text(
                            stringResource(R.string.shell_system_app),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CenterMessage(@StringRes text: Int, progress: Boolean = false) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (progress) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
        }
        Text(stringResource(text), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlaceholderPage(@StringRes title: Int?) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (title != null) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
        }
        Text(
            stringResource(R.string.shell_placeholder),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---- 应用图标 ----

@Composable
internal fun AppIcon(packageName: String, label: String, userId: Int?, size: Dp = 44.dp) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val cacheKey = if (userId == null) packageName else "$userId:$packageName"
    val bitmap = produceState<ImageBitmap?>(
        initialValue = IconCache[cacheKey],
        key1 = cacheKey,
        key2 = sizePx,
    ) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            IconCache[cacheKey]
                ?: runCatching {
                    val userIcon = userId?.let {
                        (context.applicationContext as AppOpsNextApplication)
                            .userAppsRepository.loadIcon(packageName, it)
                    }
                    (userIcon ?: context.packageManager.getApplicationIcon(packageName))
                        .render(sizePx)
                        .also { IconCache.put(cacheKey, it) }
                }.getOrNull()
        }
    }
    val loaded = bitmap.value
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = label,
            modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_default_app),
                contentDescription = label,
                modifier = Modifier.size(size * 0.58f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Drawable.render(sizePx: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val previousBounds = copyBounds()
    setBounds(0, 0, sizePx, sizePx)
    draw(Canvas(bitmap))
    bounds = previousBounds
    return bitmap.asImageBitmap()
}

private object IconCache {
    private val cache = LruCache<String, ImageBitmap>(128)

    operator fun get(key: String): ImageBitmap? = synchronized(cache) { cache[key] }

    fun put(key: String, bitmap: ImageBitmap) {
        synchronized(cache) { cache.put(key, bitmap) }
    }
}
