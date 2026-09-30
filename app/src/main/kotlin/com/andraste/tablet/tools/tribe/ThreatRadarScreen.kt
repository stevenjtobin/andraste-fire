package com.andraste.tablet.tools.tribe

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Passive combined threat radar. Runs a WiFi scan and a BLE scan together and
 * presents an aggregate dashboard: total WiFi APs, open networks, total BLE
 * devices, likely trackers (Apple Find My / Tile / Flipper), and strongest
 * signals — plus a simple radar visual.
 */
@Composable
fun ThreatRadarScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val scanner = remember { btMgr?.adapter?.bluetoothLeScanner }

    var running by remember { mutableStateOf(false) }
    var wifiTotal by remember { mutableStateOf(0) }
    var wifiOpen by remember { mutableStateOf(0) }
    var wifiStrongest by remember { mutableStateOf<Int?>(null) }
    val bleDevices = remember { mutableStateMapOf<String, BleBlip>() }

    fun refreshWifi() {
        val list = try { @Suppress("DEPRECATION") wm.scanResults } catch (_: Exception) { emptyList() }
        wifiTotal = list.size
        wifiOpen = list.count { r ->
            val c = r.capabilities
            !(c.contains("WPA") || c.contains("WEP") || c.contains("RSN") || c.contains("SAE"))
        }
        wifiStrongest = list.maxByOrNull { it.level }?.level
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { refreshWifi() }
        }
        try { ctx.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) } catch (_: Exception) {}
        onDispose { try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    val bleCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val rec = result.scanRecord
                    val name = result.device.name
                    var tracker: String? = null
                    val mfr = rec?.manufacturerSpecificData
                    if (mfr != null) {
                        for (idx in 0 until mfr.size()) {
                            val cid = mfr.keyAt(idx)
                            val v = mfr.valueAt(idx)
                            if (cid == 0x004C && v != null && v.isNotEmpty() && (v[0].toInt() and 0xFF) == 0x12) {
                                tracker = "Apple Find My"
                            }
                        }
                    }
                    if (tracker == null && name != null) {
                        val up = name.uppercase()
                        tracker = when {
                            up.contains("TILE") -> "Tile"
                            up.contains("FLIPPER") -> "Flipper"
                            else -> null
                        }
                    }
                    bleDevices[result.device.address] = BleBlip(result.device.address, name, result.rssi, tracker)
                } catch (_: SecurityException) {} catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        // start BLE
        try {
            if (scanner != null) {
                val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
                scanner.startScan(null, settings, bleCallback)
            }
        } catch (_: Exception) {}
        // periodic WiFi sweeps
        while (isActive && running) {
            try { @Suppress("DEPRECATION") wm.startScan() } catch (_: Exception) {}
            delay(1500)
            refreshWifi()
            delay(4000)
        }
    }

    DisposableEffect(Unit) {
        onDispose { try { scanner?.stopScan(bleCallback) } catch (_: Exception) {} }
    }

    LaunchedEffect(Unit) { refreshWifi() }

    val trackers = bleDevices.values.count { it.tracker != null }
    val bleStrongest = bleDevices.values.maxByOrNull { it.rssi }?.rssi

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Threat Radar",
            subtitle = if (running) "sweeping…" else "passive · tap ▶ to sweep",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (running) { running = false; try { scanner?.stopScan(bleCallback) } catch (_: Exception) {} }
                    else running = true
                }) {
                    Icon(
                        if (running) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = if (running) "Stop" else "Start",
                        tint = IceniGold
                    )
                }
            }
        )
        if (running) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniTeal, trackColor = IceniGreenLight)

        // Radar visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            val blips = bleDevices.values.toList()
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = min(cx, cy) * 0.95f
                // concentric rings
                for (i in 1..3) {
                    drawCircle(
                        color = IceniTealDark.copy(alpha = 0.5f),
                        radius = maxR * i / 3f,
                        center = Offset(cx, cy),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
                    )
                }
                // crosshairs
                drawLine(IceniTealDark.copy(alpha = 0.4f), Offset(cx - maxR, cy), Offset(cx + maxR, cy), 1f)
                drawLine(IceniTealDark.copy(alpha = 0.4f), Offset(cx, cy - maxR), Offset(cx, cy + maxR), 1f)
                // blips: radius from RSSI (stronger = nearer centre), angle by hash
                blips.forEachIndexed { idx, b ->
                    val norm = ((b.rssi + 100).coerceIn(0, 70)) / 70f  // 0 far .. 1 near
                    val r = maxR * (1f - norm)
                    val angle = (b.address.hashCode() and 0xFFFF) / 65535.0 * 2 * Math.PI
                    val x = cx + (r * cos(angle)).toFloat()
                    val y = cy + (r * sin(angle)).toFloat()
                    val col: Color = if (b.tracker != null) IceniRed else IceniGold
                    drawCircle(col, radius = 5f, center = Offset(x, y))
                }
            }
        }

        // Stat cards
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("WiFi APs", "$wifiTotal", IceniGold, Modifier.weight(1f))
                StatCard("Open nets", "$wifiOpen", if (wifiOpen > 0) IceniAmber else IceniTeal, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("BLE devices", "${bleDevices.size}", IceniTeal, Modifier.weight(1f))
                StatCard("Trackers", "$trackers", if (trackers > 0) IceniRed else IceniTeal, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("WiFi peak", wifiStrongest?.let { "$it dBm" } ?: "—", IceniGold, Modifier.weight(1f))
                StatCard("BLE peak", bleStrongest?.let { "$it dBm" } ?: "—", IceniTeal, Modifier.weight(1f))
            }
            if (trackers > 0) {
                Text("Likely trackers nearby:", style = IceniTypography.labelLarge, color = IceniRed)
                bleDevices.values.filter { it.tracker != null }.take(6).forEach {
                    Text("• ${it.tracker}  ${it.name ?: it.address}  ${it.rssi}dBm", style = IceniTypography.bodyMedium, color = IceniAmber)
                }
            }
        }
    }
}

private data class BleBlip(val address: String, val name: String?, val rssi: Int, val tracker: String?)

@Composable
private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(IceniGreen, RoundedCornerShape(6.dp))
            .padding(14.dp)
    ) {
        Text(label, style = IceniTypography.labelSmall)
        Text(value, style = IceniTypography.headlineMedium, color = valueColor)
    }
}
