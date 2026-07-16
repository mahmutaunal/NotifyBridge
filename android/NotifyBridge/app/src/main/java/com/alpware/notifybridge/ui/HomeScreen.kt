package com.alpware.notifybridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.DesktopMac
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import com.alpware.notifybridge.R
import com.alpware.notifybridge.network.SendResult
import com.alpware.notifybridge.model.PairedMac
import kotlin.time.Duration.Companion.milliseconds

/**
 * Main screen that shows pairing status, forwarding controls, permissions, and connection actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    hasNotificationAccess: Boolean,
    bridgeEnabled: Boolean,
    isMacOnline: Boolean,
    isRefreshingConnection: Boolean,
    macIp: String,
    macPort: String,
    macName: String,
    pairingToken: String,
    pairedMacs: List<PairedMac>,
    selectedMacId: String?,
    onSelectMac: (String) -> Unit,
    sendResult: SendResult?,
    isIgnoringBatteryOptimizations: Boolean,
    onBridgeEnabledChanged: (Boolean) -> Unit,
    onSaveMacConnection: (String, String, String) -> Unit,
    onSendTestNotification: () -> Unit,
    onScanPairingQr: () -> Unit,
    onManualPairing: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRefreshConnection: () -> Unit,
    showNotificationContent: Boolean,
    onShowNotificationContentChanged: (Boolean) -> Unit,
    onRequestBatteryOptimizationIgnore: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppFilters: () -> Unit,
    selectedAppFilterCount: Int,
    onResetPairing: () -> Unit,
) {
    var ipInput by remember { mutableStateOf(macIp) }
    var portInput by remember { mutableStateOf(macPort) }
    var tokenInput by remember { mutableStateOf(pairingToken) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showTestResultDialog by remember { mutableStateOf(false) }
    var showNotificationAccessDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMacId, macIp, macPort, pairingToken) {
        ipInput = macIp
        portInput = macPort
        tokenInput = pairingToken
        showAdvanced = false
    }

    // A valid pairing requires both a Mac address and a shared pairing token.
    val isPaired = macIp.isNotBlank() && pairingToken.isNotBlank()
    val isConnected = isPaired && isMacOnline
    val fallbackMacName = stringResource(R.string.home_default_mac_name)
    val pairedMacName = macName.ifBlank { macIp.ifBlank { fallbackMacName } }

    // Automatically dismiss completed connection test results after a short delay.
    LaunchedEffect(sendResult) {
        when (sendResult) {
            is SendResult.Loading -> {
                showTestResultDialog = true
            }

            is SendResult.Success,
            is SendResult.Error -> {
                showTestResultDialog = true
                delay(2600.milliseconds)
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
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = isRefreshingConnection,
                onRefresh = onRefreshConnection
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

                DeviceConnectionCarousel(
                    pairedMacs = pairedMacs,
                    selectedMacId = selectedMacId,
                    isSelectedMacOnline = isMacOnline,
                    hasNotificationAccess = hasNotificationAccess,
                    bridgeEnabled = bridgeEnabled,
                    macIp = macIp,
                    macPort = macPort,
                    macName = pairedMacName,
                    showAdvanced = showAdvanced,
                    ipInput = ipInput,
                    portInput = portInput,
                    tokenInput = tokenInput,
                    onSelectMac = onSelectMac,
                    onBridgeEnabledChanged = onBridgeEnabledChanged,
                    onToggleAdvanced = { showAdvanced = !showAdvanced },
                    onIpChange = { ipInput = it },
                    onPortChange = { portInput = it },
                    onTokenChange = { tokenInput = it },
                    onSaveAdvanced = {
                        onSaveMacConnection(ipInput.trim(), portInput.trim(), tokenInput.trim())
                    },
                    onScanPairingQr = onScanPairingQr,
                    onManualPairing = onManualPairing,
                    onSendTestNotification = onSendTestNotification,
                    onOpenNotificationSettings = { showNotificationAccessDisclosure = true },
                    onResetPairing = onResetPairing
                )

                NotificationPreferencesCard(
                    selectedAppFilterCount = selectedAppFilterCount,
                    onOpenAppFilters = onOpenAppFilters,
                    showNotificationContent = showNotificationContent,
                    onShowNotificationContentChanged = onShowNotificationContentChanged
                )

                if (!hasNotificationAccess || !isIgnoringBatteryOptimizations) {
                    PermissionsCard(
                        hasNotificationAccess = hasNotificationAccess,
                        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                        onOpenNotificationSettings = {
                            showNotificationAccessDisclosure = true
                        },
                        onRequestBatteryOptimizationIgnore = onRequestBatteryOptimizationIgnore
                    )
                }

                    InfoCard()
                }
            }

            if (showTestResultDialog && sendResult != null) {
                ConnectionTestResultDialog(
                    sendResult = sendResult,
                    onDismiss = { showTestResultDialog = false }
                )
            }

            if (showNotificationAccessDisclosure) {
                NotificationAccessDisclosureDialog(
                    onDismiss = {
                        showNotificationAccessDisclosure = false
                    },
                    onContinue = {
                        showNotificationAccessDisclosure = false
                        onOpenNotificationSettings()
                    }
                )
            }
        }
    }
}

@Composable
private fun DeviceConnectionCarousel(
    pairedMacs: List<PairedMac>,
    selectedMacId: String?,
    isSelectedMacOnline: Boolean,
    hasNotificationAccess: Boolean,
    bridgeEnabled: Boolean,
    macIp: String,
    macPort: String,
    macName: String,
    showAdvanced: Boolean,
    ipInput: String,
    portInput: String,
    tokenInput: String,
    onSelectMac: (String) -> Unit,
    onBridgeEnabledChanged: (Boolean) -> Unit,
    onToggleAdvanced: () -> Unit,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSaveAdvanced: () -> Unit,
    onScanPairingQr: () -> Unit,
    onManualPairing: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onResetPairing: () -> Unit
) {
    val pageCount = pairedMacs.size + 1
    val selectedIndex = pairedMacs.indexOfFirst { it.id == selectedMacId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedIndex, pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedMacId, pairedMacs.size) {
        val target = pairedMacs.indexOfFirst { it.id == selectedMacId }
        if (target >= 0 && target != pagerState.currentPage) pagerState.scrollToPage(target)
    }
    LaunchedEffect(pagerState.currentPage, pairedMacs) {
        pairedMacs.getOrNull(pagerState.currentPage)?.let { device ->
            if (device.id != selectedMacId) onSelectMac(device.id)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalPager(state = pagerState, pageSpacing = 12.dp) { page ->
            if (page < pairedMacs.size) {
                val device = pairedMacs[page]
                val selected = device.id == selectedMacId
                MainConnectionCard(
                    hasNotificationAccess = hasNotificationAccess,
                    isPaired = device.isValid,
                    isConnected = selected && isSelectedMacOnline,
                    bridgeEnabled = bridgeEnabled && device.enabled,
                    isMacOnline = selected && isSelectedMacOnline,
                    macIp = device.host,
                    macPort = device.port.toString(),
                    macName = device.displayName,
                    showAdvanced = selected && showAdvanced,
                    ipInput = if (selected) ipInput else device.host,
                    portInput = if (selected) portInput else device.port.toString(),
                    tokenInput = if (selected) tokenInput else device.secret,
                    onBridgeEnabledChanged = onBridgeEnabledChanged,
                    onToggleAdvanced = onToggleAdvanced,
                    onIpChange = onIpChange,
                    onPortChange = onPortChange,
                    onTokenChange = onTokenChange,
                    onSaveAdvanced = onSaveAdvanced,
                    onScanPairingQr = onScanPairingQr,
                    onSendTestNotification = onSendTestNotification,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onResetPairing = onResetPairing
                )
            } else {
                AddMacCard(onScanQr = onScanPairingQr, onManualPairing = onManualPairing)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                enabled = pagerState.currentPage > 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
            ) {
                Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = stringResource(R.string.home_previous_device))
            }
            Text(
                text = if (pagerState.currentPage < pairedMacs.size) {
                    "${pagerState.currentPage + 1} / ${pairedMacs.size}"
                } else {
                    stringResource(R.string.home_add_device_short)
                },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
            IconButton(
                enabled = pagerState.currentPage < pageCount - 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = stringResource(R.string.home_next_device))
            }
        }
    }
}

@Composable
private fun AddMacCard(
    onScanQr: () -> Unit,
    onManualPairing: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            }
            Text(stringResource(R.string.home_add_mac_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.home_add_mac_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onScanQr, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_add_mac_qr_button))
            }
            OutlinedButton(onClick = onManualPairing, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_add_mac_manual_button))
            }
        }
    }
}

/**
 * Displays the app title, short description, and settings shortcut.
 */
