package com.andraste.tablet.tools.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var result by remember { mutableStateOf<String?>(null) }
    var format by remember { mutableStateOf("") }

    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "QR / Barcode",
            subtitle = if (result != null) format else "point at a code",
            onBack = onBack
        )

        if (!granted) {
            Text(
                "Camera permission required — grant it in Permissions.",
                style = IceniTypography.bodyMedium, color = IceniAmber,
                modifier = Modifier.padding(16.dp)
            )
            return@Column
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { c ->
                    val previewView = PreviewView(c)
                    val executor = Executors.newSingleThreadExecutor()
                    val providerFuture = ProcessCameraProvider.getInstance(c)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(executor) { proxy ->
                            decode(proxy)?.let { (text, fmt) ->
                                result = text
                                format = fmt
                            }
                            proxy.close()
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                        )
                    }, ContextCompat.getMainExecutor(c))
                    previewView
                }
            )
        }

        if (result != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IceniGreen)
                    .padding(16.dp)
            ) {
                Text("DECODED", style = IceniTypography.labelLarge)
                Text(result!!, style = IceniTypography.bodyLarge, color = IceniTeal)
            }
        }
    }
}

private val reader = MultiFormatReader().apply {
    setHints(mapOf(DecodeHintType.TRY_HARDER to true))
}

private fun decode(proxy: ImageProxy): Pair<String, String>? {
    val plane = proxy.planes.firstOrNull() ?: return null
    val data = ByteArray(plane.buffer.remaining()).also { plane.buffer.get(it) }
    val source = PlanarYUVLuminanceSource(
        data, plane.rowStride, proxy.height,
        0, 0, minOf(plane.rowStride, proxy.width), proxy.height, false
    )
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    return try {
        val r = reader.decodeWithState(bitmap)
        r.text to r.barcodeFormat.name
    } catch (_: NotFoundException) {
        null
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
    }
}
