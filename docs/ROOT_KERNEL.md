# Root & custom kernel — monitor-mode WiFi on the Fire HD 8 (onyx / MT8168)

This is the reference for getting monitor mode and packet injection working. The
stock Fire OS kernel and the MediaTek MT7663 WiFi driver expose only managed
mode. There are two routes; pick by appetite.

> Nothing here is required for the passive tools (WiFi scan, BLE scan, port
> scan, SSH, GPS, QR, files, terminal). It only matters for monitor-mode
> capture, injection, and the tools that depend on them.

---

## Route 1 — USB-OTG adapter (recommended, low risk)

Leave the tablet's own kernel alone and bring your own radio.

**Hardware**
- A USB-OTG cable (USB-C or micro-USB depending on your onyx revision).
- A monitor-mode-capable adapter. Known-good chipsets:
  - RTL8812AU (Alfa AWUS036ACH) — dual-band, needs the `rtl8812au` driver.
  - RTL8188EUS (Alfa AWUS036NEH) — 2.4 GHz, `8188eu`.
  - AR9271 (Alfa AWUS036NHA) — 2.4 GHz, `ath9k_htc`, cleanest injection.

**Software (Termux, no tablet root needed for the adapter path if the kernel
already has USB + the driver module; otherwise Termux + a driver package):**
```bash
pkg install root-repo
pkg install tsu           # only if you later root
# aircrack-ng suite:
pkg install aircrack-ng
# confirm the adapter enumerates:
ls /sys/class/net        # look for wlanX / wlxXXXX
```
If the adapter appears but won't go to monitor mode, the stock kernel is missing
the driver — you then need Route 2's kernel, or a custom pen-testing kernel that
bundles the RTL/ath9k modules.

`usb_device_filter.xml` in the app already whitelists the common RTL/Ralink/Alfa
vendor IDs so Fire OS will hand the app the USB-attached intent.

**Vendor IDs already declared**
| Chipset | VID:PID |
|---|---|
| RTL-SDR (Realtek) | 0bda:2838, 0bda:2832 |
| Realtek WiFi | 0bda:* |
| Atheros/Alfa | 0cf3:* |
| Ralink | 148f:* |

---

## Route 2 — Root the tablet + custom kernel (high effort, high reward)

Full internal-radio monitor mode. This unlocks the tablet's own MT7663 for
capture without an external dongle. It is invasive and can brick — do it on a
device you're willing to lose.

### 2.1 Unlock / bypass with mtkclient

MediaTek SoCs expose a BROM download mode. `mtkclient`
(github.com/bkerler/mtkclient) uses the MT8168's BROM to read/write partitions
without a signed unlock, via the "kamakiri"/"hovatek" bypass.

```bash
git clone https://github.com/bkerler/mtkclient
cd mtkclient
pip install -r requirements.txt
# Power off the tablet. Hold both volume keys, plug in USB to enter BROM.
python mtk.py payload           # send the DA bypass payload
python mtk.py r boot boot.img   # dump the boot partition
```

Install the correct libusb / MediaTek VCOM driver on the host first (Windows:
use Zadig to bind WinUSB to the "MediaTek USB Port" that appears for ~1 s on
plug-in).

### 2.2 Patch boot for root (Magisk)

```bash
# On the tablet or host, patch the dumped boot.img with Magisk:
#   Magisk app → Install → Patch a file → boot.img
python mtk.py w boot magisk_patched-boot.img
```
Reboot. You now have `su`. The app's `CapabilityDetector` will show **ROOTED**
on the Device Info screen.

### 2.3 Custom kernel with MT7663 monitor patches

Managed-only is a driver policy, not a silicon limit. The `mt76` mainline driver
supports monitor mode for many MediaTek parts; the Fire stock driver is a locked
vendor blob. Options in rough order of practicality:

1. **A community pen-testing kernel for onyx** if/when a build exists — it ships a kernel with
   `mt76`/monitor support and the userland. Easiest if available.
2. **Build a kernel from Amazon's GPL source** for onyx
   (amazon.com/gp/help/customer/display.html?nodeId=200203720), swapping the
   vendor `mt7663` for `mt7663` from the `mt76` tree with `CONFIG_MT7663U=m` and
   `CONFIG_MAC80211` monitor support, then flash via `mtk.py w kernel`.
3. **Overlay module** — cross-compile just the `mt76` module against the stock
   kernel's headers and `insmod` it as root. Fragile across OTA kernel changes.

### 2.4 Verify

```bash
su
iw list | grep -A8 "Supported interface modes"   # expect "* monitor"
airmon-ng start wlan0
iw dev                                            # wlan0mon in monitor type
```

---

## Safety / legality

- Monitor mode is passive listening; **injection / deauth is active** and is out
  of scope for this app by policy (detectors only).
- Rooting voids the warranty and disables Amazon OTA; keep a full partition dump
  (`python mtk.py rl dump`) before writing anything.
- Only capture on networks you own or are authorised to test.
