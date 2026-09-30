package com.andraste.tablet.tools.ble

import android.bluetooth.BluetoothManager
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlin.math.pow

private data class FinderDevice(
    val address: String,
    val name: String?,
    val rssi: Int
)

private fun estDistanceMeters(rssi: Int, txPower: Int = -59): Double =
    10.0.pow((txPower - rssi) / 20.0)

@Composable
fun BleFinderScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var devices by remember { mutableStateOf<Map<String, FinderDevice>>(emptyMap()) }
    var scanning by remember { mutableStateOf(false) }
    var tracked by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = FinderDevice(
                    address = result.device.address,
                    name = try { result.device.name } catch (e: SecurityException) { null },
                    rssi = result.rssi
                )
                devices = devices + (dev.address to dev)
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

    val trackedDev = tracked?.let { devices[it] }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = if (trackedDev != null) "Tracking" else "BLE Finder",
            subtitle = if (trackedDev != null) (trackedDev.name ?: trackedDev.address)
                       else "${devices.size} devices",
            onBack = { if (tracked != null) tracked = null else onBack() },
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

        if (trackedDev != null) {
            // Live RSSI + proximity bar. RSSI ~ -30 (very close) .. -100 (far)
            val rssi = trackedDev.rssi
            val frac = ((rssi + 100).coerceIn(0, 70)) / 70f
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "$rssi",
                    style = IceniTypography.headlineLarge.copy(fontSize = 96.sp),
                    color = when {
                        rssi > -55 -> IceniTeal
                        rssi > -80 -> IceniGold
                        else -> IceniTextMuted
                    },
                    textAlign = TextAlign.Center
                )
                Text("dBm", style = IceniTypography.titleLarge, color = IceniTextMuted)
                Text(
                    "~ %.1f m".format(estDistanceMeters(rssi)),
                    style = IceniTypography.headlineMedium
                )
                // Proximity bar (closer = fuller)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(IceniGreenLight, RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .fillMaxHeight()
                            .background(
                                when {
                                    frac > 0.7f -> IceniTeal
                                    frac > 0.4f -> IceniGold
                                    else -> IceniGoldDim
                                },
                                RoundedCornerShape(6.dp)
                            )
                    )
                }
                Text(trackedDev.address, style = IceniTypography.labelSmall)
            }
        } else {
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
                            .clickable { tracked = dev.address }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dev.name ?: "<unnamed>", style = IceniTypography.titleMedium)
                            Text(dev.address, style = IceniTypography.labelSmall)
                        }
                        Text("${dev.rssi} dBm", style = IceniTypography.bodyMedium)
                    }
                }
            }
        }
    }
}
