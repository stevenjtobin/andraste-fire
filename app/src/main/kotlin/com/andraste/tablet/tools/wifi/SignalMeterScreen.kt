package com.andraste.tablet.tools.wifi

import android.content.Context
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Walk-test signal meter. Pick an AP, then watch a large live RSSI readout with
 * a bar and running min/max, re-scanning every ~2s. Back returns to the list.
 */
@Composable
fun SignalMeterScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var selectedBssid by remember { mutableStateOf<String?>(null) }
    var selectedLabel by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<Int?>(null) }
    var minRssi by remember { mutableStateOf<Int?>(null) }
    var maxRssi by remember { mutableStateOf<Int?>(null) }

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults.sortedByDescending { it.level }
    } catch (_: Exception) { emptyList() }

    LaunchedEffect(Unit) { results = readCached() }

    // Live loop when an AP is selected
    LaunchedEffect(selectedBssid) {
        val target = selectedBssid ?: return@LaunchedEffect
        while (isActive && selectedBssid == target) {
            try { @Suppress("DEPRECATION") wm.startScan() } catch (_: Exception) {}
            delay(2000)
            val list = readCached()
            val ap = list.firstOrNull { it.BSSID == target }
            if (ap != null) {
                current = ap.level
                minRssi = minOf(minRssi ?: ap.level, ap.level)
                maxRssi = maxOf(maxRssi ?: ap.level, ap.level)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Signal Meter",
            subtitle = if (selectedBssid != null) selectedLabel else "select an AP to walk-test",
            onBack = if (selectedBssid != null) ({
                selectedBssid = null; current = null; minRssi = null; maxRssi = null
            }) else onBack
        )

        if (selectedBssid == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results, key = { it.BSSID ?: it.SSID }) { ap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreen, RoundedCornerShape(6.dp))
                            .clickable {
                                selectedBssid = ap.BSSID
                                selectedLabel = ap.SSID.ifEmpty { ap.BSSID ?: "?" }
                                current = ap.level; minRssi = ap.level; maxRssi = ap.level
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(ap.SSID.ifEmpty { "<hidden>" }, style = IceniTypography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${ap.level}dBm", style = IceniTypography.bodySmall, color = IceniTeal)
                    }
                }
            }
        } else {
            val rssi = current
            val bars = if (rssi != null) WifiManager.calculateSignalLevel(rssi, 5) else 0
            val color: Color = when {
                rssi == null -> IceniTextMuted
                rssi > -55 -> IceniTeal
                rssi > -70 -> IceniGold
                rssi > -82 -> IceniAmber
                else -> IceniRed
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(selectedLabel, style = IceniTypography.headlineMedium)
                Text(
                    text = rssi?.let { "$it" } ?: "—",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 72.sp,
                    color = color
                )
                Text("dBm", style = IceniTypography.titleLarge, color = color)

                // signal bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(IceniGreenLight, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((bars / 5f).coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MIN", style = IceniTypography.labelSmall)
                        Text(minRssi?.let { "$it" } ?: "—", style = IceniTypography.titleLarge, color = IceniTextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MAX", style = IceniTypography.labelSmall)
                        Text(maxRssi?.let { "$it" } ?: "—", style = IceniTypography.titleLarge, color = IceniTeal)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BARS", style = IceniTypography.labelSmall)
                        Text("$bars/5", style = IceniTypography.titleLarge, color = color)
                    }
                }
                Text("Re-scanning every 2s · walk toward/away to test coverage", style = IceniTypography.bodySmall)
            }
        }
    }
}
