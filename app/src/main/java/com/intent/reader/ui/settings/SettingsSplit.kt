package com.intent.reader.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intent.reader.R
import com.intent.reader.ui.theme.IntentTheme
import com.kyant.backdrop.Backdrop

/** Every page the right pane can host. */
private enum class SettingsPane {
    UI, ICON, FONT, WIDGET, LANGUAGE, THEME,
    A11Y, NOTIFY, USER, PRIVACY, PERMISSIONS, READING, ADVANCED, DEVELOPER, ABOUT, AI, STORAGE,
}

/**
 * Two-pane settings for tablets and unfolded foldables. The familiar index stays in the
 * left column while the chosen section occupies the right one, so browsing settings no
 * longer replaces the whole page with a sub-screen. Phones keep the single-page flow.
 */
@Composable
fun SettingsSplitScreen(
    widthSizeClass: WindowWidthSizeClass,
    popupBackdrop: Backdrop? = null,
    onExit: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenWeb: (title: String, url: String) -> Unit,
) {
    val c = IntentTheme.colors
    val activity = LocalContext.current as? android.app.Activity
    // A plain stack: deeper pages (界面 → 图标) render over their parent, and both the
    // system back gesture and the header arrow peel one level off.
    var stack by remember { mutableStateOf(listOf<SettingsPane>()) }
    // Tapping another section replaces the visible root instead of stacking it, so the
    // index keeps acting like an index and root pages never gain a back arrow.
    val select: (SettingsPane) -> Unit = { pane -> stack = listOf(pane) }
    val push: (SettingsPane) -> Unit = { pane -> stack = stack + pane }
    val pop: () -> Unit = { stack = stack.dropLast(1) }

    BackHandler(enabled = stack.isNotEmpty()) { pop() }

    Row(Modifier.fillMaxSize().background(c.bg)) {
        Box(
            Modifier
                .width(if (widthSizeClass == WindowWidthSizeClass.Expanded) 380.dp else 320.dp)
                .fillMaxHeight()
        ) {
            SettingsScreen(
                onBack = onExit,
                onOpenUi = { select(SettingsPane.UI) },
                onOpenA11y = { select(SettingsPane.A11Y) },
                onOpenNotify = { select(SettingsPane.NOTIFY) },
                onOpenUser = { select(SettingsPane.USER) },
                onOpenPrivacy = { select(SettingsPane.PRIVACY) },
                onOpenPermissions = { select(SettingsPane.PERMISSIONS) },
                onOpenAdvanced = { select(SettingsPane.ADVANCED) },
                onOpenAbout = { select(SettingsPane.ABOUT) },
                onOpenAi = { select(SettingsPane.AI) },
                onOpenStorage = { select(SettingsPane.STORAGE) },
            )
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(c.divider))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            val top = stack.lastOrNull()
            if (top == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.settings_pick_pane),
                        style = IntentTheme.typography.body,
                        color = c.textTertiary,
                    )
                }
            } else {
                // Level one drops its back arrow: the index beside it already says where
                // you are. Deeper pages keep the arrow to step back to their parent.
                CompositionLocalProvider(LocalSettingsPaneEmbedded provides (stack.size <= 1)) {
                    when (top) {
                        SettingsPane.UI -> UiSettingsScreen(
                            onBack = pop,
                            onOpenIcon = { push(SettingsPane.ICON) },
                            onOpenFont = { push(SettingsPane.FONT) },
                            onOpenWidget = { push(SettingsPane.WIDGET) },
                            onOpenLanguage = { push(SettingsPane.LANGUAGE) },
                            onOpenTheme = { push(SettingsPane.THEME) },
                        )
                        SettingsPane.ICON -> IconSettingsScreen(onBack = pop)
                        SettingsPane.FONT -> FontSettingsScreen(onBack = pop)
                        SettingsPane.WIDGET -> WidgetStyleSettingsScreen(onBack = pop)
                        SettingsPane.LANGUAGE -> LanguageSettingsScreen(
                            onBack = pop,
                            onRecreate = { activity?.recreate() },
                        )
                        SettingsPane.THEME -> ThemeSettingsScreen(onBack = pop)
                        SettingsPane.A11Y -> AccessibilityScreen(onBack = pop)
                        SettingsPane.NOTIFY -> NotificationScreen(onBack = pop)
                        SettingsPane.USER -> UserSettingsScreen(onBack = pop)
                        SettingsPane.PRIVACY -> PrivacyScreen(
                            onBack = pop,
                            onOpenSources = onOpenSources,
                            onOpenReadingHistory = { push(SettingsPane.READING) },
                            onOpenAi = { push(SettingsPane.AI) },
                        )
                        SettingsPane.READING -> ReadingHistoryScreen(onBack = pop)
                        SettingsPane.PERMISSIONS -> DevicePermissionScreen(onBack = pop)
                        SettingsPane.ADVANCED -> AdvancedMaterialScreen(onBack = pop)
                        SettingsPane.DEVELOPER -> DeveloperScreen(onBack = pop)
                        SettingsPane.ABOUT -> AboutScreen(
                            onBack = pop,
                            onOpenPage = onOpenWeb,
                            onOpenDeveloper = { push(SettingsPane.DEVELOPER) },
                        )
                        SettingsPane.AI -> AiSettingsScreen(onBack = pop, popupBackdrop = popupBackdrop)
                        SettingsPane.STORAGE -> StorageSettingsScreen(onBack = pop)
                    }
                }
            }
        }
    }
}
