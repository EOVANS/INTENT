package com.intent.reader.ui.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.intent.reader.IntentApplication
import com.intent.reader.R
import com.intent.reader.ui.components.HeaderIconButton
import com.intent.reader.data.CacheCleaner
import com.intent.reader.data.prefs.SettingsState
import com.intent.reader.data.prefs.ThemeMode
import com.intent.reader.ui.theme.AccentPresets
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentFonts
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import com.intent.reader.widget.IntentWidgets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Settings ViewModel: single source of truth over SettingsState. */
class SettingsViewModel(private val app: IntentApplication) : ViewModel() {
    private val repo = app.container.settingsRepository
    private val sourceRepo = app.container.sourceRepository

    var settings by mutableStateOf(SettingsState())
        private set

    init {
        viewModelScope.launch { repo.settings.collect { settings = it } }
    }

    private fun launch(block: suspend () -> Unit): Job =
        viewModelScope.launch { block() }

    fun setUserName(v: String) = launch { repo.setUserName(v) }
    fun setAvatarPath(v: String) = launch { repo.setAvatarPath(v) }
    // Home-screen widgets mirror accent/theme, so repaint them once the write lands.
    fun setThemeMode(m: ThemeMode) = launch { repo.setThemeMode(m); IntentWidgets.refreshAll(app) }
    fun setAccentColor(argb: Long) = launch { repo.setAccentColor(argb); IntentWidgets.refreshAll(app) }
    fun setDynamicColor(v: Boolean) = launch { repo.setDynamicColor(v) }
    fun setDynamicBackground(v: Boolean) = launch { repo.setDynamicBackground(v) }
    fun setWidgetStyle(v: String) = launch { repo.setWidgetStyle(v); IntentWidgets.setStyle(app, v) }
    fun setFontChoice(v: String) = launch { repo.setFontChoice(v) }
    fun setFontWeight(v: Int) = launch { repo.setFontWeight(v) }
    fun setLanguageTag(v: String) = launch { repo.setLanguageTag(v) }
    fun setTextScaleLevel(v: Int) = launch { repo.setTextScaleLevel(v) }
    fun setFollowSystemFontScale(v: Boolean) = launch { repo.setFollowSystemFontScale(v) }
    fun setDisplaySizeLevel(v: Int) = launch { repo.setDisplaySizeLevel(v) }
    fun setHighContrast(v: Boolean) = launch { repo.setHighContrast(v) }
    fun setLargeTouchTargets(v: Boolean) = launch { repo.setLargeTouchTargets(v) }
    fun setReduceAnimations(v: Boolean) = launch { repo.setReduceAnimations(v) }
    fun setGlassEffectEnabled(v: Boolean) = launch { repo.setGlassEffectEnabled(v) }
    fun setGlassMaterial(v: String) = launch { repo.setGlassMaterial(v) }
    fun setGlassTransparency(v: Float) = launch { repo.setGlassTransparency(v) }
    fun setGlassAdaptiveTint(v: Boolean) = launch { repo.setGlassAdaptiveTint(v) }
    fun setGlassScrollEdgeBlur(v: Boolean) = launch { repo.setGlassScrollEdgeBlur(v) }
    fun setTtsEngine(v: String) = launch { repo.setTtsEngine(v) }
    fun setTtsRate(v: Float) = launch { repo.setTtsRate(v) }
    fun setTtsPitch(v: Float) = launch { repo.setTtsPitch(v) }
    fun setDailySummaryEnabled(v: Boolean) = launch { repo.setDailySummaryEnabled(v) }
    fun setDailySummaryTime(h: Int, m: Int) = launch { repo.setDailySummaryTime(h, m) }
    fun setQuietHours(start: Int, end: Int) = launch { repo.setQuietHours(start, end) }
    fun setLiveUpdateEnabled(v: Boolean) = launch {
        repo.setLiveUpdateEnabled(v)
        // Turning the live activity off should retract the pinned card too.
        if (!v) com.intent.reader.notify.LiveUpdate.cancel(app)
    }
    fun setReadingGoalMinutes(v: Int) = launch { repo.setReadingGoalMinutes(v) }
    fun setClearCacheOnExit(v: Boolean) = launch { repo.setClearCacheOnExit(v) }

    /** Clears image / disk caches off the main thread, then reports the freed bytes. */
    fun clearCache(onDone: (Long) -> Unit) = viewModelScope.launch {
        val freed = CacheCleaner.clear(app)
        onDone(freed)
    }
    fun setSyncIntervalMin(v: Int) = launch { repo.setSyncIntervalMin(v) }
    fun setSyncWifiOnly(v: Boolean) = launch { repo.setSyncWifiOnly(v) }
    fun setWifiOnlyImages(v: Boolean) = launch { repo.setWifiOnlyImages(v) }
    fun setRetainDays(v: Int) = launch { repo.setRetainDays(v) }

    fun importOpml(context: Context, uri: android.net.Uri, onDone: (Int) -> Unit) =
        viewModelScope.launch {
            val xml = sourceRepo.readTextFrom(uri)
            val n = if (xml != null) sourceRepo.importOpml(xml) else 0
            onDone(n)
        }

    fun exportOpml(context: Context, uri: android.net.Uri, onDone: (Boolean) -> Unit) =
        viewModelScope.launch {
            val xml = sourceRepo.exportOpml()
            onDone(sourceRepo.writeOpmlTo(uri, xml))
        }
}

@Composable
private fun settingsVm(): SettingsViewModel {
    val app = LocalContext.current.applicationContext as IntentApplication
    return viewModel(initializer = { SettingsViewModel(app) })
}

// ---------- shared widgets ----------

/**
 * True while a settings page is shown inside the wide two-pane layout. The pane keeps
 * the section list visible, so a top-level page has no back arrow of its own.
 */
internal val LocalSettingsPaneEmbedded = staticCompositionLocalOf { false }

@Composable
internal fun SettingsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val embedded = LocalSettingsPaneEmbedded.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                // Same top offset as the search page title (status bar + 24dp),
                // and the same 16dp gap above the first row (8dp here + the 8dp
                // spacer inside the scroll content).
                .padding(
                    start = IntentDimens.pageHMarginPhone.dp,
                    end = IntentDimens.pageHMarginPhone.dp,
                    top = 24.dp,
                    bottom = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null && !embedded) {
                HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back), onBack)
            }
            Text(
                text = title,
                style = t.pageTitle,
                color = c.text,
            )
        }
        // The floating glass tab bar is drawn over the scroll content, so the
        // content must reserve its footprint (nav inset + 12dp + 56dp) or the
        // last rows end up unreachable. The root page (no back arrow) carries the
        // tab bar; sub-pages do not, so they only need a hairline bottom margin.
        val bottomReserve = if (onBack == null) 84.dp else 16.dp
        Column(
            Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = IntentDimens.pageHMarginPhone.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            content()
            Spacer(Modifier.height(bottomReserve))
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Text(
        text = text.withCjkSpacing(),
        style = t.micro,
        color = c.textTertiary,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun EntryRow(title: String, value: String? = null, icon: ImageVector? = null, iconColor: Color = Color(0xFF438BFF), onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(14.dp))
        }
        Text(
            text = title.withCjkSpacing(),
            style = t.body,
            color = c.text,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value.withCjkSpacing(),
                style = t.meta,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(text = "→", style = t.body, color = c.textTertiary)
    }
}

