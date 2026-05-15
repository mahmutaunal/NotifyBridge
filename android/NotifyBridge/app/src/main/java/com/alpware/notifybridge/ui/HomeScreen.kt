package com.alpware.notifybridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.DesktopMac
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.alpware.notifybridge.R
import com.alpware.notifybridge.network.SendResult

/**
 * Main screen that shows pairing status, forwarding controls, permissions, and connection actions.
 */
@Composable
fun HomeScreen(
    hasNotificationAccess: Boolean,
    bridgeEnabled: Boolean,
    macIp: String,
    macPort: String,
    macName: String,
    pairingToken: String,
    sendResult: SendResult?,
    isIgnoringBatteryOptimizations: Boolean,
    onBridgeEnabledChanged: (Boolean) -> Unit,
    onSaveMacConnection: (String, String, String) -> Unit,
    onSendTestNotification: () -> Unit,
    onScanPairingQr: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestBatteryOptimizationIgnore: () -> Unit,
    onOpenSettings: () -> Unit,
    onResetPairing: () -> Unit,
) {
    var ipInput by remember { mutableStateOf(macIp) }
    var portInput by remember { mutableStateOf(macPort) }
    var tokenInput by remember { mutableStateOf(pairingToken) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showTestResultDialog by remember { mutableStateOf(false) }

    val isPaired = macIp.isNotBlank() && pairingToken.isNotBlank()
    val fallbackMacName = stringResource(R.string.home_default_mac_name)
    val pairedMacName = macName.ifBlank { macIp.ifBlank { fallbackMacName } }

    LaunchedEffect(sendResult) {
        when (sendResult) {
            is SendResult.Loading -> {
                showTestResultDialog = true
            }

            is SendResult.Success,
            is SendResult.Error -> {
                showTestResultDialog = true
                delay(2600)
                showTestResultDialog = false
            }

            null -> {
                showTestResultDialog = false
            }
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderSection(
                    onOpenSettings = onOpenSettings
                )

                ConnectionHeroCard(
                    isPaired = isPaired,
                    bridgeEnabled = bridgeEnabled,
                    macIp = macIp,
                    macPort = macPort,
                    macName = pairedMacName
                )

                TransferCard(
                    bridgeEnabled = bridgeEnabled,
                    hasNotificationAccess = hasNotificationAccess,
                    onBridgeEnabledChanged = onBridgeEnabledChanged
                )

                ConnectionCard(
                    hasNotificationAccess = hasNotificationAccess,
                    isPaired = isPaired,
                    macIp = macIp,
                    macPort = macPort,
                    macName = pairedMacName,
                    showAdvanced = showAdvanced,
                    ipInput = ipInput,
                    portInput = portInput,
                    tokenInput = tokenInput,
                    onToggleAdvanced = { showAdvanced = !showAdvanced },
                    onIpChange = { ipInput = it },
                    onPortChange = { portInput = it },
                    onTokenChange = { tokenInput = it },
                    onSaveAdvanced = {
                        onSaveMacConnection(
                            ipInput.trim(),
                            portInput.trim(),
                            tokenInput.trim()
                        )
                    },
                    onScanPairingQr = onScanPairingQr,
                    onSendTestNotification = onSendTestNotification,
                    onResetPairing = onResetPairing
                )

                if (!hasNotificationAccess || !isIgnoringBatteryOptimizations) {
                    PermissionsCard(
                        hasNotificationAccess = hasNotificationAccess,
                        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                        onOpenNotificationSettings = onOpenNotificationSettings,
                        onRequestBatteryOptimizationIgnore = onRequestBatteryOptimizationIgnore
                    )
                }

                InfoCard()
            }
            if (showTestResultDialog && sendResult != null) {
                ConnectionTestResultDialog(
                    sendResult = sendResult,
                    onDismiss = { showTestResultDialog = false }
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_header_description),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            onClick = onOpenSettings
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.home_settings_content_description),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}


/**
 * Highlights the current Mac pairing and notification forwarding status.
 */
@Composable
private fun ConnectionHeroCard(
    isPaired: Boolean,
    bridgeEnabled: Boolean,
    macIp: String,
    macPort: String,
    macName: String
) {
    val connectedBadgeText = stringResource(R.string.home_status_connected)
    val disconnectedBadgeText = stringResource(R.string.home_status_disconnected)
    val disconnectedMacText = stringResource(R.string.home_mac_not_connected)
    val pairingHintText = stringResource(R.string.home_pairing_hint)
    val portLabel = stringResource(R.string.home_port_label)
    val containerColor = if (isPaired) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DesktopMac,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(
                        text = if (isPaired) connectedBadgeText else disconnectedBadgeText,
                        positive = isPaired
                    )

                    Text(
                        text = if (isPaired) macName else disconnectedMacText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (isPaired) {
                            "$macIp • $portLabel $macPort"
                        } else {
                            pairingHintText
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (bridgeEnabled && isPaired) "✓" else "•",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = if (bridgeEnabled && isPaired) {
                            stringResource(R.string.home_transfer_status_forwarding)
                        } else if (isPaired) {
                            stringResource(R.string.home_transfer_status_off)
                        } else {
                            stringResource(R.string.home_transfer_status_setup_pending)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (bridgeEnabled && isPaired) {
                            stringResource(R.string.home_transfer_status_forwarding_description)
                        } else if (isPaired) {
                            stringResource(R.string.home_transfer_status_off_description)
                        } else {
                            stringResource(R.string.home_transfer_status_setup_pending_description)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Provides the primary switch for enabling or disabling notification forwarding.
 */
@Composable
private fun TransferCard(
    bridgeEnabled: Boolean,
    hasNotificationAccess: Boolean,
    onBridgeEnabledChanged: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_transfer_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (hasNotificationAccess) {
                        stringResource(R.string.home_transfer_description_enabled)
                    } else {
                        stringResource(R.string.home_transfer_description_permission_required)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = bridgeEnabled,
                enabled = hasNotificationAccess,
                onCheckedChange = onBridgeEnabledChanged
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    positive: Boolean
) {
    val color = if (positive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}


/**
 * Displays pairing actions, connection testing, and optional manual connection settings.
 */
@Composable
private fun ConnectionCard(
    hasNotificationAccess: Boolean,
    isPaired: Boolean,
    macIp: String,
    macPort: String,
    macName: String,
    showAdvanced: Boolean,
    ipInput: String,
    portInput: String,
    tokenInput: String,
    onToggleAdvanced: () -> Unit,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSaveAdvanced: () -> Unit,
    onScanPairingQr: () -> Unit,
    onSendTestNotification: () -> Unit,
    onResetPairing: () -> Unit
) {
    val portLocalNetworkText = stringResource(R.string.home_paired_port_local_network, macPort)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "▭",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_connection_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (isPaired) {
                            stringResource(R.string.home_connection_description_paired)
                        } else {
                            stringResource(R.string.home_connection_description_unpaired)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isPaired) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.home_paired_mac_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = macName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = macIp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = portLocalNetworkText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        enabled = hasNotificationAccess,
                        onClick = onSendTestNotification
                    ) {
                        Text(stringResource(R.string.home_test_button))
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasNotificationAccess,
                    onClick = onScanPairingQr
                ) {
                    Text(stringResource(R.string.home_change_mac_button))
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onResetPairing
                ) {
                    Text(stringResource(R.string.home_reset_pairing_button))
                }
            } else {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasNotificationAccess,
                    onClick = onScanPairingQr
                ) {
                    Text(stringResource(R.string.home_pair_with_qr_button))
                }
            }


            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_advanced_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = stringResource(R.string.home_advanced_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    enabled = hasNotificationAccess,
                    onClick = onToggleAdvanced
                ) {
                    Text(
                        if (showAdvanced) {
                            stringResource(R.string.home_hide_button)
                        } else {
                            stringResource(R.string.home_show_button)
                        }
                    )
                }
            }

            if (showAdvanced) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = ipInput,
                    onValueChange = onIpChange,
                    label = { Text(stringResource(R.string.home_mac_ip_label)) },
                    placeholder = { Text("192.168.1.25") },
                    singleLine = true
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = portInput,
                    onValueChange = onPortChange,
                    label = { Text(stringResource(R.string.home_port_label)) },
                    placeholder = { Text("8787") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = tokenInput,
                    onValueChange = onTokenChange,
                    label = { Text(stringResource(R.string.home_advanced_pairing_key_label)) },
                    singleLine = true
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasNotificationAccess,
                    onClick = onSaveAdvanced
                ) {
                    Text(stringResource(R.string.home_save_manual_settings_button))
                }
            }
        }
    }
}

/**
 * Shows the current result of a manual Mac connection test.
 */
@Composable
private fun ConnectionTestResultDialog(
    sendResult: SendResult,
    onDismiss: () -> Unit
) {
    val title = when (sendResult) {
        is SendResult.Loading -> stringResource(R.string.home_connection_test_loading_title)
        is SendResult.Success -> stringResource(R.string.home_connection_test_success_title)
        is SendResult.Error -> stringResource(R.string.home_connection_test_error_title)
    }

    val message = when (sendResult) {
        is SendResult.Loading -> stringResource(R.string.home_connection_test_loading_message)
        is SendResult.Success -> stringResource(R.string.home_connection_test_success_message)
        is SendResult.Error -> sendResult.message
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            when (sendResult) {
                is SendResult.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                }

                is SendResult.Success -> {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                is SendResult.Error -> {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        }
    )
}

/**
 * Prompts the user to grant permissions required for reliable background forwarding.
 */
@Composable
private fun PermissionsCard(
    hasNotificationAccess: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onRequestBatteryOptimizationIgnore: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.home_permissions_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.home_permissions_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasNotificationAccess) {
                PermissionRow(
                    title = stringResource(R.string.home_notification_access_title),
                    description = stringResource(R.string.home_notification_access_description),
                    buttonText = stringResource(R.string.home_open_settings_button),
                    onClick = onOpenNotificationSettings
                )
            }

            if (!hasNotificationAccess && !isIgnoringBatteryOptimizations) {
                HorizontalDivider()
            }

            if (!isIgnoringBatteryOptimizations) {
                PermissionRow(
                    title = stringResource(R.string.home_battery_optimization_title),
                    description = stringResource(R.string.home_battery_optimization_description),
                    buttonText = stringResource(R.string.home_exclude_button),
                    onClick = onRequestBatteryOptimizationIgnore
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(onClick = onClick) {
            Text(buttonText)
        }
    }
}
/**
 * Explains how notification forwarding works at a high level.
 */
@Composable
private fun InfoCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_how_it_works_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = stringResource(R.string.home_how_it_works_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
