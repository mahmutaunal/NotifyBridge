package com.alpware.notifybridge.ui

import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.alpware.notifybridge.R
import com.alpware.notifybridge.model.InstalledAppItem
import androidx.core.graphics.createBitmap


/**
 * Displays the notification forwarding filter screen for installed apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFilterScreen(
    apps: List<InstalledAppItem>,
    sendAllApps: Boolean,
    onBack: () -> Unit,
    onSendAllAppsChanged: (Boolean) -> Unit,
    onAppFilterChanged: (String, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showOnlySelected by remember { mutableStateOf(false) }

    val selectedCount = apps.count { it.isEnabled }
    val forwardedCount = if (sendAllApps) apps.size else selectedCount

    // Applies both search text and selected-only filters to the app list.
    val filteredApps = apps.filter { app ->
        val matchesSearch =
            app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

        val matchesSelectedFilter =
            !showOnlySelected || app.isEnabled

        matchesSearch && matchesSelectedFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_filter_title),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                sendAllApps -> stringResource(R.string.app_filter_all_apps_subtitle)
                                selectedCount == 0 -> stringResource(R.string.app_filter_no_apps_subtitle)
                                else -> stringResource(R.string.app_filter_selected_apps_subtitle, selectedCount)
                            },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FilterInfoCard(
                        sendAllApps = sendAllApps,
                        selectedCount = selectedCount
                    )
                }

                item {
                    SendAllAppsCard(
                        sendAllApps = sendAllApps,
                        forwardedCount = forwardedCount,
                        onSendAllAppsChanged = { enabled ->
                            onSendAllAppsChanged(enabled)

                            if (enabled) {
                                apps
                                    .filterNot { it.isEnabled }
                                    .forEach { app ->
                                        onAppFilterChanged(app.packageName, true)
                                    }
                            }
                        }
                    )
                }

                item {
                    FilterSearchCard(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        showOnlySelected = showOnlySelected,
                        onShowOnlySelectedChange = { showOnlySelected = it },
                        sendAllApps = sendAllApps,
                        selectedCount = selectedCount,
                        onClearFilters = {
                            apps.filter { it.isEnabled }.forEach { app ->
                                onAppFilterChanged(app.packageName, false)
                            }
                        }
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.app_filter_apps_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(
                    items = filteredApps,
                    key = { it.packageName }
                ) { app ->
                    AppFilterRow(
                        app = app,
                        sendAllApps = sendAllApps,
                        onCheckedChange = { enabled ->
                            onAppFilterChanged(app.packageName, enabled)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Explains how notification filtering behaves based on the current selection state.
 */
@Composable
private fun FilterInfoCard(
    sendAllApps: Boolean,
    selectedCount: Int
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.app_filter_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = when {
                    sendAllApps -> stringResource(R.string.app_filter_info_all_apps_description)
                    selectedCount == 0 -> stringResource(R.string.app_filter_info_no_apps_description)
                    else -> stringResource(R.string.app_filter_info_selected_apps_description)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SendAllAppsCard(
    sendAllApps: Boolean,
    forwardedCount: Int,
    onSendAllAppsChanged: (Boolean) -> Unit
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.app_filter_send_all_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (sendAllApps) {
                        stringResource(R.string.app_filter_send_all_enabled_description, forwardedCount)
                    } else {
                        stringResource(R.string.app_filter_send_all_disabled_description)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = sendAllApps,
                onCheckedChange = onSendAllAppsChanged
            )
        }
    }
}

/**
 * Provides search, selected-only filtering, and clear-all actions for the app list.
 */
@Composable
private fun FilterSearchCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showOnlySelected: Boolean,
    onShowOnlySelectedChange: (Boolean) -> Unit,
    sendAllApps: Boolean,
    selectedCount: Int,
    onClearFilters: () -> Unit
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
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                label = {
                    Text(stringResource(R.string.app_filter_search_label))
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_filter_show_selected_only_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = stringResource(R.string.app_filter_selected_count, selectedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = showOnlySelected,
                    enabled = !sendAllApps,
                    onCheckedChange = onShowOnlySelectedChange
                )
            }

            if (!sendAllApps && selectedCount > 0) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onClearFilters
                ) {
                    Text(stringResource(R.string.app_filter_clear_all_button))
                }
            }
        }
    }
}

/**
 * Displays a single installed app with its forwarding toggle.
 */
@Composable
private fun AppFilterRow(
    app: InstalledAppItem,
    sendAllApps: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val appIconState = rememberAppIcon(
        packageName = app.packageName,
        packageManager = context.packageManager
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val appIcon = appIconState.value

                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp)
                    )
                } else {
                    Text(
                        text = app.appName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = if (sendAllApps) true else app.isEnabled,
                enabled = !sendAllApps,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun rememberAppIcon(
    packageName: String,
    packageManager: PackageManager
): State<ImageBitmap?> {
    return produceState(initialValue = null, packageName) {
        value = runCatching {
            packageManager
                .getApplicationIcon(packageName)
                .toImageBitmap(40, 40)
        }.getOrNull()
    }
}

private fun Drawable.toImageBitmap(
    width: Int,
    height: Int
): ImageBitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)

    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)

    return bitmap.asImageBitmap()
}