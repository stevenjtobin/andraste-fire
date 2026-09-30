package com.andraste.tablet.tools.ble

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

private data class ClassicDevice(
    val address: String,
    val name: String?,
    val deviceClass: String,
    val rssi: Int
)

private fun majorClassName(majorClass: Int): String = when (majorClass) {
    BluetoothClass.Device.Major.PHONE -> "Phone"
    BluetoothClass.Device.Major.COMPUTER -> "Computer"
    BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video"
    BluetoothClass.Device.Major.WEARABLE -> "Wearable"
    BluetoothClass.Device.Major.HEALTH -> "Health"
    BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
    BluetoothClass.Device.Major.IMAGING -> "Imaging"
    BluetoothClass.Device.Major.NETWORKING -> "Networking"
    BluetoothClass.Device.Major.TOY -> "Toy"
    BluetoothClass.Device.Major.MISC -> "Misc"
    BluetoothClass.Device.Major.UNCATEGORIZED -> "Uncategorized"
    else -> "0x%X".format(majorClass)
}

@Composable
fun ClassicBtScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val btMgr = remember { ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val adapter = remember { btMgr.adapter }
    var devices by remember { mutableStateOf<Map<String, ClassicDevice>>(emptyMap()) }
    var discovering by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }

    val receiver = remember {
        object : BroadcastReceiver() {
            @Suppress("DEPRECATION")
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    val btClass: BluetoothClass? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS)
                    if (device != null) {
                        val name = try { device.name } catch (e: SecurityException) { null }
                        val cls = btClass?.let { majorClassName(it.majorDeviceClass) } ?: "?"
                        devices = devices + (device.address to ClassicDevice(
                            address = device.address,
                            name = name,
                            deviceClass = cls,
                            rssi = rssi
                        ))
                    }
                }
            }
        }
    }

    DisposableEffect(discovering) {
        if (discovering && adapter != null) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            ContextCompat.registerReceiver(
                ctx, receiver, filter, ContextCompat.RECEIVER_EXPORTED
            )
            try {
                if (adapter.isDiscovering) adapter.cancelDiscovery()
                val ok = adapter.startDiscovery()
                if (!ok) hint = "startDiscovery() returned false"
            } catch (e: SecurityException) {
                hint = "grant Bluetooth/Location in Permissions"
            }
        }
        onDispose {
            try { adapter?.cancelDiscovery() } catch (e: SecurityException) { }
            try { ctx.unregisterReceiver(receiver) } catch (e: IllegalArgumentException) { }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(
            title = "Classic BT",
            subtitle = "${devices.size} classic devices",
            onBack = onBack,
            actions = {
                IconButton(onClick = { discovering = !discovering }) {
                    Icon(
                        imageVector = if (discovering) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = if (discovering) "Stop" else "Start",
                        tint = IceniGold
                    )
                }
            }
        )
        if (discovering) {
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
            val sorted = devices.values.sortedByDescending { it.rssi }
            items(sorted, key = { it.address }) { dev ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row {
                        Text(dev.name ?: "<unnamed>", style = IceniTypography.titleMedium,
                            modifier = Modifier.weight(1f))
                        val rssiText = if (dev.rssi == Short.MIN_VALUE.toInt()) "n/a"
                                       else "${dev.rssi} dBm"
                        Text(rssiText, style = IceniTypography.bodyMedium)
                    }
                    Text("class: ${dev.deviceClass}", style = IceniTypography.bodySmall,
                        color = IceniTeal)
                    Text(dev.address, style = IceniTypography.labelSmall)
                }
            }
        }
    }
}
