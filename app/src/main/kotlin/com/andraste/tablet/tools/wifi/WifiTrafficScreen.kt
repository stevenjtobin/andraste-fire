package com.andraste.tablet.tools.wifi

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * WiFi "traffic" monitor. Re-scans roughly every 5s, tracking per-SSID sighting
 * count and last RSSI, plus 2.4 GHz channel occupancy (APs per channel 1-13).
 * Note: this is beacon-presence monitoring, not packet capture (no monitor mode).
 */
@Composable
fun WifiTrafficScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var running by remember { mutableStateOf(false) }
    val sightings = remember { mutableStateMapOf<String, TrafficStat>() }
    var sweeps by remember { mutableStateOf(0) }

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults
    } catch (_: Exception) { emptyList() }

    fun ingest(list: List<ScanResult>) {
        for (ap in list) {
            val key = if (ap.SSID.isNotEmpty()) ap.SSID else (ap.BSSID ?: "")
            if (key.isEmpty()) continue
            val prev = sightings[key]
            sightings[key] = TrafficStat(
                label = key,
                count = (prev?.count ?: 0) + 1,
                lastRssi = ap.level,
                channel = traffFreqToChannel(ap.frequency),
                is24 = ap.frequency in 2401..2495
            )
        }
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        while (isActive && running) {
            try { @Suppress("DEPRECATION") wm.startScan() } catch (_: Exception) {}
            delay(1200)
            ingest(readCached())
            sweeps++
            delay(3800)
        }
    }

    // Channel occupancy for 2.4GHz channels 1..13
    val channelCounts = remember(sightings.size, sweeps) {
        val map = IntArray(14)
        sightings.values.forEach { if (it.is24 && it.channel in 1..13) map[it.channel]++ }
        map
    }
    val maxCh = (channelCounts.maxOrNull() ?: 1).coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "WiFi Traffic",
            subtitle = "${sightings.size} SSIDs · $sweeps sweeps",
            onBack = onBack,
            actions = {
                IconButton(onClick = { running = !running }) {
                    Icon(
                        if (running) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = if (running) "Stop" else "Start",
                        tint = IceniGold
                    )
                }
            }
        )
        if (running) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniTeal, trackColor = IceniGreenLight)

        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("2.4 GHz channel occupancy", style = IceniTypography.labelLarge)
            Spacer(Modifier.height(4.dp))
            for (ch in 1..13) {
                val c = channelCounts[ch]
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.dp)) {
                    Text("ch %2d".format(ch), style = IceniTypography.bodySmall, modifier = Modifier.width(44.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.05f + 0.9f * (c.toFloat() / maxCh))
                            .height(12.dp)
                            .background(
                                if (c >= maxCh && c > 0) IceniAmber else IceniTeal,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Text(" $c", style = IceniTypography.bodySmall)
                }
            }
        }

        HorizontalDivider(color = IceniGoldDim)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val sorted = sightings.values.sortedByDescending { it.count }
            items(sorted, key = { it.label }) { s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(s.label, style = IceniTypography.titleMedium, modifier = Modifier.weight(1f))
                    Text("×${s.count}  ch${s.channel}  ${s.lastRssi}dBm", style = IceniTypography.bodySmall, color = IceniTeal)
                }
            }
        }
    }
}

private data class TrafficStat(
    val label: String,
    val count: Int,
    val lastRssi: Int,
    val channel: Int,
    val is24: Boolean
)

private fun traffFreqToChannel(f: Int): Int = when {
    f == 2484 -> 14
    f in 2412..2472 -> (f - 2407) / 5
    f in 5150..5895 -> (f - 5000) / 5
    f in 5925..7125 -> (f - 5950) / 5
    else -> -1
}
