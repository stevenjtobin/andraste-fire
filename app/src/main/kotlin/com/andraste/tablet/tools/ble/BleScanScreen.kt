package com.andraste.tablet.tools.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class BleDevice(
    val address: String,
    val name: String?,
    val rssi: Int,
    val manufacturer: String
)

@Composable
fun BleScanScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var devices by remember { mutableStateOf<Map<String, BleDevice>>(emptyMap()) }
    var scanning by remember { mutableStateOf(false) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val mfr = result.scanRecord?.manufacturerSpecificData
                val mfrStr = if (mfr != null && mfr.size() > 0) {
                    "MFR:0x${mfr.keyAt(0).toString(16).padStart(4, '0').uppercase()}"
                } else ""
                val dev = BleDevice(
                    address      = result.device.address,
                    name         = result.device.name,
                    rssi         = result.rssi,
                    manufacturer = mfrStr
                )
                devices = devices + (dev.address to dev)
            }
        }
    }

    DisposableEffect(scanning) {
        if (scanning && scanner != null) {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(null, settings, callback)
        }
        onDispose {
            scanner?.stopScan(callback)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "BLE Scanner",
            subtitle = "${devices.size} devices",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (scanning) {
                        scanner?.stopScan(callback)
                        scanning = false
                    } else {
                        scanning = true
                    }
                }) {
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
                color = IceniTeal,
                trackColor = IceniGreenLight
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sorted = devices.values.sortedByDescending { it.rssi }
            items(sorted, key = { it.address }) { dev ->
                BleDeviceRow(dev)
            }
        }
    }
}

@Composable
private fun BleDeviceRow(dev: BleDevice) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IceniGreen, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row {
            Text(
                text = dev.name ?: "<unnamed>",
                style = IceniTypography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            val rssiColor = when {
                dev.rssi > -60 -> IceniTeal
                dev.rssi > -80 -> IceniGold
                else           -> IceniTextMuted
            }
            Text(
                text = "${dev.rssi} dBm",
                style = IceniTypography.bodyMedium,
                color = rssiColor
            )
        }
        Text(
            text = "${dev.address}   ${dev.manufacturer}",
            style = IceniTypography.bodySmall
        )
    }
}
