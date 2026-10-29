package dev.pipos.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pipos.launcher.data.FavoriteLayout
import dev.pipos.launcher.data.LaunchableApp
import dev.pipos.launcher.data.LauncherPreferences
import dev.pipos.launcher.data.ThemeProfile
import dev.pipos.launcher.media.MediaControllerPanel
import dev.pipos.launcher.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val Sections = listOf("APPS", "STAT", "DATA", "MAP", "RADIO")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.rgb(5, 10, 6)
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.rgb(5, 10, 6)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = PipGreen, surface = PipBlack, onSurface = PipGreen)) {
                LauncherScreen()
            }
        }
    }
}

@Composable
private fun LauncherScreen(vm: LauncherViewModel = viewModel()) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val prefs by vm.preferences.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var tab by remember { mutableStateOf("APPS") }
    var category by remember { mutableStateOf(AppCategory.ALL) }
    var search by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("SYSTEM READY // ENTER >help FOR COMMANDS") }
    var settings by remember { mutableStateOf(false) }
    var editLayout by remember { mutableStateOf(false) }
    var booted by remember { mutableStateOf(false) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val accent = when (prefs.theme) {
        ThemeProfile.GREEN -> PipGreen
        ThemeProfile.AMBER -> Color(0xFFFFBF52)
        ThemeProfile.ICE -> Color(0xFFBCEFEA)
    }
    val visible = remember(apps, search, category, prefs.hidden) {
        filterApps(apps.filter { it.key !in prefs.hidden && (category == AppCategory.ALL || categoryFor(it) == category) }, search)
    }
    val favoriteApps = remember(apps, favorites, prefs.hidden) {
        val indexed = apps.associateBy { it.key }
        favorites.mapNotNull { indexed[it] }.filter { it.key !in prefs.hidden }
    }

    val submit: () -> Unit = {
        when (val command = parseCommand(search)) {
            is TerminalCommand.Open -> {
                val matches = apps.filter { it.label.contains(command.app, true) || it.packageName.contains(command.app, true) }
                when (matches.size) {
                    0 -> message = "ERROR: APPLICATION NOT FOUND"
                    1 -> { vm.launch(matches.first()); message = "LAUNCHING ${matches.first().label}" }
                    else -> { tab = "APPS"; message = "${matches.size} MATCHES // REFINE QUERY" }
                }
            }
            is TerminalCommand.Theme -> { vm.setTheme(command.profile); message = "PHOSPHOR PROFILE: ${command.profile.name}" }
            TerminalCommand.Battery -> { tab = "STAT"; message = "DISPLAYING BATTERY STATUS" }
            TerminalCommand.Storage -> { tab = "STAT"; message = "DISPLAYING STORAGE STATUS" }
            TerminalCommand.Apps -> { tab = "APPS"; category = AppCategory.ALL; message = "${apps.size} APPLICATIONS INDEXED" }
            TerminalCommand.Settings -> settings = true
            TerminalCommand.Help -> message = "COMMANDS: >open NAME, >apps, >battery, >storage, >settings, >theme NAME"
            TerminalCommand.Invalid -> message = "UNKNOWN COMMAND // ENTER >help"
            null -> tab = "APPS"
        }
        if (search.startsWith(">")) vm.addCommand(search)
        search = ""
        historyIndex = -1
        keyboard?.hide()
    }
    BackHandler(settings || editLayout || tab != "APPS" || search.isNotBlank()) {
        when {
            settings -> settings = false
            editLayout -> editLayout = false
            search.isNotBlank() -> search = ""
            else -> tab = "APPS"
        }
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refreshApps() }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(error) { if (error != null) { delay(4000); vm.clearError() } }
    LaunchedEffect(Unit) { delay(600); booted = true }

    Box(Modifier.fillMaxSize().background(PipBlack).then(if (prefs.scanlines) Modifier.crtScanlines(accent) else Modifier)) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            PipHeader(accent, onSettings = { settings = !settings })
            Spacer(Modifier.height(12.dp))
            SectionTabs(tab, accent, onSelected = { tab = it; settings = false; editLayout = false })
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f)) {
                val section = if (settings) "SETTINGS" else tab
                if (prefs.reduceMotion) {
                    SectionContent(section, accent, apps, visible, favoriteApps, favorites, prefs, category,
                        onCategory = { category = it }, editLayout = editLayout,
                        onToggleEditing = { editLayout = !editLayout }, vm = vm, context = context,
                        onCloseSettings = { settings = false })
                } else {
                    AnimatedContent(
                        targetState = section,
                        transitionSpec = {
                            val direction = (Sections + "SETTINGS").indexOf(targetState) - (Sections + "SETTINGS").indexOf(initialState)
                            (slideInHorizontally(tween(210)) { if (direction >= 0) it / 6 else -it / 6 } + fadeIn(tween(210)))
                                .togetherWith(slideOutHorizontally(tween(150)) { if (direction >= 0) -it / 6 else it / 6 } + fadeOut(tween(150)))
                        },
                        label = "PIP-OS section"
                    ) { current ->
                        SectionContent(current, accent, apps, visible, favoriteApps, favorites, prefs, category,
                            onCategory = { category = it }, editLayout = editLayout,
                            onToggleEditing = { editLayout = !editLayout }, vm = vm, context = context,
                            onCloseSettings = { settings = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (error != null) PipText(error.orEmpty(), color = Color(0xFFFFAD82))
            if (prefs.reduceMotion) {
                PipText(message, color = accent.copy(alpha = .7f), maxLines = 2)
            } else {
                AnimatedContent(targetState = message, transitionSpec = {
                    fadeIn(tween(120)).togetherWith(fadeOut(tween(120)))
                }, label = "terminal response") { current ->
                    PipText(current, color = accent.copy(alpha = .7f), maxLines = 2)
                }
            }
            Spacer(Modifier.height(6.dp))
            TerminalInput(
                value = search,
                onValueChange = { search = it; if (!it.startsWith(">")) tab = "APPS" },
                onSubmit = submit,
                onHistory = {
                    if (prefs.commandHistory.isNotEmpty()) {
                        historyIndex = (historyIndex + 1).coerceAtMost(prefs.commandHistory.lastIndex)
                        search = prefs.commandHistory[historyIndex]
                    }
                },
                accent = accent,
                suggest = !prefs.reduceMotion,
                hasHistory = prefs.commandHistory.isNotEmpty()
            )
        }
        if (!booted && !prefs.reduceMotion) {
            Box(Modifier.fillMaxSize().background(PipBlack), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PipText("PIP-OS", color = accent, prominent = true)
                    Spacer(Modifier.height(8.dp))
                    PipText("INITIALIZING PERSONAL TERMINAL_", color = accent)
                }
            }
        }
    }
}

@Composable
private fun PipHeader(accent: Color, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("PIP-OS", color = accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, fontSize = 27.sp)
            PipText("PERSONAL INFORMATION PROCESSOR  //  V.02", color = accent.copy(alpha = .7f))
        }
        PipButton("CFG", accent, onClick = onSettings)
    }
    HorizontalDivider(color = accent.copy(alpha = .75f), thickness = 2.dp)
}

