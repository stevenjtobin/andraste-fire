package com.andraste.tablet.tools.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.net.InetSocketAddress
import java.net.Socket

data class PortResult(val port: Int, val open: Boolean, val banner: String = "")

val COMMON_PORTS = listOf(
    21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 3306, 3389, 5432,
    8080, 8443, 8888, 9200, 27017, 6379, 1883, 5672, 5900
)

@Composable
fun PortScanScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("192.168.1.1") }
    var results by remember { mutableStateOf<List<PortResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "Port Scanner",
            subtitle = if (scanning) "Scanning…" else "${results.count { it.open }} open ports",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (scanning) {
                        job?.cancel(); scanning = false
                    } else {
                        results = emptyList()
                        scanning = true
                        job = scope.launch(Dispatchers.IO) {
                            val ports = COMMON_PORTS
                            ports.forEachIndexed { idx, port ->
                                if (!isActive) return@launch
                                val open = try {
                                    Socket().use { s ->
                                        s.connect(InetSocketAddress(host, port), 800)
                                        true
                                    }
                                } catch (_: Exception) { false }
                                val banner = if (open) grabBanner(host, port) else ""
                                results = results + PortResult(port, open, banner)
                                progress = (idx + 1f) / ports.size
                            }
                            scanning = false
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (scanning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null, tint = IceniGold
                    )
                }
            }
        )

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Target host / IP", color = IceniTextMuted) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = IceniText,
                unfocusedTextColor = IceniText,
                focusedBorderColor = IceniGold,
                unfocusedBorderColor = IceniGoldDim,
                cursorColor = IceniGold
            ),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )

        if (scanning) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = IceniGold,
                trackColor = IceniGreenLight
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(results.filter { it.open }) { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "  PORT ${r.port}",
                        style = IceniTypography.titleMedium,
                        color = IceniTeal,
                        modifier = Modifier.width(120.dp)
                    )
                    Text(
                        text = r.banner.ifEmpty { portService(r.port) },
                        style = IceniTypography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun grabBanner(host: String, port: Int): String = try {
    Socket().use { s ->
        s.soTimeout = 800
        s.connect(InetSocketAddress(host, port), 800)
        s.getInputStream().bufferedReader().readLine()?.take(60) ?: ""
    }
} catch (_: Exception) { "" }

private fun portService(port: Int) = mapOf(
    21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP", 53 to "DNS",
    80 to "HTTP", 110 to "POP3", 143 to "IMAP", 443 to "HTTPS",
    445 to "SMB", 3306 to "MySQL", 3389 to "RDP", 5432 to "PostgreSQL",
    8080 to "HTTP-Alt", 8443 to "HTTPS-Alt", 9200 to "Elasticsearch",
    27017 to "MongoDB", 6379 to "Redis", 1883 to "MQTT", 5900 to "VNC"
)[port] ?: "unknown"
