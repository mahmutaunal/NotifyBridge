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
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alpware.notifybridge.R
import com.alpware.notifybridge.BuildConfig

private const val GITHUB_URL = "https://github.com/mahmutaunal"
private const val ALPWARE_URL = "https://www.alpwarestudio.com/"
private const val GOOGLE_PLAY_URL = "https://play.google.com/store/apps/dev?id=5245599652065968716"

/**
 * User-selectable app theme preference.
 */
enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * User-selectable app language preference.
 */
enum class AppLanguageMode {
    SYSTEM,
    TURKISH,
    ENGLISH
}

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
    onOpenHistory: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onRequestBatteryOptimizationIgnore: () -> Unit,
    onRequestPostNotificationPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onOpenUrl: (String) -> Unit,
    currentThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onThemeModeChanged: (AppThemeMode) -> Unit = {},
    currentLanguageMode: AppLanguageMode = AppLanguageMode.SYSTEM,
    onLanguageModeChanged: (AppLanguageMode) -> Unit = {},
    onRestartApp: () -> Unit = {}
) {
    var showNotificationAccessDisclosure by remember { mutableStateOf(false) }
    var showLanguageRestartDialog by remember { mutableStateOf(false) }

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
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

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenHistory),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null)
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                stringResource(R.string.settings_notification_history),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.settings_notification_history_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }
                }

                PermissionManagementCard(
                    hasNotificationAccess = hasNotificationAccess,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                    isPostNotificationPermissionGranted = isPostNotificationPermissionGranted,
                    isCameraPermissionGranted = isCameraPermissionGranted,
                    onOpenNotificationSettings = {
                        showNotificationAccessDisclosure = true
                    },
                    onRequestBatteryOptimizationIgnore = onRequestBatteryOptimizationIgnore,
                    onRequestPostNotificationPermission = onRequestPostNotificationPermission,
                    onRequestCameraPermission = onRequestCameraPermission
                )

                AppearanceCard(
                    currentThemeMode = currentThemeMode,
                    onThemeModeChanged = onThemeModeChanged,
                    currentLanguageMode = currentLanguageMode,
                    onLanguageModeChanged = { mode ->
                        onLanguageModeChanged(mode)
                        showLanguageRestartDialog = true
                    }
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

            if (showLanguageRestartDialog) {
                LanguageRestartDialog(
                    onDismiss = {
                        showLanguageRestartDialog = false
                    },
                    onRestart = {
                        showLanguageRestartDialog = false
                        onRestartApp()
                    }
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
            trailingText = BuildConfig.VERSION_NAME
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
            TextButton(onClick = onContinue) {
                Text(
                    stringResource(
                        R.string.notification_access_disclosure_continue
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

/**
 * Lets the user choose app appearance and language preferences.
 */
@Composable
private fun AppearanceCard(
    currentThemeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    currentLanguageMode: AppLanguageMode,
    onLanguageModeChanged: (AppLanguageMode) -> Unit
) {
    var showThemeDialog by remember {
        mutableStateOf(false)
    }
    var showLanguageDialog by remember {
        mutableStateOf(false)
    }

    SettingsCard(
        title = stringResource(R.string.settings_appearance_title),
        subtitle = stringResource(R.string.settings_appearance_subtitle)
    ) {
        SettingsRow(
            icon = Icons.Outlined.Palette,
            title = stringResource(R.string.settings_theme_title),
            description = stringResource(R.string.settings_theme_description),
            trailingText = currentThemeMode.label(),
            onClick = {
                showThemeDialog = true
            }
        )

        SettingsDivider()

        SettingsRow(
            icon = Icons.Outlined.Language,
            title = stringResource(R.string.settings_language_title),
            description = stringResource(R.string.settings_language_description),
            trailingText = currentLanguageMode.label(),
            onClick = {
                showLanguageDialog = true
            }
        )
    }

    if (showThemeDialog) {
        ThemeModeDialog(
            currentThemeMode = currentThemeMode,
            onDismiss = {
                showThemeDialog = false
            },
            onThemeModeChanged = {
                showThemeDialog = false
                onThemeModeChanged(it)
            }
        )
    }

    if (showLanguageDialog) {
        LanguageModeDialog(
            currentLanguageMode = currentLanguageMode,
            onDismiss = {
                showLanguageDialog = false
            },
            onLanguageModeChanged = {
                showLanguageDialog = false
                onLanguageModeChanged(it)
            }
        )
    }
}

@Composable
private fun LanguageModeDialog(
    currentLanguageMode: AppLanguageMode,
    onDismiss: () -> Unit,
    onLanguageModeChanged: (AppLanguageMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.settings_language_dialog_title
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LanguageModeOption(
                    title = stringResource(
                        R.string.settings_language_system_title
                    ),
                    description = stringResource(
                        R.string.settings_language_system_description
                    ),
                    selected = currentLanguageMode == AppLanguageMode.SYSTEM,
                    onClick = {
                        onLanguageModeChanged(AppLanguageMode.SYSTEM)
                    }
                )

                LanguageModeOption(
                    title = stringResource(
                        R.string.settings_language_turkish_title
                    ),
                    description = stringResource(
                        R.string.settings_language_turkish_description
                    ),
                    selected = currentLanguageMode == AppLanguageMode.TURKISH,
                    onClick = {
                        onLanguageModeChanged(AppLanguageMode.TURKISH)
                    }
                )

                LanguageModeOption(
                    title = stringResource(
                        R.string.settings_language_english_title
                    ),
                    description = stringResource(
                        R.string.settings_language_english_description
                    ),
                    selected = currentLanguageMode == AppLanguageMode.ENGLISH,
                    onClick = {
                        onLanguageModeChanged(AppLanguageMode.ENGLISH)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    stringResource(R.string.common_cancel)
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun LanguageModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

@Composable
private fun ThemeModeDialog(
    currentThemeMode: AppThemeMode,
    onDismiss: () -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.settings_theme_dialog_title
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                ThemeModeOption(
                    title = stringResource(
                        R.string.settings_theme_system_title
                    ),
                    description = stringResource(
                        R.string.settings_theme_system_description
                    ),
                    selected = currentThemeMode == AppThemeMode.SYSTEM,
                    onClick = {
                        onThemeModeChanged(AppThemeMode.SYSTEM)
                    }
                )

                ThemeModeOption(
                    title = stringResource(
                        R.string.settings_theme_light_title
                    ),
                    description = stringResource(
                        R.string.settings_theme_light_description
                    ),
                    selected = currentThemeMode == AppThemeMode.LIGHT,
                    onClick = {
                        onThemeModeChanged(AppThemeMode.LIGHT)
                    }
                )

                ThemeModeOption(
                    title = stringResource(
                        R.string.settings_theme_dark_title
                    ),
                    description = stringResource(
                        R.string.settings_theme_dark_description
                    ),
                    selected = currentThemeMode == AppThemeMode.DARK,
                    onClick = {
                        onThemeModeChanged(AppThemeMode.DARK)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    stringResource(R.string.common_cancel)
                )
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ThemeModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

/**
 * Returns the user-facing label for a theme mode.
 */
@Composable
private fun AppThemeMode.label(): String {
    return when (this) {
        AppThemeMode.SYSTEM ->
            stringResource(
                R.string.settings_theme_system_title
            )

        AppThemeMode.LIGHT ->
            stringResource(
                R.string.settings_theme_light_title
            )

        AppThemeMode.DARK ->
            stringResource(
                R.string.settings_theme_dark_title
            )
    }
}

/**
 * Returns the user-facing label for a language mode.
 */
@Composable
private fun AppLanguageMode.label(): String {
    return when (this) {
        AppLanguageMode.SYSTEM ->
            stringResource(
                R.string.settings_language_system_title
            )

        AppLanguageMode.TURKISH ->
            stringResource(
                R.string.settings_language_turkish_title
            )

        AppLanguageMode.ENGLISH ->
            stringResource(
                R.string.settings_language_english_title
            )
    }
}

fun AppLanguageMode.languageTag(): String? {
    return when (this) {
        AppLanguageMode.SYSTEM -> null
        AppLanguageMode.TURKISH -> "tr"
        AppLanguageMode.ENGLISH -> "en"
    }
}

@Composable
private fun LanguageRestartDialog(
    onDismiss: () -> Unit,
    onRestart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.language_restart_dialog_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.language_restart_dialog_message),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onRestart) {
                Text(stringResource(R.string.language_restart_dialog_restart_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.language_restart_dialog_later))
            }
        }
    )
}