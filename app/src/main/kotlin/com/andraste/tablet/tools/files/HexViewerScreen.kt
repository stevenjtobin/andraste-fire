package com.andraste.tablet.tools.files

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val HEX_MAX_BYTES = 64 * 1024

@Composable
fun HexViewerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var fileName by remember { mutableStateOf<String?>(null) }
    var fileSize by remember { mutableStateOf<Long?>(null) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var truncated by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        error = null
        bytes = null
        var name = "file"
        var size = -1L
        try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val si = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (ni >= 0) name = c.getString(ni) ?: name
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        } catch (_: Exception) {
        }
        fileName = name
        fileSize = if (size >= 0) size else null
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val stream = ctx.contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure<ByteArray>(Exception("Cannot open file"))
                    stream.use { ins ->
                        val out = java.io.ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        var total = 0
                        while (total < HEX_MAX_BYTES) {
                            val n = ins.read(buf, 0, minOf(buf.size, HEX_MAX_BYTES - total))
                            if (n < 0) break
                            out.write(buf, 0, n)
                            total += n
                        }
                        Result.success(out.toByteArray())
                    }
                } catch (e: Exception) {
                    Result.failure<ByteArray>(e)
                }
            }
            busy = false
            result.onSuccess { arr ->
                bytes = arr
                truncated = (fileSize ?: arr.size.toLong()) > arr.size.toLong()
            }.onFailure { error = it.message ?: "Read failed" }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "Hex Viewer", subtitle = fileName ?: "pick a file", onBack = onBack)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { try { picker.launch("*/*") } catch (e: Exception) { error = e.message } },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen)
            ) { Text(if (busy) "Reading…" else "Pick a file") }
            fileSize?.let {
                Text(fmtSizeHex(it), style = IceniTypography.bodySmall, modifier = Modifier.weight(1f))
            }
        }

        if (busy) LinearProgressIndicator(
            color = IceniGold, trackColor = IceniGreenLight,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
        error?.let {
            Text("Error: $it", style = IceniTypography.bodyMedium, color = IceniRed,
                modifier = Modifier.padding(12.dp))
        }
        if (truncated) {
            Text(
                "Showing first ${HEX_MAX_BYTES / 1024} KB only",
                style = IceniTypography.bodySmall, color = IceniAmber,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        val data = bytes
        if (data != null) {
            val rowCount = (data.size + 15) / 16
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp)
            ) {
                items(rowCount) { rowIdx ->
                    Text(
                        text = hexRow(data, rowIdx * 16),
                        style = IceniTypography.bodySmall,
                        color = IceniTextMuted
                    )
                }
            }
        } else if (!busy) {
            Text(
                "No file loaded. Pick a file to view its bytes as a hex dump.",
                style = IceniTypography.bodyMedium, color = IceniTextMuted,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun hexRow(data: ByteArray, offset: Int): String {
    val sb = StringBuilder()
    sb.append("%08X  ".format(offset))
    val ascii = StringBuilder()
    for (i in 0 until 16) {
        val idx = offset + i
        if (idx < data.size) {
            val v = data[idx].toInt() and 0xFF
            sb.append("%02x ".format(v))
            ascii.append(if (v in 32..126) v.toChar() else '.')
        } else {
            sb.append("   ")
            ascii.append(' ')
        }
        if (i == 7) sb.append(' ')
    }
    sb.append(" |").append(ascii).append('|')
    return sb.toString()
}

private fun fmtSizeHex(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
} + " ($bytes bytes)"
