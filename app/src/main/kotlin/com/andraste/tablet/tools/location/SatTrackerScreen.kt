package com.andraste.tablet.tools.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

/**
 * GNSS sky view. Live list of satellites reported by GnssStatus.Callback
 * (API 24+): constellation, Cn0 signal, used-in-fix, azimuth/elevation.
 */
@Composable
fun SatTrackerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lm = remember { ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var sats by remember { mutableStateOf<List<SatInfo>>(emptyList()) }
    var usedCount by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("waiting for GNSS…") }

    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    DisposableEffect(granted) {
        if (!granted) { status = "location permission not granted"; return@DisposableEffect onDispose {} }
        if (Build.VERSION.SDK_INT < 24) { status = "GnssStatus requires API 24+"; return@DisposableEffect onDispose {} }

        // A location request is needed to keep the GNSS engine active.
        val locListener = LocationListener { }
        val cb = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(s: GnssStatus) {
                val list = ArrayList<SatInfo>(s.satelliteCount)
                var used = 0
                for (i in 0 until s.satelliteCount) {
                    val u = s.usedInFix(i)
                    if (u) used++
                    list.add(
                        SatInfo(
                            svid = s.getSvid(i),
                            constellation = constellationName(s.getConstellationType(i)),
                            cn0 = s.getCn0DbHz(i),
                            used = u,
                            az = s.getAzimuthDegrees(i),
                            el = s.getElevationDegrees(i)
                        )
                    )
                }
                sats = list.sortedByDescending { it.cn0 }
                usedCount = used
                status = "live"
            }
        }
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locListener)
            lm.registerGnssStatusCallback(cb)
        } catch (_: SecurityException) { status = "permission error" }
        onDispose {
            try { lm.unregisterGnssStatusCallback(cb) } catch (_: Exception) {}
            try { lm.removeUpdates(locListener) } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "GNSS Sky View",
            subtitle = "$usedCount used / ${sats.size} in view · $status",
            onBack = onBack
        )

        if (sats.isEmpty()) {
            Text(
                "No satellites reported yet. Move near a window / outdoors for a fix.",
                style = IceniTypography.bodyLarge,
                color = IceniTextMuted,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(sats, key = { "${it.constellation}-${it.svid}" }) { sat ->
                val sigColor: Color = when {
                    sat.cn0 >= 35 -> IceniTeal
                    sat.cn0 >= 20 -> IceniGold
                    else -> IceniTextMuted
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(if (sat.used) "● " else "○ ", color = if (sat.used) IceniTeal else IceniTextHint, style = IceniTypography.bodyMedium)
                            Text("${sat.constellation} #${sat.svid}", style = IceniTypography.titleMedium)
                        }
                        Text("az ${"%.0f".format(sat.az)}°  el ${"%.0f".format(sat.el)}°", style = IceniTypography.bodySmall)
                    }
                    Text("%.0f dBHz".format(sat.cn0), style = IceniTypography.bodyLarge, color = sigColor)
                }
            }
        }
    }
}

private data class SatInfo(
    val svid: Int,
    val constellation: String,
    val cn0: Float,
    val used: Boolean,
    val az: Float,
    val el: Float
)

private fun constellationName(type: Int): String = if (Build.VERSION.SDK_INT >= 24) when (type) {
    GnssStatus.CONSTELLATION_GPS -> "GPS"
    GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
    GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
    GnssStatus.CONSTELLATION_BEIDOU -> "BeiDou"
    GnssStatus.CONSTELLATION_QZSS -> "QZSS"
    GnssStatus.CONSTELLATION_SBAS -> "SBAS"
    GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
    else -> "Unknown"
} else "?"
