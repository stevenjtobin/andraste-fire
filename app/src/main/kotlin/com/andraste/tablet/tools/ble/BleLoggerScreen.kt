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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LogEntry(
    val timestamp: Long,
    val address: String,
    val name: String?,
    val rssi: Int,
    val companyId: String
)

@Composable
fun BleLoggerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    val log = remember { mutableStateListOf<LogEntry>() }
    val seen = remember { mutableStateMapOf<String, Int>() } // key -> last rssi
    var scanning by remember { mutableStateOf(false) }
    var filePath by remember { mutableStateOf<String?>(null) }
    var writer by remember { mutableStateOf<java.io.Writer?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    val fullFmt = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US) }
    val fileTsFmt = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val addr = result.device.address
                val rssi = result.rssi
                val msd = result.scanRecord?.manufacturerSpecificData
                val cid = if (msd != null && msd.size() > 0)
                    "0x%04X".format(msd.keyAt(0)) else ""
                val name = try { result.device.name } catch (e: SecurityException) { null }
                // unique per (addr,rssi,name,mfr) sighting
                val key = "$addr|$rssi|${name ?: ""}|$cid"
                if (seen.containsKey(key)) return
                seen[key] = rssi
                val entry = LogEntry(System.currentTimeMillis(), addr, name, rssi, cid)
                log.add(entry)
                try {
                    writer?.apply {
                        write("${fullFmt.format(Date(entry.timestamp))},$addr," +
                              "${name ?: ""},$rssi,$cid\n")
                        flush()
                    }
                } catch (e: Exception) { }
            }
        }
    }

    DisposableEffect(scanning) {
        if (scanning && scanner != null) {
            try {
                val dir = File(ctx.getExternalFilesDir(null), "andraste")
                dir.mkdirs()
                val ts = fileTsFmt.format(Date())
                val f = File(dir, "ble_log_$ts.csv")
                val w = f.bufferedWriter()
                w.write("timestamp,address,name,rssi,companyId\n")
                w.flush()
                writer = w
                filePath = f.absolutePath

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner.startScan(null, settings, callback)
            } catch (e: SecurityException) {
                hint = "grant Bluetooth/Location in Permissions"
            } catch (e: Exception) {
                hint = "log file error: ${e.message}"
            }
        }
        onDispose {
            try { scanner?.stopScan(callback) } catch (e: SecurityException) { }
            try { writer?.close() } catch (e: Exception) { }
            writer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "BLE Logger",
            subtitle = "${log.size} sightings",
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
        filePath?.let {
            Text("CSV: $it", style = IceniTypography.labelSmall, color = IceniTeal,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
        hint?.let {
            Text(it, style = IceniTypography.bodySmall, color = IceniAmber,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val recent = log.asReversed()
            items(recent, key = { "${it.timestamp}-${it.address}-${it.rssi}" }) { e ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row {
                        Text(e.name ?: "<unnamed>", style = IceniTypography.bodyLarge,
                            modifier = Modifier.weight(1f))
                        Text("${e.rssi} dBm", style = IceniTypography.bodyMedium)
                    }
                    Text("${e.address}   ${e.companyId}", style = IceniTypography.labelSmall)
                }
            }
        }
    }
}
