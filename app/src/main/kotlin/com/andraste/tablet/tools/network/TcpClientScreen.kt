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
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

@Composable
fun TcpClientScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("80") }
    var payload by remember { mutableStateOf("GET / HTTP/1.0") }
    var hexInput by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "TCP Client",
            subtitle = if (busy) "sending…" else "hex + ASCII response viewer",
            onBack = onBack
        )

        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tcpField(host, { host = it }, "Host / IP", Modifier.weight(2f))
                tcpField(port, { port = it }, "Port", Modifier.weight(1f))
            }
            tcpField(payload, { payload = it }, if (hexInput) "Payload (hex bytes)" else "Payload (text)", Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = hexInput, onCheckedChange = { hexInput = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = IceniGold, checkedTrackColor = IceniGoldDim,
                        uncheckedThumbColor = IceniTextMuted, uncheckedTrackColor = IceniGreenLight
                    )
                )
                Text("  Interpret payload as hex", style = IceniTypography.bodyMedium)
            }

            Button(
                onClick = {
                    busy = true; output = ""
                    scope.launch {
                        output = tcpExchange(host, port.toIntOrNull() ?: 0, payload, hexInput)
                        busy = false
                    }
                },
                enabled = !busy && host.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("SEND", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }

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
private fun tcpField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, color = IceniTextMuted) }, singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = IceniText, unfocusedTextColor = IceniText,
            focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
        ),
        modifier = modifier
    )
}

private fun parseHex(s: String): ByteArray {
    val clean = s.filter { !it.isWhitespace() }
    val n = clean.length / 2
    val out = ByteArray(n)
    for (i in 0 until n) {
        out[i] = clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
    return out
}

private fun hexDump(data: ByteArray, len: Int): String {
    val sb = StringBuilder()
    var i = 0
    while (i < len) {
        sb.append(String.format("%04X  ", i))
        for (j in i until i + 16) {
            if (j < len) sb.append(String.format("%02X ", data[j])) else sb.append("   ")
        }
        sb.append(" |")
        var j = i
        while (j < i + 16 && j < len) {
            val c = data[j].toInt() and 0xFF
            sb.append(if (c in 32..126) c.toChar() else '.')
            j++
        }
        sb.append("|\n")
        i += 16
    }
    return sb.toString()
}

private suspend fun tcpExchange(host: String, port: Int, payload: String, hex: Boolean): String = withContext(Dispatchers.IO) {
    try {
        val bytes = if (hex) parseHex(payload) else (payload + "\r\n\r\n").toByteArray(Charsets.ISO_8859_1)
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), 5000)
            s.soTimeout = 3000
            s.getOutputStream().apply { write(bytes); flush() }
            val buf = ByteArray(8192)
            var total = 0
            val ins = s.getInputStream()
            try {
                while (total < buf.size) {
                    val n = ins.read(buf, total, buf.size - total)
                    if (n < 0) break
                    total += n
                }
            } catch (_: SocketTimeoutException) { /* burst complete */ }
            if (total == 0) "[connected — no data received]"
            else "Received $total bytes:\n\n" + hexDump(buf, total)
        }
    } catch (e: Exception) {
        if (e is NumberFormatException) "ERROR: invalid hex payload" else "ERROR: ${e.message}"
    }
}
