# Andraste — Tablet Edition

A field cyberdeck for the **Amazon Fire HD 8 (2020/2022, codename `onyx`)**. The
Iceni-themed tool launcher from the Andraste CYD firmware, rebuilt as a native
Android app with room for the full toolchain the ESP32 board never had space for.

> Named for the Iceni war-goddess. Deep-green + Snettisham gold. Defensive,
> passive and educational tooling — detectors, not emitters.

---

## What it is

- **130 tools** across 11 categories (WiFi, Bluetooth, Network, NFC, Location,
  Camera, SDR, Terminal, Files, Tribe, System), organised behind an app-button
  launcher.
- **Native implementations** for the tools the tablet's own hardware can drive:
  WiFi scanner, BLE scanner, TCP port scanner, SSH client, GPS display,
  QR/barcode reader, file browser, device-info / capability probe, and the rank
  system ("The Rising").
- **Termux bridge** for the Linux security toolchain — nmap, masscan, hashcat,
  hydra, sqlmap, metasploit, john, nikto, gobuster, netcat, socat, enum4linux,
  crackmapexec, evil-winrm, impacket, ffuf, AnonSurf/Tor — launched by intent.
- **Capability-aware**: every screen checks for the radio / sensor / root / USB
  host it needs and greys out cleanly when the hardware isn't there (the Fire HD
  8 has no NFC and no SDR without a USB dongle).

## Hardware target

| | |
|---|---|
| Device | Fire HD 8, codename `onyx`, model `KFONWI` |
| SoC | MediaTek MT8168, **ARMv7 32-bit** |
| OS | Fire OS 7 (Android 9 / API 28), **no Google Play Services** |
| RAM / free | ~1.78 GB / ~21 GB |
| Radios | 2.4 GHz WiFi (MT7663), Bluetooth; **no NFC**, no GPS-grade GNSS on WiFi-only SKUs |

Every dependency is pure Java / AndroidX (ZXing, JSch, CameraX, raw
`LocationManager`, accompanist-permissions) — **nothing needs GMS**, because Fire
OS doesn't have it.

## Build

Requirements: Android Studio (or the command-line SDK) with **platform 36 +
build-tools 36.0.0** and a JDK the wrapper accepts (Gradle 9.1 / JDK 25 here).

```bash
# from the project root
./gradlew :app:assembleDebug
```

APKs land in `app/build/outputs/apk/debug/`:

- `app-armeabi-v7a-debug.apk` — the 32-bit build for the Fire HD 8 (install this)
- `app-universal-debug.apk` — fallback that carries every ABI

The toolchain is pinned in `build.gradle.kts` / `gradle/wrapper` (AGP 8.13,
Kotlin 2.0.21, Gradle 9.1). `compileSdk = 36` because platform 34 isn't needed;
`targetSdk = 34`, `minSdk = 28`.

## Sideload

1. On the tablet: **Settings → Device Options → Developer Options → USB debugging
   → on**, then plug into USB and accept the RSA prompt.
2. From the project root:

```bash
sideload.bat
```

or manually:

```bash
../platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk
```

3. Launch **Andraste** from the Fire home screen.

Fire OS may warn about installing from an unknown source — that's expected for a
sideloaded debug build.

## Termux (for the terminal security tools)

The Terminal category shells out to Termux. Install Termux from **F-Droid**
(the Play-store build is abandoned), then:

```bash
pkg update && pkg upgrade
pkg install root-repo tur-repo
pkg install nmap masscan hydra sqlmap hashcat john nikto gobuster netcat socat tor
```

Some tools (aircrack monitor mode, responder, tcpdump on `wlan0`) need root
and/or an external adapter — see `docs/ROOT_KERNEL.md`.

## Monitor-mode WiFi

The stock kernel + MT7663 driver won't do monitor mode or injection without
root and a patched driver. Two paths are documented in
[`docs/ROOT_KERNEL.md`](docs/ROOT_KERNEL.md):

1. **Easy** — a USB-OTG adapter with a monitor-capable chipset (Alfa
   AWUS036ACH / RTL8812AU, etc.). No root of the tablet required for the adapter
   itself once a driver is present in Termux with a suitable driver.
2. **Deep** — root the MT8168 (mtkclient bypass) and flash a custom kernel with
   MT7663 monitor patches.

## Layout

```
app/src/main/kotlin/com/andraste/tablet/
  MainActivity.kt            entry point → NavGraph
  AndrasterApp.kt            Application, runs capability detection on boot
  nav/NavGraph.kt            routes: home → category → tool
  hal/CapabilityDetector.kt  root / NFC / camera / GPS / BT / WiFi / USB / Termux / SD
  tools/ToolModel.kt         RiskLevel, Capability, ToolCategory, ToolDef
  tools/ToolRegistry.kt      the 130-tool catalogue
  tools/wifi|ble|network|location|camera|files|terminal|tribe|system/…  screens
  ui/theme/                  Iceni colours, monospace type, dark scheme
  ui/components/             IceniHeader, ToolCard
  ui/home/                   HomeScreen (category grid), CategoryScreen (tool list)
docs/                        ROOT_KERNEL.md, TOOLS.md
sideload.bat                 build + adb install
```

## Ethos & scope

Passive / defensive / educational. Detectors (Deauth Detector, Threat Radar),
not attack emitters — no deauth, no jammers, no spoofers. Run recon only on
networks you own or are explicitly authorised to test.

## Licence

AGPL-3.0.

*For Norfolk. For the Iceni.*