@Composable
internal fun SwitchRow(title: String, checked: Boolean, enabled: Boolean = true, icon: ImageVector? = null, iconColor: Color = Color(0xFF438BFF), onChange: (Boolean) -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(14.dp))
        }
        Text(
            text = title.withCjkSpacing(),
            style = t.body,
            color = if (enabled) c.text else c.textTertiary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.text,
                checkedThumbColor = c.bg,
                uncheckedTrackColor = c.divider,
                uncheckedThumbColor = c.bg,
                uncheckedBorderColor = c.divider,
            ),
        )
    }
}

/** Segmented pill selector (Swiss minimal). */
@Composable
private fun Segmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(IntentDimens.radiusInput.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (selected) c.text else Color.Transparent,
                        RoundedCornerShape((IntentDimens.radiusInput - 3).dp),
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = t.meta,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) c.bg else c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StepperRow(label: String, valueText: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label.withCjkSpacing(), style = t.body, color = c.text, modifier = Modifier.weight(1f))
        Text(
            text = "−",
            style = t.body,
            color = c.text,
            modifier = Modifier
                .clickable(onClick = onMinus)
                .background(c.surface, CircleShape)
                .padding(12.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = valueText, style = t.body, color = c.text, modifier = Modifier.width(72.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = "+",
            style = t.body,
            color = c.text,
            modifier = Modifier
                .clickable(onClick = onPlus)
                .background(c.surface, CircleShape)
                .padding(12.dp),
        )
    }
}

// ---------- icon switching ----------

private val iconAliases = listOf(
    "default" to ".LauncherDefault",
    "black" to ".LauncherBlack",
    "white" to ".LauncherWhite",
    "serif" to ".LauncherSerif",
    "pixel" to ".LauncherPixel",
)

fun setAppIcon(context: Context, choice: String) {
    val pm = context.packageManager
    val pkg = context.packageName
    iconAliases.forEach { (key, cls) ->
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, "$pkg$cls"),
                if (key == choice) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
    com.intent.reader.notify.LiveUpdate.refreshPermissions()
}

/**
 * Reads the launcher icon actually enabled right now. The choice lives in the
 * PackageManager (activity-alias states), not in DataStore, so the segmented
 * control must query it instead of assuming the first option.
 */
fun currentAppIcon(context: Context): String {
    val pm = context.packageManager
    val pkg = context.packageName
    var dormantDefault = false
    for ((key, cls) in iconAliases) {
        val state = runCatching {
            pm.getComponentEnabledSetting(ComponentName(pkg, "$pkg$cls"))
        }.getOrDefault(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        when {
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> return key
            state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && key == "default" ->
                dormantDefault = true
        }
    }
    return if (dormantDefault) "default" else iconAliases.first().first
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(IntentTheme.colors.surface).padding(horizontal = 16.dp, vertical = 4.dp)) {
        content()
    }
}

@Composable
private fun SettingsControlLabel(title: String, icon: ImageVector, color: Color) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(14.dp))
        Text(title.withCjkSpacing(), style = IntentTheme.typography.body, color = IntentTheme.colors.text)
    }
}

// ---------- root settings ----------

/** Compact cache size for the storage row ("12.4 MB", "0 B"). */
private fun formatCacheSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(Locale.US, "%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024L -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUi: () -> Unit,
    onOpenA11y: () -> Unit,
    onOpenNotify: () -> Unit,
    onOpenUser: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenStorage: () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }

    val opmlImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importOpml(context, uri) { n ->
                Toast.makeText(context, context.getString(R.string.source_added) + " ×$n", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val opmlExport = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/xml")) { uri ->
        if (uri != null) {
            vm.exportOpml(context, uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) context.getString(R.string.export_opml) + " ✓" else "✗",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val syncOptions = listOf(60, 180, 360, 720, 1440)
    val syncLabels = listOf("1h", "3h", "6h", "12h", "24h")
    val retainOptions = listOf(7, 30, 90, 180)

    SettingsScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        SettingsGroup {
            EntryRow(stringResource(R.string.settings_ui), icon = Icons.Filled.Palette, iconColor = Color(0xFFAE66E9), onClick = onOpenUi)
            EntryRow(stringResource(R.string.settings_a11y), icon = Icons.Filled.AccessibilityNew, iconColor = Color(0xFF2587FF), onClick = onOpenA11y)
            EntryRow(stringResource(R.string.settings_notify), icon = Icons.Filled.Notifications, iconColor = Color(0xFF31C76A), onClick = onOpenNotify)
            EntryRow(stringResource(R.string.ai_settings), icon = Icons.Filled.AutoAwesome, iconColor = Color(0xFF9B70EF), onClick = onOpenAi)
            EntryRow(stringResource(R.string.settings_user), value = s.userName.ifBlank { null }, icon = Icons.Filled.Person, iconColor = Color(0xFFEC9450), onClick = onOpenUser)
            EntryRow(stringResource(R.string.settings_privacy), icon = Icons.Filled.PrivacyTip, iconColor = Color(0xFF5A64D8), onClick = onOpenPrivacy)
            EntryRow(stringResource(R.string.settings_advanced), icon = Icons.Filled.Tune, iconColor = Color(0xFF50B7C5), onClick = onOpenAdvanced)
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            SettingsControlLabel(stringResource(R.string.sync_freq), Icons.Filled.Sync, Color(0xFF2587FF))
            Segmented(options = syncLabels, selectedIndex = syncOptions.indexOf(s.syncIntervalMin).let { if (it >= 0) it else 1 }) { vm.setSyncIntervalMin(syncOptions[it]) }
            SwitchRow(stringResource(R.string.sync_wifi_only), s.syncWifiOnly, icon = Icons.Filled.Wifi, iconColor = Color(0xFF45A4E8)) { vm.setSyncWifiOnly(it) }
            SwitchRow(stringResource(R.string.wifi_only_images), s.wifiOnlyImages, icon = Icons.Filled.Image, iconColor = Color(0xFF6ABF88)) { vm.setWifiOnlyImages(it) }
            SettingsControlLabel(stringResource(R.string.retain_settings), Icons.Filled.History, Color(0xFFEA9854))
            Segmented(options = retainOptions.map { "$it 天" }, selectedIndex = retainOptions.indexOf(s.retainDays).let { if (it >= 0) it else 1 }) { vm.setRetainDays(retainOptions[it]) }
            Spacer(Modifier.height(8.dp))
            EntryRow(stringResource(R.string.import_opml), icon = Icons.Filled.FileDownload, iconColor = Color(0xFF56BC83)) { opmlImport.launch(arrayOf("application/xml", "text/xml", "text/plain", "*/*")) }
            EntryRow(stringResource(R.string.export_opml), icon = Icons.Filled.FileUpload, iconColor = Color(0xFF56A6E9)) { opmlExport.launch("intent-subscriptions.opml") }
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            EntryRow(stringResource(R.string.settings_storage), icon = Icons.Filled.Storage, iconColor = Color(0xFFEBA04F), onClick = onOpenStorage)
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            EntryRow(stringResource(R.string.about), icon = Icons.Filled.Info, iconColor = Color(0xFF4E9AF4), onClick = onOpenAbout)
            EntryRow(stringResource(R.string.licenses), icon = Icons.Filled.Description, iconColor = Color(0xFF8B9BAF)) { showLicenses = true }
        }
    }

    if (showLicenses) {
        val intentLicense = remember(context) {
            context.assets.open("licenses/INTENT-MIT.txt").bufferedReader().use { it.readText() }
        }
        val apacheLicense = remember(context) {
            context.assets.open("licenses/AndroidLiquidGlass-LICENSE.txt").bufferedReader().use { it.readText() }
        }
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) { Text(stringResource(R.string.close)) }
            },
            title = { Text(stringResource(R.string.licenses)) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.license_components_intro), style = t.body)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "AndroidX (Core, Lifecycle, Activity, Compose, Navigation, Room, DataStore, WorkManager, Glance)\n" +
                            "Coil · OkHttp · Okio · Kotlin · kotlinx.coroutines · kotlinx.serialization\n" +
                            "Android Liquid Glass / Backdrop — Kyant (Apache License 2.0)\n" +
                            "https://github.com/Kyant0/AndroidLiquidGlass",
                        style = t.bodySmall,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("SharedViewKit 1.0.0.5 — ITGSA", style = t.body)
                    Text(stringResource(R.string.license_sharedview_description), style = t.bodySmall)
                    Text("https://gitee.com/itgsa_sharedviewkit/SharedViewDemo", style = t.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    Text("INTENT — MIT License", style = t.body)
                    Text(intentLicense, style = t.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    Text("Apache License 2.0", style = t.body)
                    Text(apacheLicense, style = t.bodySmall)
                }
            },
            containerColor = c.card,
        )
    }
}

