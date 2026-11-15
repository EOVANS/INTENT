package com.intent.reader.ui.reader

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.StringRes
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.intent.reader.IntentApplication
import com.intent.reader.ai.SummaryLanguage
import com.intent.reader.ai.AiException
import com.intent.reader.ai.AiError
import com.intent.reader.ui.settings.aiErrorResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.intent.reader.R
import com.intent.reader.notes.*
import com.intent.reader.ui.components.HeaderIconButton
import com.intent.reader.ui.components.LocalHeaderMaterial
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.intent.reader.ui.theme.IntentTheme
import java.time.*
import java.time.format.TextStyle
import java.util.Locale

@Composable
private fun reportString(@StringRes id: Int, vararg args: Any): String =
    LocalContext.current.getString(id, *args)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReportScreen(onBack: () -> Unit) {
    val repository = (LocalContext.current.applicationContext as IntentApplication).container.notesRepository
    val state by repository.state.collectAsState()
    val zone = remember { ZoneId.systemDefault() }
    val dated = remember(state.notes, zone) { state.notes.groupBy { NoteReport.date(it, zone) } }
    var yearly by rememberSaveable { mutableStateOf(false) }
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val material = LocalHeaderMaterial.current
    val backdrop = if (Build.VERSION.SDK_INT >= 33 && material.liquidGlass) rememberLayerBackdrop() else null
    val statusTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val periods = remember(dated, yearly) {
        if (yearly) dated.keys.map { it.year.toString() }.distinct().sortedDescending()
        else dated.keys.map { YearMonth.from(it).toString() }.distinct().sortedDescending()
    }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier).background(c.bg),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = statusTop + 152.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            if (state.unreadableFiles > 0) item(key = "error") {
                Text(reportString(R.string.notes_load_error), color = c.accent, style = t.meta)
            }
            if (state.loaded && dated.isEmpty()) item(key = "empty") {
                Text(reportString(R.string.report_empty), color = c.textSecondary, style = t.bodySmall)
            }
            items(periods, key = { it }) { period ->
                val periodDates = remember(dated, period, yearly) { dated.filterKeys { if (yearly) it.year.toString() == period else YearMonth.from(it).toString() == period } }
                val notes = remember(periodDates) { periodDates.values.flatten() }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(period, style = t.sectionTitle, color = c.text)
                    if (yearly) {
                        YearCharts(periodDates, notes)
                    } else {
                        Text(reportString(R.string.report_summary, NoteReport.articleCount(notes), NoteArticles.characterCount(notes), periodDates.size), style = t.meta, color = c.textSecondary)
                        MonthGrid(YearMonth.parse(period), periodDates)
                    }
                    SourceBars(notes)
                    InsightCard(period, notes)
                }
            }
        }
        // Match the home/settings status-bar fade; no solid panel below it.
        Box(Modifier.fillMaxWidth().height(statusTop + 104.dp).background(
            Brush.verticalGradient(0f to c.bg, 0.6f to c.bg.copy(alpha = 0.85f), 1f to c.bg.copy(alpha = 0f)),
        ))
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, reportString(R.string.notes_back), onBack)
                Text(reportString(R.string.notes_report), style = t.pageTitle, color = c.text)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ReportPeriodToggle(yearly = yearly, onChange = { yearly = it }, backdrop = backdrop)
            }
        }
    }
}

