package com.andraste.tablet.tools.network

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
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

private data class UpnpDevice(val server: String, val location: String, val st: String)

@Composable
fun UpnpScreen(onBack: () -> Unit) {
    var devices by remember { mutableStateOf<List<UpnpDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "UPnP / SSDP",
            subtitle = if (scanning) "M-SEARCH multicast…" else "${devices.size} devices found",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (scanning) {
                        job?.cancel(); scanning = false
                    } else {
                        devices = emptyList(); scanning = true
                        job = scope.launch(Dispatchers.IO) {
                            try {
                                ssdpSearch { dev ->
                                    if (devices.none { it.location == dev.location }) devices = devices + dev
                                }
                            } catch (_: Exception) { }
                            scanning = false
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (scanning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null, tint = IceniGold
                    )
                }
            }
        )

        if (!scanning && devices.isEmpty()) {
            Text(
                "Tap play to broadcast an SSDP M-SEARCH (ssdp:all) and collect responses for ~3s.",
                style = IceniTypography.bodyMedium, color = IceniTextMuted,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(devices) { d ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(d.location.ifBlank { "(no LOCATION)" }, style = IceniTypography.titleMedium, color = IceniTeal)
                    if (d.server.isNotBlank()) Text("SERVER: ${d.server}", style = IceniTypography.bodyMedium, color = IceniText)
                    if (d.st.isNotBlank()) Text("ST: ${d.st}", style = IceniTypography.bodySmall, color = IceniTextMuted)
                }
            }
        }
    }
}

private fun ssdpSearch(onDevice: (UpnpDevice) -> Unit) {
    val msearch = (
        "M-SEARCH * HTTP/1.1\r\n" +
        "HOST: 239.255.255.250:1900\r\n" +
        "MAN: \"ssdp:discover\"\r\n" +
        "MX: 2\r\n" +
        "ST: ssdp:all\r\n\r\n"
    ).toByteArray()

    DatagramSocket().use { socket ->
        socket.soTimeout = 600
        val group = InetAddress.getByName("239.255.255.250")
        socket.send(DatagramPacket(msearch, msearch.size, group, 1900))

        val deadline = System.currentTimeMillis() + 3000
        while (System.currentTimeMillis() < deadline) {
            val buf = ByteArray(2048)
            val pkt = DatagramPacket(buf, buf.size)
            try {
                socket.receive(pkt)
                val text = String(pkt.data, 0, pkt.length, Charsets.ISO_8859_1)
                val server = headerValue(text, "SERVER")
                val location = headerValue(text, "LOCATION")
                val st = headerValue(text, "ST")
                onDevice(UpnpDevice(server, location, st))
            } catch (_: SocketTimeoutException) {
                // keep looping until deadline
            }
        }
    }
}

private fun headerValue(response: String, name: String): String {
    return response.lineSequence()
        .firstOrNull { it.substringBefore(":").trim().equals(name, ignoreCase = true) }
        ?.substringAfter(":")?.trim() ?: ""
}
