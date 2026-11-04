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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Tune
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
private const val PAGE_APPS = 0

// ---- 主界面 ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOpsApp(
    diagnostics: DiagnosticsUiState,
    appList: AppListUiState,
    hideSystemApps: Boolean,
    modeChosen: Boolean,
    onModeChosenChange: (Boolean) -> Unit,
    onHideSystemAppsChange: (Boolean) -> Unit,
    onShizukuAction: () -> Unit,
    onRetryService: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var shizukuPicked by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(PAGE_APPS) }
    var modePanel by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var showFramework by rememberSaveable { mutableStateOf(false) }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    BackHandler(enabled = searching) {
        searching = false
        onSearchQueryChange("")
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { modePanel = !modePanel }
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (modePanel) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()

                if (modePanel) {
                    Text(
                        stringResource(R.string.shell_nav_work_mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp),
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.shell_nav_reselect_mode)) },
                        icon = { Icon(Icons.Filled.Settings, null) },
                        selected = false,
                        onClick = {
                            shizukuPicked = false
                            modePanel = false
                            onModeChosenChange(false)
                            closeDrawer()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Spacer(Modifier.height(8.dp))
                        NAV_MAIN.forEach { e -> DrawerRow(e, page == e.id) { page = e.id; closeDrawer() } }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        NAV_TOOLS.forEach { e -> DrawerRow(e, page == e.id) { page = e.id; closeDrawer() } }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        NAV_TEXT.forEach { res ->
                            Text(
                                stringResource(res),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { closeDrawer() }
                                    .padding(horizontal = 28.dp, vertical = 16.dp),
                            )
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
                        if (searching && modeChosen && page == PAGE_APPS) {
                            TextField(
                                value = appList.searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.shell_search_hint)) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            Text(stringResource(R.string.app_name))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    },
                    actions = {
                        if (searching && modeChosen && page == PAGE_APPS) {
                            IconButton(onClick = {
                                searching = false
                                onSearchQueryChange("")
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.shell_close))
                            }
                        } else {
                            IconButton(onClick = {
                                if (modeChosen && page == PAGE_APPS) searching = true
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.shell_search))
                            }
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.shell_filter))
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
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (!modeChosen) {
                    ModeScreen(
                        picked = shizukuPicked,
                        onPick = { shizukuPicked = it },
                        onContinue = { onModeChosenChange(true) },
                    )
                } else {
                    Column(Modifier.fillMaxSize()) {
                        ConnectionBanner(diagnostics, onShizukuAction, onRetryService)
                        if (page == PAGE_APPS) {
                            AppListPage(appList, showFramework)
                        } else {
                            PlaceholderPage(ALL_ENTRIES.firstOrNull { it.id == page }?.label)
                        }
                    }
                }
            }
        }
    }
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
        label = { Text(stringResource(entry.label)) },
        icon = { Icon(entry.icon, contentDescription = null) },
        badge = chip,
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

// ---- 工作模式选择页 ----

@Composable
private fun ModeScreen(picked: Boolean, onPick: (Boolean) -> Unit, onContinue: () -> Unit) {
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

        Box(
            Modifier
                .fillMaxWidth()
                .clickable { onPick(!picked) },
        ) {
            Column(Modifier.padding(end = 56.dp)) {
                Text(
                    stringResource(R.string.shell_mode_shizuku),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.shell_mode_shizuku_pros),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.shell_mode_shizuku_cons),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = picked,
                onClick = { onPick(!picked) },
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = 7.dp).size(48.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        FilledTonalButton(
            onClick = onContinue,
            enabled = picked,
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

// ---- Shizuku 连接状态条 ----

@Composable
private fun ConnectionBanner(
    diagnostics: DiagnosticsUiState,
    onAuthorize: () -> Unit,
    onRetry: () -> Unit,
) {
    val shizuku = diagnostics.shizukuState
    val service = diagnostics.privilegedServiceState

    var text: Int
    var button: Int? = null
    var action: () -> Unit = {}
    var problem = true
    when (shizuku) {
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
private fun AppListPage(state: AppListUiState, showFramework: Boolean) {
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
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(@StringRes text: Int, progress: Boolean = false) {
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
private fun AppIcon(packageName: String, label: String, userId: Int?, size: Dp = 44.dp) {
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
