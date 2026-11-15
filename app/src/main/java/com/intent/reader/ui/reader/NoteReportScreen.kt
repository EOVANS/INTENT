package com.intent.reader.ui.reader

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.intent.reader.IntentApplication
import com.intent.reader.R
import com.intent.reader.notes.*
import com.intent.reader.ui.components.HeaderIconButton
import com.intent.reader.ui.theme.IntentTheme
import java.time.*
import java.time.format.TextStyle
import java.util.Locale

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
    Column(Modifier.fillMaxSize().background(c.bg)) {
        TopAppBar(title = { Text(stringResource(R.string.notes_report), style = t.meta) },
            navigationIcon = { HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.notes_back), onBack) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bg, titleContentColor = c.text))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center) {
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(selected = !yearly, onClick = { yearly = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.report_month)) }
                SegmentedButton(selected = yearly, onClick = { yearly = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.report_year)) }
            }
        }
        if (state.unreadableFiles > 0) Text(stringResource(R.string.notes_load_error), Modifier.padding(16.dp), color = c.accent, style = t.meta)
        if (state.loaded && dated.isEmpty()) {
            Text(stringResource(R.string.report_empty), Modifier.padding(24.dp), color = c.textSecondary, style = t.bodySmall)
        } else {
            val periods = remember(dated, yearly) {
                if (yearly) dated.keys.map { it.year.toString() }.distinct().sortedDescending()
                else dated.keys.map { YearMonth.from(it).toString() }.distinct().sortedDescending()
            }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(36.dp)) {
                items(periods, key = { it }) { period ->
                    val periodDates = remember(dated, period, yearly) { dated.filterKeys { if (yearly) it.year.toString() == period else YearMonth.from(it).toString() == period } }
                    val notes = remember(periodDates) { periodDates.values.flatten() }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(period, style = t.sectionTitle, color = c.text)
                        Text(stringResource(R.string.report_summary, NoteReport.articleCount(notes), NoteArticles.characterCount(notes), periodDates.size), style = t.meta, color = c.textSecondary)
                        if (yearly) YearGrid(period.toInt(), periodDates) else MonthGrid(YearMonth.parse(period), periodDates)
                        SourceBars(notes)
                    }
                }
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
private fun YearGrid(year: Int, days: Map<LocalDate, List<IslandNote>>) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..12).chunked(4).forEach { months ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                months.forEach { month ->
                    val count = NoteReport.articleCount(days.filterKeys { it.year == year && it.monthValue == month }.values.flatten())
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(month.toString(), style = t.micro, color = c.textSecondary)
                        Box(contentAlignment = Alignment.Center) {
                            HeatCell(count, Modifier.fillMaxWidth().height(38.dp))
                            Text(count.toString(), style = t.meta, color = c.text)
                        }
                    }
                }
            }
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
private fun SourceBars(notes: List<IslandNote>) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val sources = remember(notes) { NoteReport.sources(notes) }
    val total = NoteReport.articleCount(notes)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(stringResource(R.string.report_sources), style = t.sectionTitle, color = c.text)
        Text(stringResource(R.string.report_sources_hint), style = t.micro, color = c.textSecondary)
        sources.forEach { source ->
            val fraction = source.count.toFloat() / total
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(source.title.ifBlank { stringResource(R.string.report_unknown_source) }, Modifier.weight(1f), style = t.meta, color = c.text, maxLines = 1)
                    Text("${source.count} · ${kotlin.math.round(fraction * 100).toInt()}%", style = t.meta, color = c.textSecondary)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.textSecondary.copy(alpha = .15f))) {
                    Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(c.accent))
                }
            }
        }
    }
}