@Composable
private fun SectionTabs(current: String, accent: Color, onSelected: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Sections.forEach { section ->
            val active = current == section
            PipButton(section, accent,
                modifier = Modifier.weight(1f).semantics { selected = active },
                active = active, onClick = { onSelected(section) })
        }
    }
}

@Composable
private fun SectionContent(
    section: String,
    accent: Color,
    apps: List<LaunchableApp>,
    visible: List<LaunchableApp>,
    favoriteApps: List<LaunchableApp>,
    favorites: List<String>,
    prefs: LauncherPreferences,
    category: AppCategory,
    onCategory: (AppCategory) -> Unit,
    editLayout: Boolean,
    onToggleEditing: () -> Unit,
    vm: LauncherViewModel,
    context: Context,
    onCloseSettings: () -> Unit
) {
    when (section) {
        "APPS" -> AppsPanel(accent, visible, favoriteApps, favorites, category, prefs.favoriteLayout, editLayout,
            onCategory, onToggleEditing, vm::launch, vm::toggleFavorite, vm::moveFavorite, vm::hideApp)
        "STAT" -> StatPanel(accent)
        "DATA" -> LinkPanel("DATA ARCHIVE", accent, listOf(
            "CALENDAR" to { openPackage(context, "com.google.android.calendar") },
            "DEVICE SETTINGS" to { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
        ), "Notifications are available through Android's notification shade.")
        "MAP" -> LinkPanel("NAVIGATION SYSTEM", accent, listOf("OPEN MAPS" to { openMap(context) }),
            "PIP-OS does not collect your location.")
        "RADIO" -> RadioPanel(accent, apps, vm::launch)
        "SETTINGS" -> SettingsPanel(prefs, accent, vm::setTheme, vm::setEffects,
            vm::setReduceMotion, vm::setFavoriteLayout, vm::clearHidden, onCloseSettings)
    }
}

@Composable
private fun AppsPanel(
    accent: Color,
    visible: List<LaunchableApp>,
    favoriteApps: List<LaunchableApp>,
    favorites: List<String>,
    category: AppCategory,
    layout: FavoriteLayout,
    editing: Boolean,
    onCategory: (AppCategory) -> Unit,
    onEdit: () -> Unit,
    launch: (LaunchableApp) -> Unit,
    toggle: (LaunchableApp) -> Unit,
    move: (String, String) -> Unit,
    hide: (LaunchableApp) -> Unit
) {
    val listState = rememberLazyListState()
    val dragScope = rememberCoroutineScope()
    val edgeThreshold = with(LocalDensity.current) { 60.dp.toPx() }
    val autoScrollStep = with(LocalDensity.current) { 12.dp.toPx() }
    var listTop by remember { mutableFloatStateOf(0f) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dropTargetKey by remember { mutableStateOf<String?>(null) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Only commits once when the finger is released. No DataStore writes during dragging.
    fun resetDrag() {
        draggedKey = null
        dropTargetKey = null
        dragOffsetY = 0f
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            PipText("APPLICATION DIRECTORY", Modifier.weight(1f), accent, prominent = true)
            PipButton(if (editing) "DONE" else "EDIT", accent, onClick = onEdit)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppCategory.entries.forEach { item ->
                PipButton(item.name, accent, Modifier.weight(1f), active = category == item) { onCategory(item) }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize().onGloballyPositioned { listTop = it.positionInRoot().y },
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (favoriteApps.isNotEmpty() && category == AppCategory.ALL) {
                item {
                    PipText(
                        if (editing) "★ PINNED // HOLD ≡ AND DRAG TO REORDER" else "★ PINNED // ${favoriteApps.size} SHORTCUTS",
                        Modifier.padding(vertical = 6.dp), accent, prominent = true
                    )
                }
                if (editing || layout == FavoriteLayout.LIST) {
                    items(favoriteApps, key = { "pin:${it.key}" }) { app ->
                        val index = favoriteApps.indexOfFirst { it.key == app.key }
                        var handleTop by remember(app.key) { mutableFloatStateOf(0f) }
                        FavoriteItem(
                            app = app,
                            index = index,
                            count = favoriteApps.size,
                            editing = editing,
                            accent = accent,
                            launch = launch,
                            toggle = toggle,
                            moveUp = { favoriteApps.getOrNull(index - 1)?.let { move(app.key, it.key) } },
                            moveDown = { favoriteApps.getOrNull(index + 1)?.let { move(app.key, it.key) } },
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = if (draggedKey == app.key) dragOffsetY else 0f
                                    alpha = if (draggedKey == app.key) 0.83f else 1f
                                }
                                .background(if (dropTargetKey == app.key) accent.copy(alpha = 0.22f) else Color.Transparent),
                            dragHandleModifier = Modifier
                                .onGloballyPositioned { handleTop = it.positionInRoot().y }
                                .pointerInput(app.key, editing) {
                                    if (!editing) return@pointerInput
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { start ->
                                            draggedKey = app.key
                                            dropTargetKey = null
                                            dragOffsetY = 0f
                                            pointerY = handleTop + start.y
                                        },
                                        onDrag = { change, distance ->
                                            change.consume()
                                            dragOffsetY += distance.y
                                            pointerY += distance.y
                                            val localY = pointerY - listTop
                                            val rows = listState.layoutInfo.visibleItemsInfo.filter {
                                                (it.key as? String)?.startsWith("pin:") == true
                                            }
                                            val hovered = rows.minByOrNull {
                                                abs(localY - (it.offset + it.size / 2f))
                                            }
                                            dropTargetKey = if (
                                                abs(dragOffsetY) >= size.height / 2f &&
                                                hovered != null &&
                                                hovered.key != "pin:${app.key}"
                                            ) (hovered.key as String).removePrefix("pin:") else null

                                            val viewport = listState.layoutInfo.viewportSize.height
                                            val scroll = when {
                                                localY < edgeThreshold -> -autoScrollStep
                                                localY > viewport - edgeThreshold -> autoScrollStep
                                                else -> 0f
                                            }
                                            if (scroll != 0f) {
                                                dragScope.launch { listState.scrollBy(scroll) }
                                            }
                                        },
                                        onDragEnd = {
                                            dropTargetKey?.let { target -> move(app.key, target) }
                                            resetDrag()
                                        },
                                        onDragCancel = { resetDrag() }
                                    )
                                }
                        )
                    }
                } else {
                    items(favoriteApps.chunked(2), key = { group -> "row:${group.first().key}" }) { group ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            group.forEach { app ->
                                FavoriteTile(app, accent, Modifier.weight(1f), launch)
                            }
                            if (group.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = accent.copy(alpha = .25f))
                    PipText("ALL APPLICATIONS", Modifier.padding(vertical = 8.dp), accent, prominent = true)
                }
            }
            items(visible, key = { "app:${it.key}" }) { app ->
                AppItem(app, app.key in favorites, accent, launch, toggle, hide)
            }
            if (visible.isEmpty()) item { PipText("NO APPLICATIONS FOUND", Modifier.padding(18.dp), accent) }
        }
    }
}

@Composable
private fun FavoriteTile(app: LaunchableApp, accent: Color, modifier: Modifier, launch: (LaunchableApp) -> Unit) {
    Column(modifier.pipFrame(accent).clickable { launch(app) }.heightIn(min = 90.dp).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonochromeAppIcon(app.packageName, app.activityName, accent, Modifier.size(32.dp))
        PipText(app.label, color = accent, maxLines = 1)
    }
}

@Composable
private fun FavoriteItem(
    app: LaunchableApp, index: Int, count: Int, editing: Boolean, accent: Color,
    launch: (LaunchableApp) -> Unit, toggle: (LaunchableApp) -> Unit,
    moveUp: () -> Unit, moveDown: () -> Unit,
    modifier: Modifier = Modifier, dragHandleModifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth().pipFrame(accent).heightIn(min = 64.dp).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
        MonochromeAppIcon(app.packageName, app.activityName, accent, Modifier.padding(horizontal = 8.dp).size(28.dp))
        PipText(app.label, Modifier.weight(1f).clickable { if (!editing) launch(app) }.padding(8.dp), accent)
        if (editing) {
            PipButton("↑", accent, enabled = index > 0) { moveUp() }
            Spacer(Modifier.width(4.dp))
            PipButton("↓", accent, enabled = index < count - 1) { moveDown() }
            Spacer(Modifier.width(4.dp))
            PipButton("×", accent) { toggle(app) }
            Box(
                Modifier.padding(start = 4.dp).size(48.dp).then(dragHandleModifier)
                    .semantics { contentDescription = "Hold and drag to reorder ${app.label}" },
                contentAlignment = Alignment.Center
            ) {
                PipText("≡", color = accent, prominent = true)
            }
        } else {
            PipButton("OPEN", accent) { launch(app) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppItem(
    app: LaunchableApp, pinned: Boolean, accent: Color,
    launch: (LaunchableApp) -> Unit, toggle: (LaunchableApp) -> Unit, hide: (LaunchableApp) -> Unit
) {
    var expanded by remember(app.key) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().pipFrame(accent)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            MonochromeAppIcon(app.packageName, app.activityName, accent, Modifier.padding(start = 10.dp).size(30.dp))
            Box(Modifier.weight(1f).combinedClickable(onClick = { launch(app) }, onLongClick = { expanded = !expanded }).padding(12.dp)) {
                PipText(app.label, color = accent, prominent = true, maxLines = 1)
            }
            PipButton(if (pinned) "★" else "+", accent, modifier = Modifier.semantics {
                contentDescription = if (pinned) "Unpin ${app.label}" else "Pin ${app.label}"
            }) { toggle(app) }
        }
        if (expanded) Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PipButton("HIDE APP", accent) { hide(app); expanded = false }
            PipButton("CLOSE", accent) { expanded = false }
        }
    }
}

@Composable
private fun TerminalInput(
    value: String, onValueChange: (String) -> Unit, onSubmit: () -> Unit,
    onHistory: () -> Unit, accent: Color, suggest: Boolean, hasHistory: Boolean
) {
    Column(Modifier.fillMaxWidth()) {
        if (suggest) {
            AnimatedVisibility(
                visible = value.startsWith(">"),
                enter = fadeIn(tween(140)) + expandVertically(tween(140)),
                exit = fadeOut(tween(100)) + shrinkVertically(tween(100))
            ) {
                PipText("CMD // open NAME · apps · battery · storage · settings · theme · help",
                    Modifier.fillMaxWidth().padding(vertical = 4.dp), accent.copy(alpha = .8f), maxLines = 2)
            }
        }
        Row(Modifier.fillMaxWidth().pipFrame(accent).heightIn(min = 54.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            PipText(">", color = accent, prominent = true)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value, onValueChange = onValueChange, singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = accent),
                cursorBrush = SolidColor(accent), modifier = Modifier.weight(1f).semantics { contentDescription = "Search apps or enter a terminal command" },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                decorationBox = { inner ->
                    Box { if (value.isEmpty()) PipText("SEARCH APPS / >COMMAND", color = accent.copy(alpha = .55f)); inner() }
                }
            )
            PipButton("RUN", accent) { onSubmit() }
            if (hasHistory) {
                Spacer(Modifier.width(4.dp))
                PipButton("↑", accent, modifier = Modifier.semantics { contentDescription = "Previous command" }) { onHistory() }
            }
        }
    }
}

@Composable
private fun StatPanel(accent: Color) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); tick++ } }
    val battery = remember(tick) {
        context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
    }
    val storage = remember(tick) {
        StatFs(Environment.getDataDirectory().path).let { it.totalBytes to it.availableBytes }
    }
    val used = (storage.first - storage.second).coerceAtLeast(0)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PipText("SYSTEM STATUS // LIVE TELEMETRY", color = accent, prominent = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PipGauge("BATTERY", if (battery >= 0) battery / 100f else 0f,
                if (battery >= 0) "$battery%" else "UNKNOWN", accent, Modifier.weight(1f))
            PipGauge("STORAGE", if (storage.first > 0) used.toFloat() / storage.first else 0f,
                "${used / 1_000_000_000} / ${storage.first / 1_000_000_000} GB", accent, Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().pipFrame(accent).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PipText("DEVICE: ${Build.MODEL}", color = accent)
            PipText("ANDROID: ${Build.VERSION.RELEASE}", color = accent)
            PipText("STORAGE FREE: ${storage.second / 1_000_000_000} GB", color = accent)
            PipText("STATUS: ALL SYSTEMS NOMINAL", color = accent)
        }
    }
}

