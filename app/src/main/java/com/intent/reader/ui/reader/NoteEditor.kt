package com.intent.reader.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.intent.reader.R
import com.intent.reader.ui.theme.IntentTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Shared editor for a reader highlight and a saved note, with errors kept beside the draft. */
@Composable
fun NoteEditor(
    quote: String,
    initialRemark: String,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Unit,
    onDelete: (suspend () -> Unit)? = null,
) {
    val c = IntentTheme.colors
    val t = IntentTheme.typography
    val scope = rememberCoroutineScope()
    var remark by rememberSaveable { mutableStateOf(initialRemark) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        failed = false
        scope.launch {
            try { action(); onDismiss() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
            finally { busy = false }
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = c.bg,
        title = { Text(stringResource(R.string.island_notes), style = t.pageTitle, color = c.text) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(quote, style = t.bodySmall, color = c.textSecondary)
                OutlinedTextField(
                    value = remark, onValueChange = { remark = it }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                    label = { Text(stringResource(R.string.notes_remark)) },
                    placeholder = { Text(stringResource(R.string.notes_remark_hint)) },
                )
                if (failed) Text(stringResource(R.string.notes_save_error), color = c.accent, style = t.meta)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { perform { onSave(remark) } }) {
                Text(stringResource(R.string.notes_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(enabled = !busy, onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.delete))
                }
                TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        },
    )
    if (confirmDelete && onDelete != null) AlertDialog(
        onDismissRequest = { if (!busy) confirmDelete = false },
        title = { Text(stringResource(R.string.notes_delete_title)) },
        text = { Text(stringResource(R.string.notes_delete_message)) },
        confirmButton = {
            TextButton(enabled = !busy, onClick = { confirmDelete = false; perform(onDelete) }) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = { confirmDelete = false }) { Text(stringResource(R.string.close)) }
        },
    )
}
