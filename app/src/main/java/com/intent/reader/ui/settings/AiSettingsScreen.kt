package com.intent.reader.ui.settings

import android.os.Build
import androidx.compose.foundation.border
import com.intent.reader.ui.components.LocalHeaderMaterial
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.intent.reader.ui.theme.withCjkSpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.intent.reader.IntentApplication
import com.intent.reader.R
import com.intent.reader.ai.AiConfig
import com.intent.reader.ai.AiError
import com.intent.reader.ai.AiException
import com.intent.reader.ai.AiProvider
import com.intent.reader.ai.editBaseUrl
import com.intent.reader.ai.selectProvider
import com.intent.reader.ui.theme.IntentTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun aiErrorResource(error: Throwable): Int = when ((error as? AiException)?.reason) {
    AiError.CONFIG -> R.string.ai_error_config
    AiError.URL -> R.string.ai_error_url
    AiError.EMPTY -> R.string.ai_error_empty
    AiError.TOO_LONG -> R.string.ai_error_long
    AiError.AUTH -> R.string.ai_error_auth
    AiError.RATE_LIMIT -> R.string.ai_error_rate
    AiError.RESPONSE -> R.string.ai_error_response
    AiError.QUOTE -> R.string.ai_error_quote
    AiError.STORAGE -> R.string.ai_error_storage
    AiError.SERVICE -> R.string.ai_error_service
    else -> R.string.ai_error_network
}

