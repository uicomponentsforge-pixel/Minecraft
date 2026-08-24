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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomBlockConfig
import com.example.data.ModEntity
import com.example.data.ModManager
import com.example.model.ToolType

@Composable
fun ModManagerScreen(
    mods: List<ModEntity>,
    onToggleMod: (ModEntity) -> Unit,
    onAddCustomBlock: (CustomBlockConfig) -> Unit,
    onBack: () -> Unit
) {
    var showCreateBlockDialog by remember { mutableStateOf(false) }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("mods_back_btn")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "🧩 Community Modding Engine",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Create Custom Block / Mod Button
            Button(
                onClick = { showCreateBlockDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("create_custom_block_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Build, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom Modded Block", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "📦 Installed Mod Packs & Extensions",
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mods) { mod ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E272C),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (mod.isEnabled) Color(0xFF8E24AA) else Color(0xFF37474F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mod.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mod.version,
                                        color = Color(0xFF81C784),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Author: ${mod.author}",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mod.description,
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = mod.isEnabled,
                                onCheckedChange = { onToggleMod(mod.copy(isEnabled = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFE1BEE7),
                                    checkedTrackColor = Color(0xFF8E24AA)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateBlockDialog) {
        CreateCustomBlockDialog(
            onDismiss = { showCreateBlockDialog = false },
            onCreate = { cfg ->
                onAddCustomBlock(cfg)
                showCreateBlockDialog = false
            }
        )
    }
}

@Composable
fun CreateCustomBlockDialog(
    onDismiss: () -> Unit,
    onCreate: (CustomBlockConfig) -> Unit
) {
    var blockName by remember { mutableStateOf("") }
    var hardness by remember { mutableFloatStateOf(2.0f) }
    var lightEmission by remember { mutableIntStateOf(0) }
    var selectedColorHex by remember { mutableIntStateOf(0xFF9C27B0.toInt()) }

    val colorOptions = listOf(
        0xFF9C27B0.toInt(), // Purple
        0xFFE91E63.toInt(), // Pink
        0xFF00E676.toInt(), // Neon Green
        0xFF00E5FF.toInt(), // Cyan
        0xFFFF9100.toInt(), // Orange
        0xFFFFD600.toInt()  // Gold
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🛠️ Custom Mod Block Maker", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = blockName,
                    onValueChange = { blockName = it },
                    label = { Text("Block Name (e.g. Plasma Ore)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Text("Pick Block Color:", color = Color.White, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in colorOptions) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(c))
                                .border(
                                    width = if (selectedColorHex == c) 2.5.dp else 1.dp,
                                    color = if (selectedColorHex == c) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedColorHex = c }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Light Emitting:", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = lightEmission > 0,
                        onCheckedChange = { lightEmission = if (it) 15 else 0 }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (blockName.isNotBlank()) {
                        val newId = 100 + (1..899).random()
                        onCreate(
                            CustomBlockConfig(
                                id = newId,
                                name = blockName,
                                displayName = blockName,
                                colorHex = selectedColorHex,
                                hardness = hardness,
                                lightEmission = lightEmission,
                                toolType = ToolType.PICKAXE
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
            ) {
                Text("Register Block", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF263238)
    )
}