@Composable
fun StorageSettingsScreen(onBack: () -> Unit) {
    val vm = settingsVm()
    val s = vm.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { cacheBytes = CacheCleaner.sizeBytes(context) }
    SettingsScaffold(title = stringResource(R.string.settings_storage), onBack = onBack) {
        SettingsGroup {
            SwitchRow(stringResource(R.string.clear_cache_on_exit), s.clearCacheOnExit, icon = Icons.Filled.DeleteSweep, iconColor = Color(0xFFEBA04F)) { vm.setClearCacheOnExit(it) }
            EntryRow(title = stringResource(R.string.clear_cache_now), value = cacheBytes?.let { formatCacheSize(it) }, icon = Icons.Filled.CleaningServices, iconColor = Color(0xFF50B7C5)) {
                vm.clearCache { freed ->
                    Toast.makeText(context, context.getString(R.string.cache_cleared, formatCacheSize(freed)), Toast.LENGTH_SHORT).show()
                    scope.launch { cacheBytes = CacheCleaner.sizeBytes(context) }
                }
            }
        }
    }
}

// ---------- user settings (level 1) ----------

@Composable
fun UserSettingsScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    val context = LocalContext.current

    // Copy the picked image into app-private storage: the picker only grants a
    // temporary read permission on the returned URI.
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val target = java.io.File(
                java.io.File(context.filesDir, "avatars").apply { mkdirs() },
                "avatar_${System.currentTimeMillis()}.jpg",
            )
            val ok = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { input.copyTo(it) }
                }
                true
            } catch (_: Exception) {
                false
            }
            if (ok) {
                vm.setAvatarPath(target.absolutePath)
                Toast.makeText(context, context.getString(R.string.avatar_updated), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Debounced username input
    var nameInput by remember(s.userName) { mutableStateOf(s.userName) }
    LaunchedEffect(nameInput) {
        if (nameInput != s.userName) {
            delay(500)
            vm.setUserName(nameInput)
        }
    }

    SettingsScaffold(title = stringResource(R.string.settings_user), onBack = onBack) {
        SectionLabel(stringResource(R.string.settings_user_name))
        TextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            placeholder = { Text(stringResource(R.string.onboarding_name_hint), color = c.textTertiary) },
            singleLine = true,
            shape = RoundedCornerShape(IntentDimens.radiusInput.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                focusedIndicatorColor = c.text,
                unfocusedIndicatorColor = c.divider,
                cursorColor = c.text,
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(stringResource(R.string.user_change_avatar))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(IntentDimens.radiusInput.dp))
                .clickable { avatarPicker.launch(arrayOf("image/*")) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(c.surface)
                    .border(1.dp, c.divider, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (s.avatarPath.isNotBlank() && java.io.File(s.avatarPath).exists()) {
                    coil.compose.AsyncImage(
                        model = java.io.File(s.avatarPath),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = s.userName.trim().take(1).ifBlank { "·" },
                        style = t.greetingSerif.copy(fontWeight = FontWeight.Bold),
                        color = c.textSecondary,
                    )
                }
            }
            Text(
                text = stringResource(R.string.user_change_avatar),
                style = t.body,
                color = c.text,
            )
        }
    }
}

// ---------- advanced material & interaction (level 1) ----------

@Composable
fun AdvancedMaterialScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    // AGSL lens pipeline needs Android 13 (API 33); below that the switch is
    // disabled rather than silently degrading to the classic pipeline.
    val supported = Build.VERSION.SDK_INT >= 33
    // The lens pipeline only runs while the master switch is on, so every
    // sub-option below is gated on it as well.
    val on = supported && s.glassEffectEnabled
    var transparency by remember { mutableFloatStateOf(s.glassTransparency) }
    LaunchedEffect(s.glassTransparency) { transparency = s.glassTransparency }

    SettingsScaffold(title = stringResource(R.string.settings_advanced), onBack = onBack) {
        GlassMaterialPreview(transparency = transparency, adaptive = s.glassAdaptiveTint)
        Text(stringResource(R.string.glass_transparency), style = t.body, color = c.text,
            modifier = Modifier.padding(top = 20.dp))
        Slider(
            value = transparency,
            onValueChange = { transparency = it },
            onValueChangeFinished = { vm.setGlassTransparency(transparency) },
            enabled = supported,
            valueRange = 0f..1f,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.glass_transparency_tinted), style = t.micro, color = c.textSecondary)
            Text(stringResource(R.string.glass_transparency_clear), style = t.micro, color = c.textSecondary)
        }
        Text(stringResource(R.string.glass_transparency_note), style = t.micro, color = c.textTertiary,
            modifier = Modifier.padding(top = 8.dp))
        SectionLabel(stringResource(R.string.settings_advanced))
        SwitchRow(
            title = stringResource(R.string.glass_effect),
            checked = s.glassEffectEnabled,
            enabled = supported,
        ) { vm.setGlassEffectEnabled(it) }
        Text(
            text = stringResource(R.string.glass_effect_note).withCjkSpacing(),
            style = t.micro,
            color = c.textTertiary,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        SwitchRow(
            title = stringResource(R.string.glass_adaptive_tint),
            checked = s.glassAdaptiveTint,
            enabled = on,
        ) { vm.setGlassAdaptiveTint(it) }
        Text(
            text = stringResource(R.string.glass_adaptive_tint_note).withCjkSpacing(),
            style = t.micro,
            color = c.textTertiary,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
        SwitchRow(
            title = stringResource(R.string.glass_scroll_edge_blur),
            checked = s.glassScrollEdgeBlur,
            enabled = on,
        ) { vm.setGlassScrollEdgeBlur(it) }
        Text(
            text = stringResource(R.string.glass_scroll_edge_blur_note).withCjkSpacing(),
            style = t.micro,
            color = c.textTertiary,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
        )
    }
}

