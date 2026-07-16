package com.alpware.notifybridge.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(repository: NotificationHistoryRepository, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val records = remember(query, refresh) { repository.list(query = query) }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Bildirim Geçmişi") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        null
                    )
                }
            },
            actions = {
                IconButton(onClick = { repository.clear(); refresh++ }) {
                    Icon(
                        Icons.Default.Delete,
                        "Tümünü temizle"
                    )
                }
            })
    }) { padding ->
        Column(Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp)) {
            OutlinedTextField(
                query,
                { query = it },
                label = { Text("Bildirimlerde ara") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                singleLine = true
            )
            if (records.isEmpty()) Box(
                Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    "Henüz kayıtlı bildirim yok.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(records, key = { it.historyId }) { item ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    item.appName ?: item.packageName,
                                    style = MaterialTheme.typography.titleSmall
                                ); Spacer(Modifier.weight(1f)); Text(
                                DateFormat.getDateTimeInstance(
                                    DateFormat.SHORT,
                                    DateFormat.SHORT
                                ).format(Date(item.postedAt)),
                                style = MaterialTheme.typography.labelSmall
                            )
                            }
                            item.title?.takeIf { it.isNotBlank() }
                                ?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            item.text?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${item.lifecycleState} • ${item.deliveryState}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { repository.delete(item.historyId); refresh++ },
                                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                            ) { Text("Sil") }
                        }
                    }
                }
            }
        }
    }
}
