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
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

// Minimal, best-effort MQTT 3.1.1 client over a raw socket:
// anonymous CONNECT, single SUBSCRIBE, then print received PUBLISH payloads.
@Composable
fun MqttScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("1883") }
    var topic by remember { mutableStateOf("#") }
    var log by remember { mutableStateOf<List<String>>(emptyList()) }
    var connected by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var sock by remember { mutableStateOf<Socket?>(null) }
    val scope = rememberCoroutineScope()

    fun stop() {
        job?.cancel()
        try { sock?.close() } catch (_: Exception) {}
        sock = null
        connected = false
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "MQTT (best-effort)",
            subtitle = if (connected) "subscribed to $topic" else "raw MQTT 3.1.1 · anonymous",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (connected) {
                        stop()
                    } else {
                        log = emptyList(); connected = true
                        job = scope.launch(Dispatchers.IO) {
                            try {
                                val s = Socket()
                                s.connect(InetSocketAddress(host, port.toIntOrNull() ?: 1883), 6000)
                                s.soTimeout = 0
                                sock = s
                                val out = s.getOutputStream()
                                val ins = s.getInputStream()

                                out.write(buildConnect("andraste-" + (System.currentTimeMillis() % 100000)))
                                out.flush()

                                val connack = readPacket(ins)
                                if (connack == null || (connack.first and 0xF0) != 0x20) {
                                    log = log + "ERROR: no CONNACK"
                                    connected = false; return@launch
                                }
                                val rc = if (connack.second.size >= 2) connack.second[1].toInt() and 0xFF else -1
                                log = log + "CONNACK code=$rc (${connackText(rc)})"
                                if (rc != 0) { connected = false; return@launch }

                                out.write(buildSubscribe(topic))
                                out.flush()
                                val suback = readPacket(ins)
                                if (suback != null && (suback.first and 0xF0) == 0x90) {
                                    log = log + "SUBACK ok — waiting for messages…"
                                } else {
                                    log = log + "WARN: no SUBACK"
                                }

                                while (isActive) {
                                    val pkt = readPacket(ins) ?: break
                                    val type = pkt.first and 0xF0
                                    if (type == 0x30) {
                                        val (t, payload) = parsePublish(pkt.first, pkt.second)
                                        log = log + "[$t] $payload"
                                    } else if (type == 0xD0) {
                                        // PINGRESP, ignore
                                    }
                                }
                            } catch (e: Exception) {
                                if (isActive) log = log + "ERROR: ${e.message}"
                            } finally {
                                connected = false
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (connected) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null, tint = IceniGold
                    )
                }
            }
        )

        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            mqttField(host, { host = it }, "Broker host", Modifier.weight(2f), connected)
            mqttField(port, { port = it }, "Port", Modifier.weight(1f), connected)
        }
        mqttField(topic, { topic = it }, "Topic filter", Modifier.fillMaxWidth().padding(horizontal = 12.dp), connected)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(log) { l ->
                Text(
                    l, style = IceniTypography.bodyMedium,
                    color = if (l.startsWith("ERROR")) IceniRed else IceniText,
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            job?.cancel()
            try { sock?.close() } catch (_: Exception) {}
        }
    }
}

@Composable
private fun mqttField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, disabled: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, color = IceniTextMuted) }, singleLine = true, enabled = !disabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = IceniText, unfocusedTextColor = IceniText,
            focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
        ),
        modifier = modifier
    )
}

private fun encodeRemainingLength(length: Int): ByteArray {
    var x = length
    val out = ArrayList<Byte>()
    do {
        var b = x % 128
        x /= 128
        if (x > 0) b = b or 128
        out.add(b.toByte())
    } while (x > 0)
    return out.toByteArray()
}

private fun mqttString(s: String): ByteArray {
    val b = s.toByteArray(Charsets.UTF_8)
    return byteArrayOf((b.size shr 8).toByte(), (b.size and 0xFF).toByte()) + b
}

private fun buildConnect(clientId: String): ByteArray {
    val varHeader = mqttString("MQTT") +
        byteArrayOf(0x04) +          // protocol level 4 (3.1.1)
        byteArrayOf(0x02) +          // connect flags: clean session
        byteArrayOf(0x00, 0x3C)      // keepalive 60s
    val payload = mqttString(clientId)
    val body = varHeader + payload
    return byteArrayOf(0x10) + encodeRemainingLength(body.size) + body
}

private fun buildSubscribe(topic: String): ByteArray {
    val packetId = byteArrayOf(0x00, 0x01)
    val body = packetId + mqttString(topic) + byteArrayOf(0x00) // QoS 0
    return byteArrayOf(0x82.toByte()) + encodeRemainingLength(body.size) + body
}

private fun readPacket(ins: InputStream): Pair<Int, ByteArray>? {
    val first = ins.read()
    if (first < 0) return null
    var multiplier = 1
    var length = 0
    while (true) {
        val b = ins.read()
        if (b < 0) return null
        length += (b and 127) * multiplier
        if ((b and 128) == 0) break
        multiplier *= 128
    }
    val buf = ByteArray(length)
    var off = 0
    while (off < length) {
        val n = ins.read(buf, off, length - off)
        if (n < 0) return null
        off += n
    }
    return Pair(first, buf)
}

private fun parsePublish(first: Int, body: ByteArray): Pair<String, String> {
    if (body.size < 2) return Pair("?", "")
    val topicLen = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF)
    val topic = String(body, 2, minOf(topicLen, body.size - 2), Charsets.UTF_8)
    val qos = (first shr 1) and 0x03
    var idx = 2 + topicLen
    if (qos > 0) idx += 2 // packet identifier present for QoS > 0
    val payload = if (idx < body.size) String(body, idx, body.size - idx, Charsets.UTF_8) else ""
    return Pair(topic, payload)
}

private fun connackText(rc: Int) = when (rc) {
    0 -> "accepted"
    1 -> "unacceptable protocol version"
    2 -> "identifier rejected"
    3 -> "server unavailable"
    4 -> "bad username/password"
    5 -> "not authorized"
    else -> "unknown"
}

@Suppress("unused")
private fun OutputStream.pingReq() = write(byteArrayOf(0xC0.toByte(), 0x00))
