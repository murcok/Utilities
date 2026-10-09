package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AudioProfile
import com.example.data.model.CallPriorityMode
import com.example.data.model.CallSignalMode
import com.example.service.ActivationSource
import com.example.ui.viewmodel.SoundFocusUiState

@Composable
fun ProfilesScreen(
    uiState: SoundFocusUiState,
    onToggleManualProfile: (AudioProfile) -> Unit,
    onSaveProfile: (AudioProfile) -> Unit,
    onDeleteProfile: (AudioProfile) -> Unit,
    onCreateProfile: (name: String, desc: String, icon: String, silence: Boolean, signal: CallSignalMode, priority: CallPriorityMode, autoTrigger: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<AudioProfile?>(null) }

    val activeProfileId = uiState.playbackFocusState.activeProfileId
    val activationSource = uiState.playbackFocusState.activationSource

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Profili Audio Personalizzabili",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Crea e gestisci i tuoi profili. Il 'Profilo Musica' si attiva automaticamente quando riproduci una traccia e si disattiva appena premi pausa, ripristinando all'istante le notifiche.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(uiState.profiles, key = { it.id }) { profile ->
                val isThisProfileActive = (profile.id == activeProfileId && uiState.playbackFocusState.isSilenceActive) ||
                        profile.isManuallyActive || profile.isAutoActive

                ProfileCard(
                    profile = profile,
                    isActive = isThisProfileActive,
                    activationSource = if (isThisProfileActive) activationSource else ActivationSource.NONE,
                    onToggleActive = { onToggleManualProfile(profile) },
                    onEdit = { editingProfile = profile },
                    onDelete = if (!profile.isDefaultMusicProfile) {
                        { onDeleteProfile(profile) }
                    } else null
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB to create a new profile
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("create_profile_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Nuovo Profilo Audio")
        }
    }

    if (showCreateDialog) {
        ProfileEditDialog(
            profile = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc, icon, silence, signal, priority, autoTrigger ->
                onCreateProfile(name, desc, icon, silence, signal, priority, autoTrigger)
                showCreateDialog = false
            }
        )
    }

    if (editingProfile != null) {
        ProfileEditDialog(
            profile = editingProfile,
            onDismiss = { editingProfile = null },
            onConfirm = { name, desc, icon, silence, signal, priority, autoTrigger ->
                editingProfile?.let { original ->
                    onSaveProfile(
                        original.copy(
                            name = name,
                            description = desc,
                            iconName = icon,
                            silenceMessages = silence,
                            callSignalMode = signal,
                            callPriorityMode = priority,
                            isAutoTriggerOnMusic = autoTrigger
                        )
                    )
                }
                editingProfile = null
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: AudioProfile,
    isActive: Boolean,
    activationSource: ActivationSource,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("profile_card_${profile.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getProfileIcon(profile.iconName),
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (profile.isDefaultMusicProfile) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Musica",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isActive) {
                                when (activationSource) {
                                    ActivationSource.AUTO_MUSIC_APP -> "Attivo: Riproduzione in corso"
                                    ActivationSource.MANUAL_USER -> "Attivo: Attivazione Manuale"
                                    ActivationSource.SIMULATION -> "Attivo: Modalità Simulazione"
                                    else -> "Attivo"
                                }
                            } else {
                                if (profile.isAutoTriggerOnMusic) "Automatico all'avvio della musica" else "Attivazione manuale"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Action button: Toggle manual state
                Button(
                    onClick = onToggleActive,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_profile_${profile.id}")
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isActive) "Disattiva" else "Attiva",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Profile specification chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Messages indicator
                ChipTag(
                    icon = Icons.Default.NotificationsOff,
                    text = "Messaggi Silenziati",
                    color = MaterialTheme.colorScheme.secondary
                )

                // Calls indicator
                ChipTag(
                    icon = Icons.Default.Call,
                    text = "${profile.callSignalMode.labelIt} (${profile.callPriorityMode.labelIt})",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer controls: Edit and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifica",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Personalizza", style = MaterialTheme.typography.labelSmall)
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Elimina", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipTag(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(
                color.copy(alpha = 0.12f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProfileEditDialog(
    profile: AudioProfile?,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        desc: String,
        icon: String,
        silenceMessages: Boolean,
        callSignalMode: CallSignalMode,
        callPriorityMode: CallPriorityMode,
        autoTriggerOnMusic: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var desc by remember { mutableStateOf(profile?.description ?: "") }
    var icon by remember { mutableStateOf(profile?.iconName ?: "headphones") }
    var silenceMessages by remember { mutableStateOf(profile?.silenceMessages ?: true) }
    var callSignalMode by remember { mutableStateOf(profile?.callSignalMode ?: CallSignalMode.RING_AND_VIBRATE) }
    var callPriorityMode by remember { mutableStateOf(profile?.callPriorityMode ?: CallPriorityMode.ANY_CALLER) }
    var autoTriggerOnMusic by remember { mutableStateOf(profile?.isAutoTriggerOnMusic ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (profile == null) "Nuovo Profilo Audio" else "Personalizza ${profile.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Profilo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Descrizione") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "Icona",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            "headphones" to Icons.Default.Headphones,
                            "work" to Icons.Default.Work,
                            "car" to Icons.Default.DirectionsCar
                        ).forEach { (key, ic) ->
                            val selected = icon == key
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { icon = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ic,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Silenzia Notifiche Messaggi", fontWeight = FontWeight.SemiBold)
                            Text("Disattiva audio chat (WhatsApp, Telegram, SMS)", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = silenceMessages,
                            onCheckedChange = { silenceMessages = it }
                        )
                    }
                }

                item {
                    Text(
                        text = "Segnale Chiamate Telefoniche",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    CallSignalMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { callSignalMode = mode },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = callSignalMode == mode,
                                onClick = { callSignalMode = mode }
                            )
                            Text(mode.labelIt, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    Text(
                        text = "Filtro Chiamanti",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    CallPriorityMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { callPriorityMode = mode },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = callPriorityMode == mode,
                                onClick = { callPriorityMode = mode }
                            )
                            Text(mode.labelIt, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Regola Automatica con Musica", fontWeight = FontWeight.SemiBold)
                            Text("Attiva automaticamente all'avvio della riproduzione", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = autoTriggerOnMusic,
                            onCheckedChange = { autoTriggerOnMusic = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name.trim(),
                            desc.trim(),
                            icon,
                            silenceMessages,
                            callSignalMode,
                            callPriorityMode,
                            autoTriggerOnMusic
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun getProfileIcon(name: String): ImageVector {
    return when (name) {
        "work" -> Icons.Default.Work
        "car" -> Icons.Default.DirectionsCar
        else -> Icons.Default.Headphones
    }
}
