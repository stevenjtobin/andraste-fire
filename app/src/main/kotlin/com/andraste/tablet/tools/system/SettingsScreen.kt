package com.andraste.tablet.tools.system

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

private const val PREFS = "andraste_prefs"

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var consent by remember { mutableStateOf(prefs.getBoolean("consent", false)) }
    var termuxPath by remember { mutableStateOf(prefs.getString("termux_path", "/data/data/com.termux/files/usr/bin") ?: "") }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "Settings", onBack = onBack)
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IceniGreen, RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Authorised-use consent", style = IceniTypography.titleMedium)
                        Text(
                            "I only run active tools on networks I own or am authorised to test.",
                            style = IceniTypography.bodySmall
                        )
                    }
                    Switch(
                        checked = consent,
                        onCheckedChange = {
                            consent = it
                            prefs.edit().putBoolean("consent", it).apply()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = IceniGold,
                            checkedTrackColor = IceniGoldDim,
                            uncheckedThumbColor = IceniTextHint
                        )
                    )
                }
            }

            OutlinedTextField(
                value = termuxPath,
                onValueChange = {
                    termuxPath = it
                    prefs.edit().putString("termux_path", it).apply()
                },
                label = { Text("Termux bin path", color = IceniTextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = IceniText,
                    unfocusedTextColor = IceniText,
                    focusedBorderColor = IceniGold,
                    unfocusedBorderColor = IceniGoldDim,
                    cursorColor = IceniGold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Theme: Iceni (deep green + Snettisham gold). Locked in this build.",
                style = IceniTypography.bodySmall,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