@Composable
private fun LinkPanel(title: String, accent: Color, actions: List<Pair<String, () -> Unit>>, note: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PipText(title, color = accent, prominent = true)
        actions.forEach { (label, action) -> PipButton("▸ $label", accent, Modifier.fillMaxWidth(), onClick = action) }
        PipText(note, color = accent.copy(alpha = .7f))
    }
}

@Composable
private fun RadioPanel(accent: Color, apps: List<LaunchableApp>, launch: (LaunchableApp) -> Unit) {
    val players = remember(apps) {
        apps.filter { app -> listOf("music", "spotify", "youtube", "podcast", "vlc", "radio", "tidal").any {
            app.label.contains(it, true) || app.packageName.contains(it, true)
        } }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PipText("RADIO // MEDIA RECEIVER", color = accent, prominent = true)
        MediaControllerPanel(accent)
        HorizontalDivider(color = accent.copy(alpha = .35f))
        PipText("MEDIA APPLICATIONS", color = accent, prominent = true)
        if (players.isEmpty()) PipText("NO MEDIA APPS DETECTED", color = accent.copy(alpha = .7f))
        players.forEach { app -> PipButton("▸ ${app.label}", accent, Modifier.fillMaxWidth()) { launch(app) } }
    }
}

@Composable
private fun SettingsPanel(
    prefs: LauncherPreferences, accent: Color,
    onTheme: (ThemeProfile) -> Unit, onEffects: (Boolean) -> Unit,
    onReduceMotion: (Boolean) -> Unit, onLayout: (FavoriteLayout) -> Unit,
    onRestore: () -> Unit, onClose: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PipText("CONFIGURATION // DISPLAY", color = accent, prominent = true)
        ThemeProfile.entries.forEach { profile ->
            PipButton("${if (prefs.theme == profile) "[X]" else "[ ]"} ${profile.name}", accent,
                Modifier.fillMaxWidth(), active = prefs.theme == profile) { onTheme(profile) }
        }
        PipButton("${if (prefs.scanlines) "[X]" else "[ ]"} CRT SCANLINES", accent, Modifier.fillMaxWidth()) {
            onEffects(!prefs.scanlines)
        }
        PipButton("${if (prefs.reduceMotion) "[X]" else "[ ]"} REDUCE MOTION", accent, Modifier.fillMaxWidth()) {
            onReduceMotion(!prefs.reduceMotion)
        }
        PipText("HOME SCREEN // PINNED APPLICATIONS", color = accent, prominent = true)
        FavoriteLayout.entries.forEach { layout ->
            PipButton("${if (prefs.favoriteLayout == layout) "[X]" else "[ ]"} ${layout.name}", accent,
                Modifier.fillMaxWidth(), active = prefs.favoriteLayout == layout) { onLayout(layout) }
        }
        PipButton("RESTORE HIDDEN APPS (${prefs.hidden.size})", accent, Modifier.fillMaxWidth()) { onRestore() }
        PipButton("CLOSE", accent, Modifier.fillMaxWidth()) { onClose() }
    }
}

private fun openPackage(context: Context, packageName: String) {
    val appIntent = context.packageManager.getLaunchIntentForPackage(packageName)
    context.startActivity(appIntent ?: Intent(Settings.ACTION_SETTINGS))
}

private fun openMap(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
    context.startActivity(if (intent.resolveActivity(context.packageManager) != null) intent else Intent(Settings.ACTION_SETTINGS))
}