// ---------- theme settings ----------

@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    SettingsScaffold(title = stringResource(R.string.ui_theme), onBack = onBack) {
        SectionLabel(stringResource(R.string.ui_accent))
        SwitchRow(
            title = stringResource(R.string.color_follow_system),
            checked = s.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onChange = { vm.setDynamicColor(it) },
        )
        Text(
            text = stringResource(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.string.color_system_note else R.string.color_system_unavailable),
            style = t.micro, color = c.textSecondary,
        )
        SwitchRow(
            title = stringResource(R.string.color_background),
            checked = s.dynamicBackground,
            enabled = s.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onChange = { vm.setDynamicBackground(it) },
        )
        Text(stringResource(R.string.color_manual), style = t.meta, color = c.textSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccentPresets.forEach { color ->
                val isSel = !s.dynamicColor && s.accentColor == color.toArgbLong()
                Box(
                    Modifier
                        .size(36.dp)
                        .background(color, CircleShape)
                        .border(
                            width = if (isSel) 2.dp else 1.dp,
                            color = if (isSel) c.text else c.divider,
                            shape = CircleShape,
                        )
                        // Clip the touch target to the circle and drop the ripple:
                        // without both, tapping paints a square ripple block over
                        // the round swatch.
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { vm.setDynamicColor(false); vm.setAccentColor(color.toArgbLong()) },
                )
            }
        }

        SectionLabel(stringResource(R.string.ui_theme))
        Segmented(
            options = listOf(
                stringResource(R.string.theme_follow),
                stringResource(R.string.theme_light),
                stringResource(R.string.theme_dark),
            ),
            selectedIndex = when (s.themeMode) {
                ThemeMode.FOLLOW_SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
            },
        ) { i ->
            vm.setThemeMode(
                when (i) {
                    0 -> ThemeMode.FOLLOW_SYSTEM
                    1 -> ThemeMode.LIGHT
                    else -> ThemeMode.DARK
                }
            )
        }

    }
}

// ---------- UI settings (level 1) ----------

@Composable
fun UiSettingsScreen(onBack: () -> Unit, onOpenIcon: () -> Unit, onOpenFont: () -> Unit, onOpenTheme: () -> Unit, onOpenWidget: () -> Unit, onOpenLanguage: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    SettingsScaffold(title = stringResource(R.string.settings_ui), onBack = onBack) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Color(0xFFAE66E9), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.ui_section_interface), style = t.pageTitle, fontWeight = FontWeight.Bold, color = c.text)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.ui_intro).withCjkSpacing(), style = t.bodySmall, color = c.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        SectionLabel(stringResource(R.string.ui_section_interface))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 16.dp)) {
            EntryRow(stringResource(R.string.ui_icon), onClick = onOpenIcon)
            EntryRow(stringResource(R.string.ui_font), onClick = onOpenFont)
            EntryRow(stringResource(R.string.ui_theme), onClick = onOpenTheme)
            EntryRow(stringResource(R.string.ui_widget_style), onClick = onOpenWidget)
        }
        SectionLabel(stringResource(R.string.ui_section_language))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 16.dp)) {
            EntryRow(stringResource(R.string.ui_language), onClick = onOpenLanguage)
        }
    }
}

