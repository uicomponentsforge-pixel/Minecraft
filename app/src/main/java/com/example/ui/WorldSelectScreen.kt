package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorldEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorldSelectScreen(
    worlds: List<WorldEntity>,
    onSelectWorld: (WorldEntity) -> Unit,
    onCreateWorld: (String, Long, String, String) -> Unit,
    onDeleteWorld: (String) -> Unit,
    onExportWorld: (WorldEntity) -> Unit,
    onImportWorld: (String) -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenMods: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF10141E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main VoxelCraft Title Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = "⛏️ VOXELCRAFT",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFFD54F)
                )
                Text(
                    text = "Voxel Engine • Survival • Co-op • Modding",
                    fontSize = 12.sp,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Action Buttons (Create World, Multiplayer, Mods, Import Sync)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("create_world_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New World", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenMultiplayer,
                    modifier = Modifier.weight(1f).height(48.dp).testTag("multiplayer_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Multiplayer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenMods,
                    modifier = Modifier.weight(1f).height(42.dp).testTag("mods_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Extension, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mods API", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f).height(42.dp).testTag("import_sync_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cross-Save Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🌍 Saved Worlds (${worlds.size})",
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (worlds.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No worlds yet! Tap 'New World' to generate infinite terrain.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(worlds) { world ->
                        WorldCardItem(
                            world = world,
                            onPlay = { onSelectWorld(world) },
                            onExport = { onExportWorld(world) },
                            onDelete = { onDeleteWorld(world.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateWorldDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, seed, gm, diff ->
                onCreateWorld(name, seed, gm, diff)
                showCreateDialog = false
            }
        )
    }

    if (showImportDialog) {
        ImportSyncDialog(
            onDismiss = { showImportDialog = false },
            onImport = { json ->
                onImportWorld(json)
                showImportDialog = false
            }
        )
    }
}

@Composable
fun WorldCardItem(
    world: WorldEntity,
    onPlay: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E272C),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("world_item_${world.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // World Thumbnail Block
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (world.gameMode == "CREATIVE") Color(0xFF1976D2) else Color(0xFF388E3C),
                                    Color(0xFF1B5E20)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (world.gameMode == "CREATIVE") "✨" else "⛏️", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = world.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${world.gameMode} • ${world.difficulty} • Lv.${world.level}",
                        color = Color(0xFFFFD54F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Last played: ${dateFormat.format(Date(world.lastPlayedTime))}",
                        color = Color(0xFF90A4AE),
                        fontSize = 10.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Export JSON / Sync
                IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Sync", tint = Color(0xFF64B5F6), modifier = Modifier.size(18.dp))
                }

                // Delete
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CreateWorldDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Long, String, String) -> Unit
) {
    var worldName by remember { mutableStateOf("My World") }
    var seedText by remember { mutableStateOf("${System.currentTimeMillis() % 1000000}") }
    var gameMode by remember { mutableStateOf("SURVIVAL") }
    var difficulty by remember { mutableStateOf("NORMAL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🌍 Create New Voxel World", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = worldName,
                    onValueChange = { worldName = it },
                    label = { Text("World Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = seedText,
                    onValueChange = { seedText = it },
                    label = { Text("World Seed (Number)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Game Mode:", color = Color.White, fontSize = 12.sp)
                    Button(
                        onClick = { gameMode = if (gameMode == "SURVIVAL") "CREATIVE" else "SURVIVAL" },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                    ) {
                        Text(gameMode, color = Color(0xFFFFD54F), fontSize = 11.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Difficulty:", color = Color.White, fontSize = 12.sp)
                    Button(
                        onClick = {
                            difficulty = when (difficulty) {
                                "PEACEFUL" -> "EASY"
                                "EASY" -> "NORMAL"
                                "NORMAL" -> "HARD"
                                else -> "PEACEFUL"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                    ) {
                        Text(difficulty, color = Color(0xFF81C784), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val seed = seedText.toLongOrNull() ?: System.currentTimeMillis()
                    onCreate(worldName.ifBlank { "Voxel World" }, seed, gameMode, difficulty)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Generate World", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF263238)
    )
}

@Composable
fun ImportSyncDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var syncJson by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔄 Cross-Platform Save Sync", color = Color(0xFF90CAF9), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste your exported JSON save code below to synchronize your world across devices seamlessly:",
                    color = Color(0xFFB0BEC5),
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = syncJson,
                    onValueChange = { syncJson = it },
                    label = { Text("Paste JSON Save Code") },
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (syncJson.isNotBlank()) onImport(syncJson)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
            ) {
                Text("Import Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF263238)
    )
}
