package com.andraste.tablet.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.andraste.tablet.tools.ToolCategory
import com.andraste.tablet.tools.ToolRegistry
import com.andraste.tablet.tools.StubToolScreen
import com.andraste.tablet.tools.ble.*
import com.andraste.tablet.tools.camera.*
import com.andraste.tablet.tools.files.*
import com.andraste.tablet.tools.location.*
import com.andraste.tablet.tools.network.*
import com.andraste.tablet.tools.system.*
import com.andraste.tablet.tools.terminal.*
import com.andraste.tablet.tools.tribe.*
import com.andraste.tablet.tools.wifi.*
import com.andraste.tablet.ui.home.CategoryScreen
import com.andraste.tablet.ui.home.HomeScreen

@Composable
fun NavGraph(nav: NavHostController) {
    NavHost(navController = nav, startDestination = "home") {

        composable("home") {
            HomeScreen(onCategorySelected = { cat -> nav.navigate("category/${cat.name}") })
        }

        composable(
            "category/{cat}",
            arguments = listOf(navArgument("cat") { type = NavType.StringType })
        ) { entry ->
            val cat = ToolCategory.valueOf(entry.arguments!!.getString("cat")!!)
            CategoryScreen(
                category = cat,
                onBack = { nav.popBackStack() },
                onToolSelected = { tool -> nav.navigate("tool/${tool.id}") }
            )
        }

        composable(
            "tool/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments!!.getString("id")!!
            val back: () -> Unit = { nav.popBackStack() }

            when (id) {
                // ── WiFi ──
                "wifi.scan"       -> WifiScanScreen(back)
                "wifi.eviltwin"   -> EvilTwinDetectorScreen(back)
                "wifi.connect"    -> WifiConnectScreen(back)
                "wifi.apinfo"     -> ApInfoScreen(back)
                "wifi.wigle"      -> WigleExportScreen(back)
                "wifi.wardrive"   -> WigleExportScreen(back)
                "wifi.wps"        -> WpsScannerScreen(back)
                "wifi.traffic"    -> WifiTrafficScreen(back)
                "wifi.channel"    -> ChannelAnalyserScreen(back)
                "wifi.sig_meter"  -> SignalMeterScreen(back)

                // ── BLE ──
                "ble.scan"        -> BleScanScreen(back)
                "ble.beacon"      -> BeaconDecoderScreen(back)
                "ble.tracker"     -> TrackerHunterScreen(back)
                "ble.finder"      -> BleFinderScreen(back)
                "ble.logger"      -> BleLoggerScreen(back)
                "ble.gatt"        -> GattExplorerScreen(back)
                "ble.skimmer"     -> SkimmerDetectorScreen(back)
                "ble.flipper"     -> FlipperDetectorScreen(back)
                "ble.spam_det"    -> BleSpamDetectorScreen(back)
                "ble.sour_apple"  -> SourAppleScreen(back)
                "ble.clone_det"   -> CloneDetectorScreen(back)
                "ble.findmy"      -> FindMyObserverScreen(back)
                "ble.classic"     -> ClassicBtScreen(back)

                // ── Network ──
                "net.ssh_client"  -> SshClientScreen(back)
                "net.portscan"    -> PortScanScreen(back)
                "net.telnet"      -> TelnetScreen(back)
                "net.tcp_client"  -> TcpClientScreen(back)
                "net.tcp_listen"  -> TcpListenScreen(back)
                "net.lan_recon"   -> LanReconScreen(back)
                "net.netmap"      -> NetMapScreen(back)
                "net.svc_disc"    -> ServiceDiscoveryScreen(back)
                "net.dns"         -> DnsToolsScreen(back)
                "net.arp"         -> ArpScanScreen(back)
                "net.http_probe"  -> HttpProbeScreen(back)
                "net.mqtt"        -> MqttScreen(back)
                "net.utils"       -> NetUtilsScreen(back)
                "net.upnp"        -> UpnpScreen(back)
                "net.printer"     -> PrinterDiscoveryScreen(back)
                "net.bw_test"     -> BandwidthTestScreen(back)
                "net.wpad"        -> WpadDetectorScreen(back)

                // ── Location ──
                "gps.display"     -> GpsScreen(back)
                "gps.track"       -> GpsTrackScreen(back)
                "gps.sat_track"   -> SatTrackerScreen(back)
                "gps.wardrive"    -> WigleExportScreen(back)
                "gps.wigle"       -> WigleExportScreen(back)

                // ── Camera ──
                "cam.qr"          -> QrScanScreen(back)
                "cam.surv"        -> SurveillanceHunterScreen(back)
                "cam.exif"        -> ExifInspectorScreen(back)
                "cam.drone_rid"   -> DroneRidScreen(back)

                // ── Files ──
                "files.browse"    -> FileBrowserScreen(back)
                "files.pcap"      -> PcapViewerScreen(back)
                "files.log"       -> LogViewerScreen(back)
                "files.hex"       -> HexViewerScreen(back)
                "files.hash"      -> HashCalculatorScreen(back)
                "files.export"    -> ExportShareScreen(back)

                // ── System ──
                "sys.devinfo"     -> DeviceInfoScreen(back)
                "sys.settings"    -> SettingsScreen(back)
                "sys.perms"       -> PermissionsScreen(back)
                "sys.about"       -> AboutScreen(back)
                "sys.usb"         -> UsbDevicesScreen(back)
                "sys.power"       -> PowerMenuScreen(back)
                "sys.update"      -> OtaUpdateScreen(back)

                // ── Terminal ──
                "term.wireshark"  -> PcapViewerScreen(back)
                "term.scripts"    -> ScriptRunnerScreen(back)

                // ── Tribe ──
                "tribe.rising"    -> TheRisingScreen(back)
                "tribe.radar"     -> ThreatRadarScreen(back)

                // All other term.* launch Termux with the tool's preset command
                else -> if (id.startsWith("term.")) {
                    TerminalScreen(toolId = id, onBack = back)
                } else {
                    val tool = ToolRegistry.find(id)
                    if (tool != null) StubToolScreen(tool, back)
                }
            }
        }
    }
}
