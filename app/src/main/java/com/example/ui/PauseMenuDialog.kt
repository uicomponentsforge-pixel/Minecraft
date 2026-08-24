package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.example.engine.*

@Composable
fun PauseMenuDialog(
    world: VoxelWorld,
    onResume: () -> Unit,
    onSaveAndQuit: () -> Unit
) {
    var gameMode by remember { mutableStateOf(world.gameMode) }
    var difficulty by remember { mutableStateOf(world.difficulty) }
    var isMuted by remember { mutableStateOf(world.soundEngine.isMuted) }

    Dialog(onDismissRequest = onResume) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF263238),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF455A64)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "⏸️ Game Paused",
                    color = Color(0xFFFFD54F),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Divider(color = Color(0xFF455A64), thickness = 1.dp)

                // Resume Button
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth().testTag("resume_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back to Game", fontWeight = FontWeight.Bold)
                }

                // Game Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Game Mode:", color = Color.White, fontSize = 13.sp)
                    Button(
                        onClick = {
                            gameMode = if (gameMode == GameMode.SURVIVAL) GameMode.CREATIVE else GameMode.SURVIVAL
                            world.gameMode = gameMode
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("gamemode_toggle")
                    ) {
                        Text(gameMode.name, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Difficulty Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Difficulty:", color = Color.White, fontSize = 13.sp)
                    Button(
                        onClick = {
                            difficulty = when (difficulty) {
                                Difficulty.PEACEFUL -> Difficulty.EASY
                                Difficulty.EASY -> Difficulty.NORMAL
                                Difficulty.NORMAL -> Difficulty.HARD
                                Difficulty.HARD -> Difficulty.PEACEFUL
                            }
                            world.difficulty = difficulty
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("difficulty_toggle")
                    ) {
                        Text(difficulty.name, color = Color(0xFF81C784), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Time of Day Setter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time of Day:", color = Color.White, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SmallTimeButton("☀️ Noon") { world.timeOfDay = 6000f }
                        SmallTimeButton("🌅 Dusk") { world.timeOfDay = 11000f }
                        SmallTimeButton("🌙 Night") { world.timeOfDay = 16000f }
                    }
                }

                // Sound Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Audio Effects:", color = Color.White, fontSize = 13.sp)
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            world.soundEngine.isMuted = isMuted
                        }
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Mute Toggle",
                            tint = if (isMuted) Color.Red else Color.Green
                        )
                    }
                }

                Divider(color = Color(0xFF455A64), thickness = 1.dp)

                // Save & Quit
                Button(
                    onClick = onSaveAndQuit,
                    modifier = Modifier.fillMaxWidth().testTag("save_quit_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Quit to Title", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SmallTimeButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF1E272C),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF455A64))
    ) {
        Text(text = text, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
    }
}