@Composable
private fun HeaderSection(
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
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
 * Groups notification filtering and privacy controls in a single preferences card.
 */
@Composable
private fun NotificationPreferencesCard(
    selectedAppFilterCount: Int,
    onOpenAppFilters: () -> Unit,
    showNotificationContent: Boolean,
    onShowNotificationContentChanged: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreferenceIcon(
                    icon = Icons.Outlined.FilterList,
                    contentDescription = null
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_app_filters_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (selectedAppFilterCount == 0) {
                            stringResource(R.string.home_app_filters_all_apps_description)
                        } else {
                            stringResource(
                                R.string.home_app_filters_selected_apps_description,
                                selectedAppFilterCount
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = onOpenAppFilters) {
                    Text(stringResource(R.string.home_manage_button))
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PreferenceIcon(
                    icon = Icons.Outlined.Visibility,
                    contentDescription = null
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_notification_content_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (showNotificationContent) {
                            stringResource(R.string.home_notification_content_visible_description)
                        } else {
                            stringResource(R.string.home_notification_content_hidden_description)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = showNotificationContent,
                    onCheckedChange = onShowNotificationContentChanged
                )
            }
        }
    }
}

/**
 * Displays a circular icon used by notification preference rows.
 */
@Composable
private fun PreferenceIcon(
    icon: ImageVector,
    contentDescription: String?
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Displays a compact visual indicator for connection state.
 */
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
 * Shows the primary Mac connection state, forwarding switch, and essential connection actions.
 */
@Composable
private fun MainConnectionCard(
    hasNotificationAccess: Boolean,
    isPaired: Boolean,
    isConnected: Boolean,
    bridgeEnabled: Boolean,
    isMacOnline: Boolean,
    macIp: String,
    macPort: String,
    macName: String,
    showAdvanced: Boolean,
    ipInput: String,
    portInput: String,
    tokenInput: String,
    onBridgeEnabledChanged: (Boolean) -> Unit,
    onToggleAdvanced: () -> Unit,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onSaveAdvanced: () -> Unit,
    onScanPairingQr: () -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onResetPairing: () -> Unit
) {
    val connectedBadgeText = stringResource(R.string.home_status_connected)
    val disconnectedBadgeText = stringResource(R.string.home_status_disconnected)
    val offlineBadgeText = stringResource(R.string.home_status_offline)
    val disconnectedMacText = stringResource(R.string.home_mac_not_connected)
    val pairingHintText = stringResource(R.string.home_pairing_hint)
    val portLabel = stringResource(R.string.home_port_label)

    val containerColor = when {
        isConnected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
        isPaired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceContainer
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
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DesktopMac,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(
                        text = when {
                            isConnected -> connectedBadgeText
                            isPaired -> offlineBadgeText
                            else -> disconnectedBadgeText
                        },
                        positive = isConnected
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!hasNotificationAccess) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_transfer_description_permission_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenNotificationSettings
                    ) {
                        Text(stringResource(R.string.home_open_settings_button))
                    }
                }
            } else if (isPaired && !isMacOnline) {
                Text(
                    text = stringResource(R.string.home_mac_offline_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_transfer_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = when {
                            !hasNotificationAccess -> stringResource(R.string.home_transfer_description_permission_required)
                            !isPaired -> stringResource(R.string.home_transfer_description_pairing_required)
                            !isMacOnline -> stringResource(R.string.home_transfer_description_mac_offline)
                            bridgeEnabled -> stringResource(R.string.home_transfer_description_enabled)
                            else -> stringResource(R.string.home_transfer_status_off_description)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = bridgeEnabled && isPaired,
                    enabled = hasNotificationAccess && isPaired && isMacOnline,
                    onCheckedChange = onBridgeEnabledChanged
                )
            }

            if (!isPaired) {
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
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = stringResource(R.string.home_advanced_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
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

                if (isPaired) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onResetPairing
                    ) {
                        Text(stringResource(R.string.home_reset_pairing_button))
                    }
                }
            }

            if (isPaired) {
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MainConnectionActionItem(
                        enabled = hasNotificationAccess,
                        icon = Icons.Outlined.FlashOn,
                        text = stringResource(R.string.home_test_button),
                        onClick = onSendTestNotification
                    )

                    Spacer(modifier = Modifier.width(28.dp))

                    MainConnectionActionItem(
                        enabled = hasNotificationAccess,
                        icon = Icons.Outlined.Refresh,
                        text = stringResource(R.string.home_change_mac_button),
                        onClick = onScanPairingQr
                    )
                }
            }
        }
    }
}

/**
 * Displays a compact inline text action inside the main connection card.
 */
@Composable
private fun MainConnectionActionItem(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

/**
 * Displays a single permission requirement with its related action.
 */
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

@Composable
private fun NotificationAccessDisclosureDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.notification_access_disclosure_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.notification_access_disclosure_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text(stringResource(R.string.notification_access_disclosure_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}