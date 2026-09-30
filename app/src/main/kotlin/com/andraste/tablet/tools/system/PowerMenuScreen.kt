package com.andraste.tablet.tools.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
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
import java.io.File

private data class PowerRow(val label: String, val value: String)

@Composable
fun PowerMenuScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current

    var rows by remember { mutableStateOf<List<PowerRow>>(emptyList()) }
    var confirmClear by remember { mutableStateOf(false) }
    var clearResult by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        rows = try { readPower(ctx) } catch (e: Exception) {
            listOf(PowerRow("Error", e.message ?: "unknown"))
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Power & Battery",
            subtitle = "status only — no reboot/shutdown",
            onBack = onBack,
            actions = {
                TextButton(onClick = { refresh() }) {
                    Text("REFRESH", color = IceniTeal, style = IceniTypography.labelSmall)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(rows) { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(r.label, style = IceniTypography.titleMedium, modifier = Modifier.width(130.dp))
                    Text(r.value, style = IceniTypography.bodyMedium, color = IceniTextMuted,
                        modifier = Modifier.weight(1f))
                }
            }
        }

        HorizontalDivider(color = IceniGoldDim)
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("MAINTENANCE", style = IceniTypography.labelLarge)
            Text(
                "System reboot/shutdown require privileged permissions and are not available.",
                style = IceniTypography.bodySmall, color = IceniTextHint
            )
            Button(
                onClick = { clearResult = null; confirmClear = true },
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen)
            ) { Text("Clear Andraste logs") }
            clearResult?.let {
                Text(it, style = IceniTypography.bodyMedium, color = IceniTeal)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = IceniGreen,
            titleContentColor = IceniGold,
            textContentColor = IceniText,
            title = { Text("Clear Andraste logs?") },
            text = {
                Text(
                    "This permanently deletes all files under\n" +
                        "${File(ctx.getExternalFilesDir(null), "andraste").absolutePath}\n\nThis cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    clearResult = clearAndrasteLogs(ctx)
                }) { Text("DELETE", color = IceniRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = IceniTextMuted) }
            }
        )
    }
}

private fun readPower(ctx: Context): List<PowerRow> {
    val out = ArrayList<PowerRow>()
    val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val sticky = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    val capacity = try { bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1 } catch (_: Exception) { -1 }

    if (sticky != null) {
        val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = when {
            capacity in 0..100 -> capacity
            level >= 0 && scale > 0 -> (level * 100) / scale
            else -> -1
        }
        out.add(PowerRow("Level", if (pct >= 0) "$pct%" else "unknown"))

        val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        out.add(PowerRow("Status", statusName(status)))

        val plugged = sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        out.add(PowerRow("Plugged", pluggedName(plugged)))

        val health = sticky.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        out.add(PowerRow("Health", healthName(health)))

        val temp = sticky.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        out.add(PowerRow("Temperature", if (temp >= 0) "%.1f °C".format(temp / 10.0) else "unknown"))

        val volt = sticky.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        out.add(PowerRow("Voltage", if (volt >= 0) "$volt mV" else "unknown"))

        val tech = sticky.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        out.add(PowerRow("Technology", tech ?: "unknown"))

        val present = sticky.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true)
        out.add(PowerRow("Battery present", if (present) "yes" else "no"))
    } else {
        out.add(PowerRow("Battery", "no sticky broadcast available"))
    }

    out.add(PowerRow("Uptime", fmtUptime(SystemClock.elapsedRealtime())))
    return out
}

private fun statusName(s: Int) = when (s) {
    BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
    BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
    BatteryManager.BATTERY_STATUS_FULL -> "full"
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not charging"
    else -> "unknown"
}

private fun pluggedName(p: Int) = when (p) {
    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
    0 -> "on battery"
    else -> "unknown"
}

private fun healthName(h: Int) = when (h) {
    BatteryManager.BATTERY_HEALTH_GOOD -> "good"
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "overheat"
    BatteryManager.BATTERY_HEALTH_DEAD -> "dead"
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "over-voltage"
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "failure"
    BatteryManager.BATTERY_HEALTH_COLD -> "cold"
    else -> "unknown"
}

private fun fmtUptime(ms: Long): String {
    val totalSec = ms / 1000
    val d = totalSec / 86400
    val h = (totalSec % 86400) / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return buildString {
        if (d > 0) append("${d}d ")
        append("%02d:%02d:%02d".format(h, m, s))
    }
}

private fun clearAndrasteLogs(ctx: Context): String {
    return try {
        val dir = File(ctx.getExternalFilesDir(null), "andraste")
        if (!dir.exists()) return "Nothing to clear (directory does not exist)."
        var deleted = 0
        var failed = 0
        dir.listFiles()?.forEach { f ->
            if (f.isFile) {
                if (f.delete()) deleted++ else failed++
            }
        }
        "Deleted $deleted file(s)" + if (failed > 0) ", $failed could not be removed." else "."
    } catch (e: Exception) {
        "Clear failed: ${e.message}"
    }
}
