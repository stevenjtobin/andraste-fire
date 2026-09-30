package com.andraste.tablet.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andraste.tablet.tools.RiskLevel
import com.andraste.tablet.tools.ToolDef
import com.andraste.tablet.ui.theme.*

@Composable
fun ToolRow(tool: ToolDef, onClick: () -> Unit) {
    val unavailable = tool.requiresHardware != null
    val alpha = if (unavailable) 0.4f else 1f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !unavailable, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.5.dp, IceniGoldDim.copy(alpha = alpha * 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tool.name,
                        style = IceniTypography.titleMedium,
                        color = if (unavailable) IceniTextHint else IceniText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    RiskBadge(tool.risk)
                    if (tool.requiresRoot) {
                        Spacer(Modifier.width(4.dp))
                        Badge(label = "ROOT", color = IceniAmber)
                    }
                    if (tool.isImplemented) {
                        Spacer(Modifier.width(4.dp))
                        Badge(label = "✓", color = IceniTeal)
                    }
                }
                Text(
                    text = tool.requiresHardware ?: tool.description,
                    style = IceniTypography.bodySmall,
                    color = if (unavailable) IceniTextHint else IceniTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: com.andraste.tablet.tools.ToolCategory,
    count: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(6.dp),
        color = IceniGreen,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, IceniGold.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = category.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = category.displayName.uppercase(),
                style = IceniTypography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count tools",
                style = IceniTypography.bodySmall
            )
        }
    }
}

@Composable
fun RiskBadge(risk: RiskLevel) {
    val (label, color) = when (risk) {
        RiskLevel.PASSIVE   -> "RX"    to RiskPassive
        RiskLevel.ACTIVE    -> "ACT"   to RiskActive
        RiskLevel.OFFENSIVE -> "OFF"   to RiskOffensive
    }
    Badge(label = label, color = color)
}

@Composable
fun Badge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.7f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = IceniTypography.labelSmall,
            color = color
        )
    }
}