@Composable
fun AiSettingsScreen(onBack: () -> Unit, popupBackdrop: Backdrop? = null) {
    val container = (LocalContext.current.applicationContext as IntentApplication).container
    val stored by container.aiConfigStore.config.collectAsState()
    // Deliberately not saveable: API keys must not enter saved-instance-state bundles.
    var draft by remember(stored) { mutableStateOf(stored) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Int?>(null) }
    var providerMenu by remember { mutableStateOf(false) }
    val provider = AiProvider.fromId(draft.providerId)
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val colors = IntentTheme.colors
    val material = LocalHeaderMaterial.current
    val menuShape = RoundedCornerShape(20.dp)
    val glassMenu = Build.VERSION.SDK_INT >= 33 && material.liquidGlass && popupBackdrop != null
    val menuModifier = if (glassMenu) Modifier.drawBackdrop(
        backdrop = popupBackdrop!!,
        shape = { menuShape },
        effects = {
            blur((10.dp - 4.dp * material.transparency.coerceIn(0f, 1f)).toPx())
            lens(12.dp.toPx(), 14.dp.toPx(), chromaticAberration = false)
        },
        highlight = { Highlight(width = 0.6.dp, alpha = 0.55f) },
        shadow = null,
        onDrawSurface = {
            val opacity = 0.48f - 0.20f * material.transparency.coerceIn(0f, 1f)
            drawRect((if (colors.isDark) Color.Black else Color.White).copy(alpha = opacity))
        },
    ).border(0.6.dp, (if (colors.isDark) Color.White else Color.Black).copy(alpha = if (colors.isDark) 0.16f else 0.10f), menuShape) else Modifier
    fun perform(message: Int, action: suspend () -> Unit) {
        if (busy) return
        busy = true
        status = null
        scope.launch {
            try { action(); status = message }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { status = aiErrorResource(error) }
            finally { busy = false }
        }
    }
    // Toggle rows save immediately. A not-yet-complete setup makes `save` reject
    // the write; surface that as a message instead of letting the coroutine
    // exception tear down the screen.
    fun persist(next: AiConfig) {
        scope.launch {
            try { container.aiConfigStore.save(next) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                draft = container.aiConfigStore.config.value
                status = aiErrorResource(error)
            }
        }
    }

    SettingsScaffold(title = stringResource(R.string.ai_settings), onBack = onBack) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(IntentTheme.colors.surface).padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(Color(0xFF9B70EF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.ai_settings), style = IntentTheme.typography.pageTitle, fontWeight = FontWeight.Bold, color = IntentTheme.colors.text)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.ai_intro).withCjkSpacing(), style = IntentTheme.typography.bodySmall, color = IntentTheme.colors.textSecondary, textAlign = TextAlign.Center)
        }
        SectionLabel(stringResource(R.string.ai_service_section))
        Text(stringResource(R.string.ai_privacy), style = IntentTheme.typography.meta, color = IntentTheme.colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
        SwitchRow(stringResource(R.string.ai_enabled), draft.enabled, !busy) { want ->
            draft = draft.copy(enabled = want); status = null
            // A toggle is a setting of its own: apply it without a separate save tap.
            val next = draft
            persist(next)
        }
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(enabled = !busy, onClick = { keyboard?.hide(); providerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.ai_provider_label, stringResource(provider.label)), modifier = Modifier.weight(1f))
                Text("▾")
            }
            DropdownMenu(
                expanded = providerMenu, onDismissRequest = { providerMenu = false },
                modifier = menuModifier, shape = menuShape,
                containerColor = if (glassMenu) Color.Transparent else colors.surface,
                shadowElevation = if (glassMenu) 0.dp else 3.dp, tonalElevation = 0.dp,
            ) {
                AiProvider.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.label)) },
                        onClick = { draft = draft.selectProvider(option); providerMenu = false; status = null },
                    )
                }
            }
        }
        Text(stringResource(R.string.ai_provider_hint), style = IntentTheme.typography.meta, color = IntentTheme.colors.textSecondary,
            modifier = Modifier.padding(vertical = 8.dp))
        if (provider.hint != R.string.ai_provider_hint) {
            Text(stringResource(provider.hint), style = IntentTheme.typography.meta, color = IntentTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.baseUrl, onValueChange = { draft = draft.editBaseUrl(it); status = null },
                label = { Text(stringResource(R.string.ai_url)) },
                placeholder = { Text("https://your-provider.example/v1") },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            OutlinedTextField(
                value = draft.model, onValueChange = { draft = draft.copy(model = it); status = null },
                label = { Text(stringResource(R.string.ai_model)) },
                supportingText = { Text(stringResource(R.string.ai_model_hint)) },
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.apiKey, onValueChange = { draft = draft.copy(apiKey = it); status = null },
                label = { Text(stringResource(R.string.ai_key)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            if (provider == AiProvider.CLAUDE || draft.baseUrl.toHttpUrlOrNull()?.host == "api.anthropic.com") {
                OutlinedTextField(
                    value = draft.workspaceId, onValueChange = { draft = draft.copy(workspaceId = it); status = null },
                    label = { Text(stringResource(R.string.ai_workspace)) },
                    singleLine = true, enabled = !busy, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Text(stringResource(R.string.ai_save_hint), style = IntentTheme.typography.micro, color = IntentTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp))
        Row {
            TextButton(enabled = !busy, onClick = { perform(R.string.ai_saved) { container.aiConfigStore.save(draft) } }) {
                Text(stringResource(R.string.ai_save))
            }
            TextButton(enabled = !busy, onClick = { perform(R.string.ai_test_ok) { container.aiRepository.testConnection(draft) } }) {
                Text(stringResource(R.string.ai_test))
            }
        }
        if (busy) Text(stringResource(R.string.ai_loading), color = IntentTheme.colors.textSecondary)
        status?.let { Text(stringResource(it), color = IntentTheme.colors.text, modifier = Modifier.padding(vertical = 12.dp)) }
        SectionLabel(stringResource(R.string.ai_features_section))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surface).padding(horizontal = 16.dp, vertical = 6.dp)) {
            AiFeatureRow(Icons.Filled.FormatQuote, Color(0xFF9B70EF), stringResource(R.string.ai_feature_quote), stringResource(R.string.ai_feature_quote_desc)) {
                Switch(
                    checked = draft.quoteEnabled,
                    enabled = draft.enabled && !busy,
                    onCheckedChange = { want ->
                        draft = draft.copy(quoteEnabled = want); status = null
                        // Daily quote is live too — no save button needed.
                        val next = draft
                        persist(next)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = colors.text, checkedThumbColor = colors.bg, uncheckedTrackColor = colors.divider, uncheckedThumbColor = colors.bg, uncheckedBorderColor = colors.divider),
                )
            }
            AiFeatureRow(Icons.Filled.PieChart, Color(0xFF19AA86), stringResource(R.string.ai_feature_diet), stringResource(R.string.ai_feature_diet_desc)) {
                Text(stringResource(R.string.ai_feature_unsupported), style = IntentTheme.typography.micro, color = colors.textSecondary)
            }
            AiFeatureRow(Icons.Filled.AutoStories, Color(0xFF2587FF), stringResource(R.string.ai_feature_read), stringResource(R.string.ai_feature_read_desc))
        }
        SectionLabel(stringResource(R.string.ai_data_section))
        TextButton(enabled = !busy, onClick = { perform(R.string.ai_cleared) { container.aiRepository.clearCache() } }) {
            Text(stringResource(R.string.ai_clear_cache))
        }
        TextButton(enabled = !busy, onClick = { perform(R.string.ai_cleared) { container.aiConfigStore.clear() } }) {
            Text(stringResource(R.string.ai_clear_config))
        }
    }
}

@Composable
private fun AiFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = IntentTheme.colors
    val typography = IntentTheme.typography
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp).padding(top = 2.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title.withCjkSpacing(), style = typography.body, fontWeight = FontWeight.SemiBold, color = colors.text, modifier = Modifier.weight(1f))
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(description.withCjkSpacing(), style = typography.bodySmall, color = colors.textSecondary)
        }
    }
}
