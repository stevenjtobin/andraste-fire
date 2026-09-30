package com.andraste.tablet.hal

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import java.io.File

data class DeviceCapabilities(
    val hasRoot: Boolean,
    val hasNfc: Boolean,
    val hasCamera: Boolean,
    val hasGps: Boolean,
    val hasBluetooth: Boolean,
    val hasWifi: Boolean,
    val hasUsbHost: Boolean,
    val isTermuxInstalled: Boolean,
    val hasExternalStorage: Boolean
)

object CapabilityDetector {

    fun detect(ctx: Context): DeviceCapabilities = DeviceCapabilities(
        hasRoot             = checkRoot(),
        hasNfc              = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC),
        hasCamera           = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
        hasGps              = hasGps(ctx),
        hasBluetooth        = BluetoothAdapter.getDefaultAdapter() != null,
        hasWifi             = hasWifi(ctx),
        hasUsbHost          = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
        isTermuxInstalled   = isTermuxInstalled(ctx),
        hasExternalStorage  = hasExternalSd(ctx)
    )

    private fun checkRoot(): Boolean =
        listOf("/system/bin/su", "/system/xbin/su", "/su/bin/su", "/magisk/.core/bin/su")
            .any { File(it).exists() }

    private fun hasGps(ctx: Context): Boolean = try {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.getProviders(true).isNotEmpty()
    } catch (_: Exception) { false }

    private fun hasWifi(ctx: Context): Boolean = try {
        (ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager) != null
    } catch (_: Exception) { false }

    private fun isTermuxInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo("com.termux", 0)
        true
    } catch (_: Exception) { false }

    private fun hasExternalSd(ctx: Context): Boolean =
        ctx.getExternalFilesDirs(null).size > 1

    fun usbDevices(ctx: Context): List<String> {
        val mgr = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
        return mgr.deviceList.values.map {
            "${it.deviceName}  vid=${it.vendorId.toString(16)}  pid=${it.productId.toString(16)}"
        }
    }
}
