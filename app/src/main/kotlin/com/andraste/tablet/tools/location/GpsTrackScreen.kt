package com.andraste.tablet.tools.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GPX track recorder. Start/Stop capture GPS fixes into a GPX 1.1 file at
 * getExternalFilesDir(null)/andraste/track_<ts>.gpx.
 */
@Composable
fun GpsTrackScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lm = remember { ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val scope = rememberCoroutineScope()

    var recording by remember { mutableStateOf(false) }
    val points = remember { mutableStateListOf<TrackPoint>() }
    var distanceM by remember { mutableStateOf(0.0) }
    var startTime by remember { mutableStateOf(0L) }
    var elapsed by remember { mutableStateOf(0L) }
    var savedPath by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Idle") }

    val isoFmt = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US) }

    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    DisposableEffect(recording, granted) {
        if (!recording || !granted) {
            if (!granted) status = "location permission not granted"
            return@DisposableEffect onDispose {}
        }
        status = "recording…"
        val listener = LocationListener { l ->
            val tp = TrackPoint(l.latitude, l.longitude, l.altitude, System.currentTimeMillis())
            val last = points.lastOrNull()
            if (last != null) distanceM += haversine(last.lat, last.lon, tp.lat, tp.lon)
            points.add(tp)
            elapsed = System.currentTimeMillis() - startTime
        }
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
        } catch (_: SecurityException) { status = "permission error" }
        onDispose { try { lm.removeUpdates(listener) } catch (_: Exception) {} }
    }

    fun saveGpx() {
        scope.launch {
            status = "writing GPX…"
            val path = withContext(Dispatchers.IO) {
                try {
                    val dir = File(ctx.getExternalFilesDir(null), "andraste").apply { mkdirs() }
                    val f = File(dir, "track_${System.currentTimeMillis()}.gpx")
                    f.bufferedWriter().use { w ->
                        w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                        w.write("<gpx version=\"1.1\" creator=\"Andraste Tablet\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
                        w.write("  <trk>\n    <name>Andraste track</name>\n    <trkseg>\n")
                        for (p in points) {
                            w.write("      <trkpt lat=\"${"%.7f".format(p.lat)}\" lon=\"${"%.7f".format(p.lon)}\">\n")
                            w.write("        <ele>${"%.1f".format(p.alt)}</ele>\n")
                            w.write("        <time>${isoFmt.format(Date(p.time))}</time>\n")
                            w.write("      </trkpt>\n")
                        }
                        w.write("    </trkseg>\n  </trk>\n</gpx>\n")
                    }
                    f.absolutePath
                } catch (e: Exception) { null }
            }
            savedPath = path
            status = if (path != null) "saved ${points.size} points" else "save failed"
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "GPS Track",
            subtitle = status,
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    if (recording) {
                        recording = false
                        saveGpx()
                    } else {
                        points.clear(); distanceM = 0.0; savedPath = null
                        startTime = System.currentTimeMillis(); elapsed = 0
                        recording = true
                    }
                }) {
                    Icon(
                        if (recording) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        contentDescription = if (recording) "Stop" else "Start",
                        tint = IceniGold
                    )
                }
            }
        )
        if (recording) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniTeal, trackColor = IceniGreenLight)

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatRow("Points", "${points.size}")
            StatRow("Distance", if (distanceM >= 1000) "%.2f km".format(distanceM / 1000) else "%.0f m".format(distanceM))
            StatRow("Elapsed", formatElapsed(elapsed))
            points.lastOrNull()?.let {
                StatRow("Last fix", "%.6f, %.6f".format(it.lat, it.lon))
            }
            savedPath?.let {
                Spacer(Modifier.height(6.dp))
                Text("File:", style = IceniTypography.labelSmall)
                Text(it, style = IceniTypography.bodySmall)
            }
        }
    }
}

private data class TrackPoint(val lat: Double, val lon: Double, val alt: Double, val time: Long)

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IceniGreen, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(label, style = IceniTypography.titleMedium, modifier = Modifier.width(120.dp))
        Text(value, style = IceniTypography.bodyLarge, color = IceniTeal)
    }
}

private fun formatElapsed(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}
