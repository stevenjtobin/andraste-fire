package com.andraste.tablet.tools.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Composable
fun HttpProbeScreen(onBack: () -> Unit) {
    var url by remember { mutableStateOf("https://example.com") }
    var method by remember { mutableStateOf("GET") }
    var menuOpen by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val methods = listOf("GET", "HEAD", "POST")

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "HTTP Probe",
            subtitle = if (busy) "requesting…" else "status · headers · body preview",
            onBack = onBack
        )

        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = url, onValueChange = { url = it },
                label = { Text("URL", color = IceniTextMuted) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceniText, unfocusedTextColor = IceniText,
                    focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IceniGold)
                    ) { Text("METHOD: $method", style = IceniTypography.labelLarge) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        methods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m, style = IceniTypography.bodyLarge) },
                                onClick = { method = m; menuOpen = false }
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        busy = true; output = ""
                        scope.launch { output = httpProbe(url.trim(), method); busy = false }
                    },
                    enabled = !busy && url.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                    modifier = Modifier.weight(1f)
                ) { Text("PROBE", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }
            }

            if (output.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(output, style = IceniTypography.bodyMedium, color = IceniText)
                }
            }
        }
    }
}

private suspend fun httpProbe(url: String, method: String): String = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val builder = Request.Builder().url(url)
        when (method) {
            "HEAD" -> builder.head()
            "POST" -> builder.post("".toRequestBody(null))
            else -> builder.get()
        }

        client.newCall(builder.build()).execute().use { resp ->
            val sb = StringBuilder()
            sb.append("$method $url\n")
            sb.append("── STATUS ──\n")
            sb.append("HTTP ${resp.code} ${resp.message}\n")
            if (resp.priorResponse != null) {
                sb.append("(followed redirect(s); final URL: ${resp.request.url})\n")
            } else {
                sb.append("(no redirects)\n")
            }
            sb.append("\n── HEADERS ──\n")
            for (i in 0 until resp.headers.size) {
                sb.append("${resp.headers.name(i)}: ${resp.headers.value(i)}\n")
            }
            if (method != "HEAD") {
                sb.append("\n── BODY (first 2KB) ──\n")
                val stream = resp.body?.byteStream()
                if (stream != null) {
                    val buf = ByteArray(2048)
                    var total = 0
                    while (total < buf.size) {
                        val n = stream.read(buf, total, buf.size - total)
                        if (n < 0) break
                        total += n
                    }
                    sb.append(String(buf, 0, total, Charsets.UTF_8))
                    if (total >= buf.size) sb.append("\n…(truncated)")
                } else {
                    sb.append("(empty body)")
                }
            }
            sb.toString()
        }
    } catch (e: Exception) { "ERROR: ${e.message}" }
}
