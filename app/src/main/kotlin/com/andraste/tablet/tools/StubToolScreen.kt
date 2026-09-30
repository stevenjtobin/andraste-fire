package com.andraste.tablet.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
fun StubToolScreen(tool: ToolDef, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        IceniHeader(title = tool.name, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = tool.category.emoji,
                style = IceniTypography.displayLarge.copy(fontSize = 48.sp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = tool.name,
                style = IceniTypography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = tool.description,
                style = IceniTypography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            if (tool.requiresHardware != null) {
                Text(
                    text = "⚠  Requires: ${tool.requiresHardware}",
                    style = IceniTypography.bodyMedium,
                    color = IceniAmber,
                    textAlign = TextAlign.Center
                )
            } else if (tool.requiresRoot) {
                Text(
                    text = "⚠  Requires root access",
                    style = IceniTypography.bodyMedium,
                    color = IceniAmber,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "[ Implementation in progress ]",
                    style = IceniTypography.bodySmall,
                    color = IceniTextHint,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
