package com.andraste.tablet.tools.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** Detailed per-AP inspector. Scan list → tap → decoded detail page. */
@Composable
fun ApInfoScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("…") }
    var selected by remember { mutableStateOf<ScanResult?>(null) }
    val scope = rememberCoroutineScope()

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults.sortedByDescending { it.level }
    } catch (_: Exception) { emptyList() }

    LaunchedEffect(Unit) {
        results = readCached()
        status = if (results.isEmpty()) "Tap ↻ to scan" else "${results.size} networks"
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                results = readCached(); scanning = false; status = "${results.size} networks"
            }
        }
        try { ctx.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) } catch (_: Exception) {}
        onDispose { try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    fun triggerScan() {
        scanning = true; status = "Scanning…"
        val ok = try { @Suppress("DEPRECATION") wm.startScan() } catch (_: Exception) { false }
        scope.launch {
            delay(6000)
            if (scanning) {
                results = readCached(); scanning = false
                status = if (results.isNotEmpty()) "${results.size} networks (${if (ok) "scan" else "cached"})" else "No results"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "AP Info",
            subtitle = status,
            onBack = if (selected != null) ({ selected = null }) else onBack,
            actions = {
                if (selected == null) IconButton(onClick = { triggerScan() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Scan", tint = IceniGold)
                }
            }
        )
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniGold, trackColor = IceniGreenLight)

        val sel = selected
        if (sel == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results, key = { it.BSSID ?: it.SSID }) { ap ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreen, RoundedCornerShape(6.dp))
                            .clickable { selected = ap }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row {
                            Text(ap.SSID.ifEmpty { "<hidden>" }, style = IceniTypography.titleMedium, modifier = Modifier.weight(1f))
                            Text("${ap.level}dBm", style = IceniTypography.bodySmall, color = IceniTeal)
                        }
                        Text("${ap.BSSID}  ch ${freqToChannelApInfo(ap.frequency)}", style = IceniTypography.bodySmall)
                    }
                }
            }
        } else {
            ApDetail(sel)
        }
    }
}

@Composable
private fun ApDetail(ap: ScanResult) {
    val ch = freqToChannelApInfo(ap.frequency)
    val band = when {
        ap.frequency in 2401..2495 -> "2.4 GHz"
        ap.frequency in 5150..5895 -> "5 GHz"
        ap.frequency in 5925..7125 -> "6 GHz (Wi-Fi 6E)"
        else -> "?"
    }
    val width = if (Build.VERSION.SDK_INT >= 23) when (ap.channelWidth) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
        ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
        ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
        else -> "?"
    } else "n/a"

    val caps = ap.capabilities
    val tokens = buildList {
        if (caps.contains("WPA3") || caps.contains("SAE")) add("WPA3/SAE")
        if (caps.contains("WPA2") || caps.contains("RSN")) add("WPA2/RSN")
        if (caps.contains("WPA-") || Regex("WPA[^23]").containsMatchIn(caps)) add("WPA")
        if (caps.contains("WEP")) add("WEP")
        if (caps.contains("PSK")) add("PSK")
        if (caps.contains("EAP")) add("EAP (802.1X)")
        if (caps.contains("WPS")) add("WPS")
        if (caps.contains("[ESS]")) add("Infrastructure (ESS)")
        if (caps.contains("[IBSS]")) add("Ad-hoc (IBSS)")
        if (isEmpty() || (!caps.contains("WPA") && !caps.contains("WEP") && !caps.contains("RSN") && !caps.contains("SAE"))) add("OPEN")
    }.distinct()

    val hidden = ap.SSID.isEmpty()
    val oui = (ap.BSSID ?: "").split(":").take(3).joinToString(":").uppercase()

    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ap.SSID.ifEmpty { "<hidden network>" }, style = IceniTypography.headlineMedium)
        DetailRow("BSSID", ap.BSSID ?: "?")
        DetailRow("Vendor OUI", oui)
        DetailRow("Channel", if (ch > 0) "$ch" else "?")
        DetailRow("Band", band)
        DetailRow("Width", width)
        DetailRow("Frequency", "${ap.frequency} MHz")
        DetailRow("RSSI", "${ap.level} dBm")
        DetailRow("Hidden", if (hidden) "yes" else "no")
        Spacer(Modifier.height(4.dp))
        Text("Security tokens", style = IceniTypography.labelLarge)
        tokens.forEach { Text("• $it", style = IceniTypography.bodyMedium, color = IceniTeal) }
        Spacer(Modifier.height(4.dp))
        Text("Raw capabilities", style = IceniTypography.labelSmall)
        Text(caps, style = IceniTypography.bodySmall)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IceniGreen, RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = IceniTypography.titleMedium, modifier = Modifier.width(110.dp))
        Text(value, style = IceniTypography.bodyLarge, color = IceniTeal, modifier = Modifier.weight(1f))
    }
}

private fun freqToChannelApInfo(f: Int): Int = when {
    f == 2484 -> 14
    f in 2412..2472 -> (f - 2407) / 5
    f in 5150..5895 -> (f - 5000) / 5
    f in 5925..7125 -> (f - 5950) / 5
    else -> -1
}
