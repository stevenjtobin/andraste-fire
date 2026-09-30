package com.andraste.tablet.tools

object ToolRegistry {

    val all: List<ToolDef> = listOf(

        // ── WiFi ────────────────────────────────────────────────────────────
        ToolDef("wifi.scan",        "WiFi Scanner",           ToolCategory.WIFI, "Passive scan — all nearby APs, BSSID, RSSI, security", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.monitor",     "WiFi Monitor",           ToolCategory.WIFI, "Promiscuous 802.11 frame monitor (root + monitor-mode adapter)", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION, Capability.ROOT), requiresRoot = true),
        ToolDef("wifi.deauth_det",  "Deauth Detector",        ToolCategory.WIFI, "Detect deauthentication / disassociation attack frames (passive)", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION, Capability.ROOT), requiresRoot = true),
        ToolDef("wifi.sniffer",     "Packet Sniffer",         ToolCategory.WIFI, "Capture 802.11 frames to PCAP (root + monitor-mode adapter)", capabilities = setOf(Capability.WIFI, Capability.ROOT), requiresRoot = true),
        ToolDef("wifi.probe",       "Probe Harvester",        ToolCategory.WIFI, "Capture probe-request frames from nearby devices", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION, Capability.ROOT), requiresRoot = true),
        ToolDef("wifi.eviltwin",    "Evil Twin Detector",     ToolCategory.WIFI, "Detect rogue APs cloning known SSIDs", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.wardrive",    "Wardriving",             ToolCategory.WIFI, "GPS-tagged WiFi scan log — exports to CSV / WiGLE format", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.connect",     "WiFi Connect",           ToolCategory.WIFI, "Scan, pick, and connect to a network with saved credentials", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.apinfo",      "AP Info",                ToolCategory.WIFI, "Deep info on a selected AP — country, capabilities, beacon interval", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.hidden",      "Hidden SSID Finder",     ToolCategory.WIFI, "Probe for hidden SSIDs by sending directed probe-request frames", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = false),
        ToolDef("wifi.wpa_cap",     "WPA Handshake Capture",  ToolCategory.WIFI, "Capture WPA 4-way handshakes (monitor-mode adapter required)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.WIFI, Capability.ROOT), requiresRoot = true, requiresHardware = "monitor-mode USB WiFi adapter"),
        ToolDef("wifi.pmkid",       "PMKID Capture",          ToolCategory.WIFI, "Capture PMKID from AP without full handshake (monitor-mode)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.WIFI, Capability.ROOT), requiresRoot = true, requiresHardware = "monitor-mode USB WiFi adapter"),
        ToolDef("wifi.wpa_crack",   "WPA Crack (Termux)",     ToolCategory.WIFI, "Launch hashcat / aircrack-ng on captured handshakes via Termux", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX), isImplemented = false),
        ToolDef("wifi.wigle",       "WiGLE Upload",           ToolCategory.WIFI, "Upload wardrive CSV to WiGLE.net", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("wifi.karma",       "KARMA Detector",         ToolCategory.WIFI, "Detect KARMA / auto-association attack responses", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = false),
        ToolDef("wifi.beacon_det",  "Beacon Spam Detector",   ToolCategory.WIFI, "Detect beacon flooding / SSID spray attacks", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = false),
        ToolDef("wifi.flood_det",   "Flood Detector",         ToolCategory.WIFI, "Detect high-rate 802.11 flooding on current channel", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = false),
        ToolDef("wifi.jam_det",     "Jam Detector",           ToolCategory.WIFI, "Detect RF interference and WiFi channel jamming signatures", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = false),
        ToolDef("wifi.dead_drop",   "WiFi Dead Drop",         ToolCategory.WIFI, "Covert data exchange via custom beacon IEs", capabilities = setOf(Capability.WIFI, Capability.ROOT), requiresRoot = true),
        ToolDef("wifi.ciw",         "Client in Wild",         ToolCategory.WIFI, "Find devices probing for specific SSIDs (CIW scanner)", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION, Capability.ROOT), requiresRoot = true),
        ToolDef("wifi.stations",    "WiFi Stations",          ToolCategory.WIFI, "Enumerate clients connected to a given AP", capabilities = setOf(Capability.WIFI, Capability.ROOT), requiresRoot = true),
        ToolDef("wifi.wps",         "WPS Scanner",            ToolCategory.WIFI, "Find WPS-enabled APs and check for known-weak PINs", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.traffic",     "Traffic Stats",          ToolCategory.WIFI, "Per-SSID frame counts and channel utilisation", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.channel",     "Channel Analyser",       ToolCategory.WIFI, "2.4GHz channel overlap and congestion map", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.sig_meter",   "Signal Meter",           ToolCategory.WIFI, "Live RSSI bar for a selected AP — walk-test tool", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("wifi.webui",       "Web UI Server",          ToolCategory.WIFI, "Serve Andraste tool results via local HTTP", capabilities = setOf(Capability.INTERNET), isImplemented = false),

        // ── BLE / Bluetooth ─────────────────────────────────────────────────
        ToolDef("ble.scan",         "BLE Scanner",            ToolCategory.BLE,  "Scan all nearby Bluetooth LE devices", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.beacon",       "Beacon Decoder",         ToolCategory.BLE,  "Decode iBeacon, Eddystone, Continuity, Exposure Notification", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.tracker",      "Tracker Hunter",         ToolCategory.BLE,  "Detect AirTag, Tile, Samsung SmartTag, Chipolo, Flipper, Google", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.finder",       "BLE Finder",             ToolCategory.BLE,  "Live RSSI meter — locate a selected BLE device", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.logger",       "BLE Logger",             ToolCategory.BLE,  "Log all seen BLE devices to CSV with timestamps", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION, Capability.STORAGE), isImplemented = true),
        ToolDef("ble.gatt",         "GATT Explorer",          ToolCategory.BLE,  "Connect to a BLE device and browse services / characteristics", capabilities = setOf(Capability.BLUETOOTH), isImplemented = true),
        ToolDef("ble.skimmer",      "Skimmer Detector",       ToolCategory.BLE,  "Detect Bluetooth card skimmer signatures by name / UUID patterns", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.flipper",      "Flipper Detector",       ToolCategory.BLE,  "Detect active Flipper Zero devices by BLE advertisement", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.spam_det",     "BLE Spam Detector",      ToolCategory.BLE,  "Detect Apple / Android / Windows BLE spam and crash attacks", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.sour_apple",   "Sour Apple Watch",       ToolCategory.BLE,  "Detect Sour Apple BLE crash advertisement targeting iPhones", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.clone_det",    "Clone Detector",         ToolCategory.BLE,  "Flag identical MAC + appearance on multiple devices", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.findmy",       "Find My Observer",       ToolCategory.BLE,  "Observe Apple Find My network tag advertisements", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.classic",      "Classic BT Scanner",     ToolCategory.BLE,  "Discover classic Bluetooth devices (non-BLE)", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("ble.force_pair",   "Force Pair Detector",    ToolCategory.BLE,  "Detect force-pairing attack advertisements", capabilities = setOf(Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = false),
        ToolDef("ble.whisper",      "BLE Whisper",            ToolCategory.BLE,  "Covert short-range BLE message exchange", capabilities = setOf(Capability.BLUETOOTH), isImplemented = false),

        // ── Network ─────────────────────────────────────────────────────────
        ToolDef("net.ssh_client",   "SSH Client",             ToolCategory.NETWORK, "Full SSH client with keyboard support — JSch backend", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.ssh_server",   "SSH Server",             ToolCategory.NETWORK, "Run an SSH server on the tablet (port 8022)", capabilities = setOf(Capability.INTERNET), isImplemented = false),
        ToolDef("net.telnet",       "Telnet Client",          ToolCategory.NETWORK, "Telnet / raw TCP terminal session", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.tcp_client",   "TCP Client",             ToolCategory.NETWORK, "Raw TCP client with hex / ASCII output", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.tcp_listen",   "TCP Listener",           ToolCategory.NETWORK, "Bind a port and capture incoming connections", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.portscan",     "Port Scanner",           ToolCategory.NETWORK, "TCP connect scan with banner grabbing — native + Termux nmap", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.lan_recon",    "LAN Recon",              ToolCategory.NETWORK, "ARP sweep, hostname resolve, OS fingerprint on local subnet", capabilities = setOf(Capability.WIFI, Capability.INTERNET), isImplemented = true),
        ToolDef("net.netmap",       "Net Map",                ToolCategory.NETWORK, "Visual network graph of discovered LAN hosts", capabilities = setOf(Capability.WIFI, Capability.INTERNET), isImplemented = true),
        ToolDef("net.svc_disc",     "Service Discovery",      ToolCategory.NETWORK, "mDNS / SSDP / NetBIOS / UPnP service enumeration", capabilities = setOf(Capability.WIFI, Capability.INTERNET), isImplemented = true),
        ToolDef("net.dns",          "DNS Tools",              ToolCategory.NETWORK, "Lookup, reverse, zone transfer, DNS-over-HTTPS test", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.dhcp",         "DHCP Inspector",         ToolCategory.NETWORK, "Capture and decode DHCP offers, detect rogue DHCP", capabilities = setOf(Capability.WIFI, Capability.ROOT), requiresRoot = true),
        ToolDef("net.arp",          "ARP Scanner",            ToolCategory.NETWORK, "ARP ping sweep and ARP cache dump", capabilities = setOf(Capability.WIFI, Capability.INTERNET), isImplemented = true),
        ToolDef("net.wireguard",    "WireGuard VPN",          ToolCategory.NETWORK, "WireGuard VPN client with quick-connect profiles", capabilities = setOf(Capability.INTERNET), isImplemented = false),
        ToolDef("net.http_probe",   "HTTP Probe",             ToolCategory.NETWORK, "HTTP/HTTPS request builder, header inspector, redirect tracer", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.mqtt",         "MQTT Inspector",         ToolCategory.NETWORK, "Connect to MQTT broker, subscribe topics, decode payloads", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.utils",        "Net Utils",              ToolCategory.NETWORK, "Ping, traceroute, whois, nslookup, curl", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.wpad",         "WPAD Detector",          ToolCategory.NETWORK, "Detect WPAD / proxy auto-config poisoning on the LAN", capabilities = setOf(Capability.WIFI, Capability.INTERNET), isImplemented = true),
        ToolDef("net.rev_shell",    "Reverse Shell",          ToolCategory.NETWORK, "Catch a reverse shell connection (netcat-style listener)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.INTERNET), isImplemented = false),
        ToolDef("net.responder",    "Responder (Termux)",     ToolCategory.NETWORK, "LLMNR/NBT-NS/mDNS poisoner via Termux", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.ROOT), requiresRoot = true),
        ToolDef("net.ntlm_crack",   "NTLM Cracker (Termux)", ToolCategory.NETWORK, "Crack captured NTLMv2 hashes via Termux + hashcat", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX), isImplemented = false),
        ToolDef("net.upnp",         "UPnP / SSDP Scanner",   ToolCategory.NETWORK, "Enumerate UPnP devices and services on the LAN", capabilities = setOf(Capability.WIFI, Capability.INTERNET), isImplemented = true),
        ToolDef("net.printer",      "Printer Discovery",      ToolCategory.NETWORK, "Find network printers via mDNS / IPP / LPD", capabilities = setOf(Capability.WIFI, Capability.INTERNET), isImplemented = true),
        ToolDef("net.bw_test",      "Bandwidth Test",         ToolCategory.NETWORK, "Throughput test to a remote host or local iperf3 server", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("net.sniff_pass",   "Traffic Monitor",        ToolCategory.NETWORK, "Monitor per-app network usage and connections", capabilities = setOf(Capability.ROOT), requiresRoot = true),
        ToolDef("net.dhcp_starv",   "DHCP Starv Detector",    ToolCategory.NETWORK, "Detect DHCP starvation attacks on local subnet", capabilities = setOf(Capability.WIFI, Capability.INTERNET), isImplemented = false),

        // ── NFC (hardware absent on Fire HD 8 — all greyed) ─────────────────
        ToolDef("nfc.read",         "NFC Read",               ToolCategory.NFC, "Read NFC tag — NDEF, MIFARE, NTAG", requiresHardware = "NFC hardware (not present)"),
        ToolDef("nfc.dump",         "MIFARE Dump",            ToolCategory.NFC, "Dump all 64 sectors of a MIFARE Classic card", requiresHardware = "NFC hardware"),
        ToolDef("nfc.write",        "NFC Write",              ToolCategory.NFC, "Write NDEF record to a blank tag", requiresHardware = "NFC hardware"),
        ToolDef("nfc.magic_uid",    "Magic UID",              ToolCategory.NFC, "Clone UID to a MIFARE Magic (gen1/gen2) card", risk = RiskLevel.ACTIVE, requiresHardware = "NFC hardware"),
        ToolDef("nfc.amiibo",       "Amiibo Clone",           ToolCategory.NFC, "Read and write Amiibo NTAG215 figures", requiresHardware = "NFC hardware"),
        ToolDef("nfc.rfid_125",     "RFID 125kHz",            ToolCategory.NFC, "Read 125kHz EM4100 / HID proximity cards", requiresHardware = "USB RFID reader (OTG)"),
        ToolDef("nfc.toolkit",      "NFC Toolkit",            ToolCategory.NFC, "Fuzzer, raw APDU sender, NDEF inspector", requiresHardware = "NFC hardware"),

        // ── Location / GPS ──────────────────────────────────────────────────
        ToolDef("gps.display",      "GPS Display",            ToolCategory.LOCATION, "Live fix: lat/lon, altitude, speed, satellites in view", capabilities = setOf(Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("gps.track",        "GPS Track",              ToolCategory.LOCATION, "Record a GPS track to GPX file", capabilities = setOf(Capability.FINE_LOCATION, Capability.STORAGE), isImplemented = true),
        ToolDef("gps.wardrive",     "Wardriving",             ToolCategory.LOCATION, "GPS + WiFi simultaneous logging (linked from WiFi category)", capabilities = setOf(Capability.FINE_LOCATION, Capability.WIFI), isImplemented = true),
        ToolDef("gps.wigle",        "WiGLE Upload",           ToolCategory.LOCATION, "Send accumulated wardrive data to WiGLE.net", capabilities = setOf(Capability.FINE_LOCATION, Capability.INTERNET), isImplemented = true),
        ToolDef("gps.sat_track",    "Satellite Tracker",      ToolCategory.LOCATION, "SGP4 orbital predictions for ISS, weather and amateur sats", capabilities = setOf(Capability.INTERNET), isImplemented = true),

        // ── Camera ──────────────────────────────────────────────────────────
        ToolDef("cam.qr",           "QR / Barcode",           ToolCategory.CAMERA, "Decode QR codes and 1D barcodes via camera (ZXing)", capabilities = setOf(Capability.CAMERA), isImplemented = true),
        ToolDef("cam.surv",         "Surveillance Hunter",    ToolCategory.CAMERA, "Detect known camera OUIs / SSIDs (Hikvision, Dahua, Reolink…)", capabilities = setOf(Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("cam.drone_rid",    "Drone RID",              ToolCategory.CAMERA, "Decode FAA/CAA Remote ID broadcasts (WiFi Broadcast + BLE)", capabilities = setOf(Capability.WIFI, Capability.BLUETOOTH, Capability.FINE_LOCATION), isImplemented = true),
        ToolDef("cam.ocr",          "OCR Reader",             ToolCategory.CAMERA, "Extract text from images using ML Kit / Tesseract", capabilities = setOf(Capability.CAMERA), isImplemented = false),
        ToolDef("cam.exif",         "EXIF Inspector",         ToolCategory.CAMERA, "Read EXIF metadata from photos (GPS, device, timestamp)", capabilities = setOf(Capability.STORAGE), isImplemented = true),

        // ── SDR Radio (USB-OTG + RTL-SDR required) ──────────────────────────
        ToolDef("sdr.scan",         "RTL-SDR Scanner",        ToolCategory.SDR, "Wideband spectrum scan via RTL-SDR USB dongle + Termux", capabilities = setOf(Capability.USB_HOST, Capability.TERMUX), requiresHardware = "RTL-SDR USB dongle"),
        ToolDef("sdr.fm",           "FM Radio",               ToolCategory.SDR, "FM broadcast reception (rtl_fm via Termux)", capabilities = setOf(Capability.USB_HOST, Capability.TERMUX), requiresHardware = "RTL-SDR USB dongle"),
        ToolDef("sdr.adsb",         "ADS-B Receiver",         ToolCategory.SDR, "Live aircraft tracking (dump1090 via Termux)", capabilities = setOf(Capability.USB_HOST, Capability.TERMUX), requiresHardware = "RTL-SDR USB dongle"),
        ToolDef("sdr.ais",          "AIS Decoder",            ToolCategory.SDR, "Ship tracking from 162MHz AIS signals", capabilities = setOf(Capability.USB_HOST, Capability.TERMUX), requiresHardware = "RTL-SDR USB dongle"),
        ToolDef("sdr.wx_sat",       "Weather Satellite",      ToolCategory.SDR, "NOAA APT / Meteor-M image reception", capabilities = setOf(Capability.USB_HOST, Capability.TERMUX), requiresHardware = "RTL-SDR USB dongle"),
        ToolDef("sdr.rtl433",       "rtl_433 IoT Sensors",   ToolCategory.SDR, "Decode 433MHz IoT sensors, meters, remotes", capabilities = setOf(Capability.USB_HOST, Capability.TERMUX), requiresHardware = "RTL-SDR USB dongle"),
        ToolDef("sdr.acars",        "ACARS Decoder",          ToolCategory.SDR, "Decode ACARS aircraft comms on 131.5MHz", capabilities = setOf(Capability.USB_HOST, Capability.TERMUX), requiresHardware = "RTL-SDR USB dongle"),
        ToolDef("sdr.spectrum",     "Spectrum Analyser",      ToolCategory.SDR, "Live spectrum waterfall display", capabilities = setOf(Capability.USB_HOST, Capability.TERMUX), requiresHardware = "RTL-SDR USB dongle"),
        ToolDef("sdr.lora",         "LoRa Monitor",           ToolCategory.SDR, "Decode LoRa / LoRaWAN packets", capabilities = setOf(Capability.USB_HOST), requiresHardware = "USB LoRa adapter (OTG)"),

        // ── Terminal / security tools (via Termux) ──────────────────────────
        ToolDef("term.terminal",    "Termux Terminal",        ToolCategory.TERMINAL, "Full Termux shell — run any Linux security tool", capabilities = setOf(Capability.TERMUX), isImplemented = true),
        ToolDef("term.nmap",        "Nmap",                   ToolCategory.TERMINAL, "Network mapper — host discovery, port scan, OS detection", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.masscan",     "Masscan",                ToolCategory.TERMINAL, "Ultra-fast port scanner (Masscan via Termux)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.aircrack",    "Aircrack-ng",            ToolCategory.TERMINAL, "WPA/WEP cracking suite (Termux + root + monitor adapter)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.ROOT), requiresRoot = true, requiresHardware = "monitor-mode USB WiFi adapter"),
        ToolDef("term.hashcat",     "Hashcat",                ToolCategory.TERMINAL, "GPU/CPU password cracker (Termux)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX), isImplemented = true),
        ToolDef("term.hydra",       "Hydra",                  ToolCategory.TERMINAL, "Network login brute-forcer (Termux)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.sqlmap",      "SQLMap",                 ToolCategory.TERMINAL, "Automated SQL injection detection and exploitation (Termux)", risk = RiskLevel.OFFENSIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.metasploit",  "Metasploit",             ToolCategory.TERMINAL, "Exploitation framework — msfconsole via Termux", risk = RiskLevel.OFFENSIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.john",        "John the Ripper",        ToolCategory.TERMINAL, "Password cracker for many hash formats (Termux)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX), isImplemented = true),
        ToolDef("term.nikto",       "Nikto",                  ToolCategory.TERMINAL, "Web server vulnerability scanner (Termux)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.gobuster",    "Gobuster / dirb",        ToolCategory.TERMINAL, "Directory and DNS brute-forcing (Termux)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.netcat",      "Netcat",                 ToolCategory.TERMINAL, "Swiss-army TCP/UDP tool (Termux nc)", capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.socat",       "Socat",                  ToolCategory.TERMINAL, "Advanced relay / port-forward / proxy (Termux)", capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.tcpdump",     "tcpdump",                ToolCategory.TERMINAL, "PCAP capture via Termux + root", capabilities = setOf(Capability.TERMUX, Capability.ROOT), requiresRoot = true),
        ToolDef("term.wireshark",   "PCAP Viewer",            ToolCategory.TERMINAL, "Native viewer for .pcap files (no Wireshark binary needed)", capabilities = setOf(Capability.STORAGE), isImplemented = true),
        ToolDef("term.anonsurf",    "AnonSurf / Tor",         ToolCategory.TERMINAL, "Route all traffic through Tor (Termux + torsocks)", capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.scripts",     "Script Runner",          ToolCategory.TERMINAL, "Run saved shell / Python scripts from Files", capabilities = setOf(Capability.TERMUX), isImplemented = true),
        ToolDef("term.enum4linux",  "enum4linux",             ToolCategory.TERMINAL, "Enumerate SMB/Samba shares and users (Termux)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.crackmapexec","CrackMapExec",           ToolCategory.TERMINAL, "SMB / AD network attack and audit tool (Termux)", risk = RiskLevel.OFFENSIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.evil_winrm",  "Evil-WinRM",             ToolCategory.TERMINAL, "Windows Remote Management shell (Termux)", risk = RiskLevel.OFFENSIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.impacket",    "Impacket Suite",         ToolCategory.TERMINAL, "Python SMB/Kerberos/NTLM tools (Termux)", risk = RiskLevel.OFFENSIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),
        ToolDef("term.ffuf",        "ffuf (fuzzer)",          ToolCategory.TERMINAL, "Web fuzzer for paths, params, vhosts (Termux)", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.TERMUX, Capability.INTERNET), isImplemented = true),

        // ── Files ────────────────────────────────────────────────────────────
        ToolDef("files.browse",     "File Browser",           ToolCategory.FILES, "Browse internal storage, SD card, and Andraste data folder", capabilities = setOf(Capability.STORAGE), isImplemented = true),
        ToolDef("files.pcap",       "PCAP Viewer",            ToolCategory.FILES, "Parse and display .pcap capture files", capabilities = setOf(Capability.STORAGE), isImplemented = true),
        ToolDef("files.log",        "Log Viewer",             ToolCategory.FILES, "Tail / search Andraste log files and scan results", capabilities = setOf(Capability.STORAGE), isImplemented = true),
        ToolDef("files.hex",        "Hex Viewer",             ToolCategory.FILES, "View any file in hex + ASCII (editor coming)", capabilities = setOf(Capability.STORAGE), isImplemented = true),
        ToolDef("files.hash",       "Hash Calculator",        ToolCategory.FILES, "MD5 / SHA1 / SHA256 / SHA512 for any file", capabilities = setOf(Capability.STORAGE), isImplemented = true),
        ToolDef("files.export",     "Export / Share",         ToolCategory.FILES, "Zip and share any capture/log via Android share sheet", capabilities = setOf(Capability.STORAGE), isImplemented = true),
        ToolDef("files.sd_format",  "Format SD Card",         ToolCategory.FILES, "Wipe the microSD card", risk = RiskLevel.ACTIVE, capabilities = setOf(Capability.STORAGE), isImplemented = false),

        // ── Tribe (Iceni gamification) ───────────────────────────────────────
        ToolDef("tribe.rising",     "The Rising",             ToolCategory.TRIBE, "Iceni rank progression — XP, unlocks, history cards", isImplemented = true),
        ToolDef("tribe.radar",      "Threat Radar",           ToolCategory.TRIBE, "Passive multi-spectrum threat overview (BLE + WiFi + RF)", capabilities = setOf(Capability.BLUETOOTH, Capability.WIFI, Capability.FINE_LOCATION), isImplemented = true),

        // ── System ───────────────────────────────────────────────────────────
        ToolDef("sys.devinfo",      "Device Info",            ToolCategory.SYSTEM, "Hardware capabilities, radio state, root status, ADB status", isImplemented = true),
        ToolDef("sys.settings",     "Settings",               ToolCategory.SYSTEM, "Theme, permissions, Termux path, consent gate", isImplemented = true),
        ToolDef("sys.perms",        "Permissions",            ToolCategory.SYSTEM, "Grant / revoke all required Android permissions", isImplemented = true),
        ToolDef("sys.usb",          "USB Devices",            ToolCategory.SYSTEM, "Enumerate OTG USB devices — RTL-SDR, WiFi adapters, etc.", capabilities = setOf(Capability.USB_HOST), isImplemented = true),
        ToolDef("sys.update",       "OTA Update",             ToolCategory.SYSTEM, "Check for and download Andraste Tablet updates", capabilities = setOf(Capability.INTERNET), isImplemented = true),
        ToolDef("sys.power",        "Power Menu",             ToolCategory.SYSTEM, "Reboot options and wipe logs", isImplemented = true),
        ToolDef("sys.about",        "About Andraste",         ToolCategory.SYSTEM, "Version, credits, AGPL-3.0 licence", isImplemented = true)
    )

    fun byCategory(cat: ToolCategory) = all.filter { it.category == cat }
    fun categories() = ToolCategory.entries
    fun find(id: String) = all.firstOrNull { it.id == id }
}