@Composable
private fun ReportPeriodToggle(yearly: Boolean, onChange: (Boolean) -> Unit, backdrop: Backdrop?) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val material = LocalHeaderMaterial.current
    val advanced = Build.VERSION.SDK_INT >= 33 && material.liquidGlass
    val shape = RoundedCornerShape(50)
    val surface = if (backdrop != null) Modifier.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            blur((8.dp - 6.dp * material.transparency.coerceIn(0f, 1f)).toPx())
            lens(12.dp.toPx(), 14.dp.toPx(), chromaticAberration = false)
        },
        highlight = { Highlight(width = 0.6.dp, alpha = 0.55f) },
        shadow = null,
        onDrawSurface = {
            val tint = if (c.isDark) Color.White else Color.Black
            drawRect(tint.copy(alpha = 0.14f - 0.08f * material.transparency.coerceIn(0f, 1f)))
        },
    ).border(0.6.dp, c.text.copy(alpha = 0.12f), shape)
    else Modifier.clip(shape).background(c.surface).border(1.dp, c.divider, shape)
    Row(Modifier.widthIn(min = 200.dp).height(44.dp).then(surface).padding(4.dp).selectableGroup()) {
        listOf(false, true).forEach { isYear ->
            val selected = yearly == isYear
            Box(Modifier.weight(1f).fillMaxHeight().clip(shape)
                .background(if (selected) c.text.copy(alpha = if (advanced) 0.14f else 0.12f) else Color.Transparent, shape)
                .selectable(selected = selected, role = Role.Tab, onClick = { onChange(isYear) }),
                contentAlignment = Alignment.Center) {
                Text(reportString(if (isYear) R.string.report_year else R.string.report_month),
                    style = t.body, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) c.text else c.textSecondary)
            }
        }
    }
}
@Composable
private fun MonthGrid(month: YearMonth, days: Map<LocalDate, List<IslandNote>>) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val locale = LocalContext.current.resources.configuration.locales[0] ?: Locale.getDefault()
    val offset = month.atDay(1).dayOfWeek.value - 1
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row { (1..7).forEach { day -> Text(DayOfWeek.of(day).getDisplayName(TextStyle.NARROW, locale), Modifier.weight(1f), textAlign = TextAlign.Center, color = c.textSecondary, style = t.micro) } }
        val count = ((offset + month.lengthOfMonth() + 6) / 7) * 7
        (0 until count).chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                week.forEach { index ->
                    val day = index - offset + 1
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (day in 1..month.lengthOfMonth()) {
                            val value = NoteReport.articleCount(days[month.atDay(day)].orEmpty())
                            Text(day.toString(), style = t.micro, color = c.textSecondary)
                            HeatCell(value, Modifier.fillMaxWidth().aspectRatio(1f))
                        } else Spacer(Modifier.fillMaxWidth().aspectRatio(0.75f))
                    }
                }
            }
        }
    }
}

@Composable
private fun YearCharts(days: Map<LocalDate, List<IslandNote>>, notes: List<IslandNote>) {
    val monthly = remember(days) { (1..12).map { month -> days.filterKeys { it.monthValue == month } } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        YearChart(NoteReport.articleCount(notes).toLong(), reportString(R.string.report_chart_notes),
            monthly.map { NoteReport.articleCount(it.values.flatten()).toLong() }, Color(0xFF4BCD89))
        YearChart(NoteArticles.characterCount(notes), reportString(R.string.report_chart_characters),
            monthly.map { NoteArticles.characterCount(it.values.flatten()) }, Color(0xFF7797F6))
        YearChart(days.size.toLong(), reportString(R.string.report_chart_days),
            monthly.map { it.size.toLong() }, Color(0xFFE98E8E))
    }
}

