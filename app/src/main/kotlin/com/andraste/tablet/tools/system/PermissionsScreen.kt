package com.andraste.tablet.tools.system

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val perms = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 31) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val state = rememberMultiplePermissionsState(perms)

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Permissions",
            subtitle = "${state.permissions.count { it.status.isGranted }} / ${perms.size} granted",
            onBack = onBack
        )
        Button(
            onClick = { state.launchMultiplePermissionRequest() },
            colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) { Text("GRANT ALL", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state.permissions) { p ->
                val granted = p.status.isGranted
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (granted) "✓ " else "✗ ", color = if (granted) IceniTeal else IceniRed)
                    Text(
                        text = p.permission.substringAfterLast('.'),
                        style = IceniTypography.bodyMedium,
                        color = if (granted) IceniText else IceniTextMuted
                    )
                }
            }
        }
    }
}
