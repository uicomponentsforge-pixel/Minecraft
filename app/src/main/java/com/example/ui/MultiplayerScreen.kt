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
import com.example.data.MultiplayerManager
import com.example.data.MultiplayerRoom

@Composable
fun MultiplayerScreen(
    multiplayerManager: MultiplayerManager,
    onBack: () -> Unit,
    onStartGame: () -> Unit
) {
    val isConnected by multiplayerManager.isConnected.collectAsState()
    val currentRoom by multiplayerManager.currentRoom.collectAsState()
    val availableRooms by multiplayerManager.availableRooms.collectAsState()

    var playerName by remember { mutableStateOf(multiplayerManager.myPlayerName) }
    var directCode by remember { mutableStateOf("") }
    var isHostDialogShowing by remember { mutableStateOf(false) }

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
                IconButton(onClick = onBack, modifier = Modifier.testTag("mp_back_btn")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "🌐 Multiplayer Co-op Lobby",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Player Profile Name Input
            OutlinedTextField(
                value = playerName,
                onValueChange = {
                    playerName = it
                    multiplayerManager.myPlayerName = it
                },
                label = { Text("Your Player Tag") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFFD54F),
                    unfocusedBorderColor = Color(0xFF455A64),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Host Room / Join by Direct Code)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        multiplayerManager.hostRoom("Co-op Realm", playerName)
                        onStartGame()
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("host_room_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Host LAN World", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        multiplayerManager.hostRoom("Bluetooth Realm", playerName, isBluetooth = true)
                        onStartGame()
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("host_bt_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bluetooth Co-op", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "📡 Nearby Local Discoverable Servers",
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableRooms) { room ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E272C),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (room.isBluetooth) Icons.Default.Bluetooth else Icons.Default.Wifi,
                                        contentDescription = null,
                                        tint = Color(0xFF81C784),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = room.worldName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Host: ${room.hostName} • ${room.playerCount}/${room.maxPlayers} Players • ${room.pingMs}ms",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = {
                                    multiplayerManager.joinRoom(room, playerName)
                                    onStartGame()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
