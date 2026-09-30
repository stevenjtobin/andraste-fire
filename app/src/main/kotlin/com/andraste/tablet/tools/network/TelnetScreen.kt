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
fun TelnetScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("23") }
    var line by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Telnet",
            subtitle = if (busy) "connecting…" else "raw socket request / response",
            onBack = onBack
        )

        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                telnetField(host, { host = it }, "Host / IP", Modifier.weight(2f))
                telnetField(port, { port = it }, "Port", Modifier.weight(1f))
            }
            telnetField(line, { line = it }, "Line to send (optional)", Modifier.fillMaxWidth())

            Button(
                onClick = {
                    busy = true; output = ""
                    scope.launch {
                        output = telnetExchange(host, port.toIntOrNull() ?: 23, line)
                        busy = false
                    }
                },
                enabled = !busy && host.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("CONNECT & SEND", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }

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
private fun telnetField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
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

private suspend fun telnetExchange(host: String, port: Int, line: String): String = withContext(Dispatchers.IO) {
    try {
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), 5000)
            s.soTimeout = 3000
            if (line.isNotEmpty()) {
                s.getOutputStream().apply { write((line + "\r\n").toByteArray()); flush() }
            }
            val buf = ByteArray(4096)
            val sb = StringBuilder()
            val ins = s.getInputStream()
            try {
                while (true) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    sb.append(String(buf, 0, n, Charsets.ISO_8859_1))
                    if (sb.length > 16384) break
                }
            } catch (_: SocketTimeoutException) { /* end of burst */ }
            if (sb.isEmpty()) "[connected — no data received]" else sb.toString()
        }
    } catch (e: Exception) { "ERROR: ${e.message}" }
}
