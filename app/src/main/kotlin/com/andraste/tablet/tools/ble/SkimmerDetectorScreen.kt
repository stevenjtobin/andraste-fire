package com.andraste.tablet.tools.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
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

private data class SkimmerSuspect(
    val address: String,
    val name: String?,
    val reason: String,
    val rssi: Int
)

// Known cheap serial/SPP BT module names used in card skimmers
private val SKIMMER_NAMES = listOf(
    "HC-05", "HC-06", "HC-03", "HC-08", "BT-", "free2move", "RNBT",
    "CVT", "linvor", "JDY-", "ZS-040", "MLT-BT05", "AT-09", "SPP-CA",
    "BOLUTEK", "FFD0"
)

private fun skimmerReason(result: ScanResult): String? {
    val rec = result.scanRecord
    val name = try { result.device.name } catch (e: SecurityException) { null } ?: rec?.deviceName
    if (name != null) {
        for (sig in SKIMMER_NAMES) {
            if (name.contains(sig, ignoreCase = true)) {
                return "name matches skimmer module \"$sig\""
            }
        }
    }
    // Unnamed device advertising with no manufacturer data / classic SPP-style:
    // cheap modules often advertise as unnamed BLE bridges.
    val msd = rec?.manufacturerSpecificData
    val hasMfr = msd != null && msd.size() > 0
    if (name.isNullOrBlank() && !hasMfr && result.rssi > -70) {
        return "unnamed close device, no mfr data (possible SPP bridge)"
    }
    return null
}

@Composable
fun SkimmerDetectorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var suspects by remember { mutableStateOf<Map<String, SkimmerSuspect>>(emptyMap()) }
    var scanning by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val reason = skimmerReason(result) ?: return
                val addr = result.device.address
                suspects = suspects + (addr to SkimmerSuspect(
                    address = addr,
                    name = try { result.device.name } catch (e: SecurityException) { null },
                    reason = reason,
                    rssi = result.rssi
                ))
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
            title = "Skimmer Detector",
            subtitle = "${suspects.size} suspects",
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
            val sorted = suspects.values.sortedByDescending { it.rssi }
            items(sorted, key = { it.address }) { s ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row {
                        Text(
                            s.name ?: "<unnamed>",
                            style = IceniTypography.titleMedium,
                            color = IceniRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${s.rssi} dBm", style = IceniTypography.bodyMedium)
                    }
                    Text(s.reason, style = IceniTypography.bodySmall, color = IceniRed)
                    Text(s.address, style = IceniTypography.labelSmall)
                }
            }
        }
    }
}
