package com.appops.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Text
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppOpsTheme { AppOpsApp() }
        }
    }
}

@Composable
private fun AppOpsTheme(content: @Composable () -> Unit) {
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
    NavEntry(0, Icons.Filled.Apps, R.string.nav_apps_view),
    NavEntry(1, Icons.AutoMirrored.Filled.ViewList, R.string.nav_permissions_view),
    NavEntry(2, Icons.Filled.History, R.string.nav_usage_history, chip = true),
)
private val NAV_TOOLS = listOf(
    NavEntry(3, Icons.Filled.ContentPaste, R.string.nav_clipboard_monitor),
    NavEntry(4, Icons.AutoMirrored.Filled.ManageSearch, R.string.nav_usage_monitor),
    NavEntry(5, Icons.Filled.Description, R.string.nav_templates),
    NavEntry(6, Icons.Filled.Autorenew, R.string.nav_new_app_behavior),
    NavEntry(7, Icons.Filled.SettingsBackupRestore, R.string.nav_backup_restore),
    NavEntry(8, Icons.Filled.RestorePage, R.string.nav_auto_restore),
    NavEntry(9, Icons.Filled.Settings, R.string.nav_settings),
)
private val NAV_TEXT = listOf(
    R.string.nav_my_purchases,
    R.string.nav_help,
    R.string.nav_changelog,
    R.string.nav_about,
)

private val ALL_ENTRIES = NAV_MAIN + NAV_TOOLS

// ---- 主界面 ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppOpsApp() {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var modeChosen by rememberSaveable { mutableStateOf(false) }
    var shizukuPicked by rememberSaveable { mutableStateOf(false) }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var modePanel by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var showSystem by rememberSaveable { mutableStateOf(true) }
    var showFramework by rememberSaveable { mutableStateOf(false) }

    fun closeDrawer() {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // 顶部: App Ops ▾ / ▴
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
                        stringResource(R.string.nav_work_mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp),
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.nav_reselect_mode)) },
                        icon = { Icon(Icons.Filled.Settings, null) },
                        selected = false,
                        onClick = {
                            modeChosen = false
                            shizukuPicked = false
                            modePanel = false
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
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* TODO 搜索 */ }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.filter))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                Text(
                                    stringResource(R.string.menu_options),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_show_system)) },
                                    trailingIcon = { Checkbox(checked = showSystem, onCheckedChange = null) },
                                    onClick = { showSystem = !showSystem },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_show_framework)) },
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
                        onContinue = { modeChosen = true },
                    )
                } else {
                    val entry = ALL_ENTRIES.firstOrNull { it.id == page }
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (entry != null) {
                            Text(stringResource(entry.label), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            stringResource(R.string.placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                    stringResource(R.string.nav_try_first),
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
            stringResource(R.string.choose_mode),
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
                    stringResource(R.string.mode_shizuku),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.mode_shizuku_pros),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.mode_shizuku_cons),
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
                stringResource(R.string.action_continue),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
