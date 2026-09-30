package com.andraste.tablet.tools.files

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import java.io.File

@Composable
fun FileBrowserScreen(onBack: () -> Unit) {
    val root = remember { Environment.getExternalStorageDirectory() }
    var current by remember { mutableStateOf(root) }

    val entries = remember(current) {
        (current.listFiles()?.toList() ?: emptyList())
            .sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "File Browser",
            subtitle = current.absolutePath,
            onBack = {
                if (current.absolutePath == root.absolutePath) onBack()
                else current = current.parentFile ?: root
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (current.parentFile != null && current.absolutePath != root.absolutePath) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreenLight, RoundedCornerShape(4.dp))
                            .clickable { current = current.parentFile!! }
                            .padding(12.dp)
                    ) { Text(".. (up)", style = IceniTypography.titleMedium, color = IceniGold) }
                }
            }
            items(entries) { f ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreen, RoundedCornerShape(4.dp))
                        .clickable(enabled = f.isDirectory) { if (f.isDirectory) current = f }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(if (f.isDirectory) "📁 " else "📄 ", style = IceniTypography.bodyLarge)
                    Text(
                        text = f.name,
                        style = IceniTypography.bodyLarge,
                        color = if (f.isDirectory) IceniText else IceniTextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    if (!f.isDirectory) {
                        Text(humanSize(f.length()), style = IceniTypography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun humanSize(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576     -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024          -> "%.1f KB".format(bytes / 1024.0)
    else                   -> "$bytes B"
}
