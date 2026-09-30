package com.andraste.tablet.tools.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

@Composable
fun DnsToolsScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("example.com") }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "DNS Tools",
            subtitle = if (busy) "resolving…" else "forward · reverse · DNS-over-HTTPS",
            onBack = onBack
        )

        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = host, onValueChange = { host = it },
                label = { Text("Hostname or IP", color = IceniTextMuted) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceniText, unfocusedTextColor = IceniText,
                    focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    busy = true; output = ""
                    scope.launch { output = runDnsTools(host.trim()); busy = false }
                },
                enabled = !busy && host.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("LOOKUP", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }

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

private suspend fun runDnsTools(host: String): String = withContext(Dispatchers.IO) {
    val sb = StringBuilder()

    sb.append("== FORWARD (getAllByName) ==\n")
    try {
        val addrs = InetAddress.getAllByName(host)
        if (addrs.isEmpty()) sb.append("(none)\n")
        else addrs.forEach { sb.append("  ${it.hostAddress}\n") }
    } catch (e: Exception) { sb.append("  ERROR: ${e.message}\n") }

    sb.append("\n== REVERSE (canonicalHostName) ==\n")
    try {
        val name = InetAddress.getByName(host).canonicalHostName
        sb.append("  $name\n")
    } catch (e: Exception) { sb.append("  ERROR: ${e.message}\n") }

    sb.append("\n== DNS-over-HTTPS (dns.google, A) ==\n")
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
        val req = Request.Builder()
            .url("https://dns.google/resolve?name=$host&type=A")
            .header("Accept", "application/dns-json")
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            val json = JsonParser.parseString(body).asJsonObject
            val status = json.get("Status")?.asInt ?: -1
            sb.append("  Status: $status\n")
            val answers = json.getAsJsonArray("Answer")
            if (answers == null || answers.size() == 0) {
                sb.append("  (no A records)\n")
            } else {
                answers.forEach { a ->
                    val o = a.asJsonObject
                    val type = o.get("type")?.asInt ?: -1
                    val data = o.get("data")?.asString ?: ""
                    val label = if (type == 1) "A" else "type $type"
                    sb.append("  [$label] $data\n")
                }
            }
        }
    } catch (e: Exception) { sb.append("  ERROR: ${e.message}\n") }

    sb.toString()
}
