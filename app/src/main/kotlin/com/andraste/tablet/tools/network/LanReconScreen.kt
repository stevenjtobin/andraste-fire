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

private data class LanHost(val ip: String, val name: String)

@Composable
fun LanReconScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var prefix by remember { mutableStateOf(lanSubnetPrefix(ctx)) }
    var hosts by remember { mutableStateOf<List<LanHost>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "LAN Recon",
            subtitle = if (scanning) "sweeping ${prefix}.0/24…" else "${hosts.size} live hosts",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (scanning) {
                        job?.cancel(); scanning = false
                    } else {
                        hosts = emptyList(); progress = 0f; scanning = true
                        job = scope.launch(Dispatchers.IO) {
                            var done = 0
                            for (chunk in (1..254).chunked(32)) {
                                if (!isActive) break
                                val live = chunk.map { i ->
                                    async {
                                        val ip = "$prefix.$i"
                                        try {
                                            if (InetAddress.getByName(ip).isReachable(300)) ip else null
                                        } catch (_: Exception) { null }
                                    }
                                }.awaitAll().filterNotNull()
                                for (ip in live) {
                                    val name = try { InetAddress.getByName(ip).canonicalHostName } catch (_: Exception) { ip }
                                    hosts = (hosts + LanHost(ip, name)).sortedBy { h ->
                                        h.ip.substringAfterLast('.').toIntOrNull() ?: 0
                                    }
                                }
                                done += chunk.size
                                progress = done / 254f
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
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(h.ip, style = IceniTypography.titleMedium, color = IceniTeal, modifier = Modifier.width(150.dp))
                    Text(
                        if (h.name == h.ip) "(no reverse DNS)" else h.name,
                        style = IceniTypography.bodyMedium, modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun lanSubnetPrefix(ctx: Context): String {
    return try {
        val wifi = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifi.dhcpInfo.ipAddress
        if (ip == 0) "192.168.1"
        else Formatter.formatIpAddress(ip).substringBeforeLast('.')
    } catch (_: Exception) { "192.168.1" }
}
