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
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

private data class FindMyAdv(
    val address: String,
    val rssi: Int,
    val statusByte: Int,
    val state: String
)

// Apple Find My / offline-finding advert = manufacturer 0x004C, type byte 0x12.
private fun parseFindMy(result: ScanResult): FindMyAdv? {
    val msd = result.scanRecord?.manufacturerSpecificData ?: return null
    for (i in 0 until msd.size()) {
        if (msd.keyAt(i) == 0x004C) {
            val data = msd.valueAt(i) ?: continue
            if (data.isNotEmpty() && (data[0].toInt() and 0xFF) == 0x12) {
                // data[1] = length, data[2] = status byte in Find My spec
                val status = if (data.size >= 3) data[2].toInt() and 0xFF else -1
                // Bit interpretation is approximate: high bit / maintained flag hints
                // whether the device is separated from its owner.
                val separated = status != -1 && (status and 0x04) != 0
                val state = when {
                    status == -1 -> "unknown"
                    separated -> "lost / separated"
                    else -> "owner nearby"
                }
                return FindMyAdv(result.device.address, result.rssi, status, state)
            }
        }
    }
    return null
}

@Composable
fun FindMyObserverScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var advs by remember { mutableStateOf<Map<String, FindMyAdv>>(emptyMap()) }
    var scanning by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val a = parseFindMy(result) ?: return
                advs = advs + (a.address to a)
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
            title = "Find My Observer",
            subtitle = "${advs.size} offline-finding beacons",
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
            val sorted = advs.values.sortedByDescending { it.rssi }
            items(sorted, key = { it.address }) { a ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row {
                        Text(
                            a.state,
                            style = IceniTypography.titleMedium,
                            color = if (a.state.startsWith("lost")) IceniAmber else IceniTeal,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${a.rssi} dBm", style = IceniTypography.bodyMedium)
                    }
                    Text(
                        "status=0x%02X".format(a.statusByte),
                        style = IceniTypography.bodySmall
                    )
                    Text(a.address, style = IceniTypography.labelSmall)
                }
            }
        }
    }
}
