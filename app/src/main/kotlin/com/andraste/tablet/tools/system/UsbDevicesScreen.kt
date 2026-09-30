package com.andraste.tablet.tools.system

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

private data class UsbInfo(
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturer: String?,
    val product: String?,
    val deviceClass: Int,
    val interfaceCount: Int,
    val known: String?
)

@Composable
fun UsbDevicesScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var devices by remember { mutableStateOf<List<UsbInfo>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        error = null
        devices = try {
            val mgr = ctx.getSystemService(Context.USB_SERVICE) as? UsbManager
            if (mgr == null) {
                error = "USB service unavailable"
                emptyList()
            } else {
                mgr.deviceList.values.map { it.toInfo() }.sortedBy { it.name }
            }
        } catch (e: Exception) {
            error = e.message
            emptyList()
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "USB Devices",
            subtitle = "OTG host enumeration",
            onBack = onBack,
            actions = {
                TextButton(onClick = { refresh() }) {
                    Text("RESCAN", color = IceniTeal, style = IceniTypography.labelSmall)
                }
            }
        )

        error?.let {
            Text("Error: $it", style = IceniTypography.bodyMedium, color = IceniRed,
                modifier = Modifier.padding(12.dp))
        }

        if (devices.isEmpty() && error == null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No OTG devices attached", style = IceniTypography.titleMedium, color = IceniTextMuted)
                Text("Plug in via USB-OTG, then tap RESCAN.", style = IceniTypography.bodyMedium, color = IceniTextHint)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(devices) { d ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = IceniGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                d.product ?: d.name,
                                style = IceniTypography.titleMedium
                            )
                            d.known?.let {
                                Text("★ $it", style = IceniTypography.labelSmall, color = IceniGold)
                            }
                            Spacer(Modifier.height(4.dp))
                            UsbRow("VID:PID", "%04x:%04x".format(d.vendorId, d.productId))
                            UsbRow("Vendor", d.manufacturer ?: "(unknown)")
                            UsbRow("Device name", d.name)
                            UsbRow("Class", "${d.deviceClass} (${usbClassName(d.deviceClass)})")
                            UsbRow("Interfaces", d.interfaceCount.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsbRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(label, style = IceniTypography.bodySmall, color = IceniTextHint, modifier = Modifier.width(110.dp))
        Text(value, style = IceniTypography.bodyMedium, color = IceniTextMuted, modifier = Modifier.weight(1f))
    }
}

private fun UsbDevice.toInfo(): UsbInfo {
    val manu = try { manufacturerName } catch (_: Exception) { null }
    val prod = try { productName } catch (_: Exception) { null }
    return UsbInfo(
        name = deviceName,
        vendorId = vendorId,
        productId = productId,
        manufacturer = manu,
        product = prod,
        deviceClass = deviceClass,
        interfaceCount = try { interfaceCount } catch (_: Exception) { 0 },
        known = knownDevice(vendorId, productId)
    )
}

private fun knownDevice(vid: Int, pid: Int): String? = when {
    vid == 0x0bda && (pid == 0x2838 || pid == 0x2832) -> "RTL-SDR (Realtek RTL2832U)"
    vid == 0x0bda -> "Realtek (WiFi / RTL-SDR family)"
    vid == 0x0cf3 -> "Atheros / Alfa WiFi"
    vid == 0x148f -> "Ralink / MediaTek WiFi"
    else -> null
}

private fun usbClassName(cls: Int): String = when (cls) {
    0 -> "per-interface"
    1 -> "audio"
    2 -> "comm/CDC"
    3 -> "HID"
    8 -> "mass storage"
    9 -> "hub"
    10 -> "CDC data"
    0xE0 -> "wireless"
    0xEF -> "misc"
    0xFF -> "vendor-specific"
    else -> "0x%02X".format(cls)
}
