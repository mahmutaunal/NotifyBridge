package com.alpware.notifybridge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DesktopMac
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alpware.notifybridge.R

private const val GITHUB_URL = "https://github.com/mahmutaunal"
private const val ALPWARE_URL = "https://www.alpwarestudio.com/"
private const val GOOGLE_PLAY_URL = "https://play.google.com/store/apps/dev?id=5245599652065968716"

/**
 * Settings screen for permissions, connection security, app information, and developer links.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    hasNotificationAccess: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    isPostNotificationPermissionGranted: Boolean,
    isCameraPermissionGranted: Boolean,
    bridgeEnabled: Boolean,
    macIp: String,
    macPort: String,
    macName: String,
    hasPairingSecret: Boolean,
    onBack: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestBatteryOptimizationIgnore: () -> Unit,
    onRequestPostNotificationPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionManagementCard(
                    hasNotificationAccess = hasNotificationAccess,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                    isPostNotificationPermissionGranted = isPostNotificationPermissionGranted,
                    isCameraPermissionGranted = isCameraPermissionGranted,
                    onOpenNotificationSettings = onOpenNotificationSettings,
                    onRequestBatteryOptimizationIgnore = onRequestBatteryOptimizationIgnore,
                    onRequestPostNotificationPermission = onRequestPostNotificationPermission,
                    onRequestCameraPermission = onRequestCameraPermission
                )

                SecurityCard(
                    bridgeEnabled = bridgeEnabled,
                    macIp = macIp,
                    macPort = macPort,
                    macName = macName,
                    hasPairingSecret = hasPairingSecret
                )

                ApplicationCard(
                    onOpenUrl = onOpenUrl
                )

                DeveloperCard(
                    onOpenUrl = onOpenUrl
                )
            }
        }
    }
}

/**
 * Lists required and recommended permissions with direct actions to system settings.
 */
@Composable
private fun PermissionManagementCard(
    hasNotificationAccess: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    isPostNotificationPermissionGranted: Boolean,
    isCameraPermissionGranted: Boolean,
    onOpenNotificationSettings: () -> Unit,
    onRequestBatteryOptimizationIgnore: () -> Unit,
    onRequestPostNotificationPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit
) {
    SettingsCard(
        title = stringResource(R.string.settings_permissions_title),
        subtitle = stringResource(R.string.settings_permissions_subtitle)
    ) {
        PermissionRow(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.settings_notification_access_title),
            description = stringResource(R.string.settings_notification_access_description),
            status = if (hasNotificationAccess) {
                stringResource(R.string.permission_status_granted)
            } else {
                stringResource(R.string.permission_status_required)
            },
            positive = hasNotificationAccess,
            onClick = onOpenNotificationSettings
        )

        SettingsDivider()

        PermissionRow(
            icon = Icons.Outlined.CameraAlt,
            title = stringResource(R.string.settings_camera_permission_title),
            description = stringResource(R.string.settings_camera_permission_description),
            status = if (isCameraPermissionGranted) {
                stringResource(R.string.permission_status_granted)
            } else {
                stringResource(R.string.permission_status_required)
            },
            positive = isCameraPermissionGranted,
            onClick = onRequestCameraPermission
        )

        SettingsDivider()

        PermissionRow(
            icon = Icons.Outlined.BatteryChargingFull,
            title = stringResource(R.string.settings_battery_optimization_title),
            description = stringResource(R.string.settings_battery_optimization_description),
            status = if (isIgnoringBatteryOptimizations) {
                stringResource(R.string.permission_status_granted)
            } else {
                stringResource(R.string.permission_status_recommended)
            },
            positive = isIgnoringBatteryOptimizations,
            onClick = onRequestBatteryOptimizationIgnore
        )

        SettingsDivider()

        PermissionRow(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.settings_post_notification_permission_title),
            description = stringResource(R.string.settings_post_notification_permission_description),
            status = if (isPostNotificationPermissionGranted) {
                stringResource(R.string.permission_status_granted)
            } else {
                stringResource(R.string.permission_status_required)
            },
            positive = isPostNotificationPermissionGranted,
            onClick = onRequestPostNotificationPermission
        )
    }
}

/**
 * Shows paired Mac details and the active local security configuration.
 */
