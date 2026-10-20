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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.intent.reader.IntentApplication
import com.intent.reader.R
import com.intent.reader.data.prefs.SettingsState
import com.intent.reader.data.prefs.ThemeMode
import com.intent.reader.ui.theme.AccentPresets
import com.intent.reader.ui.theme.IntentDimens
import com.intent.reader.ui.theme.IntentTheme
import com.intent.reader.ui.theme.withCjkSpacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/** Settings ViewModel: single source of truth over SettingsState. */
class SettingsViewModel(app: IntentApplication) : ViewModel() {
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
    fun setThemeMode(m: ThemeMode) = launch { repo.setThemeMode(m) }
    fun setAccentColor(argb: Long) = launch { repo.setAccentColor(argb) }
    fun setFontChoice(v: String) = launch { repo.setFontChoice(v) }
    fun setLanguageTag(v: String) = launch { repo.setLanguageTag(v) }
    fun setTextScaleLevel(v: Int) = launch { repo.setTextScaleLevel(v) }
    fun setFollowSystemFontScale(v: Boolean) = launch { repo.setFollowSystemFontScale(v) }
    fun setDisplaySizeLevel(v: Int) = launch { repo.setDisplaySizeLevel(v) }
    fun setHighContrast(v: Boolean) = launch { repo.setHighContrast(v) }
    fun setLargeTouchTargets(v: Boolean) = launch { repo.setLargeTouchTargets(v) }
    fun setReduceAnimations(v: Boolean) = launch { repo.setReduceAnimations(v) }
    fun setGlassEffectEnabled(v: Boolean) = launch { repo.setGlassEffectEnabled(v) }
    fun setGlassMaterial(v: String) = launch { repo.setGlassMaterial(v) }
    fun setGlassAdaptiveTint(v: Boolean) = launch { repo.setGlassAdaptiveTint(v) }
    fun setGlassScrollEdgeBlur(v: Boolean) = launch { repo.setGlassScrollEdgeBlur(v) }
    fun setTtsEngine(v: String) = launch { repo.setTtsEngine(v) }
    fun setTtsRate(v: Float) = launch { repo.setTtsRate(v) }
    fun setTtsPitch(v: Float) = launch { repo.setTtsPitch(v) }
    fun setDailySummaryEnabled(v: Boolean) = launch { repo.setDailySummaryEnabled(v) }
    fun setDailySummaryTime(h: Int, m: Int) = launch { repo.setDailySummaryTime(h, m) }
    fun setQuietHours(start: Int, end: Int) = launch { repo.setQuietHours(start, end) }
    fun setLiveUpdateEnabled(v: Boolean) = launch { repo.setLiveUpdateEnabled(v) }
    fun setReadingGoalMinutes(v: Int) = launch { repo.setReadingGoalMinutes(v) }
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

@Composable
private fun SettingsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
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
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = null,
                        tint = c.text,
                    )
                }
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
                .fillMaxSize()
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
private fun SectionLabel(text: String) {
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
private fun EntryRow(title: String, value: String? = null, onClick: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
private fun SwitchRow(title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
}

// ---------- root settings ----------

@Composable
fun SettingsScreen(
    onOpenUi: () -> Unit,
    onOpenA11y: () -> Unit,
    onOpenNotify: () -> Unit,
    onOpenUser: () -> Unit,
    onOpenAdvanced: () -> Unit,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAbout by remember { mutableStateOf(false) }
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

    SettingsScaffold(title = stringResource(R.string.settings_title)) {
        SectionLabel(stringResource(R.string.settings_title))
        EntryRow(stringResource(R.string.settings_ui), onClick = onOpenUi)
        EntryRow(stringResource(R.string.settings_a11y), onClick = onOpenA11y)
        EntryRow(stringResource(R.string.settings_notify), onClick = onOpenNotify)
        EntryRow(
            title = stringResource(R.string.settings_user),
            value = s.userName.ifBlank { null },
            onClick = onOpenUser,
        )
        EntryRow(stringResource(R.string.settings_advanced), onClick = onOpenAdvanced)

        SectionLabel(stringResource(R.string.sync_freq))
        Segmented(
            options = syncLabels,
            selectedIndex = syncOptions.indexOf(s.syncIntervalMin).let { if (it >= 0) it else 1 },
        ) { vm.setSyncIntervalMin(syncOptions[it]) }
        SwitchRow(stringResource(R.string.sync_wifi_only), s.syncWifiOnly) { vm.setSyncWifiOnly(it) }
        SwitchRow(stringResource(R.string.wifi_only_images), s.wifiOnlyImages) { vm.setWifiOnlyImages(it) }

        SectionLabel(stringResource(R.string.retain_settings))
        Segmented(
            options = retainOptions.map { "$it 天" },
            selectedIndex = retainOptions.indexOf(s.retainDays).let { if (it >= 0) it else 1 },
        ) { vm.setRetainDays(retainOptions[it]) }

        SectionLabel(stringResource(R.string.import_opml) + " / " + stringResource(R.string.export_opml))
        EntryRow(stringResource(R.string.import_opml)) { opmlImport.launch(arrayOf("application/xml", "text/xml", "text/plain", "*/*")) }
        EntryRow(stringResource(R.string.export_opml)) { opmlExport.launch("intent-subscriptions.opml") }

        SectionLabel(stringResource(R.string.about))
        EntryRow(stringResource(R.string.about)) { showAbout = true }
        EntryRow(stringResource(R.string.licenses)) { showLicenses = true }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.filter_all)) }
            },
            title = { Text("INTENT", fontFamily = androidx.compose.ui.text.font.FontFamily.Serif) },
            text = {
                Text("v1.0\n\n" + stringResource(R.string.onboarding_slogan_zh))
            },
            containerColor = c.card,
        )
    }
    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) { Text(stringResource(R.string.filter_all)) }
            },
            title = { Text(stringResource(R.string.licenses)) },
            text = {
                Text(
                    "Jetpack Compose · Room · DataStore · WorkManager · Coil · OkHttp · kotlinx.serialization\nApache License 2.0"
                )
            },
            containerColor = c.card,
        )
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

    SettingsScaffold(title = stringResource(R.string.settings_advanced), onBack = onBack) {
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
        // Regular = adaptive, readable glass; Clear = high transparency + a
        // darkening layer (Apple's two material variants).
        Text(
            text = stringResource(R.string.glass_material).withCjkSpacing(),
            style = t.body,
            color = if (on) c.text else c.textTertiary,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
        )
        Segmented(
            options = listOf(
                stringResource(R.string.glass_material_regular),
                stringResource(R.string.glass_material_clear),
            ),
            selectedIndex = if (s.glassMaterial == "clear") 1 else 0,
            onSelect = { vm.setGlassMaterial(if (it == 1) "clear" else "regular") },
        )
        SwitchRow(
            title = stringResource(R.string.glass_adaptive_tint),
            checked = s.glassAdaptiveTint,
            enabled = on && s.glassMaterial != "clear",
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

// ---------- UI settings (level 1) ----------

@Composable
fun UiSettingsScreen(onBack: () -> Unit, onRecreate: () -> Unit) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val vm = settingsVm()
    val s = vm.settings
    val context = LocalContext.current

    val fontImport = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val dir = java.io.File(context.filesDir, "fonts").apply { mkdirs() }
        val dest = java.io.File(dir, "custom_font" +
            (context.contentResolver.getType(uri)?.takeIf { it.contains("otf") }?.let { ".otf" } ?: ".ttf"))
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            vm.setFontChoice("custom:${dest.absolutePath}")
            Toast.makeText(context, R.string.font_imported, Toast.LENGTH_SHORT).show()
        }
    }

    SettingsScaffold(title = stringResource(R.string.settings_ui), onBack = onBack) {
        SectionLabel(stringResource(R.string.ui_icon))
        val iconChoices = listOf(
            "default" to stringResource(R.string.icon_default),
            "black" to stringResource(R.string.icon_black),
            "white" to stringResource(R.string.icon_white),
            "serif" to stringResource(R.string.icon_serif),
        )
        Segmented(
            options = iconChoices.map { it.second },
            selectedIndex = 0, // system-level, not persisted in DataStore
        ) { i ->
            setAppIcon(context, iconChoices[i].first)
            Toast.makeText(context, R.string.icon_restart_hint, Toast.LENGTH_LONG).show()
        }

        SectionLabel(stringResource(R.string.ui_font))
        Segmented(
            options = listOf(
                stringResource(R.string.font_system),
                stringResource(R.string.font_serif),
            ),
            selectedIndex = if (s.fontChoice == "serif") 1 else 0,
        ) { i ->
            vm.setFontChoice(if (i == 1) "serif" else "system")
        }
        EntryRow(stringResource(R.string.font_import)) { fontImport.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "*/*")) }
        Text(
            text = stringResource(R.string.font_note).withCjkSpacing(),
            style = t.micro,
            color = c.textTertiary,
            modifier = Modifier.padding(top = 4.dp),
        )

        SectionLabel(stringResource(R.string.ui_accent))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentPresets.forEach { color ->
                val isSel = s.accentColor == color.toArgbLong()
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
                        ) { vm.setAccentColor(color.toArgbLong()) },
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

        SectionLabel(stringResource(R.string.ui_language))
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
    LaunchedEffect(s.ttsEngine) {
        previewTts?.shutdown()
        previewTts = null
        val tts = TextToSpeech(context, { }, s.ttsEngine.ifBlank { null })
        delay(400)
        engines = tts.engines ?: emptyList()
        previewTts = tts
    }
    DisposableEffect(Unit) {
        onDispose { previewTts?.shutdown() }
    }

    SettingsScaffold(title = stringResource(R.string.settings_a11y), onBack = onBack) {
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
                            if (selected) c.text else c.surface,
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

        SectionLabel(stringResource(R.string.a11y_font_size))
        SwitchRow(stringResource(R.string.a11y_follow_system_font), s.followSystemFontScale) { vm.setFollowSystemFontScale(it) }
        if (!s.followSystemFontScale) {
            Segmented(
                options = listOf(
                    stringResource(R.string.size_small),
                    stringResource(R.string.size_default),
                    stringResource(R.string.size_large),
                    stringResource(R.string.size_xlarge),
                ),
                selectedIndex = s.textScaleLevel,
            ) { vm.setTextScaleLevel(it) }
        }

        SectionLabel(stringResource(R.string.settings_a11y))
        SwitchRow(stringResource(R.string.a11y_high_contrast), s.highContrast) { vm.setHighContrast(it) }
        SwitchRow(stringResource(R.string.a11y_large_touch), s.largeTouchTargets) { vm.setLargeTouchTargets(it) }
        SwitchRow(stringResource(R.string.a11y_reduce_anim), s.reduceAnimations) { vm.setReduceAnimations(it) }
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
    val liveCanPost = liveSupported && com.intent.reader.notify.LiveUpdate.canPost(context)

    SettingsScaffold(title = stringResource(R.string.settings_notify), onBack = onBack) {
        SectionLabel(stringResource(R.string.channel_summary))
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
            StepperRow(
                stringResource(R.string.notify_quiet),
                "%02d:00–%02d:00".format(s.quietStartHour, s.quietEndHour),
                { vm.setQuietHours((s.quietStartHour + 23) % 24, s.quietEndHour) },
                { vm.setQuietHours((s.quietStartHour + 1) % 24, s.quietEndHour) },
            )
        }

        SectionLabel(stringResource(R.string.channel_live_update))
        SwitchRow(
            stringResource(R.string.notify_live),
            s.liveUpdateEnabled,
            enabled = liveSupported,
        ) { vm.setLiveUpdateEnabled(it) }
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
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
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
