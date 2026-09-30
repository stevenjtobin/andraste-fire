package com.andraste.tablet.tools.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

@Composable
fun GpsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lm = remember { ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var loc by remember { mutableStateOf<Location?>(null) }
    var satsInView by remember { mutableStateOf(0) }
    var satsUsed by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("waiting for fix…") }

    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

    DisposableEffect(granted) {
        if (!granted) {
            status = "location permission not granted"
            return@DisposableEffect onDispose {}
        }
        val listener = LocationListener { l -> loc = l; status = "live fix" }
        val gnss = if (Build.VERSION.SDK_INT >= 24) object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(s: GnssStatus) {
                satsInView = s.satelliteCount
                satsUsed = (0 until s.satelliteCount).count { s.usedInFix(it) }
            }
        } else null

        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, listener)
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { loc = it }
            if (gnss != null && Build.VERSION.SDK_INT >= 24) lm.registerGnssStatusCallback(gnss)
        } catch (_: SecurityException) { status = "permission error" }

        onDispose {
            lm.removeUpdates(listener)
            if (gnss != null && Build.VERSION.SDK_INT >= 24) lm.unregisterGnssStatusCallback(gnss)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "GPS Display", subtitle = status, onBack = onBack)
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GpsRow("Latitude",  loc?.latitude?.let { "%.6f".format(it) } ?: "—")
            GpsRow("Longitude", loc?.longitude?.let { "%.6f".format(it) } ?: "—")
            GpsRow("Altitude",  loc?.altitude?.let { "%.1f m".format(it) } ?: "—")
            GpsRow("Speed",     loc?.speed?.let { "%.1f m/s".format(it) } ?: "—")
            GpsRow("Bearing",   loc?.bearing?.let { "%.0f°".format(it) } ?: "—")
            GpsRow("Accuracy",  loc?.accuracy?.let { "±%.0f m".format(it) } ?: "—")
            GpsRow("Provider",  loc?.provider ?: "—")
            GpsRow("Sats used", "$satsUsed / $satsInView")
        }
    }
}

@Composable
private fun GpsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IceniGreen, RoundedCornerShape(4.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text = label, style = IceniTypography.titleMedium, modifier = Modifier.width(120.dp))
        Text(text = value, style = IceniTypography.bodyLarge, color = IceniTeal)
    }
}
