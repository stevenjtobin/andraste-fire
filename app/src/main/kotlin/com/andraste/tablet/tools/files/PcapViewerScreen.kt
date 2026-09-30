package com.andraste.tablet.tools.files

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

private const val PCAP_MAX_PACKETS = 2000

private data class PcapPkt(
    val idx: Int,
    val time: String,
    val src: String,
    val dst: String,
    val proto: String,
    val len: Int
)

private data class PcapResult(
    val summary: String,
    val packets: List<PcapPkt>,
    val note: String?
)

@Composable
fun PcapViewerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var fileName by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<PcapResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        error = null
        result = null
        var name = "capture.pcap"
        try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && ni >= 0) name = c.getString(ni) ?: name
            }
        } catch (_: Exception) {
        }
        fileName = name
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                try {
                    val stream = ctx.contentResolver.openInputStream(uri)
                        ?: return@withContext PcapResult("", emptyList(), "Cannot open file")
                    stream.use { parsePcap(it) }
                } catch (e: Exception) {
                    PcapResult("", emptyList(), "Parse error: ${e.message}")
                }
            }
            busy = false
            result = r
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "PCAP Viewer", subtitle = fileName ?: "libpcap capture", onBack = onBack)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Button(
                onClick = { try { picker.launch("*/*") } catch (e: Exception) { error = e.message } },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen)
            ) { Text(if (busy) "Parsing…" else "Pick .pcap") }
        }

        if (busy) LinearProgressIndicator(
            color = IceniGold, trackColor = IceniGreenLight,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
        error?.let {
            Text("Error: $it", style = IceniTypography.bodyMedium, color = IceniRed,
                modifier = Modifier.padding(12.dp))
        }

        val r = result
        if (r != null) {
            if (r.summary.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IceniGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(Modifier.padding(10.dp)) {
                        Text("Global header", style = IceniTypography.labelLarge)
                        Text(r.summary, style = IceniTypography.bodySmall, color = IceniTextMuted)
                        Text("${r.packets.size} packet(s) decoded", style = IceniTypography.bodySmall, color = IceniTeal)
                    }
                }
            }
            r.note?.let {
                Text(it, style = IceniTypography.bodySmall, color = IceniAmber,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
            if (r.packets.isNotEmpty()) {
                Text(
                    "  #     time            src → dst                         proto   len",
                    style = IceniTypography.labelSmall, color = IceniGold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(r.packets) { p ->
                        Text(
                            text = "%-5d %-15s %-32s %-7s %d".format(
                                p.idx, p.time, "${p.src} → ${p.dst}", p.proto, p.len
                            ),
                            style = IceniTypography.bodySmall,
                            color = protoColor(p.proto)
                        )
                    }
                }
            }
        } else if (!busy) {
            Text(
                "Pick a .pcap file to decode its packets. Ethernet + IPv4 (TCP/UDP/ICMP) are parsed; " +
                    "both endian magics (d4c3b2a1 / a1b2c3d4) are supported.",
                style = IceniTypography.bodyMedium, color = IceniTextMuted,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun protoColor(proto: String) = when (proto) {
    "TCP" -> IceniTeal
    "UDP" -> IceniBlue
    "ICMP" -> IceniAmber
    "ARP" -> IceniTextMuted
    else -> IceniTextHint
}

// ---- Parser ----

private fun parsePcap(input: InputStream): PcapResult {
    val bin = input.buffered()

    fun readN(n: Int): ByteArray? {
        val arr = ByteArray(n)
        var off = 0
        while (off < n) {
            val read = bin.read(arr, off, n - off)
            if (read < 0) break
            off += read
        }
        return if (off == n) arr else null
    }

    val gh = readN(24) ?: return PcapResult("", emptyList(), "File too short for a pcap global header")
    val m0 = gh[0].toInt() and 0xFF
    val m1 = gh[1].toInt() and 0xFF
    val m2 = gh[2].toInt() and 0xFF
    val m3 = gh[3].toInt() and 0xFF
    val little: Boolean
    val nano: Boolean
    when {
        m0 == 0xD4 && m1 == 0xC3 && m2 == 0xB2 && m3 == 0xA1 -> { little = true; nano = false }
        m0 == 0xA1 && m1 == 0xB2 && m2 == 0xC3 && m3 == 0xD4 -> { little = false; nano = false }
        m0 == 0x4D && m1 == 0x3C && m2 == 0xB2 && m3 == 0xA1 -> { little = true; nano = true }
        m0 == 0xA1 && m1 == 0xB2 && m2 == 0x3C && m3 == 0x4D -> { little = false; nano = true }
        else -> return PcapResult(
            "", emptyList(),
            "Not a libpcap file (magic %02X%02X%02X%02X)".format(m0, m1, m2, m3)
        )
    }

    val snaplen = u32(gh, 16, little)
    val linktype = u32(gh, 20, little)
    val summary = "endian=${if (little) "LE" else "BE"}${if (nano) " (ns)" else ""}  " +
        "snaplen=$snaplen  linktype=$linktype (${linkName(linktype)})"

    val packets = ArrayList<PcapPkt>()
    var note: String? = null
    var count = 0
    while (count < PCAP_MAX_PACKETS) {
        val rh = readN(16) ?: break
        val tsSec = u32(rh, 0, little)
        val tsFrac = u32(rh, 4, little)
        val inclLen = u32(rh, 8, little).toInt()
        if (inclLen <= 0 || inclLen > 2_000_000) {
            note = "Stopped: implausible packet length at record #${count + 1}"
            break
        }
        val data = readN(inclLen)
        if (data == null) {
            note = "File truncated at packet #${count + 1}"
            break
        }
        count++
        val micros = if (nano) tsFrac / 1000 else tsFrac
        val time = "%d.%06d".format(tsSec, micros)
        val decoded = decodeFrame(linktype, data)
        packets.add(PcapPkt(count, time, decoded.first, decoded.second, decoded.third, inclLen))
    }
    if (count >= PCAP_MAX_PACKETS && note == null) {
        note = "Showing first $PCAP_MAX_PACKETS packets (file may contain more)"
    }
    if (packets.isEmpty() && note == null) note = "No packet records found"
    return PcapResult(summary, packets, note)
}

/** Returns (src, dst, proto). */
private fun decodeFrame(linktype: Long, d: ByteArray): Triple<String, String, String> {
    if (linktype != 1L) {
        return Triple("-", "-", "LINK$linktype")
    }
    if (d.size < 14) return Triple("-", "-", "ETH?")
    val dstMac = mac(d, 0)
    val srcMac = mac(d, 6)
    val ether = be16(d, 12)
    return when (ether) {
        0x0800 -> decodeIpv4(d, 14) ?: Triple(srcMac, dstMac, "IPv4?")
        0x0806 -> Triple(srcMac, dstMac, "ARP")
        0x86DD -> Triple(srcMac, dstMac, "IPv6")
        else -> Triple(srcMac, dstMac, "0x%04X".format(ether))
    }
}

private fun decodeIpv4(d: ByteArray, off: Int): Triple<String, String, String>? {
    if (d.size < off + 20) return null
    val ihl = (d[off].toInt() and 0x0F) * 4
    if (ihl < 20) return null
    val protoNum = d[off + 9].toInt() and 0xFF
    val srcIp = ipv4(d, off + 12)
    val dstIp = ipv4(d, off + 16)
    val protoName = when (protoNum) {
        1 -> "ICMP"
        6 -> "TCP"
        17 -> "UDP"
        else -> "IP($protoNum)"
    }
    return if ((protoNum == 6 || protoNum == 17) && d.size >= off + ihl + 4) {
        val sp = be16(d, off + ihl)
        val dp = be16(d, off + ihl + 2)
        Triple("$srcIp:$sp", "$dstIp:$dp", protoName)
    } else {
        Triple(srcIp, dstIp, protoName)
    }
}

private fun u16(b: ByteArray, o: Int, little: Boolean): Int {
    val b0 = b[o].toInt() and 0xFF
    val b1 = b[o + 1].toInt() and 0xFF
    return if (little) (b1 shl 8) or b0 else (b0 shl 8) or b1
}

private fun u32(b: ByteArray, o: Int, little: Boolean): Long {
    val b0 = (b[o].toInt() and 0xFF).toLong()
    val b1 = (b[o + 1].toInt() and 0xFF).toLong()
    val b2 = (b[o + 2].toInt() and 0xFF).toLong()
    val b3 = (b[o + 3].toInt() and 0xFF).toLong()
    return if (little) (b3 shl 24) or (b2 shl 16) or (b1 shl 8) or b0
    else (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
}

private fun be16(b: ByteArray, o: Int): Int = ((b[o].toInt() and 0xFF) shl 8) or (b[o + 1].toInt() and 0xFF)

private fun ipv4(b: ByteArray, o: Int): String =
    "${b[o].toInt() and 0xFF}.${b[o + 1].toInt() and 0xFF}.${b[o + 2].toInt() and 0xFF}.${b[o + 3].toInt() and 0xFF}"

private fun mac(b: ByteArray, o: Int): String =
    (0 until 6).joinToString(":") { "%02x".format(b[o + it].toInt() and 0xFF) }

private fun linkName(lt: Long): String = when (lt) {
    0L -> "NULL/loopback"
    1L -> "Ethernet"
    101L -> "Raw IP"
    105L -> "802.11"
    113L -> "Linux SLL"
    127L -> "802.11 radiotap"
    else -> "unknown"
}
