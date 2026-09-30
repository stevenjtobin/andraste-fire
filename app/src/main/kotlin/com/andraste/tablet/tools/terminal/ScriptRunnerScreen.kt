package com.andraste.tablet.tools.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import java.io.File

private const val TERMUX_PKG = "com.termux"
private const val BASH_PATH = "/data/data/com.termux/files/usr/bin/bash"
private const val PYTHON_PATH = "/data/data/com.termux/files/usr/bin/python"

@Composable
fun ScriptRunnerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current

    val scriptsDir = remember {
        File(ctx.getExternalFilesDir(null), "andraste/scripts").also {
            try { if (!it.exists()) it.mkdirs() } catch (_: Exception) {}
        }
    }
    var scripts by remember { mutableStateOf<List<File>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scripts = try {
            (scriptsDir.listFiles()?.filter {
                it.isFile && (it.name.endsWith(".sh") || it.name.endsWith(".py"))
            } ?: emptyList()).sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            status = e.message; emptyList()
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun run(script: File) {
        status = null
        val isPython = script.name.endsWith(".py")
        val interpreter = if (isPython) PYTHON_PATH else BASH_PATH
        try {
            val intent = Intent("com.termux.RUN_COMMAND").apply {
                setClassName(TERMUX_PKG, "com.termux.app.RunCommandService")
                putExtra("com.termux.RUN_COMMAND_PATH", interpreter)
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf(script.absolutePath))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", script.parent ?: "")
                putExtra("com.termux.RUN_COMMAND_TERMINAL", true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ContextCompat.startForegroundService(ctx, intent)
            status = "Sent '${script.name}' to Termux via RUN_COMMAND."
        } catch (e: Exception) {
            // Fallback: just open Termux
            status = "RUN_COMMAND failed (${e.message}); opening Termux…"
            try {
                val open = Intent().apply {
                    component = ComponentName(TERMUX_PKG, "com.termux.app.TermuxActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(open)
            } catch (e2: Exception) {
                status = "Could not launch Termux: ${e2.message}. Is Termux installed?"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(
            title = "Script Runner",
            subtitle = "Termux .sh / .py launcher",
            onBack = onBack,
            actions = {
                TextButton(onClick = { refresh() }) {
                    Text("REFRESH", color = IceniTeal, style = IceniTypography.labelSmall)
                }
            }
        )

        Text(
            "Enable Termux setting: allow-external-apps=true, and grant the " +
                "com.termux.permission.RUN_COMMAND permission for scripts to launch.",
            style = IceniTypography.bodySmall, color = IceniTextHint,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
        status?.let {
            Text(it, style = IceniTypography.bodyMedium, color = IceniAmber,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }

        if (scripts.isEmpty()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("No scripts found.", style = IceniTypography.titleMedium, color = IceniTextMuted)
                Text(
                    "Place .sh or .py files in:\n${scriptsDir.absolutePath}",
                    style = IceniTypography.bodyMedium, color = IceniTextHint
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(scripts) { f ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreen, RoundedCornerShape(6.dp))
                            .clickable { run(f) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            if (f.name.endsWith(".py")) "🐍 " else "⚙ ",
                            style = IceniTypography.bodyLarge
                        )
                        Text(f.name, style = IceniTypography.bodyLarge, color = IceniText,
                            modifier = Modifier.weight(1f))
                        Text("RUN", style = IceniTypography.labelSmall, color = IceniGold)
                    }
                }
            }
        }
    }
}
