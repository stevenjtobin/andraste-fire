package com.andraste.tablet.tools.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private data class GattScanDevice(
    val address: String,
    val name: String?,
    val rssi: Int
)

private data class GattCharInfo(
    val uuid: String,
    val props: String
)

private data class GattServiceInfo(
    val uuid: String,
    val characteristics: List<GattCharInfo>
)

private fun decodeProps(mask: Int): String {
    val parts = mutableListOf<String>()
    if (mask and BluetoothGattCharacteristic.PROPERTY_READ != 0) parts.add("READ")
    if (mask and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) parts.add("WRITE")
    if (mask and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) parts.add("WRITE_NR")
    if (mask and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) parts.add("NOTIFY")
    if (mask and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) parts.add("INDICATE")
    if (mask and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) parts.add("BROADCAST")
    if (mask and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) parts.add("SIGNED")
    return if (parts.isEmpty()) "-" else parts.joinToString("/")
}

@Composable
fun GattExplorerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var devices by remember { mutableStateOf<Map<String, GattScanDevice>>(emptyMap()) }
    var scanning by remember { mutableStateOf(false) }
    var connectedTo by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var services by remember { mutableStateOf<List<GattServiceInfo>>(emptyList()) }
    var gatt by remember { mutableStateOf<BluetoothGatt?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = GattScanDevice(
                    address = result.device.address,
                    name = try { result.device.name } catch (e: SecurityException) { null },
                    rssi = result.rssi
                )
                devices = devices + (dev.address to dev)
            }
        }
    }

    val gattCallback = remember {
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, gStatus: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    status = "connected — discovering services..."
                    try { g.discoverServices() } catch (e: SecurityException) {
                        status = "SecurityException on discover"
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    status = "disconnected"
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, gStatus: Int) {
                val list = g.services.map { svc ->
                    GattServiceInfo(
                        uuid = svc.uuid.toString(),
                        characteristics = svc.characteristics.map { ch ->
                            GattCharInfo(ch.uuid.toString(), decodeProps(ch.properties))
                        }
                    )
                }
                services = list
                status = "${list.size} services"
            }
        }
    }

    DisposableEffect(scanning) {
        if (scanning && scanner != null) {
            try {
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner.startScan(null, settings, scanCallback)
            } catch (e: SecurityException) {
                hint = "grant Bluetooth/Location in Permissions"
            }
        }
        onDispose { try { scanner?.stopScan(scanCallback) } catch (e: SecurityException) { } }
    }

    fun disconnect() {
        try { gatt?.disconnect(); gatt?.close() } catch (e: SecurityException) { }
        gatt = null
        connectedTo = null
        services = emptyList()
        status = ""
    }

    DisposableEffect(Unit) {
        onDispose { try { gatt?.close() } catch (e: SecurityException) { } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "GATT Explorer",
            subtitle = if (connectedTo != null) "$connectedTo  $status" else "${devices.size} devices",
            onBack = { if (connectedTo != null) disconnect() else onBack() },
            actions = {
                if (connectedTo == null) {
                    IconButton(onClick = { scanning = !scanning }) {
                        Icon(
                            imageVector = if (scanning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                            contentDescription = if (scanning) "Stop" else "Start",
                            tint = IceniGold
                        )
                    }
                }
            }
        )
        if (scanning && connectedTo == null) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = IceniTeal, trackColor = IceniGreenLight
            )
        }
        hint?.let {
            Text(it, style = IceniTypography.bodySmall, color = IceniAmber,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }

        if (connectedTo == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val sorted = devices.values.sortedByDescending { it.rssi }
                items(sorted, key = { it.address }) { dev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreen, RoundedCornerShape(6.dp))
                            .clickable {
                                try {
                                    scanner?.stopScan(scanCallback)
                                    scanning = false
                                    connectedTo = dev.address
                                    status = "connecting..."
                                    gatt = btMgr.adapter
                                        ?.getRemoteDevice(dev.address)
                                        ?.connectGatt(ctx, false, gattCallback)
                                } catch (e: SecurityException) {
                                    hint = "grant Bluetooth/Location in Permissions"
                                    connectedTo = null
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dev.name ?: "<unnamed>", style = IceniTypography.titleMedium)
                            Text(dev.address, style = IceniTypography.labelSmall)
                        }
                        Text("${dev.rssi} dBm", style = IceniTypography.bodyMedium)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(services, key = { it.uuid }) { svc ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("SERVICE", style = IceniTypography.labelSmall, color = IceniGold)
                        Text(svc.uuid, style = IceniTypography.bodyLarge)
                        svc.characteristics.forEach { ch ->
                            Column(modifier = Modifier.padding(start = 8.dp, top = 6.dp)) {
                                Text(ch.uuid, style = IceniTypography.bodyMedium)
                                Text(ch.props, style = IceniTypography.labelSmall, color = IceniTeal)
                            }
                        }
                    }
                }
            }
        }
    }
}
