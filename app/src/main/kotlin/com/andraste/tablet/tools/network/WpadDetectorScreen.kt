package com.andraste.tablet.tools.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

// Passive / read-only WPAD exposure check. A resolvable "wpad" host serving a
// wpad.dat PAC file is a classic proxy-hijack (MITM) vector.
@Composable
fun WpadDetectorScreen(onBack: () -> Unit) {
    var domain by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "WPAD Detector",
            subtitle = if (busy) "probing…" else if (output.isEmpty()) "passive proxy-autoconfig check"
                       else if (finding) "⚠ WPAD exposure detected" else "no WPAD exposure",
            onBack = onBack
        )

        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = domain, onValueChange = { domain = it },
                label = { Text("Optional DNS suffix (e.g. corp.local)", color = IceniTextMuted) }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceniText, unfocusedTextColor = IceniText,
                    focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim, cursorColor = IceniGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    busy = true; output = ""
                    scope.launch {
                        val (report, hit) = detectWpad(domain.trim())
                        output = report; finding = hit; busy = false
                    }
                },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("CHECK WPAD", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }

            if (output.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        output,
                        style = IceniTypography.bodyMedium,
                        color = if (finding) IceniAmber else IceniText
                    )
                }
            }
        }
    }
}

private suspend fun detectWpad(domain: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
    val sb = StringBuilder()
    var exposed = false

    val hosts = buildList {
        add("wpad")
        if (domain.isNotBlank()) add("wpad.$domain")
    }

    sb.append("== DNS resolution ==\n")
    val resolved = mutableListOf<String>()
    for (h in hosts) {
        try {
            val addr = InetAddress.getByName(h)
            sb.append("  $h → ${addr.hostAddress}\n")
            resolved.add(h)
            exposed = true
        } catch (_: Exception) {
            sb.append("  $h → not resolvable\n")
        }
    }

    sb.append("\n== PAC file fetch (http://wpad/wpad.dat) ==\n")
    val pacUrls = buildList {
        add("http://wpad/wpad.dat")
        if (domain.isNotBlank()) add("http://wpad.$domain/wpad.dat")
    }
    val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
    for (u in pacUrls) {
        try {
            val req = Request.Builder().url(u).build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string()?.take(400) ?: ""
                    sb.append("  $u → HTTP ${resp.code} — PAC FILE PRESENT\n")
                    if (body.isNotBlank()) sb.append("  ---\n$body\n  ---\n")
                    exposed = true
                } else {
                    sb.append("  $u → HTTP ${resp.code}\n")
                }
            }
        } catch (e: Exception) {
            sb.append("  $u → ${e.message}\n")
        }
    }

    sb.append("\n== VERDICT ==\n")
    if (exposed) {
        sb.append("  ⚠ A WPAD host and/or PAC file is reachable.\n")
        sb.append("  This can let an attacker on the LAN push proxy settings (MITM).\n")
    } else {
        sb.append("  No WPAD host or PAC file detected — no autoconfig exposure.\n")
    }

    Pair(sb.toString(), exposed)
}
