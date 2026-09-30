package com.andraste.tablet.tools.files

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import java.io.File

@Composable
fun ExportShareScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val dir = remember { File(ctx.getExternalFilesDir(null), "andraste") }

    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    val selected = remember { mutableStateListOf<File>() }
    var status by remember { mutableStateOf<String?>(null) }
    var fallback by remember { mutableStateOf<List<String>>(emptyList()) }

    fun refresh() {
        files = try {
            if (!dir.exists()) emptyList()
            else (dir.listFiles()?.filter { it.isFile } ?: emptyList()).sortedByDescending { it.lastModified() }
        } catch (e: Exception) {
            status = e.message; emptyList()
        }
        selected.retainAll(files)
    }

    LaunchedEffect(Unit) { refresh() }

    fun share() {
        status = null
        fallback = emptyList()
        if (selected.isEmpty()) { status = "Select at least one file first."; return }
        try {
            val authority = ctx.packageName + ".fileprovider"
            val uris = ArrayList<Uri>()
            selected.forEach { f -> uris.add(FileProvider.getUriForFile(ctx, authority, f)) }
            val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "Share Andraste files")
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ctx.startActivity(chooser)
            status = "Opened share sheet for ${selected.size} file(s)."
        } catch (e: Exception) {
            status = "Share failed: ${e.message}. Showing file paths instead."
            fallback = selected.map { it.absolutePath }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Export / Share",
            subtitle = dir.absolutePath,
            onBack = onBack,
            actions = {
                TextButton(onClick = { refresh() }) {
                    Text("REFRESH", color = IceniTeal, style = IceniTypography.labelSmall)
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { share() },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen)
            ) { Text("Share (${selected.size})") }
            TextButton(onClick = { selected.clear() }) {
                Text("Clear", color = IceniTextMuted)
            }
        }

        status?.let {
            Text(it, style = IceniTypography.bodyMedium, color = IceniAmber,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
        if (fallback.isNotEmpty()) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                fallback.forEach {
                    Text(it, style = IceniTypography.bodySmall, color = IceniTextMuted)
                }
            }
        }

        if (files.isEmpty()) {
            Text(
                "No files in the Andraste output directory yet.\n${dir.absolutePath}",
                style = IceniTypography.bodyMedium, color = IceniTextMuted,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(files) { f ->
                    val checked = selected.contains(f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (checked) IceniGreenLight else IceniGreen, RoundedCornerShape(6.dp))
                            .clickable { if (checked) selected.remove(f) else selected.add(f) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { if (it) selected.add(f) else selected.remove(f) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = IceniGold,
                                uncheckedColor = IceniGoldDim,
                                checkmarkColor = IceniDeepGreen
                            )
                        )
                        Text(f.name, style = IceniTypography.bodyLarge, color = IceniText,
                            modifier = Modifier.weight(1f))
                        Text(fmtSizeShare(f.length()), style = IceniTypography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun fmtSizeShare(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
