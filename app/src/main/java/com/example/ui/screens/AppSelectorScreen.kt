package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MonitoredApp
import com.example.ui.viewmodel.SoundFocusUiState

@Composable
fun AppSelectorScreen(
    uiState: SoundFocusUiState,
    onToggleApp: (MonitoredApp) -> Unit,
    onDeleteApp: (MonitoredApp) -> Unit,
    onAddCustomApp: (String, String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val query = uiState.searchQuery.trim().lowercase()
    val filteredMonitored = if (query.isEmpty()) {
        uiState.monitoredApps
    } else {
        uiState.monitoredApps.filter {
            it.appName.lowercase().contains(query) || it.packageName.lowercase().contains(query)
        }
    }

    val monitoredPackageSet = uiState.monitoredApps.map { it.packageName }.toSet()
    val filteredInstalled = uiState.installedApps.filter { app ->
        !monitoredPackageSet.contains(app.packageName) &&
                (query.isEmpty() || app.appName.lowercase().contains(query) || app.packageName.lowercase().contains(query))
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "App Musicali da Monitorare",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Seleziona le app di musica che attiveranno la modalità silenziosa per i messaggi quando riproducono un brano.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_app_input"),
                    placeholder = { Text("Cerca app musicali o installate...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cerca"
                        )
                    },
                    trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Cancella testo"
                                )
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Section 1: Monitored Apps
            item {
                Text(
                    text = "App in Elenco (${filteredMonitored.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(filteredMonitored, key = { it.packageName }) { app ->
                AppItemCard(
                    app = app,
                    onToggle = { onToggleApp(app) },
                    onDelete = if (!app.isKnownMusicApp) {
                        { onDeleteApp(app) }
                    } else null
                )
            }

            // Section 2: Other Installed Apps
            if (filteredInstalled.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Altre Applicazioni Rilevate (${filteredInstalled.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                items(filteredInstalled.take(15), key = { it.packageName }) { app ->
                    AppItemCard(
                        app = app,
                        onToggle = { onToggleApp(app) },
                        onDelete = null
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB to add custom package
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_custom_app_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Aggiungi App Personalizzata")
        }
    }

    if (showAddDialog) {
        AddCustomAppDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { pkg, name ->
                onAddCustomApp(pkg, name)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AppItemCard(
    app: MonitoredApp,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_card_${app.packageName}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (app.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else Color.Gray.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = if (app.isEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (app.isInstalled) "Installata" else "Supportata",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (app.isInstalled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${app.packageName.takeLast(22)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Switch(
                    checked = app.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("app_switch_${app.packageName}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun AddCustomAppDialog(
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, appName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Aggiungi App Personalizzata",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Inserisci il nome e il package dell'app (es. com.myplayer):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome App") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pkg,
                    onValueChange = { pkg = it },
                    label = { Text("Package Name") },
                    placeholder = { Text("com.example.music") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && pkg.isNotBlank()) {
                        onConfirm(pkg.trim(), name.trim())
                    }
                },
                enabled = name.isNotBlank() && pkg.isNotBlank()
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}
