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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Passive WPS scanner. Lists APs advertising WPS in their capabilities.
 * WPS (especially external-registrar PIN) is a known attack vector; this only
 * flags exposure, it performs no active WPS interaction.
 */
@Composable
fun WpsScannerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("…") }
    val scope = rememberCoroutineScope()

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults
    } catch (_: Exception) { emptyList() }

    val wpsAps = remember(results) {
        results.filter { it.capabilities.contains("WPS") }.sortedByDescending { it.level }
    }

    LaunchedEffect(Unit) {
        results = readCached()
        status = "${wpsCount(results)} WPS APs of ${results.size}"
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                results = readCached(); scanning = false
                status = "${wpsCount(results)} WPS APs of ${results.size}"
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
                status = "${wpsCount(results)} WPS APs of ${results.size}${if (!ok) " (cached)" else ""}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "WPS Scanner",
            subtitle = status,
            onBack = onBack,
            actions = {
                IconButton(onClick = { triggerScan() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Scan", tint = IceniGold)
                }
            }
        )
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniAmber, trackColor = IceniGreenLight)

        if (wpsAps.isEmpty()) {
            Text(
                if (results.isEmpty()) "No scan data yet." else "No WPS-enabled APs found.",
                style = IceniTypography.bodyLarge,
                color = IceniTeal,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(wpsAps, key = { it.BSSID ?: it.SSID }) { ap ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row {
                        Text("⚠ ", style = IceniTypography.titleMedium, color = IceniAmber)
                        Text(ap.SSID.ifEmpty { "<hidden>" }, style = IceniTypography.titleMedium, color = IceniAmber, modifier = Modifier.weight(1f))
                        Text("${ap.level}dBm", style = IceniTypography.bodySmall, color = IceniTeal)
                    }
                    Text("${ap.BSSID}  ch ${wpsFreqToChannel(ap.frequency)}", style = IceniTypography.bodySmall)
                    Text("WPS advertised — PIN brute-force risk", style = IceniTypography.bodyMedium, color = IceniAmber)
                }
            }
        }
    }
}

private fun wpsCount(list: List<ScanResult>): Int = list.count { it.capabilities.contains("WPS") }

private fun wpsFreqToChannel(f: Int): Int = when {
    f == 2484 -> 14
    f in 2412..2472 -> (f - 2407) / 5
    f in 5150..5895 -> (f - 5000) / 5
    f in 5925..7125 -> (f - 5950) / 5
    else -> -1
}
