package com.andraste.tablet.tools.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 2.4 GHz channel congestion analyser.
 * Counts APs and sums signal per channel (1-14), draws congestion bars, and
 * recommends the least-congested channel with a 1/6/11 preference.
 */
@Composable
fun ChannelAnalyserScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val wm = remember { ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    var results by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("…") }
    val scope = rememberCoroutineScope()

    fun readCached(): List<ScanResult> = try {
        @Suppress("DEPRECATION") wm.scanResults
    } catch (_: Exception) { emptyList() }

    LaunchedEffect(Unit) {
        results = readCached()
        status = if (results.isEmpty()) "Tap ↻ to scan" else "${results.size} APs"
    }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                results = readCached(); scanning = false; status = "${results.size} APs"
            }
        }
        try { ctx.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) } catch (_: Exception) {}
        onDispose { try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }

    fun triggerScan() {
        scanning = true; status = "Scanning…"
        val ok = try { @Suppress("DEPRECATION") wm.startScan() } catch (_: Exception) { false }
        scope.launch {
            delay(6000)
            if (scanning) {
                results = readCached(); scanning = false
                status = if (results.isNotEmpty()) "${results.size} APs${if (!ok) " (cached)" else ""}" else "No results"
            }
        }
    }

    // Build per-channel stats for 2.4GHz 1..14
    val analysis = remember(results) { analyseChannels(results) }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Channel Analyser",
            subtitle = status,
            onBack = onBack,
            actions = {
                IconButton(onClick = { triggerScan() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Scan", tint = IceniGold)
                }
            }
        )
        if (scanning) LinearProgressIndicator(Modifier.fillMaxWidth(), color = IceniGold, trackColor = IceniGreenLight)

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (analysis.recommended > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreenLight, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text("Recommended channel: ", style = IceniTypography.titleMedium)
                    Text("${analysis.recommended}", style = IceniTypography.headlineMedium, color = IceniTeal)
                }
            }
            Text("2.4 GHz congestion (AP count / signal weight)", style = IceniTypography.labelLarge)
            val maxScore = (analysis.channels.maxOfOrNull { it.score } ?: 1f).coerceAtLeast(1f)
            analysis.channels.forEach { ch ->
                val frac = 0.05f + 0.9f * (ch.score / maxScore)
                val barColor: Color = when {
                    ch.count == 0 -> IceniTextHint
                    ch.score >= 0.75f * maxScore -> IceniRed
                    ch.score >= 0.4f * maxScore -> IceniAmber
                    else -> IceniTeal
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ch %2d".format(ch.channel), style = IceniTypography.bodySmall, modifier = Modifier.width(46.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .height(14.dp)
                            .background(barColor, RoundedCornerShape(2.dp))
                    )
                    Text(" ${ch.count}", style = IceniTypography.bodySmall, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}

private data class ChannelStat(val channel: Int, val count: Int, val score: Float)
private data class ChannelAnalysis(val channels: List<ChannelStat>, val recommended: Int)

private fun analyseChannels(results: List<ScanResult>): ChannelAnalysis {
    val counts = IntArray(15)
    val weight = FloatArray(15)
    for (ap in results) {
        val ch = chFreqToChannel(ap.frequency)
        if (ap.frequency in 2401..2495 && ch in 1..14) {
            counts[ch]++
            // convert dBm to a rough linear-ish weight (stronger = more congesting)
            weight[ch] += (100 + ap.level).coerceAtLeast(1).toFloat()
        }
    }
    val stats = (1..14).map { ChannelStat(it, counts[it], weight[it]) }

    // Recommendation: among non-overlapping 1/6/11 pick lowest score; if all busy, pick global min score.
    val preferred = listOf(1, 6, 11)
    val recommended = if (results.isNotEmpty()) {
        preferred.minByOrNull { weight[it] } ?: stats.minByOrNull { it.score }?.channel ?: 1
    } else -1
    return ChannelAnalysis(stats, recommended)
}

private fun chFreqToChannel(f: Int): Int = when {
    f == 2484 -> 14
    f in 2412..2472 -> (f - 2407) / 5
    f in 5150..5895 -> (f - 5000) / 5
    f in 5925..7125 -> (f - 5950) / 5
    else -> -1
}
