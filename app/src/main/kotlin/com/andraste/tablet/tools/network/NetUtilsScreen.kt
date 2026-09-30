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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

@Composable
fun NetUtilsScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("1.1.1.1") }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun run(which: String, action: suspend () -> String) {
        busy = true; mode = which; output = ""
        scope.launch { output = action(); busy = false }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Net Utils",
            subtitle = if (busy) "running $mode…" else "ping · traceroute · whois",
            onBack = onBack
        )

        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = host, onValueChange = { host = it },
                label = { Text("Host / IP / domain", color = IceniTextMuted) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceniText, unfocusedTextColor = IceniText,
                    focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                toolButton("PING", !busy && host.isNotBlank(), Modifier.weight(1f)) {
                    run("ping") { doPing(host.trim()) }
                }
                toolButton("TRACE", !busy && host.isNotBlank(), Modifier.weight(1f)) {
                    run("traceroute") { doTraceroute(host.trim()) }
                }
                toolButton("WHOIS", !busy && host.isNotBlank(), Modifier.weight(1f)) {
                    run("whois") { doWhois(host.trim()) }
                }
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

@Composable
private fun toolButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
        modifier = modifier
    ) { Text(label, style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }
}

private fun execCapture(cmd: Array<String>): String {
    return try {
        val p = Runtime.getRuntime().exec(cmd)
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        p.waitFor()
        (out + err).trim()
    } catch (e: Exception) { "ERROR: ${e.message}" }
}

private suspend fun doPing(host: String): String = withContext(Dispatchers.IO) {
    val res = execCapture(arrayOf("/system/bin/ping", "-c", "4", host))
    if (res.isBlank()) "[no output]" else res
}

private suspend fun doTraceroute(host: String): String = withContext(Dispatchers.IO) {
    val sb = StringBuilder("traceroute-lite to $host (ttl 1..20)\n\n")
    for (ttl in 1..20) {
        val out = execCapture(arrayOf("/system/bin/ping", "-c", "1", "-W", "1", "-t", ttl.toString(), host))
        val ipMatch = Regex("""(?:from |From )(\d{1,3}(?:\.\d{1,3}){3})""").find(out)
        val hopIp = ipMatch?.groupValues?.get(1) ?: "*"
        val reached = out.contains("bytes from") && !out.contains("Time to live exceeded", ignoreCase = true)
        sb.append(String.format("%2d  %s\n", ttl, hopIp))
        if (reached) { sb.append("\n[reached target]\n"); break }
    }
    sb.toString()
}

private suspend fun doWhois(host: String): String = withContext(Dispatchers.IO) {
    try {
        Socket().use { s ->
            s.connect(InetSocketAddress("whois.iana.org", 43), 6000)
            s.soTimeout = 6000
            s.getOutputStream().apply { write((host + "\r\n").toByteArray()); flush() }
            val sb = StringBuilder()
            val buf = ByteArray(4096)
            val ins = s.getInputStream()
            try {
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    sb.append(String(buf, 0, n, Charsets.ISO_8859_1))
                    if (sb.length > 32768) break
                }
            } catch (_: SocketTimeoutException) {}
            if (sb.isEmpty()) "[no whois data]" else sb.toString()
        }
    } catch (e: Exception) { "ERROR: ${e.message}" }
}
