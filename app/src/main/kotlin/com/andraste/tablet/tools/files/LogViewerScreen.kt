package com.andraste.tablet.tools.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val LOG_TAIL_LINES = 500

@Composable
fun LogViewerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val dir = remember { File(ctx.getExternalFilesDir(null), "andraste") }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var selected by remember { mutableStateOf<File?>(null) }
    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var filter by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        files = try {
            if (!dir.exists()) emptyList()
            else (dir.listFiles()?.filter { it.isFile } ?: emptyList())
                .sortedByDescending { it.lastModified() }
        } catch (e: Exception) {
            error = e.message; emptyList()
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun open(f: File) {
        selected = f
        filter = ""
        busy = true
        error = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val all = f.readLines()
                    Result.success(if (all.size > LOG_TAIL_LINES) all.takeLast(LOG_TAIL_LINES) else all)
                } catch (e: Exception) {
                    Result.failure<List<String>>(e)
                }
            }
            busy = false
            result.onSuccess { lines = it }.onFailure { error = it.message ?: "Read failed" }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        val cur = selected
        IceniHeader(
            title = if (cur == null) "Log Viewer" else cur.name,
            subtitle = if (cur == null) dir.absolutePath else "last $LOG_TAIL_LINES lines",
            onBack = { if (cur == null) onBack() else { selected = null; lines = emptyList() } }
        )

        if (cur == null) {
            if (files.isEmpty()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No log files yet.", style = IceniTypography.titleMedium, color = IceniTextMuted)
                    Text(
                        "Scan output and logs written to\n${dir.absolutePath}\nwill appear here.",
                        style = IceniTypography.bodyMedium, color = IceniTextHint
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(files) { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(IceniGreen, RoundedCornerShape(6.dp))
                                .clickable { open(f) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text("📄 ", style = IceniTypography.bodyLarge)
                            Text(f.name, style = IceniTypography.bodyLarge, color = IceniText,
                                modifier = Modifier.weight(1f))
                            Text(fmtSizeLog(f.length()), style = IceniTypography.bodySmall)
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                label = { Text("Filter / highlight", color = IceniTextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceniText,
                    unfocusedTextColor = IceniText,
                    focusedBorderColor = IceniGold,
                    unfocusedBorderColor = IceniGoldDim,
                    cursorColor = IceniGold
                )
            )
            if (busy) LinearProgressIndicator(
                color = IceniGold, trackColor = IceniGreenLight,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            error?.let {
                Text("Error: $it", style = IceniTypography.bodyMedium, color = IceniRed,
                    modifier = Modifier.padding(12.dp))
            }
            val q = filter.trim()
            val shown = if (q.isEmpty()) lines else lines.filter { it.contains(q, ignoreCase = true) }
            if (shown.isEmpty() && !busy) {
                Text(
                    if (lines.isEmpty()) "(empty file)" else "No lines match \"$q\"",
                    style = IceniTypography.bodyMedium, color = IceniTextMuted,
                    modifier = Modifier.padding(12.dp)
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(shown) { line ->
                    Text(
                        text = highlight(line, q),
                        style = IceniTypography.bodySmall,
                        color = IceniTextMuted
                    )
                }
            }
        }
    }
}

private fun highlight(line: String, query: String) = buildAnnotatedString {
    if (query.isEmpty()) {
        append(line)
        return@buildAnnotatedString
    }
    var start = 0
    val lower = line.lowercase()
    val q = query.lowercase()
    while (true) {
        val idx = lower.indexOf(q, start)
        if (idx < 0) {
            append(line.substring(start))
            break
        }
        append(line.substring(start, idx))
        withStyle(SpanStyle(color = IceniDeepGreen, background = IceniGold)) {
            append(line.substring(idx, idx + query.length))
        }
        start = idx + query.length
    }
}

private fun fmtSizeLog(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
