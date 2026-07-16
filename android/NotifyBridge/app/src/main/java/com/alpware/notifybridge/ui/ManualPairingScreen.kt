package com.alpware.notifybridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DesktopMac
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alpware.notifybridge.R

/** Manual alternative to QR pairing for users who need to enter Mac connection data. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPairingScreen(
    isPairing: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onPair: (host: String, port: Int, code: String, fingerprint: String, name: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8787") }
    var code by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf("") }
    var showValidation by remember { mutableStateOf(false) }

    val normalizedHost = host.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
    val parsedPort = port.toIntOrNull()
    val normalizedFingerprint = fingerprint.filter { it.isLetterOrDigit() }.lowercase()
    val hostValid =
        normalizedHost.isNotBlank() && !normalizedHost.contains('/') && !normalizedHost.contains(' ')
    val portValid = parsedPort != null && parsedPort in 1..65535
    val codeValid = code.trim().isNotBlank()
    val fingerprintValid =
        normalizedFingerprint.length == 64 && normalizedFingerprint.all { it in '0'..'9' || it in 'a'..'f' }
    val formValid = hostValid && portValid && codeValid && fingerprintValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_pairing_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isPairing) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            Icons.Outlined.DesktopMac,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.manual_pairing_heading),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.manual_pairing_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.manual_pairing_name_label)) },
                supportingText = { Text(stringResource(R.string.manual_pairing_name_support)) },
                singleLine = true,
                enabled = !isPairing
            )

            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.manual_pairing_host_label)) },
                placeholder = { Text("192.168.1.25") },
                isError = showValidation && !hostValid,
                supportingText = if (showValidation && !hostValid) {
                    { Text(stringResource(R.string.manual_pairing_host_error)) }
                } else null,
                singleLine = true,
                enabled = !isPairing
            )

            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter(Char::isDigit).take(5) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.home_port_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = showValidation && !portValid,
                supportingText = if (showValidation && !portValid) {
                    { Text(stringResource(R.string.manual_pairing_port_error)) }
                } else null,
                singleLine = true,
                enabled = !isPairing
            )

            OutlinedTextField(
                value = code,
                onValueChange = { code = it.trim().take(32) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.manual_pairing_code_label)) },
                supportingText = { Text(stringResource(R.string.manual_pairing_code_support)) },
                isError = showValidation && !codeValid,
                singleLine = true,
                enabled = !isPairing
            )

            OutlinedTextField(
                value = fingerprint,
                onValueChange = { fingerprint = it.take(95) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.manual_pairing_fingerprint_label)) },
                supportingText = {
                    Text(
                        if (showValidation && !fingerprintValid) {
                            stringResource(R.string.manual_pairing_fingerprint_error)
                        } else {
                            stringResource(R.string.manual_pairing_fingerprint_support)
                        }
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.Security, contentDescription = null) },
                isError = showValidation && !fingerprintValid,
                minLines = 2,
                enabled = !isPairing
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    showValidation = true
                    if (formValid) {
                        onPair(
                            normalizedHost,
                            parsedPort,
                            code.trim(),
                            normalizedFingerprint,
                            name.trim().ifBlank { null })
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPairing
            ) {
                if (isPairing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp),
                        strokeWidth = 2.dp
                    )
                    Text(stringResource(R.string.manual_pairing_connecting))
                } else {
                    Text(stringResource(R.string.manual_pairing_button))
                }
            }

            Text(
                text = stringResource(R.string.manual_pairing_security_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