@Composable
fun IconSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val c = IntentTheme.colors
    var iconChoice by remember { mutableStateOf(currentAppIcon(context)) }
    val iconChoices = listOf(
        Triple("default", stringResource(R.string.icon_default), R.mipmap.ic_launcher),
        Triple("black", stringResource(R.string.icon_black), R.mipmap.ic_launcher_black),
        Triple("white", stringResource(R.string.icon_white), R.mipmap.ic_launcher_white),
        Triple("serif", stringResource(R.string.icon_serif), R.mipmap.ic_launcher_serif),
        Triple("pixel", stringResource(R.string.icon_pixel), R.mipmap.ic_launcher_pixel),
    )
    SettingsScaffold(title = stringResource(R.string.ui_icon), onBack = onBack) {
        Text(
            text = stringResource(R.string.icon_change_notice).withCjkSpacing(),
            style = IntentTheme.typography.meta,
            color = c.textSecondary,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        iconChoices.chunked(3).forEach { choices ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                choices.forEach { (key, label, resource) ->
                    val selected = iconChoice == key
                    val preview = remember(resource) {
                        androidx.core.content.ContextCompat.getDrawable(context, resource)!!
                            .toBitmap(width = 192, height = 192).asImageBitmap()
                    }
                    Column(
                        modifier = Modifier.weight(1f).selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = {
                                if (!selected) {
                                    iconChoice = key
                                    setAppIcon(context, key)
                                    Toast.makeText(context, R.string.icon_restart_hint, Toast.LENGTH_LONG).show()
                                }
                            },
                        ).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            bitmap = preview,
                            contentDescription = label,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(18.dp))
                                .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.divider, RoundedCornerShape(18.dp)),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(label, style = IntentTheme.typography.meta, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.textTertiary))
                    }
                }
                repeat(3 - choices.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun FontSettingsScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    val context = LocalContext.current
    val dir = remember { java.io.File(context.filesDir, "fonts").apply { mkdirs() } }
    fun listFonts() = dir.listFiles()?.filter { it.isFile && (it.extension.equals("ttf", true) || it.extension.equals("otf", true)) }?.sortedBy { it.name } ?: emptyList()
    var fontFiles by remember { mutableStateOf(listFonts()) }
    val selectedFile = s.fontChoice.takeIf { it.startsWith("custom:") }?.removePrefix("custom:")?.let { java.io.File(it) }
    val axis = remember(selectedFile?.absolutePath) { selectedFile?.let { IntentFonts.weightAxis(it) } }
    val previewFamily = remember(s.fontChoice, s.fontWeight) {
        when {
            selectedFile != null -> IntentFonts.fromFile(selectedFile.absolutePath, s.fontWeight) ?: IntentFonts.resolveGlobalFamily()
            s.fontChoice == "serif" -> IntentFonts.serifHeader
            else -> IntentFonts.resolveGlobalFamily()
        }
    }
    val fontImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            var dest: java.io.File? = null
            val ok = runCatching {
                val displayName = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }.orEmpty()
                val cleanName = displayName.substringAfterLast('/').substringAfterLast('\\').replace(Regex("[^\\p{L}\\p{N}._ -]"), "_").take(100).ifBlank { "font.ttf" }
                val extension = if (cleanName.endsWith(".otf", true)) ".otf" else ".ttf"
                val name = cleanName.removeSuffix(extension).removeSuffix(extension.uppercase())
                dest = java.io.File(dir, java.util.UUID.randomUUID().toString() + "_" + name + extension)
                val stream = context.contentResolver.openInputStream(uri) ?: error("Unable to open font")
                stream.use { input -> dest!!.outputStream().use { input.copyTo(it) } }
                check(IntentFonts.fromFile(dest!!.absolutePath) != null)
                fontFiles = listFonts()
                vm.setFontChoice("custom:" + dest!!.absolutePath)
            }.isSuccess
            if (!ok) dest?.delete()
            Toast.makeText(context, if (ok) R.string.font_imported else R.string.font_import_failed, Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxSize().background(c.surface).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().background(c.surface).padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back), onBack)
            Text(stringResource(R.string.ui_font), style = t.pageTitle, color = c.text)
        }
        Column(Modifier.weight(1f).fillMaxWidth().background(c.card).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().background(c.surface).heightIn(min = 210.dp).padding(horizontal = 20.dp, vertical = 24.dp)) {
                Text(stringResource(R.string.font_preview_title), style = t.pageTitle.copy(fontFamily = previewFamily), color = c.text)
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.font_preview_text), style = t.body.copy(fontFamily = previewFamily), color = c.text)
            }
            Box(Modifier.fillMaxWidth().background(c.surface)) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(c.card).padding(top = 20.dp, bottom = 12.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    @Composable fun FontTile(choice: String, name: String, family: androidx.compose.ui.text.font.FontFamily) {
                        val selected = s.fontChoice == choice
                        Column(Modifier.width(88.dp).clip(RoundedCornerShape(12.dp)).clickable { vm.setFontChoice(choice) }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().height(64.dp).border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.divider, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text("Aa", style = t.pageTitle.copy(fontFamily = family, fontSize = 28.sp), color = c.text)
                            }
                            Text(name, style = t.micro, color = if (selected) c.text else c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 8.dp).heightIn(min = 30.dp))
                        }
                    }
                    FontTile("system", stringResource(R.string.font_system_short), IntentFonts.resolveGlobalFamily())
                    FontTile("serif", stringResource(R.string.font_serif), IntentFonts.serifHeader)
                    fontFiles.forEach { file ->
                        val family = remember(file.absolutePath) { IntentFonts.fromFile(file.absolutePath) ?: androidx.compose.ui.text.font.FontFamily.Default }
                        val name = file.nameWithoutExtension.replace(Regex("^[0-9a-fA-F-]{36}_"), "")
                        FontTile("custom:" + file.absolutePath, name, family)
                    }
                    Column(Modifier.width(88.dp).clip(RoundedCornerShape(12.dp)).clickable { fontImport.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "*/*")) }, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(64.dp).border(1.dp, c.divider, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text("+", style = t.pageTitle.copy(fontSize = 28.sp), color = c.textSecondary)
                        }
                        Text(stringResource(R.string.font_import_short), style = t.micro, color = c.textSecondary, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.a11y_font_size), style = t.body, fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.weight(1f))
                        Text(if (s.followSystemFontScale) stringResource(R.string.font_follow_short) else listOf(stringResource(R.string.size_small), stringResource(R.string.size_default), stringResource(R.string.size_large), stringResource(R.string.size_xlarge))[s.textScaleLevel.coerceIn(0, 3)], style = t.meta, color = c.textSecondary)
                    }
                    Slider(
                        value = s.textScaleLevel.coerceIn(0, 3).toFloat(),
                        onValueChange = { vm.setTextScaleLevel(kotlin.math.round(it).toInt()) },
                        enabled = !s.followSystemFontScale,
                        valueRange = 0f..3f, steps = 2,
                        colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent),
                    )
                    SwitchRow(stringResource(R.string.a11y_follow_system_font), s.followSystemFontScale) { vm.setFollowSystemFontScale(it) }
                    if (axis != null) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.font_weight), style = t.body, fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.weight(1f))
                            Text(s.fontWeight.coerceIn(axis.first, axis.last).toString(), style = t.meta, color = c.textSecondary)
                        }
                        Slider(value = s.fontWeight.coerceIn(axis.first, axis.last).toFloat(), onValueChange = { vm.setFontWeight(it.toInt()) }, valueRange = axis.first.toFloat()..axis.last.toFloat(), colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent))
                    }
                    Text(stringResource(R.string.font_note).withCjkSpacing(), style = t.micro, color = c.textTertiary, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
                }
            }
            }
        }
        Box(Modifier.fillMaxWidth().background(c.card)) {
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp).height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent),
        ) {
            Text(stringResource(R.string.font_done), color = Color.White)
        }
        }
    }
}

