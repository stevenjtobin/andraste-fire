# Tool catalogue

130 tools across 11 categories. **30** are natively implemented today; the rest
are registered stubs (they show up in the launcher with their risk badge and
hardware requirements, and open a placeholder until wired up). The registry is
the single source of truth: `app/src/main/kotlin/com/andraste/tablet/tools/ToolRegistry.kt`.

| Category | Tools | Native today |
|---|---:|---|
| 📡 WiFi | 26 | WiFi Scanner |
| 🔷 Bluetooth | 15 | BLE Scanner |
| 🌐 Network | 25 | Port Scanner, SSH Client |
| 📲 NFC | 7 | — (Fire HD 8 has no NFC — all greyed out) |
| 📍 Location | 5 | GPS Display |
| 📷 Camera | 5 | QR / Barcode |
| 📻 SDR Radio | 9 | — (needs RTL-SDR USB dongle) |
| 💻 Terminal (security) | 22 | Termux bridge + 19 tool launchers |
| 📁 Files | 7 | File Browser |
| ⚔️ Tribe | 2 | The Rising |
| ⚙️ System | 7 | Device Info, Settings, Permissions, About |

## Risk levels

Every tool carries a badge:

- **PASSIVE** — listen-only / read-only (scanners, detectors, info).
- **ACTIVE** — touches the target (port scan, nmap, directory brute-force).
- **OFFENSIVE** — exploitation (sqlmap, metasploit, crackmapexec, evil-winrm,
  impacket).

By project policy Andraste ships **detectors, not emitters**: no deauth, no
jammers, no spoofers. Active/offensive tools are the standard Linux security userland you run
deliberately against authorised targets via Termux.

## Capability gating

Each `ToolDef` declares the capabilities it needs (`WIFI`, `BLUETOOTH`,
`FINE_LOCATION`, `CAMERA`, `USB_HOST`, `TERMUX`, `ROOT`, `STORAGE`, …) and an
optional `requiresHardware` note. `CapabilityDetector` probes the device on boot;
the Device Info screen shows exactly what's present. On the Fire HD 8:

- **NFC** — absent → the whole NFC category is greyed with "NFC hardware (not present)".
- **SDR** — absent → SDR tools show "RTL-SDR USB dongle" required.
- **Monitor mode / injection** — needs root + patched kernel or an OTG adapter
  (see `ROOT_KERNEL.md`); the tools that need it declare `requiresRoot` /
  `requiresHardware`.

## Terminal category (security tools via Termux)

The Terminal tools launch Termux with a preset command via the
`com.termux.RUN_COMMAND` intent (falls back to just opening Termux if the RUN
permission isn't granted). Registered launchers:

nmap · masscan · aircrack-ng · hashcat · hydra · sqlmap · metasploit · john ·
nikto · gobuster/dirb · netcat · socat · tcpdump · enum4linux · crackmapexec ·
evil-winrm · impacket · ffuf · AnonSurf/Tor · plus a raw Termux shell.

Install the binaries once in Termux — see the README's Termux section.

## Adding a native implementation

1. Flip `isImplemented = true` on the `ToolDef` in `ToolRegistry.kt`.
2. Write the screen under `tools/<category>/`.
3. Add a `when` branch in `nav/NavGraph.kt` mapping the tool `id` to your screen.

Unmapped implemented tools fall through to `StubToolScreen`, so nothing crashes
mid-migration.
