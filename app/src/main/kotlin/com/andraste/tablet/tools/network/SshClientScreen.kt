package com.andraste.tablet.tools.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.andraste.tablet.ui.components.IceniHeader
import com.andraste.tablet.ui.theme.*
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SshClientScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("uname -a") }
    var output by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(IceniDeepGreen)) {
        IceniHeader(title = "SSH Client", subtitle = if (busy) "connecting…" else "JSch backend", onBack = onBack)

        Column(
            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                field(host, { host = it }, "Host / IP", Modifier.weight(2f))
                field(port, { port = it }, "Port", Modifier.weight(1f))
            }
            field(user, { user = it }, "Username", Modifier.fillMaxWidth())
            field(pass, { pass = it }, "Password", Modifier.fillMaxWidth(), password = true)
            field(command, { command = it }, "Command", Modifier.fillMaxWidth())

            Button(
                onClick = {
                    busy = true; output = ""
                    scope.launch {
                        output = runSsh(host, port.toIntOrNull() ?: 22, user, pass, command)
                        busy = false
                    }
                },
                enabled = !busy && host.isNotBlank() && user.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = IceniGold, contentColor = IceniDeepGreen),
                modifier = Modifier.fillMaxWidth()
            ) { Text("RUN", style = IceniTypography.labelLarge.copy(color = IceniDeepGreen)) }

            if (output.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IceniGreenLight, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(output, style = IceniTypography.bodyMedium, color = IceniText)
                }
            }
        }
    }
}

@Composable
private fun field(
    value: String, onChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, password: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = IceniTextMuted) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = IceniText,
            unfocusedTextColor = IceniText,
            focusedBorderColor = IceniGold,
            unfocusedBorderColor = IceniGoldDim,
            cursorColor = IceniGold
        ),
        modifier = modifier
    )
}

private suspend fun runSsh(host: String, port: Int, user: String, pass: String, cmd: String): String =
    withContext(Dispatchers.IO) {
        try {
            val jsch = JSch()
            val session = jsch.getSession(user, host, port)
            session.setPassword(pass)
            session.setConfig("StrictHostKeyChecking", "no")
            session.connect(8000)
            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(cmd)
            val stream = channel.inputStream
            channel.connect()
            val out = stream.bufferedReader().readText()
            channel.disconnect()
            session.disconnect()
            out.ifBlank { "[no output — exit ${channel.exitStatus}]" }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }
