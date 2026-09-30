package com.andraste.tablet.tools.camera

import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

/**
 * EXIF metadata inspector. Pick an image (GetContent) and decode EXIF via
 * android.media.ExifInterface from the content InputStream (API 24+ ctor) —
 * avoids adding an androidx.exifinterface dependency.
 */
@Composable
fun ExifInspectorScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var tags by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var gps by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Pick an image to inspect") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) { status = "no image selected"; return@rememberLauncherForActivityResult }
        try {
            ctx.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val out = mutableListOf<Pair<String, String>>()

                fun add(label: String, tag: String) {
                    val v = exif.getAttribute(tag)
                    if (!v.isNullOrEmpty()) out.add(label to v)
                }

                // GPS — use the float[] output form (available on API 28; the
                // no-arg getLatLong() returning double[] is API 29+).
                val latLon = FloatArray(2)
                @Suppress("DEPRECATION")
                val hasGps = exif.getLatLong(latLon)
                gps = if (hasGps) "%.6f, %.6f".format(latLon[0], latLon[1]) else "none"

                add("DateTime", ExifInterface.TAG_DATETIME)
                add("DateTimeOriginal", ExifInterface.TAG_DATETIME_ORIGINAL)
                add("Make", ExifInterface.TAG_MAKE)
                add("Model", ExifInterface.TAG_MODEL)
                add("Width", ExifInterface.TAG_IMAGE_WIDTH)
                add("Height", ExifInterface.TAG_IMAGE_LENGTH)
                add("Orientation", ExifInterface.TAG_ORIENTATION)
                add("F-Number", ExifInterface.TAG_F_NUMBER)
                add("ExposureTime", ExifInterface.TAG_EXPOSURE_TIME)
                add("ISO", ExifInterface.TAG_ISO_SPEED_RATINGS)
                add("FocalLength", ExifInterface.TAG_FOCAL_LENGTH)
                add("Flash", ExifInterface.TAG_FLASH)
                add("WhiteBalance", ExifInterface.TAG_WHITE_BALANCE)
                add("Software", ExifInterface.TAG_SOFTWARE)
                add("GPSAltitude", ExifInterface.TAG_GPS_ALTITUDE)
                add("GPSTimestamp", ExifInterface.TAG_GPS_TIMESTAMP)
                add("GPSDatestamp", ExifInterface.TAG_GPS_DATESTAMP)

                tags = out
                status = if (out.isEmpty() && gps == "none") "no EXIF metadata found" else "${out.size} tags"
            } ?: run { status = "could not open image" }
        } catch (e: Exception) {
            status = "error: ${e.message}"
            tags = emptyList(); gps = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "EXIF Inspector", subtitle = status, onBack = onBack)

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { try { picker.launch("image/*") } catch (e: Exception) { status = "picker unavailable" } },
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("PICK IMAGE") }

            gps?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (it == "none") IceniGreen else IceniGreenLight, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text("GPS: ", style = IceniTypography.titleMedium)
                    Text(it, style = IceniTypography.bodyLarge, color = if (it == "none") IceniTextMuted else IceniAmber)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(tags, key = { it.first }) { (k, v) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(k, style = IceniTypography.titleMedium, modifier = Modifier.width(140.dp))
                    Text(v, style = IceniTypography.bodyMedium, color = IceniTeal, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
