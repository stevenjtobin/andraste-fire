package com.andraste.tablet.tools.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedQueue

private class SpamSample(val time: Long, val mac: String)

// Classifies whether an advert is a known BLE-spam vector.
private fun isSpamVector(result: ScanResult): Boolean {
    val rec = result.scanRecord ?: return false
    val msd = rec.manufacturerSpecificData
    if (msd != null && msd.size() > 0) {
        for (i in 0 until msd.size()) {
            val cid = msd.keyAt(i)
            val data = msd.valueAt(i)
            when (cid) {
                0x004C -> { // Apple Continuity proximity-pairing type 0x07 / 0x0F
                    val t = if (data != null && data.isNotEmpty()) data[0].toInt() and 0xFF else -1
                    if (t == 0x07 || t == 0x0F) return true
                }
                0x0006 -> return true // Microsoft Swift Pair
            }
        }
    }
    // Google Fast Pair service data 0xFE2C
    rec.serviceUuids?.forEach { pu ->
        val s = pu.uuid.toString().lowercase()
        if (s.startsWith("0000fe2c")) return true
    }
    return false
}

@Composable
fun BleSpamDetectorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val scanner = remember { btMgr.adapter?.bluetoothLeScanner }
    var scanning by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    val samples = remember { ConcurrentLinkedQueue<SpamSample>() }
    var rate by remember { mutableStateOf(0) }
    var distinctMacs by remember { mutableStateOf(0) }

    val callback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (isSpamVector(result)) {
                    samples.add(SpamSample(System.currentTimeMillis(), result.device.address))
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

    // Sliding 1-second window rate meter
    LaunchedEffect(scanning) {
        while (scanning) {
            val now = System.currentTimeMillis()
            val cutoff = now - 1000
            samples.removeIf { it.time < cutoff }
            val window = samples.toList()
            rate = window.size
            distinctMacs = window.map { it.mac }.distinct().size
            delay(250)
        }
        if (!scanning) { rate = 0; distinctMacs = 0 }
    }

    // Verdict: many packets/s AND many distinct random MACs => SPAM
    val verdict: String
    val verdictColor: androidx.compose.ui.graphics.Color
    when {
        rate >= 25 && distinctMacs >= 10 -> { verdict = "SPAM"; verdictColor = IceniRed }
        rate >= 8 || distinctMacs >= 5 -> { verdict = "ELEVATED"; verdictColor = IceniAmber }
        else -> { verdict = "CALM"; verdictColor = RiskPassive }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "BLE Spam Detector",
            subtitle = "adv spam / crash-attack monitor",
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Text(verdict,
                style = IceniTypography.headlineLarge.copy(fontSize = 48.sp),
                color = verdictColor,
                textAlign = TextAlign.Center)

            // Rate meter
            val frac = (rate / 40f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(IceniGreenLight, RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .background(verdictColor, RoundedCornerShape(6.dp))
                )
            }

            Text("$rate spam adverts / sec", style = IceniTypography.headlineMedium)
            Text("$distinctMacs distinct MACs (last 1s)", style = IceniTypography.bodyLarge)

            Text(
                "Watches Apple proximity-pairing (0x07/0x0F), Google Fast Pair (0xFE2C) " +
                "and Microsoft Swift Pair (0x0006) floods from many random MACs.",
                style = IceniTypography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
