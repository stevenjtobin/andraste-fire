package com.andraste.tablet.tools.system

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.hal.CapabilityDetector
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

data class InfoRow(val label: String, val value: String, val ok: Boolean? = null)

@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val caps = remember { CapabilityDetector.detect(ctx) }
    val usb = remember { CapabilityDetector.usbDevices(ctx) }

    val rows = buildList {
        add(InfoRow("Model",        "${Build.MANUFACTURER} ${Build.MODEL}"))
        add(InfoRow("Device",       Build.DEVICE))
        add(InfoRow("Product",      Build.PRODUCT))
        add(InfoRow("Android",      "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"))
        add(InfoRow("ABI",          Build.SUPPORTED_ABIS.joinToString(", ")))
        add(InfoRow("Fingerprint",  Build.FINGERPRINT))
        add(InfoRow("— Radios —",   ""))
        add(InfoRow("WiFi",         if (caps.hasWifi) "present" else "absent", caps.hasWifi))
        add(InfoRow("Bluetooth",    if (caps.hasBluetooth) "present" else "absent", caps.hasBluetooth))
        add(InfoRow("GPS",          if (caps.hasGps) "present" else "absent", caps.hasGps))
        add(InfoRow("NFC",          if (caps.hasNfc) "present" else "absent (expected)", caps.hasNfc))
        add(InfoRow("— Host —",     ""))
        add(InfoRow("Camera",       if (caps.hasCamera) "present" else "absent", caps.hasCamera))
        add(InfoRow("USB Host/OTG", if (caps.hasUsbHost) "supported" else "unsupported", caps.hasUsbHost))
        add(InfoRow("External SD",  if (caps.hasExternalStorage) "mounted" else "none", caps.hasExternalStorage))
        add(InfoRow("Root (su)",    if (caps.hasRoot) "ROOTED" else "not rooted", caps.hasRoot))
        add(InfoRow("Termux",       if (caps.isTermuxInstalled) "installed" else "not installed", caps.isTermuxInstalled))
        add(InfoRow("— USB devices —", ""))
        if (usb.isEmpty()) add(InfoRow("(none attached)", ""))
        else usb.forEach { add(InfoRow("USB", it)) }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "Device Info", subtitle = "hardware & capabilities", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(rows) { r ->
                if (r.value.isEmpty()) {
                    Text(
                        text = r.label,
                        style = IceniTypography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreen, RoundedCornerShape(4.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(text = r.label, style = IceniTypography.titleMedium, modifier = Modifier.width(120.dp))
                        Text(
                            text = r.value,
                            style = IceniTypography.bodyMedium,
                            color = when (r.ok) {
                                true  -> IceniTeal
                                false -> IceniTextHint
                                null  -> IceniTextMuted
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
