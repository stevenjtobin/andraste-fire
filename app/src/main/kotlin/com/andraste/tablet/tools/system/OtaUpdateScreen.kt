package com.andraste.tablet.tools.system

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.BuildConfig
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

// OTA manifest hosted at the URL below (raw GitHub, public andraste-ota repo).
private const val MANIFEST_URL =
    "https://raw.githubusercontent.com/stevenjtobin/andraste-ota/main/tablet/manifest.json"

private data class OtaManifest(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val notes: String = "",
    val sha256: String = ""
)

private sealed class OtaState {
    object Idle : OtaState()
    object Checking : OtaState()
    data class UpToDate(val m: OtaManifest) : OtaState()
    data class Available(val m: OtaManifest) : OtaState()
    data class Downloading(val pct: Int) : OtaState()
    data class Ready(val file: File, val m: OtaManifest) : OtaState()
    data class Error(val msg: String) : OtaState()
}

@Composable
fun OtaUpdateScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<OtaState>(OtaState.Idle) }
    val http = remember { OkHttpClient() }

    fun check() {
        state = OtaState.Checking
        scope.launch {
            val result: Result<OtaManifest> = withContext(Dispatchers.IO) {
                try {
                    val body = http.newCall(Request.Builder().url(MANIFEST_URL).build())
                        .execute().use { it.body?.string() ?: "" }
                    val m = Gson().fromJson(body, OtaManifest::class.java)
                        ?: return@withContext Result.failure(Exception("empty manifest"))
                    Result.success(m)
                } catch (e: Exception) { Result.failure(e) }
            }
            state = result.fold(
                onSuccess = { m ->
                    if (m.versionCode > BuildConfig.VERSION_CODE) OtaState.Available(m)
                    else OtaState.UpToDate(m)
                },
                onFailure = { OtaState.Error(it.message ?: "network error") }
            )
        }
    }

    fun download(m: OtaManifest) {
        state = OtaState.Downloading(0)
        scope.launch {
            val out = withContext(Dispatchers.IO) {
                try {
                    val resp = http.newCall(Request.Builder().url(m.apkUrl).build()).execute()
                    val bodyStream = resp.body?.byteStream() ?: return@withContext Result.failure(Exception("empty body"))
                    val total = resp.body?.contentLength() ?: -1L
                    val file = File(ctx.cacheDir, "andraste-update.apk")
                    file.outputStream().use { fos ->
                        val buf = ByteArray(64 * 1024); var read: Int; var done = 0L
                        while (bodyStream.read(buf).also { read = it } != -1) {
                            fos.write(buf, 0, read); done += read
                            if (total > 0) {
                                val pct = ((done * 100) / total).toInt()
                                withContext(Dispatchers.Main) { state = OtaState.Downloading(pct) }
                            }
                        }
                    }
                    resp.close()
                    Result.success(file)
                } catch (e: Exception) { Result.failure(e) }
            }
            state = out.fold(
                onSuccess = { OtaState.Ready(it, m) },
                onFailure = { OtaState.Error(it.message ?: "download failed") }
            )
        }
    }

    fun install(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            state = OtaState.Error("install failed: ${e.message}")
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "OTA Update",
            subtitle = "current v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            onBack = onBack
        )
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Andraste checks GitHub over WiFi for a newer build, downloads the APK, " +
                    "and hands it to the Android installer. No laptop needed.",
                style = IceniTypography.bodyMedium
            )

            when (val s = state) {
                is OtaState.Idle -> {}
                is OtaState.Checking -> {
                    LinearProgressIndicator(color = IceniGold, trackColor = IceniGreenLight,
                        modifier = Modifier.fillMaxWidth())
                    Text("Checking for updates…", style = IceniTypography.bodyMedium)
                }
                is OtaState.UpToDate -> card {
                    Text("✓ Up to date", style = IceniTypography.titleMedium, color = IceniTeal)
                    Text("Latest is v${s.m.versionName} (${s.m.versionCode}).", style = IceniTypography.bodyMedium)
                }
                is OtaState.Available -> card {
                    Text("⬇ Update available", style = IceniTypography.titleMedium, color = IceniGold)
                    Text("v${s.m.versionName} (${s.m.versionCode})", style = IceniTypography.bodyLarge, color = IceniText)
                    if (s.m.notes.isNotBlank()) Text(s.m.notes, style = IceniTypography.bodySmall)
                    Button(
                        onClick = { download(s.m) },
                        colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("DOWNLOAD & INSTALL", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }
                }
                is OtaState.Downloading -> {
                    Text("Downloading… ${s.pct}%", style = IceniTypography.bodyMedium)
                    LinearProgressIndicator(progress = { s.pct / 100f }, color = IceniGold,
                        trackColor = IceniGreenLight, modifier = Modifier.fillMaxWidth())
                }
                is OtaState.Ready -> card {
                    Text("✓ Downloaded", style = IceniTypography.titleMedium, color = IceniTeal)
                    Text("Tap install and confirm on the Android prompt.", style = IceniTypography.bodySmall)
                    Button(
                        onClick = { install(s.file) },
                        colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("INSTALL v${s.m.versionName}", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }
                }
                is OtaState.Error -> card {
                    Text("⚠ ${s.msg}", style = IceniTypography.bodyMedium, color = IceniRed)
                }
            }

            OutlinedButton(
                onClick = { check() },
                border = androidx.compose.foundation.BorderStroke(1.dp, IceniGold),
                modifier = Modifier.fillMaxWidth()
            ) { Text("CHECK FOR UPDATES", style = IceniTypography.labelLarge) }

            Text(
                "Note: updates must be signed with the same key as the installed build. " +
                    "You may need to allow \"install unknown apps\" for Andraste the first time.",
                style = IceniTypography.bodySmall, color = IceniTextHint
            )
        }
    }
}

@Composable
private fun card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(IceniGreen, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}
