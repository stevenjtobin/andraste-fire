package com.andraste.tablet.tools.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class ArpRow(val ip: String, val mac: String, val flags: String, val iface: String)

@Composable
fun ArpScanScreen(onBack: () -> Unit) {
    var rows by remember { mutableStateOf<List<ArpRow>>(emptyList()) }
    var note by remember { mutableStateOf("tap refresh to read /proc/net/arp") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        busy = true
        scope.launch {
            val (parsed, msg) = readArpTable()
            rows = parsed
            note = msg
            busy = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "ARP Scan",
            subtitle = if (rows.isEmpty()) note else "${rows.size} entries in ARP cache",
            onBack = onBack,
            actions = {
                IconButton(onClick = { if (!busy) refresh() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = IceniGold)
                }
            }
        )

        if (rows.isEmpty()) {
            Text(
                note,
                style = IceniTypography.bodyMedium,
                color = IceniTextMuted,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(rows) { r ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(r.ip, style = IceniTypography.titleMedium, color = IceniTeal)
                    Text("MAC  ${r.mac}", style = IceniTypography.bodyMedium, color = IceniText)
                    Text("iface ${r.iface}   flags ${r.flags}", style = IceniTypography.bodySmall, color = IceniTextMuted)
                }
            }
        }
    }
}

private suspend fun readArpTable(): Pair<List<ArpRow>, String> = withContext(Dispatchers.IO) {
    try {
        val file = File("/proc/net/arp")
        if (!file.exists() || !file.canRead()) {
            return@withContext Pair(emptyList(), "/proc/net/arp not readable on this device (0 rows — restricted since Android 10)")
        }
        val lines = file.readLines()
        if (lines.size <= 1) {
            return@withContext Pair(emptyList(), "ARP cache empty (0 rows — restricted on modern Android)")
        }
        val out = lines.drop(1).mapNotNull { line ->
            val cols = line.trim().split(Regex("\\s+"))
            if (cols.size >= 6) ArpRow(cols[0], cols[3], cols[2], cols[5]) else null
        }.filter { it.mac != "00:00:00:00:00:00" }
        if (out.isEmpty()) Pair(out, "ARP cache had no resolved entries (0 rows)")
        else Pair(out, "${out.size} entries")
    } catch (e: Exception) {
        Pair(emptyList(), "ERROR: ${e.message} (0 rows)")
    }
}
