package com.andraste.tablet.tools.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WifiScanScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("…") }
    val scope = rememberCoroutineScope()

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults.sortedByDescending { it.level }
    } catch (_: SecurityException) { emptyList() }

    // Show whatever the system already has, immediately.
    LaunchedEffect(Unit) {
        results = readCached()
        status = if (results.isEmpty()) "Tap ↻ to scan" else "${results.size} networks (cached)"
    }

    // Broadcast receiver for fresh scan completion
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                results = readCached()
                scanning = false
                status = "${results.size} networks found"
            }
        }
        ctx.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        onDispose { ctx.unregisterReceiver(receiver) }
    }

    fun triggerScan() {
        scanning = true
        status = "Scanning…"
        @Suppress("DEPRECATION")
        val ok = wm.startScan()
        // Fire OS throttles startScan(); the completion broadcast may never
        // arrive. Fall back to cached results after a short timeout so the UI
        // never hangs on "Scanning…".
        scope.launch {
            delay(6000)
            if (scanning) {
                results = readCached()
                scanning = false
                status = when {
                    results.isNotEmpty() -> "${results.size} networks (${if (ok) "scan" else "cached — throttled"})"
                    else -> "No results — enable Location & retry"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "WiFi Scanner",
            subtitle = status,
            onBack = onBack,
            actions = {
                IconButton(onClick = { triggerScan() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Scan", tint = IceniGold)
                }
            }
        )

        if (scanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = IceniGold,
                trackColor = IceniGreenLight
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(results) { ap ->
                WifiApRow(ap)
            }
        }
    }
}

@Composable
private fun WifiApRow(ap: ScanResult) {
    val signalBars = WifiManager.calculateSignalLevel(ap.level, 5)
    val signalIcon = "▂▄▆█".take(signalBars).padEnd(4, '░')
    val lock = if (ap.capabilities.contains("WPA") || ap.capabilities.contains("WEP")) "🔒" else "🔓"
    val color = if (signalBars >= 4) IceniTeal else if (signalBars >= 2) IceniGold else IceniTextMuted

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IceniGreen, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row {
            Text(text = "$lock  ", style = IceniTypography.bodyLarge)
            Text(
                text = ap.SSID.ifEmpty { "<hidden>" },
                style = IceniTypography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(text = signalIcon, style = IceniTypography.bodyLarge, color = color)
        }
        val ch = frequencyToChannel(ap.frequency)
        val band = when {
            ap.frequency in 2401..2495 -> "2.4G"
            ap.frequency in 5150..5895 -> "5G"
            ap.frequency in 5925..7125 -> "6G"
            else -> ""
        }
        Text(
            text = "BSSID: ${ap.BSSID}   ch:${if (ch > 0) "$ch" else "?"}${if (band.isNotEmpty()) " ($band)" else ""}   ${ap.level}dBm",
            style = IceniTypography.bodySmall
        )
        Text(
            text = ap.capabilities,
            style = IceniTypography.bodySmall,
            color = IceniTextHint
        )
    }
}

/** IEEE 802.11 centre-frequency (MHz) → channel number. */
private fun frequencyToChannel(freq: Int): Int = when {
    freq == 2484            -> 14                 // Japan ch14
    freq in 2412..2472      -> (freq - 2407) / 5  // 2.4 GHz ch1–13
    freq in 5150..5895      -> (freq - 5000) / 5  // 5 GHz
    freq in 5925..7125      -> (freq - 5950) / 5  // 6 GHz (Wi-Fi 6E)
    else                    -> -1
}
