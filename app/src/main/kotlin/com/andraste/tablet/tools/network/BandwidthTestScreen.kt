package com.andraste.tablet.tools.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

@Composable
fun BandwidthTestScreen(onBack: () -> Unit) {
    var url by remember { mutableStateOf("https://speed.hetzner.de/1MB.bin") }
    var result by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var progressBytes by remember { mutableStateOf(0L) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Bandwidth Test",
            subtitle = if (running) "downloading… ${progressBytes / 1024} KB" else "HTTP download throughput",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (running) {
                        job?.cancel(); running = false
                    } else {
                        result = ""; progressBytes = 0L; running = true
                        job = scope.launch(Dispatchers.IO) {
                            result = runBandwidth(url.trim()) { progressBytes = it }
                            running = false
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (running) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null, tint = IceniGold
                    )
                }
            }
        )

        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("Download URL", color = IceniTextMuted) }, singleLine = true, enabled = !running,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceniText, unfocusedTextColor = IceniText,
                    focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (running) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = IceniGold, trackColor = IceniGreenLight
                )
            }

            if (result.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(result, style = IceniTypography.bodyLarge, color = IceniText)
                }
            }
        }
    }
}

private suspend fun runBandwidth(url: String, onProgress: (Long) -> Unit): String = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        val req = Request.Builder().url(url).build()
        val start = System.nanoTime()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext "ERROR: HTTP ${resp.code}"
            val stream = resp.body?.byteStream() ?: return@withContext "ERROR: empty body"
            val buf = ByteArray(16 * 1024)
            var total = 0L
            while (isActive) {
                val n = stream.read(buf)
                if (n < 0) break
                total += n
                onProgress(total)
            }
            if (!isActive) return@withContext "cancelled after ${total / 1024} KB"
            val elapsedSec = (System.nanoTime() - start) / 1_000_000_000.0
            val mbps = if (elapsedSec > 0) (total * 8.0) / (elapsedSec * 1_000_000.0) else 0.0
            val mBps = if (elapsedSec > 0) total / (elapsedSec * 1_000_000.0) else 0.0
            buildString {
                append("Downloaded: ${total / 1024} KB (${"%.2f".format(total / 1_000_000.0)} MB)\n")
                append("Elapsed:    ${"%.2f".format(elapsedSec)} s\n\n")
                append("Throughput: ${"%.2f".format(mbps)} Mbps\n")
                append("            ${"%.2f".format(mBps)} MB/s")
            }
        }
    } catch (e: Exception) {
        if (e is CancellationException) "cancelled" else "ERROR: ${e.message}"
    }
}
