package com.andraste.tablet.tools.files

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

private val HASH_ALGOS = listOf("MD5", "SHA-1", "SHA-256", "SHA-512")

@Composable
fun HashCalculatorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var fileName by remember { mutableStateOf<String?>(null) }
    var fileSize by remember { mutableStateOf<Long?>(null) }
    var fileHashes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var textInput by remember { mutableStateOf("") }
    var textHashes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        error = null
        fileHashes = emptyMap()
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
                    val digests = HASH_ALGOS.associateWith { MessageDigest.getInstance(it) }
                    val stream = ctx.contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure<Map<String, String>>(Exception("Cannot open file"))
                    stream.use { ins ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = ins.read(buf)
                            if (n < 0) break
                            digests.values.forEach { it.update(buf, 0, n) }
                        }
                    }
                    Result.success(digests.mapValues { (_, md) -> md.digest().toHexLower() })
                } catch (e: Exception) {
                    Result.failure<Map<String, String>>(e)
                }
            }
            busy = false
            result.onSuccess { fileHashes = it }
                .onFailure { error = it.message ?: "Hash failed" }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "Hash Calculator", subtitle = "MD5 / SHA-1 / SHA-256 / SHA-512", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- File hashing ---
            Text("FILE", style = IceniTypography.labelLarge)
            Button(
                onClick = { try { picker.launch("*/*") } catch (e: Exception) { error = e.message } },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen)
            ) { Text(if (busy) "Hashing…" else "Pick a file") }

            if (fileName != null) {
                Card(colors = CardDefaults.cardColors(containerColor = IceniGreen), shape = RoundedCornerShape(6.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(fileName ?: "", style = IceniTypography.titleMedium)
                        Text(
                            "size: " + (fileSize?.let { fmtBytes(it) } ?: "unknown"),
                            style = IceniTypography.bodySmall
                        )
                    }
                }
            }
            if (busy) LinearProgressIndicator(color = IceniGold, trackColor = IceniGreenLight)
            error?.let { Text("Error: $it", style = IceniTypography.bodyMedium, color = IceniRed) }

            HASH_ALGOS.forEach { algo ->
                fileHashes[algo]?.let { hex ->
                    HashCard(algo, hex) { clipboard.setText(AnnotatedString(hex)) }
                }
            }

            HorizontalDivider(color = IceniGoldDim)

            // --- Text hashing ---
            Text("TEXT", style = IceniTypography.labelLarge)
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("String to hash", color = IceniTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceniText,
                    unfocusedTextColor = IceniText,
                    focusedBorderColor = IceniGold,
                    unfocusedBorderColor = IceniGoldDim,
                    cursorColor = IceniGold
                )
            )
            Button(
                onClick = {
                    scope.launch {
                        textHashes = withContext(Dispatchers.IO) {
                            try {
                                val bytes = textInput.toByteArray(Charsets.UTF_8)
                                HASH_ALGOS.associateWith {
                                    MessageDigest.getInstance(it).digest(bytes).toHexLower()
                                }
                            } catch (e: Exception) {
                                emptyMap()
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen)
            ) { Text("Hash text") }

            HASH_ALGOS.forEach { algo ->
                textHashes[algo]?.let { hex ->
                    HashCard(algo, hex) { clipboard.setText(AnnotatedString(hex)) }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HashCard(algo: String, hex: String, onCopy: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = IceniGreen), shape = RoundedCornerShape(6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(algo, style = IceniTypography.labelLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onCopy) { Text("COPY", color = IceniTeal, style = IceniTypography.labelSmall) }
            }
            SelectionContainer {
                Text(hex, style = IceniTypography.bodyMedium, color = IceniText)
            }
        }
    }
}

private fun ByteArray.toHexLower(): String {
    val sb = StringBuilder(size * 2)
    for (b in this) {
        val v = b.toInt() and 0xFF
        sb.append("0123456789abcdef"[v ushr 4])
        sb.append("0123456789abcdef"[v and 0x0F])
    }
    return sb.toString()
}

private fun fmtBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
    else -> "$bytes B"
} + " ($bytes bytes)"
