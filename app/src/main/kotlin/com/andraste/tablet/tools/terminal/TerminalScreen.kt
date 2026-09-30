package com.andraste.tablet.tools.terminal

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*

data class TermuxTool(val id: String, val name: String, val cmd: String, val description: String)

val TERMUX_TOOLS = listOf(
    TermuxTool("nmap",         "Nmap",            "nmap -sV -T4 ",     "Network mapper — add target IP after space"),
    TermuxTool("masscan",      "Masscan",         "masscan -p1-65535 ","Ultra-fast port scanner"),
    TermuxTool("hashcat",      "Hashcat",         "hashcat -m 22000 ", "WPA2 hash cracker — add hash file"),
    TermuxTool("aircrack",     "Aircrack-ng",     "aircrack-ng ",      "WPA/WEP crack — add .cap file"),
    TermuxTool("hydra",        "Hydra",           "hydra -l admin -P /data/data/com.termux/files/usr/share/wordlists/rockyou.txt ssh://", "SSH brute-force"),
    TermuxTool("sqlmap",       "SQLMap",          "sqlmap -u ",        "SQL injection tester"),
    TermuxTool("metasploit",   "Metasploit",      "msfconsole",        "Exploitation framework"),
    TermuxTool("john",         "John the Ripper", "john ",             "Password hash cracker"),
    TermuxTool("nikto",        "Nikto",           "nikto -h ",         "Web server scanner"),
    TermuxTool("gobuster",     "Gobuster",        "gobuster dir -u http://TARGET -w /data/data/com.termux/files/usr/share/wordlists/dirb/common.txt","Directory brute-force"),
    TermuxTool("netcat",       "Netcat",          "nc -lvp 4444",      "Listener on port 4444"),
    TermuxTool("socat",        "Socat",           "socat TCP-LISTEN:4444,reuseaddr,fork EXEC:sh","Reverse shell catcher"),
    TermuxTool("tcpdump",      "tcpdump",         "tcpdump -i wlan0 -w /sdcard/capture.pcap","Packet capture (root)"),
    TermuxTool("enum4linux",   "enum4linux",      "enum4linux ",       "SMB/Samba enumeration"),
    TermuxTool("crackmapexec", "CrackMapExec",    "crackmapexec smb ", "SMB auditing tool"),
    TermuxTool("impacket",     "Impacket",        "python3 /data/data/com.termux/files/usr/bin/secretsdump.py ","Extract credentials"),
    TermuxTool("ffuf",         "ffuf",            "ffuf -u http://TARGET/FUZZ -w /data/data/com.termux/files/usr/share/wordlists/dirb/common.txt","Web fuzzer"),
    TermuxTool("tor",          "AnonSurf/Tor",    "torify bash",       "Route shell through Tor"),
    TermuxTool("evil_winrm",   "Evil-WinRM",      "evil-winrm -i ",    "WinRM shell"),
    TermuxTool("responder",    "Responder",       "sudo python3 Responder.py -I wlan0","LLMNR/NBNS poisoner (root)"),
)

@Composable
fun TerminalScreen(toolId: String = "term.terminal", onBack: () -> Unit) {
    val ctx = LocalContext.current
    val isTermuxInstalled = remember(ctx) {
        try { ctx.packageManager.getPackageInfo("com.termux", 0); true } catch (_: Exception) { false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IceniDeepGreen)
    ) {
        val preset = TERMUX_TOOLS.firstOrNull { "term.${it.id}" == toolId }
        IceniHeader(
            title = preset?.name ?: "Terminal",
            subtitle = if (isTermuxInstalled) "Termux ready" else "Termux not installed",
            onBack = onBack
        )

        if (!isTermuxInstalled) {
            Text(
                text = "Install Termux from F-Droid then run:\n\npkg update && pkg upgrade\npkg install nmap masscan hydra sqlmap hashcat john nikto gobuster netcat socat",
                style = IceniTypography.bodyMedium,
                color = IceniAmber,
                modifier = Modifier.padding(16.dp)
            )
        } else if (preset != null) {
            // Launch this specific tool directly
            LaunchTermuxCommand(ctx, preset.cmd)
            Text(
                text = "Launched Termux:\n\n${preset.cmd}",
                style = IceniTypography.bodyMedium,
                color = IceniText,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            // Show tool picker
            Text(
                text = "TERMINAL SECURITY TOOLS",
                style = IceniTypography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(TERMUX_TOOLS) { tool ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(IceniGreen, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .clickable { LaunchTermuxCommand(ctx, tool.cmd) }
                            .padding(12.dp)
                    ) {
                        Text(text = tool.name, style = IceniTypography.titleMedium)
                        Text(text = tool.description, style = IceniTypography.bodySmall)
                        Text(text = "$ ${tool.cmd}", style = IceniTypography.bodySmall, color = IceniTeal)
                    }
                }
            }
        }
    }
}

private fun LaunchTermuxCommand(ctx: Context, cmd: String) {
    try {
        val intent = Intent().apply {
            component = ComponentName("com.termux", "com.termux.app.TermuxActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Pass command via RUN_COMMAND intent if available
        val runIntent = Intent("com.termux.RUN_COMMAND").apply {
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", cmd))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_TERMINAL", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ctx.startActivity(runIntent)
        } catch (_: Exception) {
            ctx.startActivity(intent)
        }
    } catch (_: Exception) {}
}