@Composable
fun WidgetStyleSettingsScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    val accent = Color(s.accentColor)
    val onAccent = if (androidx.core.graphics.ColorUtils.calculateContrast(android.graphics.Color.BLACK, s.accentColor.toInt()) >=
        androidx.core.graphics.ColorUtils.calculateContrast(android.graphics.Color.WHITE, s.accentColor.toInt())) Color.Black else Color.White
    val sampleTitle = stringResource(R.string.widget_preview_title)
    val sampleSecond = stringResource(R.string.widget_preview_second)
    val sampleSource = stringResource(R.string.widget_preview_source)
    SettingsScaffold(title = stringResource(R.string.ui_widget_style), onBack = onBack) {
        Text(stringResource(R.string.widget_preview_note), style = t.meta, color = c.textSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
        listOf("classic" to stringResource(R.string.widget_style_classic), "image" to stringResource(R.string.widget_style_image)).forEach { (style, label) ->
            val selected = s.widgetStyle == style
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(20.dp))
                    .border(if (selected) 2.dp else 1.dp, if (selected) c.accent else c.divider, RoundedCornerShape(20.dp))
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { vm.setWidgetStyle(style) })
                    .padding(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = t.body, fontWeight = FontWeight.SemiBold, color = c.text, modifier = Modifier.weight(1f))
                    RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.textTertiary))
                }
                Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).background(if (c.isDark) Color(0xFF1C1C1C) else Color.White)) {
                    if (style == "classic") {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(64.dp).fillMaxHeight().background(accent), contentAlignment = Alignment.Center) {
                                Text("INTENT", style = t.micro, fontWeight = FontWeight.Bold, color = onAccent)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(sampleTitle, style = t.meta, fontWeight = FontWeight.Bold, color = if (c.isDark) Color.White else Color(0xFF171717), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(5.dp))
                                Text(sampleSource + " · 09:30", style = t.micro, color = if (c.isDark) Color.LightGray else Color.Gray, maxLines = 1)
                            }
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(sampleSecond, style = t.meta, color = if (c.isDark) Color.White else Color(0xFF171717), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(5.dp))
                                Text("INTENT · 08:30", style = t.micro, color = if (c.isDark) Color.LightGray else Color.Gray, maxLines = 1)
                            }
                            Text("›", style = t.pageTitle, color = accent, modifier = Modifier.padding(end = 10.dp))
                        }
                    } else {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF8AAAB7), Color(0xFF425C69))))
                            val mountain = Path().apply {
                                moveTo(0f, size.height * .75f); lineTo(size.width * .28f, size.height * .32f)
                                lineTo(size.width * .46f, size.height * .60f); lineTo(size.width * .70f, size.height * .20f)
                                lineTo(size.width, size.height * .67f); lineTo(size.width, size.height); lineTo(0f, size.height); close()
                            }
                            drawPath(mountain, Color(0xFF516F72))
                            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000))))
                        }
                        Text(sampleSource, color = Color.White, style = t.micro, modifier = Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0x66000000), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp))
                        Row(Modifier.align(Alignment.BottomStart).padding(start = 14.dp, end = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(4.dp).height(30.dp).background(accent, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(sampleTitle, style = t.meta, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("09:30", style = t.micro, color = Color.LightGray)
                            }
                            Text("›", style = t.pageTitle, color = Color.White)
                        }
                    }
                }
            }
        }
        Text(stringResource(R.string.widget_style_restart_hint), style = t.micro, color = c.textSecondary, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun LanguageSettingsScreen(onBack: () -> Unit, onRecreate: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    SettingsScaffold(title = stringResource(R.string.ui_language), onBack = onBack) {
        val langs = listOf(
            "" to stringResource(R.string.lang_auto),
            "zh-CN" to stringResource(R.string.lang_zh),
            "en" to stringResource(R.string.lang_en),
            "ja" to stringResource(R.string.lang_ja),
            "ko" to stringResource(R.string.lang_ko),
        )
        val langIdx = langs.indexOfFirst { it.first == s.languageTag }.let { if (it >= 0) it else 0 }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            langs.forEachIndexed { i, (tag, label) ->
                val selected = i == langIdx
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) c.text else c.surface,
                            RoundedCornerShape(IntentDimens.radiusInput.dp),
                        )
                        .clickable {
                            vm.setLanguageTag(tag)
                            onRecreate()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = label,
                        style = t.body,
                        color = if (selected) c.bg else c.text,
                    )
                }
            }
        }
    }
}

private fun Color.toArgbLong(): Long = 0xFFFFFFFFL and android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt(),
).toLong() and 0xFFFFFFFFL

// ---------- Privacy settings (level 1) ----------

class PrivacyViewModel(private val app: IntentApplication) : ViewModel() {
    private val db = app.container.database
    private val repo = app.container.settingsRepository

    var sources by mutableStateOf<List<com.intent.reader.data.db.Source>>(emptyList())
        private set
    var readingEvents by mutableStateOf<List<com.intent.reader.data.db.ReadingEvent>>(emptyList())
        private set
    var readingCount by mutableStateOf(0)
        private set
    var islandNotes by mutableStateOf(0)
        private set
    var cachedArticles by mutableStateOf(0)
        private set
    var savedArticles by mutableStateOf(0)
        private set
    var settings by mutableStateOf(SettingsState())
        private set
    var aiConfig by mutableStateOf(com.intent.reader.ai.AiConfig())
        private set

    init {
        viewModelScope.launch { app.container.sourceRepository.observeSources().collect { sources = it } }
        viewModelScope.launch { db.readingEventDao().observeRecent(300).collect { readingEvents = it } }
        viewModelScope.launch { db.readingEventDao().observeCount().collect { readingCount = it } }
        viewModelScope.launch { app.container.notesRepository.state.collect { islandNotes = it.notes.size } }
        viewModelScope.launch { db.articleDao().observeCount().collect { cachedArticles = it } }
        viewModelScope.launch { db.articleDao().observeFavoriteCount().collect { savedArticles = it } }
        viewModelScope.launch { repo.settings.collect { settings = it } }
        viewModelScope.launch { app.container.aiConfigStore.config.collect { aiConfig = it } }
    }

    fun clearReadingHistory() = viewModelScope.launch { db.readingEventDao().clear() }
}

@Composable
private fun privacyVm(): PrivacyViewModel {
    val app = LocalContext.current.applicationContext as IntentApplication
    return viewModel(initializer = { PrivacyViewModel(app) })
}

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenReadingHistory: () -> Unit,
    onOpenAi: () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = privacyVm()

    SettingsScaffold(title = stringResource(R.string.settings_privacy), onBack = onBack) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Color(0xFF5A64D8), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_privacy), style = t.pageTitle, fontWeight = FontWeight.Bold, color = c.text)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.privacy_intro).withCjkSpacing(),
                style = t.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SectionLabel(stringResource(R.string.privacy_data_section))

        // 1. App sandbox: real local data the user can inspect or clear.
        PrivacyGroup(title = stringResource(R.string.privacy_sandbox_title)) {
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_sources),
                value = stringResource(R.string.privacy_count_sources, vm.sources.size),
                onClick = onOpenSources,
            )
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_reading),
                value = stringResource(R.string.privacy_count_records, vm.readingCount),
                onClick = onOpenReadingHistory,
            )
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_island_notes),
                value = stringResource(R.string.privacy_count_notes, vm.islandNotes),
            )
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_preferences),
                value = stringResource(R.string.privacy_local_only),
            )
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_cache),
                value = stringResource(R.string.privacy_count_articles, vm.cachedArticles),
            )
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_saved),
                value = stringResource(R.string.privacy_count_saved, vm.savedArticles),
            )
            PrivacyNote(stringResource(R.string.privacy_sandbox_description))
        }

        Spacer(Modifier.height(10.dp))

        // 2. Public storage only receives files the user exports explicitly.
        PrivacyGroup(title = stringResource(R.string.privacy_export_title)) {
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_opml),
                value = stringResource(R.string.privacy_export_on_demand),
                active = false,
            )
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_article_export),
                value = stringResource(R.string.privacy_export_on_demand),
                active = false,
            )
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_logs),
                value = stringResource(R.string.privacy_export_on_demand),
                active = false,
            )
            PrivacyNote(stringResource(R.string.privacy_export_description))
        }

        Spacer(Modifier.height(10.dp))

        // 3. Encrypted local credentials; tapping goes to AI settings to manage them.
        PrivacyGroup(title = stringResource(R.string.privacy_secure_title)) {
            PrivacyDataRow(
                label = stringResource(R.string.privacy_row_api_key),
                value = stringResource(
                    if (vm.aiConfig.apiKey.isNotBlank()) R.string.privacy_api_configured else R.string.privacy_api_missing
                ),
                icon = Icons.Filled.Lock,
                tint = Color(0xFF2587FF),
                onClick = onOpenAi,
            )
            PrivacyNote(stringResource(R.string.privacy_secure_description))
        }

        Spacer(Modifier.height(10.dp))

        // 4. AI requests actually made from this device, counted locally by feature.
        PrivacyGroup(title = stringResource(R.string.privacy_ai_title)) {
            AiUsageBars(
                quickRead = vm.settings.aiQuickReadCount,
                quote = vm.settings.aiQuoteCount,
                test = vm.settings.aiTestCount,
            )
            PrivacyNote(stringResource(R.string.privacy_ai_description))
        }
    }
}