@Composable
private fun SecurityCard(
    bridgeEnabled: Boolean,
    macIp: String,
    macPort: String,
    macName: String,
    hasPairingSecret: Boolean
) {
    val noPairedMacText = stringResource(R.string.settings_no_paired_mac)
    val statusActiveText = stringResource(R.string.settings_status_active)
    val statusInactiveText = stringResource(R.string.settings_status_inactive)
    val noQrPairingText = stringResource(R.string.settings_no_qr_pairing)
    val defaultPortText = stringResource(R.string.settings_default_port)
    val pairedText = macName.ifBlank {
        macIp.ifBlank { noPairedMacText }
    }
    val pairedDescription = if (hasPairingSecret) {
        "$pairedText • ${if (bridgeEnabled) statusActiveText else statusInactiveText}"
    } else {
        noQrPairingText
    }

    SettingsCard(
        title = stringResource(R.string.settings_connection_security_title),
        subtitle = stringResource(R.string.settings_connection_security_subtitle)
    ) {
        SettingsRow(
            icon = Icons.Outlined.DesktopMac,
            title = stringResource(R.string.settings_paired_mac_title),
            description = pairedDescription,
            trailingText = if (hasPairingSecret) {
                statusActiveText
            } else {
                stringResource(R.string.settings_status_none)
            }
        )

        SettingsDivider()

        SettingsRow(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.settings_encryption_title),
            description = stringResource(R.string.settings_encryption_description),
            trailingText = "AES-256-GCM"
        )

        SettingsDivider()

        SettingsRow(
            icon = Icons.Outlined.Tune,
            title = stringResource(R.string.settings_connection_port_title),
            description = stringResource(R.string.settings_connection_port_description),
            trailingText = macPort.ifBlank { defaultPortText }
        )

        SettingsDivider()

        SettingsRow(
            icon = Icons.Outlined.Security,
            title = stringResource(R.string.settings_signing_title),
            description = stringResource(R.string.settings_signing_description),
            trailingText = if (hasPairingSecret) {
                statusActiveText
            } else {
                stringResource(R.string.settings_status_none)
            }
        )
    }
}

/**
 * Displays app version, source code, and privacy-related information.
 */
@Composable
private fun ApplicationCard(
    onOpenUrl: (String) -> Unit
) {
    SettingsCard(
        title = stringResource(R.string.settings_app_title),
        subtitle = stringResource(R.string.settings_app_subtitle)
    ) {
        SettingsRow(
            icon = Icons.Outlined.Info,
            title = stringResource(R.string.settings_version_title),
            description = stringResource(R.string.settings_version_description),
            trailingText = "0.1.0"
        )

        SettingsDivider()

        LinkRow(
            icon = Icons.Outlined.Code,
            title = stringResource(R.string.settings_open_source_title),
            description = stringResource(R.string.settings_open_source_description),
            onClick = { onOpenUrl(GITHUB_URL) }
        )

        SettingsDivider()

        LinkRow(
            icon = Icons.Outlined.Policy,
            title = stringResource(R.string.settings_privacy_title),
            description = stringResource(R.string.settings_privacy_description),
            onClick = { onOpenUrl(ALPWARE_URL) }
        )
    }
}

/**
 * Groups external links related to the developer and published apps.
 */
@Composable
private fun DeveloperCard(
    onOpenUrl: (String) -> Unit
) {
    SettingsCard(
        title = stringResource(R.string.settings_developer_title),
        subtitle = stringResource(R.string.settings_developer_subtitle)
    ) {
        LinkRow(
            icon = Icons.Outlined.Code,
            title = "GitHub",
            description = "github.com/mahmutaunal",
            onClick = { onOpenUrl(GITHUB_URL) }
        )

        SettingsDivider()

        LinkRow(
            icon = Icons.Outlined.Language,
            title = "AlpWare Studio",
            description = stringResource(R.string.settings_alpware_description),
            onClick = { onOpenUrl(ALPWARE_URL) }
        )

        SettingsDivider()

        LinkRow(
            icon = Icons.Outlined.Store,
            title = "Google Play",
            description = stringResource(R.string.settings_google_play_description),
            onClick = { onOpenUrl(GOOGLE_PLAY_URL) }
        )
    }
}

/**
 * Shared card layout used by each settings section.
 */
@Composable
private fun SettingsCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            content()
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    status: String,
    positive: Boolean,
    onClick: () -> Unit
) {
    SettingsRowScaffold(
        icon = icon,
        title = title,
        description = description,
        onClick = onClick,
        trailing = {
            StatusIndicator(
                positive = positive,
                text = status
            )
        }
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    description: String,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null
) {
    SettingsRowScaffold(
        icon = icon,
        title = title,
        description = description,
        onClick = onClick,
        trailing = {
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    SettingsRowScaffold(
        icon = icon,
        title = title,
        description = description,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/**
 * Shared row layout for settings, permission, and external link items.
 */
@Composable
private fun SettingsRowScaffold(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
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
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        trailing()

        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * Compact visual indicator for granted, required, or recommended permission states.
 */
@Composable
private fun StatusIndicator(
    positive: Boolean,
    text: String
) {
    val symbol = if (positive) {
        stringResource(R.string.permission_status_positive_symbol)
    } else {
        stringResource(R.string.permission_status_negative_symbol)
    }
    val color = if (positive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Column(horizontalAlignment = Alignment.End) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.70f)
    )
}