package com.andraste.tablet.tools.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * WiFi connect using the legacy (minSdk28) WifiManager.addNetwork API.
 * On API 29+ this path is largely non-functional (system owns config), but it
 * still works on Fire OS builds derived from older Android. Best-effort.
 */
@Composable
fun WifiConnectScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("…") }
    var selected by remember { mutableStateOf<ScanResult?>(null) }
    var password by remember { mutableStateOf("") }
    var connectMsg by remember { mutableStateOf<String?>(null) }
    var currentSsid by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults.sortedByDescending { it.level }
    } catch (_: Exception) { emptyList() }

    fun readCurrent(): String = try {
        @Suppress("DEPRECATION") (wm.connectionInfo?.ssid ?: "").trim('"')
    } catch (_: Exception) { "" }

    LaunchedEffect(Unit) {
        results = readCached(); currentSsid = readCurrent()
        status = if (results.isEmpty()) "Tap ↻ to scan" else "${results.size} networks"
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                results = readCached(); scanning = false; status = "${results.size} networks"
            }
        }
        try { ctx.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) } catch (_: Exception) {}
        onDispose { try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    fun triggerScan() {
        scanning = true; status = "Scanning…"
        val ok = try { @Suppress("DEPRECATION") wm.startScan() } catch (_: Exception) { false }
        scope.launch {
            delay(6000)
            if (scanning) {
                results = readCached(); scanning = false
                status = if (results.isNotEmpty()) "${results.size} networks (${if (ok) "scan" else "cached"})" else "No results"
            }
        }
    }

    fun connect(ap: ScanResult, pass: String) {
        connectMsg = "Connecting…"
        scope.launch {
            val msg = try {
                @Suppress("DEPRECATION")
                run {
                    val conf = WifiConfiguration()
                    conf.SSID = "\"${ap.SSID}\""
                    val caps = ap.capabilities
                    when {
                        caps.contains("WPA") || caps.contains("RSN") || caps.contains("PSK") ->
                            conf.preSharedKey = "\"$pass\""
                        caps.contains("WEP") -> {
                            conf.wepKeys[0] = "\"$pass\""
                            conf.wepTxKeyIndex = 0
                            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                        }
                        else -> conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    }
                    val netId = wm.addNetwork(conf)
                    if (netId == -1) {
                        "addNetwork failed (system may own WiFi config on this OS version)"
                    } else {
                        wm.disconnect()
                        val enabled = wm.enableNetwork(netId, true)
                        wm.reconnect()
                        if (enabled) "Requested connection to ${ap.SSID} (netId $netId)"
                        else "enableNetwork returned false"
                    }
                }
            } catch (e: SecurityException) {
                "Permission denied: ${e.message}"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            connectMsg = msg
            currentSsid = readCurrent()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "WiFi Connect",
            subtitle = "legacy addNetwork API · current: ${currentSsid.ifEmpty { "none" }}",
            onBack = if (selected != null) ({ selected = null; connectMsg = null; password = "" }) else onBack,
            actions = {
                IconButton(onClick = { triggerScan() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Scan", tint = IceniGold)
                }
            }
        )
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniGold, trackColor = IceniGreenLight)

        val sel = selected
        if (sel == null) {
            Text(status, style = IceniTypography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(results, key = { it.BSSID ?: it.SSID }) { ap ->
                    val open = !(ap.capabilities.contains("WPA") || ap.capabilities.contains("WEP") || ap.capabilities.contains("RSN"))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreen, RoundedCornerShape(6.dp))
                            .clickable { selected = ap; password = ""; connectMsg = null }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row {
                            Text(if (open) "🔓 " else "🔒 ", style = IceniTypography.bodyLarge)
                            Text(ap.SSID.ifEmpty { "<hidden>" }, style = IceniTypography.titleMedium, modifier = Modifier.weight(1f))
                            Text("${ap.level}dBm", style = IceniTypography.bodySmall, color = IceniTeal)
                        }
                    }
                }
            }
        } else {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(sel.SSID.ifEmpty { "<hidden>" }, style = IceniTypography.headlineMedium)
                Text("BSSID ${sel.BSSID}", style = IceniTypography.bodySmall)
                Text(sel.capabilities, style = IceniTypography.bodySmall)
                val open = !(sel.capabilities.contains("WPA") || sel.capabilities.contains("WEP") || sel.capabilities.contains("RSN"))
                if (!open) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = IceniText, unfocusedTextColor = IceniText,
                            focusedBorderColor = IceniGold, unfocusedBorderColor = IceniGoldDim,
                            cursorColor = IceniGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Button(
                    onClick = { connect(sel, password) },
                    colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("CONNECT") }
                connectMsg?.let { Text(it, style = IceniTypography.bodyMedium, color = IceniAmber) }
            }
        }
    }
}