@Composable
private fun PrivacyGroup(title: String, content: @Composable () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    SettingsGroup {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title.withCjkSpacing(), style = t.body, fontWeight = FontWeight.SemiBold, color = c.text)
        }
        content()
    }
}

@Composable
private fun PrivacyDataRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    active: Boolean = true,
    icon: ImageVector? = null,
    tint: Color = Color(0xFF35B26B),
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        } else {
            Icon(
                if (active) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (active) tint else c.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Text(label.withCjkSpacing(), style = t.body, color = c.text, modifier = Modifier.weight(1f))
        Text(value, style = t.bodySmall, color = c.textSecondary)
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PrivacyNote(text: String) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Text(
        text.withCjkSpacing(),
        style = t.bodySmall,
        color = c.textTertiary,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
    )
}

/** Local AI usage: a big total on the left and one bar per feature on the right. */
@Composable
private fun AiUsageBars(quickRead: Int, quote: Int, test: Int) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val total = quickRead + quote + test
    val max = maxOf(quickRead, quote, test, 1)
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.privacy_ai_total_label), style = t.micro, color = c.textTertiary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$total", style = t.pageTitle, fontWeight = FontWeight.Bold, color = c.text)
                Text(stringResource(R.string.privacy_ai_times_unit), style = t.bodySmall, color = c.textSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
            AiUsageBar(quickRead, max, Icons.Filled.AutoAwesome, Color(0xFF9B70EF), stringResource(R.string.privacy_ai_quick_read))
            AiUsageBar(quote, max, Icons.Filled.FormatQuote, Color(0xFFDD8A3C), stringResource(R.string.privacy_ai_quote))
            AiUsageBar(test, max, Icons.Filled.Bolt, Color(0xFF2587FF), stringResource(R.string.privacy_ai_test))
        }
    }
}

@Composable
private fun AiUsageBar(count: Int, max: Int, icon: ImageVector, tint: Color, label: String) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val height = 44.dp + (56.dp * (count.toFloat() / max))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.width(44.dp).height(height).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(bottom = 10.dp).size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.privacy_ai_times, count), style = t.bodySmall, fontWeight = FontWeight.SemiBold, color = c.text)
        Text(label, style = t.micro, color = c.textTertiary)
    }
}

@Composable
fun ReadingHistoryScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = privacyVm()
    var confirmClear by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd a h:mm", Locale.getDefault()) }

    SettingsScaffold(title = stringResource(R.string.privacy_reading_title), onBack = onBack) {
        if (vm.readingEvents.isEmpty()) {
            SettingsGroup {
                Text(
                    stringResource(R.string.privacy_reading_empty),
                    style = t.bodySmall,
                    color = c.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
        } else {
            SettingsGroup {
                vm.readingEvents.forEachIndexed { index, event ->
                    if (index > 0) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.divider))
                    }
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatter.format(Date(event.at)), style = t.micro, color = c.textTertiary, modifier = Modifier.weight(1f))
                            Text(
                                stringResource(R.string.privacy_reading_minutes, event.minutes),
                                style = t.micro,
                                color = Color(0xFF35B26B),
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(event.articleTitle.withCjkSpacing(), style = t.body, color = c.text)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.privacy_reading_from, event.sourceTitle).withCjkSpacing(),
                            style = t.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.privacy_reading_note),
                style = t.bodySmall,
                color = c.textTertiary,
                modifier = Modifier.padding(top = 12.dp),
            )
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                TextButton(onClick = { confirmClear = true }) {
                    Text(stringResource(R.string.privacy_reading_clear), color = Color(0xFFE05B5B))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.privacy_reading_clear)) },
            text = { Text(stringResource(R.string.privacy_reading_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = { vm.clearReadingHistory(); confirmClear = false }) {
                    Text(stringResource(R.string.privacy_reading_clear), color = Color(0xFFE05B5B))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.privacy_cancel)) }
            },
        )
    }
}

// ---------- Accessibility settings (level 1) ----------

