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
import java.net.ServerSocket

@Composable
fun TcpListenScreen(onBack: () -> Unit) {
    var port by remember { mutableStateOf("8080") }
    var logs by remember { mutableStateOf<List<String>>(emptyList()) }
    var listening by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var server by remember { mutableStateOf<ServerSocket?>(null) }
    val scope = rememberCoroutineScope()

    fun stop() {
        job?.cancel()
        try { server?.close() } catch (_: Exception) {}
        server = null
        listening = false
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "TCP Listener",
            subtitle = if (listening) "listening on :${port}" else "${logs.size} log lines",
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (listening) {
                        stop()
                    } else {
                        val p = port.toIntOrNull()
                        if (p == null) {
                            logs = logs + "ERROR: invalid port"
                        } else {
                            listening = true
                            job = scope.launch(Dispatchers.IO) {
                                try {
                                    val ss = ServerSocket(p)
                                    server = ss
                                    logs = logs + "[listening on 0.0.0.0:$p]"
                                    while (isActive) {
                                        val client = ss.accept()
                                        val remote = client.remoteSocketAddress.toString()
                                        client.soTimeout = 1500
                                        val buf = ByteArray(256)
                                        val n = try { client.getInputStream().read(buf) } catch (_: Exception) { -1 }
                                        val preview = if (n > 0) {
                                            String(buf, 0, n, Charsets.ISO_8859_1)
                                                .map { if (it.code in 32..126) it else '.' }
                                                .joinToString("")
                                                .take(80)
                                        } else "(no data)"
                                        logs = logs + "$remote  →  $preview"
                                        try { client.close() } catch (_: Exception) {}
                                    }
                                } catch (e: Exception) {
                                    if (isActive) logs = logs + "ERROR: ${e.message}"
                                    listening = false
                                }
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (listening) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = null, tint = IceniGold
                    )
                }
            }
        )

        OutlinedTextField(
            value = port, onValueChange = { port = it },
            label = { Text("Listen port", color = IceniTextMuted) },
            singleLine = true, enabled = !listening,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = IceniText, unfocusedTextColor = IceniText,
                focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
            ),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(logs) { l ->
                Text(
                    text = l,
                    style = IceniTypography.bodyMedium,
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
            try { server?.close() } catch (_: Exception) {}
        }
    }
}