@Composable
private fun YearChart(total: Long, label: String, values: List<Long>, barColor: Color) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val max = values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.card).padding(16.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(java.text.NumberFormat.getIntegerInstance(LocalContext.current.resources.configuration.locales[0]).format(total), style = t.sectionTitle.copy(fontWeight = FontWeight.Bold), color = c.text)
            Text(label, style = t.meta, color = c.text, modifier = Modifier.padding(bottom = 1.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Row(Modifier.weight(1f).height(104.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.Bottom) {
                values.forEach { value ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        if (value > 0) Box(Modifier.fillMaxWidth(.72f).fillMaxHeight((value.toFloat() / max).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(barColor))
                    }
                }
            }
            Column(Modifier.width(44.dp).height(104.dp).padding(start = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Text(java.text.NumberFormat.getIntegerInstance(LocalContext.current.resources.configuration.locales[0]).format(max), style = t.micro, color = c.textSecondary)
                Text(java.text.NumberFormat.getIntegerInstance(LocalContext.current.resources.configuration.locales[0]).format(max / 2), style = t.micro, color = c.textSecondary)
                Text("0", style = t.micro, color = c.textSecondary)
            }
        }
        Row(Modifier.fillMaxWidth().padding(end = 44.dp)) {
            (1..12).forEach { month -> Text(month.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, style = t.micro, color = c.textSecondary, maxLines = 1) }
        }
    }
}
@Composable
private fun HeatCell(count: Int, modifier: Modifier) {
    val c = IntentTheme.colors
    val color = if (count == 0) c.textSecondary.copy(alpha = 0.18f) else c.accent.copy(alpha = when (count) { 1 -> .38f; 2 -> .55f; 3 -> .72f; else -> .9f })
    Box(modifier.clip(RoundedCornerShape(5.dp)).background(color))
}

@Composable
private fun InsightCard(period: String, notes: List<IslandNote>) {
    val context = LocalContext.current
    val container = (context.applicationContext as IntentApplication).container
    val config by container.aiConfigStore.config.collectAsState()
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val scope = rememberCoroutineScope()
    var result by remember(period, notes) { mutableStateOf<String?>(null) }
    var error by remember(period, notes) { mutableStateOf<Int?>(null) }
    var loading by remember(period, notes) { mutableStateOf(false) }
    val language = SummaryLanguage.resolve(context.resources.configuration.locales[0]?.toLanguageTag().orEmpty(), "")
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(c.card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(reportString(R.string.report_ai_title), style = t.sectionTitle, color = c.text)
        Text(reportString(R.string.report_ai_description), style = t.bodySmall, color = c.textSecondary)
        Text(reportString(R.string.report_ai_disclosure), style = t.micro, color = c.textSecondary)
        if (!config.enabled) Text(reportString(R.string.report_ai_config), style = t.meta, color = c.textSecondary)
        if (config.enabled && !config.dietEnabled) Text(reportString(R.string.report_ai_disabled), style = t.meta, color = c.textSecondary)
        Button(enabled = config.enabled && config.dietEnabled && !loading && notes.isNotEmpty(), onClick = {
            loading = true
            error = null
            result = null
            scope.launch {
                try { result = container.aiRepository.noteInsight(notes, period, language) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { error = if ((failure as? AiException)?.reason == AiError.TOO_LONG) R.string.report_ai_too_long else aiErrorResource(failure) }
                finally { loading = false }
            }
        }) { Text(reportString(if (loading) R.string.report_ai_loading else if (result == null) R.string.report_ai_generate else R.string.report_ai_regenerate)) }
        error?.let { Text(reportString(it), style = t.meta, color = c.accent) }
        result?.let { Text(it, style = t.bodySmall, color = c.text) }
    }
}
@Composable
private fun SourceBars(notes: List<IslandNote>) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val sources = remember(notes) { NoteReport.sources(notes) }
    val total = NoteReport.articleCount(notes)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(reportString(R.string.report_sources), style = t.sectionTitle, color = c.text)
        Text(reportString(R.string.report_sources_hint), style = t.micro, color = c.textSecondary)
        sources.forEach { source ->
            val fraction = source.count.toFloat() / total
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(source.title.ifBlank { reportString(R.string.report_unknown_source) }, Modifier.weight(1f), style = t.meta, color = c.text, maxLines = 1)
                    Text("${source.count} · ${kotlin.math.round(fraction * 100).toInt()}%", style = t.meta, color = c.textSecondary)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.textSecondary.copy(alpha = .15f))) {
                    Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(c.accent))
                }
            }
        }
    }
}
