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
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedQueue

private data class SourAdv(
    val address: String,
    val rssi: Int,
    val time: Long
)

// "Sour Apple": Apple 0x004C proximity-pairing type 0x07 flooded from many MACs.
private fun isSourApple(result: ScanResult): Boolean {
    val msd = result.scanRecord?.manufacturerSpecificData ?: return false
    for (i in 0 until msd.size()) {
        if (msd.keyAt(i) == 0x004C) {
            val data = msd.valueAt(i)
            val t = if (data != null && data.isNotEmpty()) data[0].toInt() and 0xFF else -1
            if (t == 0x07) return true
        }
    }
    return false
}

@Composable
fun SourAppleScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var scanning by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    val recent = remember { ConcurrentLinkedQueue<SourAdv>() }
    var offenders by remember { mutableStateOf<List<SourAdv>>(emptyList()) }
    var rate by remember { mutableStateOf(0) }
    var distinctMacs by remember { mutableStateOf(0) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (isSourApple(result)) {
                    recent.add(SourAdv(result.device.address, result.rssi, System.currentTimeMillis()))
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

    LaunchedEffect(scanning) {
        while (scanning) {
            val now = System.currentTimeMillis()
            recent.removeIf { it.time < now - 1000 }
            val window = recent.toList()
            rate = window.size
            distinctMacs = window.map { it.address }.distinct().size
            offenders = window.sortedByDescending { it.rssi }.take(50)
            delay(300)
        }
        if (!scanning) { rate = 0; distinctMacs = 0 }
    }

    val attack = rate >= 15 && distinctMacs >= 8

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "Sour Apple",
            subtitle = "iOS proximity-pairing crash detector",
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

        // Warning banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(
                    if (attack) IceniRed else IceniGreen,
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    if (attack) "SOUR APPLE ATTACK DETECTED" else "monitoring...",
                    style = IceniTypography.titleLarge,
                    color = if (attack) IceniDeepGreen else IceniTextMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$rate type-0x07 adverts/s from $distinctMacs MACs",
                    style = IceniTypography.bodyMedium,
                    color = if (attack) IceniDeepGreen else IceniTextMuted
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(offenders, key = { "${it.address}-${it.time}" }) { adv ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(adv.address, style = IceniTypography.bodyMedium,
                        color = IceniRed, modifier = Modifier.weight(1f))
                    Text("${adv.rssi} dBm", style = IceniTypography.bodySmall)
                }
            }
        }
    }
}
