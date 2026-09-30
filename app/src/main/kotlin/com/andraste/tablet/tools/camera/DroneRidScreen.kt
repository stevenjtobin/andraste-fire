package com.andraste.tablet.tools.camera

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

/**
 * EXPERIMENTAL Open Drone ID (Remote ID) monitor over BLE (ASTM F3411).
 * Listens for adverts carrying the Open Drone ID service (0xFFFA) or the ASTM
 * manufacturer data (company 0xFFFA), and decodes the message-type nibble where
 * possible. WiFi-beacon/NAN Remote ID needs monitor mode and is unavailable on
 * this hardware — BLE only.
 */
@Composable
fun DroneRidScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val scanner = remember { btMgr?.adapter?.bluetoothLeScanner }
    var scanning by remember { mutableStateOf(false) }
    val candidates = remember { mutableStateMapOf<String, RidCandidate>() }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val odidService = remember { ParcelUuid.fromString("0000FFFA-0000-1000-8000-00805F9B34FB") }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    val rec = result.scanRecord ?: return
                    var payload: ByteArray? = null
                    var source = ""

                    // 1) Service data under Open Drone ID service UUID
                    rec.serviceData?.get(odidService)?.let { payload = it; source = "serviceData 0xFFFA" }

                    // 2) ASTM manufacturer-specific data (company id 0xFFFA)
                    if (payload == null) {
                        val mfr = rec.manufacturerSpecificData
                        if (mfr != null) {
                            for (idx in 0 until mfr.size()) {
                                val cid = mfr.keyAt(idx)
                                if (cid == 0xFFFA) { payload = mfr.valueAt(idx); source = "mfrData 0xFFFA"; break }
                            }
                        }
                    }

                    // Only surface adverts that advertise the ODID service, else it's noise.
                    val advertisesOdid = rec.serviceUuids?.any { it == odidService } == true || payload != null
                    if (!advertisesOdid) return

                    val msgType = payload?.let { decodeMessageType(it) }
                    candidates[result.device.address] = RidCandidate(
                        address = result.device.address,
                        name = result.device.name,
                        rssi = result.rssi,
                        source = source.ifEmpty { "service UUID only" },
                        msgType = msgType,
                        hex = payload?.let { toHex(it).take(48) } ?: ""
                    )
                } catch (_: SecurityException) {
                } catch (_: Exception) {}
            }

            override fun onScanFailed(errorCode: Int) {
                errorMsg = "scan failed (code $errorCode)"
                scanning = false
            }
        }
    }

    DisposableEffect(scanning) {
        if (scanning) {
            if (scanner == null) {
                errorMsg = "BLE scanner unavailable"; scanning = false
            } else {
                try {
                    val settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()
                    scanner.startScan(null, settings, callback)
                    errorMsg = null
                } catch (e: SecurityException) {
                    errorMsg = "Bluetooth scan permission denied"; scanning = false
                } catch (e: Exception) {
                    errorMsg = "error: ${e.message}"; scanning = false
                }
            }
        }
        onDispose { try { scanner?.stopScan(callback) } catch (_: Exception) {} }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Drone Remote ID",
            subtitle = "EXPERIMENTAL · BLE only · ${candidates.size} candidates",
            onBack = onBack,
            actions = {
                IconButton(onClick = { scanning = !scanning }) {
                    Icon(
                        if (scanning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = if (scanning) "Stop" else "Start",
                        tint = IceniGold
                    )
                }
            }
        )
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniTeal, trackColor = IceniGreenLight)

        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                if (scanning) "Listening for Drone Remote ID (BLE / ASTM F3411)…" else "Idle. Tap ▶ to listen.",
                style = IceniTypography.bodyMedium, color = IceniTeal
            )
            errorMsg?.let { Text(it, style = IceniTypography.bodyMedium, color = IceniRed) }
            Text("WiFi-beacon RID requires monitor mode — not available on this device.", style = IceniTypography.bodySmall)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val sorted = candidates.values.sortedByDescending { it.rssi }
            items(sorted, key = { it.address }) { c ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row {
                        Text("🛸 ", style = IceniTypography.titleMedium, color = IceniAmber)
                        Text(c.name ?: "<unnamed>", style = IceniTypography.titleMedium, color = IceniAmber, modifier = Modifier.weight(1f))
                        Text("${c.rssi}dBm", style = IceniTypography.bodySmall, color = IceniTeal)
                    }
                    Text("${c.address}  (${c.source})", style = IceniTypography.bodySmall)
                    c.msgType?.let { Text("msg type: $it", style = IceniTypography.bodyMedium, color = IceniTeal) }
                    if (c.hex.isNotEmpty()) Text("payload: ${c.hex}…", style = IceniTypography.bodySmall)
                }
            }
        }
    }
}

private data class RidCandidate(
    val address: String,
    val name: String?,
    val rssi: Int,
    val source: String,
    val msgType: String?,
    val hex: String
)

/**
 * ODID BLE payloads start with an app/message-counter byte (0x0D for the ODID
 * AD application code) followed by the message header; the high nibble of the
 * header byte is the message type. Best-effort decode.
 */
private fun decodeMessageType(data: ByteArray): String {
    if (data.isEmpty()) return "empty"
    // Skip the leading ODID application code byte (0x0D) if present.
    val headerIdx = if (data[0].toInt() and 0xFF == 0x0D && data.size > 1) 1 else 0
    val header = data[headerIdx].toInt() and 0xFF
    val type = (header shr 4) and 0x0F
    return when (type) {
        0 -> "Basic ID (0)"
        1 -> "Location/Vector (1)"
        2 -> "Authentication (2)"
        3 -> "Self-ID (3)"
        4 -> "System (4)"
        5 -> "Operator ID (5)"
        0xF -> "Message Pack (F)"
        else -> "type $type"
    }
}

private fun toHex(b: ByteArray): String =
    b.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
