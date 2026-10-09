package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioProfile
import com.example.data.model.FocusEventType
import com.example.data.model.MonitoredApp
import com.example.service.ActivationSource
import com.example.ui.components.EqualizerAnimation
import com.example.ui.components.PermissionCard
import com.example.ui.components.SimulationBar
import com.example.ui.viewmodel.SoundFocusUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    uiState: SoundFocusUiState,
    onToggleMaster: (Boolean) -> Unit,
    onToggleApp: (MonitoredApp) -> Unit,
    onToggleManualProfile: (AudioProfile) -> Unit,
    onToggleSimulation: (MonitoredApp?) -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onRefreshPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playback = uiState.playbackFocusState
    val isMasterOn = uiState.settings.isMasterEnabled
    val isSilenceActive = playback.isSilenceActive && isMasterOn
    val enabledApps = uiState.monitoredApps.filter { it.isEnabled }
    val primaryApp = enabledApps.firstOrNull()

    val defaultMusicProfile = uiState.profiles.firstOrNull { it.isDefaultMusicProfile }
        ?: uiState.profiles.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header with App Title and Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "SoundFocus",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Profili Audio • Silenzioso Messaggi con Chiamate Attive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isMasterOn) "Attivo" else "Spento",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isMasterOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isMasterOn,
                        onCheckedChange = onToggleMaster,
                        modifier = Modifier.testTag("master_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Hero Status Banner
        item {
            HeroStatusCard(
                isMasterOn = isMasterOn,
                isSilenceActive = isSilenceActive,
                activeAppName = playback.activeAppName,
                activeProfileName = playback.activeProfileName ?: defaultMusicProfile?.name,
                activationSource = playback.activationSource,
                trackTitle = playback.trackTitle,
                isSimulation = playback.isSimulationActive
            )
        }

        // Quick Profile Actions Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Controllo Profilo Audio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToProfiles,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Tutti i Profili",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (defaultMusicProfile != null) {
                        val isProfileCurrentlyActive = isSilenceActive &&
                                (playback.activeProfileId == defaultMusicProfile.id || defaultMusicProfile.isManuallyActive)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = defaultMusicProfile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isProfileCurrentlyActive) {
                                        "In esecuzione • Notifiche ripristinate all'istante in pausa"
                                    } else {
                                        "Automatico all'avvio musica o attivabile subito a mano"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isProfileCurrentlyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = { onToggleManualProfile(defaultMusicProfile) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isProfileCurrentlyActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("manual_toggle_music_profile")
                            ) {
                                Icon(
                                    imageVector = if (isProfileCurrentlyActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isProfileCurrentlyActive) "Disattiva" else "Attiva Manuale",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Permissions banner if needed
        item {
            PermissionCard(
                isDndGranted = uiState.isDndPermissionGranted,
                isListenerGranted = uiState.isNotificationListenerGranted,
                onRefresh = onRefreshPermissions
            )
        }

        // Quick Status Indicators (Chiamate vs Messaggi)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Call status card
                StatusMetricCard(
                    title = "Chiamate Telefoniche",
                    statusText = "Segnale & Squillo ATTIVO",
                    subText = "La suoneria suona chiaramente",
                    icon = Icons.Default.Call,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    isActive = true,
                    modifier = Modifier.weight(1f)
                )

                // Message status card
                StatusMetricCard(
                    title = "Messaggi & Notifiche",
                    statusText = if (isSilenceActive) "Silenziati ORA" else "Pronto all'ascolto",
                    subText = if (isSilenceActive) "Ripristino istantaneo in pausa" else "Silenzioso in play",
                    icon = Icons.Default.NotificationsOff,
                    iconColor = if (isSilenceActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    isActive = isSilenceActive,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Simulation Bar
        item {
            SimulationBar(
                isSimulating = playback.isSimulationActive,
                primaryApp = primaryApp,
                onToggleSimulation = { onToggleSimulation(primaryApp) }
            )
        }

        // Monitored Music Apps Quick Strip
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "App Musicali Trigger (${enabledApps.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToApps,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Gestisci",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.monitoredApps.isEmpty()) {
                        Text(
                            text = "Nessuna app selezionata. Tocca Gestisci per aggiungere Spotify, YouTube Music, ecc.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.monitoredApps.take(6)) { app ->
                                FilterChip(
                                    selected = app.isEnabled,
                                    onClick = { onToggleApp(app) },
                                    label = { Text(app.appName) },
                                    leadingIcon = if (app.isEnabled) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Activity Snippet
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ultima Attività",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToLogs,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Vedi Tutti",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val latestLog = uiState.recentLogs.firstOrNull()
                    if (latestLog == null) {
                        Text(
                            text = "Nessun evento ancora registrato. Avvia una traccia musicale o attiva il profilo a mano per testare l'automazione!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(latestLog.timestamp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = latestLog.appName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = latestLog.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroStatusCard(
    isMasterOn: Boolean,
    isSilenceActive: Boolean,
    activeAppName: String?,
    activeProfileName: String?,
    activationSource: ActivationSource,
    trackTitle: String?,
    isSimulation: Boolean
) {
    val cardBrush = if (!isMasterOn) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
        )
    } else if (isSilenceActive) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF064E3B), // deep emerald/teal
                Color(0xFF0F172A)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E293B),
                Color(0xFF0F172A)
            )
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_status_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (!isMasterOn) Color.Gray
                                    else if (isSilenceActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.secondary
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (!isMasterOn) "SERVIZIO SOSPESO"
                            else if (isSilenceActive) "SILENZIOSO ATTIVO (${activeProfileName ?: "Profilo Musica"})"
                            else "IN ASCOLTO (In Attesa)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (!isMasterOn) Color.Gray
                            else if (isSilenceActive) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        )
                    }

                    EqualizerAnimation(
                        isPlaying = isSilenceActive,
                        barColor = if (isSilenceActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isMasterOn) {
                    Text(
                        text = "SoundFocus Disattivato",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Attiva l'interruttore in alto per abilitare l'automazione dei profili e il silenzioso messaggi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (isSilenceActive) {
                    val reasonText = when (activationSource) {
                        ActivationSource.AUTO_MUSIC_APP -> "Musica in riproduzione da ${activeAppName ?: "App"}"
                        ActivationSource.MANUAL_USER -> "${activeProfileName ?: "Profilo"} attivato manualmente"
                        ActivationSource.SIMULATION -> "Simulazione riproduzione attiva"
                        else -> "Profilo attivo"
                    }
                    Text(
                        text = reasonText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (!trackTitle.isNullOrBlank()) {
                        Text(
                            text = "Brano: $trackTitle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notifiche messaggi silenziate. Chiamate sempre attive con squillo. Volume musica inalterato (ripristino istantaneo alla pausa).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )
                } else {
                    Text(
                        text = "Pronto all'ascolto musicale",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Quando avvii Spotify, YouTube Music o un'altra app selezionata, il Profilo Musica silenzierà subito i messaggi senza toccare il volume musicale. Alla pausa, i volumi torneranno normali.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMetricCard(
    title: String,
    statusText: String,
    subText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
