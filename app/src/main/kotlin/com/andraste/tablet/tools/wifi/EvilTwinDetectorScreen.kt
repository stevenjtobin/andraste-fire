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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Passive Evil-Twin detector.
 * Groups APs by SSID and flags an SSID as suspicious when it appears with
 *  (a) multiple BSSIDs whose vendor OUIs differ, or
 *  (b) inconsistent security (one open + one WPA/WEP of the same SSID).
 */
@Composable
fun EvilTwinDetectorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("…") }
    val scope = rememberCoroutineScope()

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults
    } catch (_: SecurityException) { emptyList() } catch (_: Exception) { emptyList() }

    LaunchedEffect(Unit) {
        results = readCached()
        status = if (results.isEmpty()) "Tap ↻ to scan" else "${results.size} APs (cached)"
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                results = readCached(); scanning = false
                status = "${results.size} APs analysed"
            }
        }
        try {
            ctx.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        } catch (_: Exception) {}
        onDispose { try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    fun triggerScan() {
        scanning = true; status = "Scanning…"
        val ok = try { @Suppress("DEPRECATION") wm.startScan() } catch (_: Exception) { false }
        scope.launch {
            delay(6000)
            if (scanning) {
                results = readCached(); scanning = false
                status = if (results.isNotEmpty()) "${results.size} APs (${if (ok) "scan" else "cached"})"
                else "No results — enable Location & retry"
            }
        }
    }

    // Build suspicious groups
    val groups: List<EvilTwinGroup> = remember(results) { analyseEvilTwins(results) }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Evil-Twin Detector",
            subtitle = status,
            onBack = onBack,
            actions = {
                IconButton(onClick = { triggerScan() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Scan", tint = IceniGold)
                }
            }
        )
        if (scanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = IceniAmber, trackColor = IceniGreenLight)
        }

        if (groups.isEmpty()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = if (results.isEmpty()) "No scan data yet." else "No evil-twin patterns detected.",
                    style = IceniTypography.bodyLarge,
                    color = if (results.isEmpty()) IceniTextMuted else IceniTeal
                )
                Text(
                    text = "Passive check: duplicate SSIDs with mismatched vendor OUI or inconsistent security.",
                    style = IceniTypography.bodySmall
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(groups, key = { it.ssid }) { g -> EvilTwinRow(g) }
        }
    }
}

private data class EvilTwinAp(
    val bssid: String,
    val oui: String,
    val level: Int,
    val open: Boolean,
    val caps: String
)

private data class EvilTwinGroup(
    val ssid: String,
    val aps: List<EvilTwinAp>,
    val ouiMismatch: Boolean,
    val securityMismatch: Boolean
)

private fun analyseEvilTwins(results: List<ScanResult>): List<EvilTwinGroup> {
    val bySsid = results.filter { it.SSID.isNotEmpty() }.groupBy { it.SSID }
    val out = mutableListOf<EvilTwinGroup>()
    for ((ssid, list) in bySsid) {
        if (list.size < 2) continue
        val aps = list.map { ap ->
            val bssid = ap.BSSID ?: "??"
            val open = !(ap.capabilities.contains("WPA") || ap.capabilities.contains("WEP") ||
                ap.capabilities.contains("RSN") || ap.capabilities.contains("SAE"))
            EvilTwinAp(bssid, ouiOf(bssid), ap.level, open, ap.capabilities)
        }
        val ouiMismatch = aps.map { it.oui }.distinct().size > 1
        val securityMismatch = aps.any { it.open } && aps.any { !it.open }
        if (ouiMismatch || securityMismatch) {
            out += EvilTwinGroup(ssid, aps.sortedByDescending { it.level }, ouiMismatch, securityMismatch)
        }
    }
    return out.sortedByDescending { it.aps.size }
}

private fun ouiOf(bssid: String): String =
    bssid.split(":").take(3).joinToString(":").uppercase()

@Composable
private fun EvilTwinRow(g: EvilTwinGroup) {
    val flagColor: Color = if (g.ouiMismatch) IceniRed else IceniAmber
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IceniGreen, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row {
            Text(text = "⚠ ", style = IceniTypography.titleMedium, color = flagColor)
            Text(text = g.ssid, style = IceniTypography.titleMedium, color = flagColor, modifier = Modifier.weight(1f))
            Text(text = "${g.aps.size} BSSIDs", style = IceniTypography.bodySmall)
        }
        val reasons = buildList {
            if (g.ouiMismatch) add("vendor OUI mismatch")
            if (g.securityMismatch) add("open + secured mix")
        }.joinToString(" · ")
        Text(text = reasons, style = IceniTypography.bodyMedium, color = IceniAmber)
        Spacer(Modifier.height(4.dp))
        g.aps.forEach { ap ->
            Text(
                text = "${ap.bssid}  (OUI ${ap.oui})  ${ap.level}dBm  ${if (ap.open) "OPEN" else "SECURED"}",
                style = IceniTypography.bodySmall
            )
        }
    }
}
