package com.andraste.tablet.tools.network

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

private data class MappedHost(val ip: String, val openPorts: List<Int>)

private val NETMAP_PORTS = listOf(22, 80, 443, 445, 8080)

@Composable
fun NetMapScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var prefix by remember { mutableStateOf(netMapSubnetPrefix(ctx)) }
    var hosts by remember { mutableStateOf<List<MappedHost>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var phase by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Net Map",
            subtitle = if (scanning) phase else "${hosts.size} hosts mapped",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (scanning) {
                        job?.cancel(); scanning = false
                    } else {
                        hosts = emptyList(); progress = 0f; scanning = true
                        job = scope.launch(Dispatchers.IO) {
                            phase = "ping sweep…"
                            val live = mutableListOf<String>()
                            var done = 0
                            for (chunk in (1..254).chunked(32)) {
                                if (!isActive) break
                                val found = chunk.map { i ->
                                    async {
                                        val ip = "$prefix.$i"
                                        try { if (InetAddress.getByName(ip).isReachable(300)) ip else null }
                                        catch (_: Exception) { null }
                                    }
                                }.awaitAll().filterNotNull()
                                live += found
                                done += chunk.size
                                progress = (done / 254f) * 0.5f
                            }
                            phase = "port probe (${live.size} hosts)…"
                            live.sortedBy { it.substringAfterLast('.').toIntOrNull() ?: 0 }
                                .forEachIndexed { idx, ip ->
                                    if (!isActive) return@launch
                                    val open = NETMAP_PORTS.filter { port ->
                                        try {
                                            Socket().use { s -> s.connect(InetSocketAddress(ip, port), 500); true }
                                        } catch (_: Exception) { false }
                                    }
                                    hosts = hosts + MappedHost(ip, open)
                                    progress = 0.5f + ((idx + 1f) / live.size.coerceAtLeast(1)) * 0.5f
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
            value = prefix, onValueChange = { prefix = it },
            label = { Text("Subnet /24 prefix (e.g. 192.168.1)", color = IceniTextMuted) },
            singleLine = true, enabled = !scanning,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = IceniText, unfocusedTextColor = IceniText,
                focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
            ),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )

        if (scanning) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = IceniGold, trackColor = IceniGreenLight
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(hosts) { h ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(h.ip, style = IceniTypography.titleMedium, color = IceniTeal)
                    Text(
                        if (h.openPorts.isEmpty()) "no common ports open"
                        else "open: " + h.openPorts.joinToString(", ") { "$it/${portName(it)}" },
                        style = IceniTypography.bodyMedium,
                        color = if (h.openPorts.isEmpty()) IceniTextMuted else IceniText
                    )
                }
            }
        }
    }
}

private fun portName(p: Int) = when (p) {
    22 -> "ssh"; 80 -> "http"; 443 -> "https"; 445 -> "smb"; 8080 -> "http-alt"; else -> "?"
}

@Suppress("DEPRECATION")
private fun netMapSubnetPrefix(ctx: Context): String {
    return try {
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifi.dhcpInfo.ipAddress
        if (ip == 0) "192.168.1"
        else Formatter.formatIpAddress(ip).substringBeforeLast('.')
    } catch (_: Exception) { "192.168.1" }
}
