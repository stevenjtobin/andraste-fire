package com.andraste.tablet.tools.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * WiGLE-compatible wardriving CSV exporter (WigleWifi-1.4).
 * Accumulates unique networks across scans, tagging each with the current GPS
 * fix when available. Reused for both wifi.wigle and gps.wigle entry points.
 */
@Composable
fun WigleExportScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val lm = remember { ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val scope = rememberCoroutineScope()

    val networks = remember { mutableStateMapOf<String, WigleRow>() }
    var lastLoc by remember { mutableStateOf<Location?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Idle") }
    var savedPath by remember { mutableStateOf<String?>(null) }
    var savedFile by remember { mutableStateOf<File?>(null) }

    val tsFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }

    // GPS updates (optional)
    val locGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    DisposableEffect(locGranted) {
        if (!locGranted) return@DisposableEffect onDispose {}
        val listener = LocationListener { l -> lastLoc = l }
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { lastLoc = it }
        } catch (_: SecurityException) {}
        onDispose { try { lm.removeUpdates(listener) } catch (_: Exception) {} }
    }

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults
    } catch (_: Exception) { emptyList() }

    fun ingest(list: List<ScanResult>) {
        val loc = lastLoc
        val now = tsFmt.format(Date())
        for (ap in list) {
            val mac = ap.BSSID ?: continue
            if (!networks.containsKey(mac)) {
                networks[mac] = WigleRow(
                    mac = mac,
                    ssid = ap.SSID ?: "",
                    auth = ap.capabilities ?: "",
                    firstSeen = now,
                    channel = wigleFreqToChannel(ap.frequency),
                    rssi = ap.level,
                    lat = loc?.latitude ?: 0.0,
                    lon = loc?.longitude ?: 0.0,
                    alt = loc?.altitude ?: 0.0,
                    acc = loc?.accuracy?.toDouble() ?: 0.0
                )
            }
        }
        status = "${networks.size} unique networks captured"
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                ingest(readCached()); scanning = false
            }
        }
        try { ctx.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) } catch (_: Exception) {}
        onDispose { try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    LaunchedEffect(Unit) { ingest(readCached()) }

    fun triggerScan() {
        scanning = true; status = "Scanning…"
        val ok = try { @Suppress("DEPRECATION") wm.startScan() } catch (_: Exception) { false }
        scope.launch {
            delay(6000)
            if (scanning) { ingest(readCached()); scanning = false; if (!ok) status += " (throttled)" }
        }
    }

    fun export() {
        scope.launch {
            status = "Writing CSV…"
            val result = withContext(Dispatchers.IO) {
                try {
                    val dir = File(ctx.getExternalFilesDir(null), "andraste").apply { mkdirs() }
                    val ts = System.currentTimeMillis()
                    val f = File(dir, "wigle_$ts.csv")
                    f.bufferedWriter().use { w ->
                        w.write(wigleHeaderLine())
                        w.newLine()
                        w.write("MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type")
                        w.newLine()
                        for (r in networks.values) {
                            w.write(r.toCsv())
                            w.newLine()
                        }
                    }
                    f
                } catch (e: Exception) { null }
            }
            if (result != null) {
                savedFile = result
                savedPath = result.absolutePath
                status = "Saved ${networks.size} rows"
            } else {
                status = "Export failed"
            }
        }
    }

    fun share() {
        val f = savedFile ?: return
        try {
            val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
                ctx, "${ctx.packageName}.fileprovider", f
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(intent, "Share WiGLE CSV").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            status = "Share unavailable — file at ${f.absolutePath}"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "WiGLE Export",
            subtitle = if (lastLoc != null) "GPS locked" else "no GPS — rows use 0,0",
            onBack = onBack,
            actions = {
                IconButton(onClick = { triggerScan() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Scan", tint = IceniGold)
                }
            }
        )
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniGold, trackColor = IceniGreenLight)

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(status, style = IceniTypography.bodyLarge, color = IceniTeal)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { export() },
                    enabled = networks.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                    modifier = Modifier.weight(1f)
                ) { Text("EXPORT CSV") }
                Button(
                    onClick = { share() },
                    enabled = savedFile != null,
                    colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                    modifier = Modifier.weight(1f)
                ) { Text("SHARE") }
            }
            savedPath?.let {
                Text("File:", style = IceniTypography.labelSmall)
                Text(it, style = IceniTypography.bodySmall)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(networks.values.toList(), key = { it.mac }) { r ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(r.ssid.ifEmpty { "<hidden>" }, style = IceniTypography.titleMedium)
                    Text("${r.mac}  ch${r.channel}  ${r.rssi}dBm  ${"%.5f".format(r.lat)},${"%.5f".format(r.lon)}",
                        style = IceniTypography.bodySmall)
                }
            }
        }
    }
}

private data class WigleRow(
    val mac: String,
    val ssid: String,
    val auth: String,
    val firstSeen: String,
    val channel: Int,
    val rssi: Int,
    val lat: Double,
    val lon: Double,
    val alt: Double,
    val acc: Double
) {
    fun toCsv(): String {
        fun q(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        return listOf(
            mac, q(ssid), q(auth), firstSeen, channel.toString(), rssi.toString(),
            "%.6f".format(lat), "%.6f".format(lon), "%.1f".format(alt), "%.1f".format(acc), "WIFI"
        ).joinToString(",")
    }
}

private fun wigleHeaderLine(): String {
    val model = android.os.Build.MODEL ?: "unknown"
    val rel = android.os.Build.VERSION.RELEASE ?: "?"
    val device = android.os.Build.DEVICE ?: "?"
    val brand = android.os.Build.BRAND ?: "?"
    return "WigleWifi-1.4,appRelease=0.1.0,model=$model,release=$rel,device=$device,display=Andraste,board=?,brand=$brand"
}

private fun wigleFreqToChannel(f: Int): Int = when {
    f == 2484 -> 14
    f in 2412..2472 -> (f - 2407) / 5
    f in 5150..5895 -> (f - 5000) / 5
    f in 5925..7125 -> (f - 5950) / 5
    else -> -1
}
