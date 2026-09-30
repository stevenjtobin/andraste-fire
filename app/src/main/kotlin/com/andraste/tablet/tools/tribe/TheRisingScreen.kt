package com.andraste.tablet.tools.tribe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

data class Rank(val name: String, val xp: Int, val desc: String)

private val RANKS = listOf(
    Rank("Initiate",     0,    "First boot. The torc is placed."),
    Rank("Scout",        100,  "Ran your first passive scan."),
    Rank("Warrior",      500,  "Mapped a network end to end."),
    Rank("Shieldbearer", 1500, "Detected an active threat in the field."),
    Rank("Chieftain",    4000, "Commanded the full toolchain."),
    Rank("Boudica",      10000,"Queen of the Iceni. The Rising is complete.")
)

@Composable
fun TheRisingScreen(onBack: () -> Unit) {
    // XP is placeholder until the event log is wired to real tool usage.
    val currentXp = 100
    val rankIdx = RANKS.indexOfLast { currentXp >= it.xp }.coerceAtLeast(0)
    val rank = RANKS[rankIdx]
    val next = RANKS.getOrNull(rankIdx + 1)
    val progress = if (next != null)
        (currentXp - rank.xp).toFloat() / (next.xp - rank.xp) else 1f

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "The Rising", subtitle = "Iceni rank progression", onBack = onBack)

        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚔", style = IceniTypography.displayLarge.copy(fontSize = 48.sp, color = IceniGold))
            Text(rank.name.uppercase(), style = IceniTypography.headlineLarge)
            Text("$currentXp XP", style = IceniTypography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = IceniGold, trackColor = IceniGreenLight
            )
            if (next != null) {
                Text(
                    "${next.xp - currentXp} XP to ${next.name}",
                    style = IceniTypography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        Text("RANKS", style = IceniTypography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(RANKS) { r ->
                val reached = currentXp >= r.xp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (reached) IceniGreen else IceniGreenLight, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Row {
                        Text(
                            text = if (reached) "✓ ${r.name}" else "🔒 ${r.name}",
                            style = IceniTypography.titleMedium,
                            color = if (reached) IceniGold else IceniTextHint,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${r.xp} XP", style = IceniTypography.bodySmall)
                    }
                    Text(r.desc, style = IceniTypography.bodySmall)
                }
            }
        }
    }
}