@Composable
fun AccessibilityScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    val context = LocalContext.current

    // TTS engine listing + preview. The instance is rebuilt whenever the selected
    // engine changes, so both "试听" and the reader actually use the chosen engine.
    var engines by remember { mutableStateOf<List<TextToSpeech.EngineInfo>>(emptyList()) }
    var previewTts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(s.ttsEngine) {
        // Own the engine immediately: leaving during initialization must still
        // unbind the service. A delay before assigning previewTts leaked it.
        val tts = TextToSpeech(context.applicationContext, { }, s.ttsEngine.ifBlank { null })
        engines = tts.engines ?: emptyList()
        previewTts = tts
        onDispose {
            previewTts = null
            tts.stop()
            tts.shutdown()
        }
    }

    SettingsScaffold(title = stringResource(R.string.settings_a11y), onBack = onBack) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Color(0xFF2587FF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AccessibilityNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.settings_a11y), style = t.pageTitle, fontWeight = FontWeight.Bold, color = c.text)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.a11y_intro).withCjkSpacing(), style = t.bodySmall, color = c.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        SettingsGroup {
        SectionLabel(stringResource(R.string.a11y_tts))
        // Every installed engine is listed (the old build only showed two, which
        // made switching impossible on devices with more).
        val engineValues = listOf("") + engines.map { it.name }
        val engineLabels = listOf(stringResource(R.string.tts_default)) + engines.map { it.label ?: it.name }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            engineLabels.forEachIndexed { i, label ->
                val selected = if (i == 0) s.ttsEngine.isBlank() else s.ttsEngine == engineValues[i]
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) c.text else c.card,
                            RoundedCornerShape(IntentDimens.radiusInput.dp),
                        )
                        .clickable { vm.setTtsEngine(engineValues[i]) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = label,
                        style = t.body,
                        color = if (selected) c.bg else c.text,
                    )
                }
            }
        }

        StepperRow(stringResource(R.string.tts_rate), "%.1fx".format(s.ttsRate), { vm.setTtsRate((s.ttsRate - 0.1f).coerceIn(0.5f, 2f)) }, { vm.setTtsRate((s.ttsRate + 0.1f).coerceIn(0.5f, 2f)) })
        StepperRow(stringResource(R.string.tts_pitch), "%.1fx".format(s.ttsPitch), { vm.setTtsPitch((s.ttsPitch - 0.1f).coerceIn(0.5f, 2f)) }, { vm.setTtsPitch((s.ttsPitch + 0.1f).coerceIn(0.5f, 2f)) })
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = stringResource(R.string.tts_preview) + " ▶",
                style = t.button,
                color = c.text,
                modifier = Modifier
                    .clickable {
                        previewTts?.setSpeechRate(s.ttsRate)
                        previewTts?.setPitch(s.ttsPitch)
                        previewTts?.speak(
                            context.getString(R.string.tts_sample),
                            TextToSpeech.QUEUE_FLUSH, null, "preview",
                        )
                    }
                    .padding(8.dp),
            )
        }

        SectionLabel(stringResource(R.string.a11y_display_size))
        Segmented(
            options = listOf(
                stringResource(R.string.size_small),
                stringResource(R.string.size_default),
                stringResource(R.string.size_large),
                stringResource(R.string.size_xlarge),
            ),
            selectedIndex = s.displaySizeLevel,
        ) { vm.setDisplaySizeLevel(it) }

        SectionLabel(stringResource(R.string.settings_a11y))
        SwitchRow(stringResource(R.string.a11y_high_contrast), s.highContrast) { vm.setHighContrast(it) }
        SwitchRow(stringResource(R.string.a11y_large_touch), s.largeTouchTargets) { vm.setLargeTouchTargets(it) }
        SwitchRow(stringResource(R.string.a11y_reduce_anim), s.reduceAnimations) { vm.setReduceAnimations(it) }
        }
    }
}

// ---------- Notification settings (level 1) ----------

@Composable
fun NotificationScreen(onBack: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    val context = LocalContext.current

    val liveSupported = Build.VERSION.SDK_INT >= 36
    var liveCanPost by remember { mutableStateOf(com.intent.reader.notify.LiveUpdate.canPost(context)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        liveCanPost = com.intent.reader.notify.LiveUpdate.canPost(context)
        com.intent.reader.notify.LiveUpdate.refreshPermissions()
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                liveCanPost = com.intent.reader.notify.LiveUpdate.canPost(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsScaffold(title = stringResource(R.string.settings_notify), onBack = onBack) {
        SectionLabel(stringResource(R.string.channel_summary))
        SettingsGroup {
            SwitchRow(stringResource(R.string.notify_daily), s.dailySummaryEnabled) { vm.setDailySummaryEnabled(it) }
            if (s.dailySummaryEnabled) {
                StepperRow(
                    stringResource(R.string.notify_daily_time),
                    "%02d:%02d".format(s.dailySummaryHour, s.dailySummaryMinute),
                    { // -30min
                        var total = s.dailySummaryHour * 60 + s.dailySummaryMinute - 30
                        if (total < 0) total += 24 * 60
                        vm.setDailySummaryTime(total / 60, total % 60)
                    },
                    { // +30min
                        var total = s.dailySummaryHour * 60 + s.dailySummaryMinute + 30
                        if (total >= 24 * 60) total -= 24 * 60
                        vm.setDailySummaryTime(total / 60, total % 60)
                    },
                )
                Text(stringResource(R.string.notify_daily_time_hint).withCjkSpacing(), style = t.micro, color = c.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                StepperRow(
                    stringResource(R.string.notify_quiet),
                    "%02d:00–%02d:00".format(s.quietStartHour, s.quietEndHour),
                    { vm.setQuietHours((s.quietStartHour + 23) % 24, s.quietEndHour) },
                    { vm.setQuietHours((s.quietStartHour + 1) % 24, s.quietEndHour) },
                )
                Text(stringResource(R.string.notify_quiet_hint).withCjkSpacing(), style = t.micro, color = c.textSecondary, modifier = Modifier.padding(bottom = 12.dp))
            }
        }

        SectionLabel(stringResource(R.string.channel_live_update))
        SettingsGroup {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = c.text, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.notify_live).withCjkSpacing(), style = t.body, color = c.text)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.live_update_trigger_hint).withCjkSpacing(), style = t.micro, color = c.textSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = s.liveUpdateEnabled,
                enabled = liveSupported,
                onCheckedChange = { enabled ->
                    vm.setLiveUpdateEnabled(enabled)
                    if (enabled && Build.VERSION.SDK_INT >= 33 &&
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = c.text, checkedThumbColor = c.bg,
                    uncheckedTrackColor = c.divider, uncheckedThumbColor = c.bg, uncheckedBorderColor = c.divider,
                ),
            )
        }
        // Today's banked reading time, so the counter is visible even on devices
        // where the system does not promote the live card.
        val app = context.applicationContext as IntentApplication
        val todayLog by app.container.readingTracker.observeToday()
            .collectAsState(initial = null)
        val todayMinutes = todayLog?.minutes ?: 0
        Text(
            text = stringResource(
                R.string.reading_today_progress,
                todayMinutes,
                ((todayLog?.elapsedMillis ?: 0L) / 1_000 % 60).toInt(),
                s.readingGoalMinutes,
            ),
            style = t.meta,
            color = if (todayMinutes >= s.readingGoalMinutes) c.accent else c.textSecondary,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (!liveSupported) {
            Text(
                text = stringResource(R.string.live_update_unsupported).withCjkSpacing(),
                style = t.micro,
                color = c.textTertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else if (!liveCanPost) {
            Text(
                text = stringResource(R.string.live_update_permission_denied).withCjkSpacing(),
                style = t.micro,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(R.string.open_system_settings) + " →",
                style = t.button,
                color = c.text,
                modifier = Modifier
                    .clickable {
                        runCatching {
                            val action = if (com.intent.reader.notify.NotificationChannels.canPostNotifications(context)) {
                                Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS
                            } else Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            val intent = Intent(action).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(intent)
                        }
                    }
                    .padding(vertical = 8.dp),
            )
        }
        StepperRow(
            stringResource(R.string.notify_goal),
            "${s.readingGoalMinutes} min",
            { vm.setReadingGoalMinutes((s.readingGoalMinutes - 5).coerceAtLeast(5)) },
            { vm.setReadingGoalMinutes((s.readingGoalMinutes + 5).coerceAtMost(120)) },
        )
        }
    }
}

