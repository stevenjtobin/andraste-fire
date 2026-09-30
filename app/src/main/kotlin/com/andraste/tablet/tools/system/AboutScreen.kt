package com.andraste.tablet.tools.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "About", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚔", style = IceniTypography.displayLarge.copy(fontSize = 56.sp, color = IceniGold))
            Spacer(Modifier.height(12.dp))
            Text("ANDRASTE", style = IceniTypography.headlineLarge)
            Text("Tablet Edition", style = IceniTypography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("v0.1.0  ·  build for Fire HD 8 (onyx)", style = IceniTypography.bodySmall)
            Spacer(Modifier.height(24.dp))
            Text(
                "Named for the Iceni war-goddess. A field cyberdeck for " +
                    "the Amazon Fire tablet — WiFi, Bluetooth, network, " +
                    "location, camera and terminal tools in one place, plus " +
                    "the Linux security toolchain via Termux.",
                style = IceniTypography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Text("LINEAGE", style = IceniTypography.labelLarge)
            Text(
                "Andraste CYD (ESP32) → Cardputer ADV → Fire Tablet",
                style = IceniTypography.bodySmall, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Text("ETHOS", style = IceniTypography.labelLarge)
            Text(
                "Defensive, passive and educational tooling. " +
                    "Detectors, not emitters. Run recon only on networks " +
                    "you own or are authorised to test.",
                style = IceniTypography.bodySmall, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Text("LICENCE", style = IceniTypography.labelLarge)
            Text("AGPL-3.0", style = IceniTypography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Text(
                "For Norfolk. For the Iceni.",
                style = IceniTypography.bodySmall, color = IceniGoldDim, textAlign = TextAlign.Center
            )
        }
    }
}
