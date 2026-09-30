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
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import java.util.UUID

private const val CID_APPLE = 0x004C

private val UUID_EDDYSTONE = ParcelUuid(UUID.fromString("0000FEAA-0000-1000-8000-00805F9B34FB"))
private val UUID_EXPOSURE = ParcelUuid(UUID.fromString("0000FD6F-0000-1000-8000-00805F9B34FB"))

private data class DecodedBeacon(
    val address: String,
    val type: String,
    val detail: String,
    val rssi: Int
)

private fun bytesToHex(b: ByteArray?, max: Int = Int.MAX_VALUE): String {
    if (b == null) return ""
    val sb = StringBuilder()
    for (i in b.indices) {
        if (i >= max) break
        sb.append("%02X".format(b[i]))
    }
    return sb.toString()
}

private fun decodeBeacon(result: ScanResult): DecodedBeacon? {
    val rec = result.scanRecord ?: return null
    val addr = result.device.address
    val rssi = result.rssi

    // Eddystone (service UUID 0xFEAA)
    rec.getServiceData(UUID_EDDYSTONE)?.let { data ->
        if (data.isNotEmpty()) {
            val frame = data[0].toInt() and 0xFF
            val kind = when (frame) {
                0x00 -> "UID"
                0x10 -> "URL"
                0x20 -> "TLM"
                0x30 -> "EID"
                else -> "0x%02X".format(frame)
            }
            return DecodedBeacon(addr, "Eddystone", "frame=$kind  ${bytesToHex(data, 12)}", rssi)
        }
    }

    // Exposure Notification (service UUID 0xFD6F)
    rec.getServiceData(UUID_EXPOSURE)?.let { data ->
        return DecodedBeacon(addr, "ExposureNotif", "RPI ${bytesToHex(data, 8)}...", rssi)
    }
    if (rec.serviceUuids?.contains(UUID_EXPOSURE) == true) {
        return DecodedBeacon(addr, "ExposureNotif", "contact-tracing beacon", rssi)
    }

    // Apple manufacturer data
    val msd = rec.manufacturerSpecificData
    if (msd != null && msd.size() > 0) {
        for (i in 0 until msd.size()) {
            val cid = msd.keyAt(i)
            val data = msd.valueAt(i) ?: continue
            if (cid == CID_APPLE) {
                // iBeacon: subtype 0x02, length 0x15
                if (data.size >= 23 && (data[0].toInt() and 0xFF) == 0x02 && (data[1].toInt() and 0xFF) == 0x15) {
                    val uuid = bytesToHex(data.copyOfRange(2, 18))
                    val major = ((data[18].toInt() and 0xFF) shl 8) or (data[19].toInt() and 0xFF)
                    val minor = ((data[20].toInt() and 0xFF) shl 8) or (data[21].toInt() and 0xFF)
                    val tx = data[22].toInt()
                    return DecodedBeacon(
                        addr, "iBeacon",
                        "UUID=$uuid  major=$major minor=$minor tx=$tx",
                        rssi
                    )
                }
                // Other Apple Continuity types
                val t = if (data.isNotEmpty()) data[0].toInt() and 0xFF else -1
                val label = when (t) {
                    0x12 -> "FindMy/offline"
                    0x07 -> "AirPods/proximity"
                    0x10 -> "nearby"
                    else -> "type=0x%02X".format(t)
                }
                return DecodedBeacon(addr, "AppleContinuity", "$label  ${bytesToHex(data, 10)}", rssi)
            }
        }
    }
    return null
}

@Composable
fun BeaconDecoderScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var beacons by remember { mutableStateOf<Map<String, DecodedBeacon>>(emptyMap()) }
    var scanning by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val b = decodeBeacon(result) ?: return
                beacons = beacons + (b.address to b)
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
        onDispose {
            try { scanner?.stopScan(callback) } catch (e: SecurityException) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "Beacon Decoder",
            subtitle = "${beacons.size} beacons",
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
                color = IceniTeal,
                trackColor = IceniGreenLight
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
            val sorted = beacons.values.sortedByDescending { it.rssi }
            items(sorted, key = { it.address }) { b ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row {
                        Text(b.type, style = IceniTypography.titleMedium,
                            color = IceniGold, modifier = Modifier.weight(1f))
                        Text("${b.rssi} dBm", style = IceniTypography.bodyMedium)
                    }
                    Text(b.detail, style = IceniTypography.bodySmall)
                    Text(b.address, style = IceniTypography.labelSmall)
                }
            }
        }
    }
}
