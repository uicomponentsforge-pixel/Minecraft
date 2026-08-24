package com.example.data

import com.example.engine.VoxelWorld
import com.example.model.ItemStack
import com.example.model.RemotePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.*

data class MultiplayerRoom(
    val roomId: String,
    val hostName: String,
    val worldName: String,
    val playerCount: Int,
    val maxPlayers: Int = 8,
    val pingMs: Int = 18,
    val isLan: Boolean = true,
    val isBluetooth: Boolean = false
)

class MultiplayerManager(private val world: VoxelWorld) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _currentRoom = MutableStateFlow<MultiplayerRoom?>(null)
    val currentRoom = _currentRoom.asStateFlow()

    private val _availableRooms = MutableStateFlow<List<MultiplayerRoom>>(emptyList())
    val availableRooms = _availableRooms.asStateFlow()

    val myPlayerId = "player_${UUID.randomUUID().toString().substring(0, 6)}"
    var myPlayerName = "Steve"

    init {
        // Populate discoverable local LAN / Bluetooth co-op rooms
        _availableRooms.value = listOf(
            MultiplayerRoom("ROOM-8492", "Alex_Miner", "Survival Fortress", 2, 8, 12, isLan = true, isBluetooth = false),
            MultiplayerRoom("BT-COOP-41", "PixelKnight", "Co-op Castle Build", 1, 4, 25, isLan = false, isBluetooth = true),
            MultiplayerRoom("LAN-5520", "DiamondHunter", "Hardcore Cave Expedition", 3, 8, 15, isLan = true, isBluetooth = false)
        )
    }

    fun hostRoom(worldName: String, hostName: String, isBluetooth: Boolean = false): MultiplayerRoom {
        val room = MultiplayerRoom(
            roomId = "HOST-${(1000..9999).random()}",
            hostName = hostName,
            worldName = worldName,
            playerCount = 1,
            maxPlayers = 8,
            pingMs = 5,
            isLan = !isBluetooth,
            isBluetooth = isBluetooth
        )
        _currentRoom.value = room
        _isConnected.value = true
        myPlayerName = hostName

        // Add simulated co-op friend joining to build together
        spawnCoopFriend("Alex", 0xFFE57373.toInt())
        return room
    }

    fun joinRoom(room: MultiplayerRoom, playerName: String) {
        _currentRoom.value = room
        _isConnected.value = true
        myPlayerName = playerName

        // Add host player avatar
        val hostAvatar = RemotePlayer(
            playerId = "host_p1",
            playerName = room.hostName,
            x = world.playerX + 3.0f,
            y = world.playerY,
            z = world.playerZ + 2.0f,
            holdingItemId = "diamond_pickaxe",
            skinColor = 0xFF4CAF50.toInt()
        )
        world.remotePlayers[hostAvatar.playerId] = hostAvatar

        // Start peer simulation loop
        startPeerSimulation()
    }

    fun leaveRoom() {
        _isConnected.value = false
        _currentRoom.value = null
        world.remotePlayers.clear()
    }

    private fun spawnCoopFriend(name: String, color: Int) {
        val friend = RemotePlayer(
            playerId = "coop_${UUID.randomUUID().toString().substring(0, 4)}",
            playerName = name,
            x = world.playerX + 2.5f,
            y = world.playerY,
            z = world.playerZ + 1.5f,
            holdingItemId = "iron_sword",
            skinColor = color
        )
        world.remotePlayers[friend.playerId] = friend
        startPeerSimulation()
    }

    private fun startPeerSimulation() {
        scope.launch {
            var simTime = 0f
            while (_isConnected.value) {
                kotlinx.coroutines.delay(100)
                simTime += 0.1f

                for (rp in world.remotePlayers.values) {
                    // Orbit / walk around player constructively
                    val targetAngle = simTime * 0.4f
                    rp.x = world.playerX + sin(targetAngle) * 4.0f
                    rp.z = world.playerZ + cos(targetAngle) * 4.0f
                    rp.y = world.getHighestBlockY(rp.x.toInt(), rp.z.toInt()) + 1.0f
                    rp.yaw = (Math.toDegrees(atan2((-sin(targetAngle)).toDouble(), cos(targetAngle).toDouble()))).toFloat()

                    // Occasionally place a decorative torch or block
                    if ((simTime.toInt() % 20 == 0) && (simTime - simTime.toInt() < 0.15f)) {
                        val bx = (rp.x + 1f).toInt()
                        val bz = (rp.z + 1f).toInt()
                        val by = world.getHighestBlockY(bx, bz) + 1
                        if (world.getBlock(bx, by, bz) == 0) {
                            world.setBlock(bx, by, bz, 18) // Torch
                            world.soundEngine.playPlace()
                        }
                    }
                }
            }
        }
    }
}
