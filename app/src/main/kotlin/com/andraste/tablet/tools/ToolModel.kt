package com.andraste.tablet.tools

enum class RiskLevel(val label: String) {
    PASSIVE("PASSIVE"),
    ACTIVE("ACTIVE"),
    OFFENSIVE("OFFENSIVE")
}

enum class Capability {
    ROOT, CAMERA, NFC, FINE_LOCATION, BLUETOOTH, WIFI,
    INTERNET, USB_HOST, TERMUX, STORAGE
}

enum class ToolCategory(val displayName: String, val emoji: String) {
    WIFI      ("WiFi",      "📡"),
    BLE       ("Bluetooth", "🔷"),
    NETWORK   ("Network",   "🌐"),
    NFC       ("NFC",       "📲"),
    LOCATION  ("Location",  "📍"),
    CAMERA    ("Camera",    "📷"),
    SDR       ("SDR Radio", "📻"),
    TERMINAL  ("Terminal",  "💻"),
    FILES     ("Files",     "📁"),
    TRIBE     ("Tribe",     "⚔️"),
    SYSTEM    ("System",    "⚙️")
}

data class ToolDef(
    val id: String,
    val name: String,
    val category: ToolCategory,
    val description: String,
    val risk: RiskLevel = RiskLevel.PASSIVE,
    val capabilities: Set<Capability> = emptySet(),
    val isImplemented: Boolean = false,
    val requiresRoot: Boolean = false,
    val requiresHardware: String? = null
)
