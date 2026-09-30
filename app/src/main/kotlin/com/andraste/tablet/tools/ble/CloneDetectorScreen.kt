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

private data class CloneGroup(
    val signature: String,
    val macs: Set<String>
)

private fun advSignature(result: ScanResult): String {
    val rec = result.scanRecord
    val name = (try { result.device.name } catch (e: SecurityException) { null }
        ?: rec?.deviceName ?: "<unnamed>")
    val svc = rec?.serviceUuids
        ?.map { it.uuid.toString() }
        ?.sorted()
        ?.joinToString(",")
        ?: ""
    // include appearance-ish hint: first company id if present
    val msd = rec?.manufacturerSpecificData
    val cid = if (msd != null && msd.size() > 0) "0x%04X".format(msd.keyAt(0)) else ""
    return "name=$name | svc=[$svc] | mfr=$cid"
}

@Composable
fun CloneDetectorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var scanning by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    // signature -> set of MACs
    var groups by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val sig = advSignature(result)
                val addr = result.device.address
                val cur = groups[sig] ?: emptySet()
                if (!cur.contains(addr)) {
                    groups = groups + (sig to (cur + addr))
                }
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

    val cloneGroups = groups
        .filter { it.value.size > 1 }
        .map { CloneGroup(it.key, it.value) }
        .sortedByDescending { it.macs.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "Clone Detector",
            subtitle = "${cloneGroups.size} spoof/clone groups",
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(cloneGroups, key = { it.signature }) { grp ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row {
                        Text(
                            "${grp.macs.size}x SAME ADVERT",
                            style = IceniTypography.titleMedium,
                            color = IceniAmber,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(grp.signature, style = IceniTypography.bodySmall, color = IceniAmber)
                    grp.macs.forEach { mac ->
                        Text("  $mac", style = IceniTypography.labelSmall)
                    }
                }
            }
        }
    }
}
