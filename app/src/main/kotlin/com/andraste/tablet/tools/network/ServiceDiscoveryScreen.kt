package com.andraste.tablet.tools.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
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

private data class NsdEntry(val name: String, val host: String, val port: Int, val type: String)

private val NSD_TYPES = listOf(
    "_http._tcp.", "_ipp._tcp.", "_googlecast._tcp.", "_ssh._tcp.", "_workstation._tcp."
)

@Composable
fun ServiceDiscoveryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf<List<NsdEntry>>(emptyList()) }
    var status by remember { mutableStateOf("starting mDNS discovery…") }

    DisposableEffect(Unit) {
        val main = Handler(Looper.getMainLooper())
        val nsd = ctx.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
        val listeners = mutableListOf<NsdManager.DiscoveryListener>()

        @Suppress("DEPRECATION")
        fun resolve(info: NsdServiceInfo) {
            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}
                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    val entry = NsdEntry(
                        name = resolved.serviceName ?: "?",
                        host = resolved.host?.hostAddress ?: "?",
                        port = resolved.port,
                        type = resolved.serviceType ?: ""
                    )
                    main.post {
                        if (entries.none { it.name == entry.name && it.port == entry.port }) {
                            entries = entries + entry
                        }
                    }
                }
            }
            try { nsd.resolveService(info, resolveListener) } catch (_: Exception) {}
        }

        NSD_TYPES.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {}
                override fun onDiscoveryStarted(serviceType: String?) { main.post { status = "discovering…" } }
                override fun onDiscoveryStopped(serviceType: String?) {}
                override fun onServiceFound(serviceInfo: NsdServiceInfo) { resolve(serviceInfo) }
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            }
            try {
                nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                listeners.add(listener)
            } catch (_: Exception) {}
        }

        onDispose {
            listeners.forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Service Discovery",
            subtitle = "${entries.size} services · $status",
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(entries) { e ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    Text(e.name, style = IceniTypography.titleMedium, color = IceniTeal)
                    Text("${e.host}:${e.port}", style = IceniTypography.bodyMedium, color = IceniText)
                    Text(e.type, style = IceniTypography.bodySmall, color = IceniTextMuted)
                }
            }
        }
    }
}
