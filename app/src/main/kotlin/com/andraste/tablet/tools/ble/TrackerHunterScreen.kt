package com.andraste.tablet.tools.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private data class TrackerHit(
    val address: String,
    val name: String?,
    val brand: String,
    val rssi: Int,
    val firstSeen: Long
)

private fun pu(short: String): ParcelUuid =
    ParcelUuid(UUID.fromString("0000$short-0000-1000-8000-00805F9B34FB"))

private val UUID_TILE_A = pu("FEED")
private val UUID_TILE_B = pu("FEEC")
private val UUID_EDDY = pu("FEAA")

private fun classifyTracker(result: ScanResult): String? {
    val rec = result.scanRecord ?: return null
    val name = result.device.name ?: rec.deviceName
    if (name != null && name.contains("Flipper", ignoreCase = true)) return "Flipper Zero"

    val svc = rec.serviceUuids
    if (svc != null) {
        if (svc.contains(UUID_TILE_A) || svc.contains(UUID_TILE_B)) return "Tile"
    }

    val msd = rec.manufacturerSpecificData
    if (msd != null && msd.size() > 0) {
        for (i in 0 until msd.size()) {
            val cid = msd.keyAt(i)
            val data = msd.valueAt(i)
            when (cid) {
                0x004C -> { // Apple
                    val t = if (data != null && data.isNotEmpty()) data[0].toInt() and 0xFF else -1
                    if (t == 0x12) return "Apple Find My / AirTag"
                }
                0x0157 -> return "Tile"                 // Tile company ID
                0x0075 -> {                              // Samsung
                    return "Samsung SmartTag"
                }
                0x00E0 -> return "Google Find My"        // Google
            }
        }
    }

    // Google Find My also uses Eddystone-like service data
    if (svc != null && svc.contains(UUID_EDDY)) {
        // Chipolo and others sometimes ride Eddystone; low-confidence guess
        if (name != null && name.contains("Chipolo", ignoreCase = true)) return "Chipolo"
        return "Google Find My (Eddystone)"
    }
    if (name != null && name.contains("Chipolo", ignoreCase = true)) return "Chipolo"

    return null
}

@Composable
fun TrackerHunterScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var hits by remember { mutableStateOf<Map<String, TrackerHit>>(emptyMap()) }
    var scanning by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val brand = classifyTracker(result) ?: return
                val addr = result.device.address
                val existing = hits[addr]
                val hit = TrackerHit(
                    address = addr,
                    name = try { result.device.name } catch (e: SecurityException) { null },
                    brand = brand,
                    rssi = result.rssi,
                    firstSeen = existing?.firstSeen ?: System.currentTimeMillis()
                )
                hits = hits + (addr to hit)
            }
        }
    }

    DisposableEffect(scanning) {
        if (scanning && scanner != null) {
            try {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner.startScan(null, settings, callback)
            } catch (e: SecurityException) {
                hint = "grant Bluetooth/Location in Permissions"
            }
        }
        onDispose { try { scanner?.stopScan(callback) } catch (e: SecurityException) { } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "Tracker Hunter",
            subtitle = "${hits.size} trackers flagged",
            onBack = onBack,
            actions = {
                IconButton(onClick = { scanning = !scanning }) {
                    Icon(
                        imageVector = if (scanning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = if (scanning) "Stop" else "Start",
                        tint = IceniGold
                    )
                }
            }
        )
        if (scanning) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = IceniTeal, trackColor = IceniGreenLight
            )
        }
        hint?.let {
            Text(it, style = IceniTypography.bodySmall, color = IceniAmber,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sorted = hits.values.sortedByDescending { it.rssi }
            items(sorted, key = { it.address }) { hit ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row {
                        Text(
                            hit.brand,
                            style = IceniTypography.titleMedium,
                            color = IceniAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${hit.rssi} dBm", style = IceniTypography.bodyMedium)
                    }
                    Text(hit.name ?: "<unnamed>", style = IceniTypography.bodySmall)
                    Text(
                        "${hit.address}   first ${timeFmt.format(Date(hit.firstSeen))}",
                        style = IceniTypography.labelSmall
                    )
                }
            }
        }
    }
}
