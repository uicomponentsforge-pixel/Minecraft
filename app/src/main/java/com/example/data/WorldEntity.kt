package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "worlds")
data class WorldEntity(
    @PrimaryKey val id: String,
    val name: String,
    val seed: Long,
    val gameMode: String = "SURVIVAL",
    val difficulty: String = "NORMAL",
    val dayTime: Float = 6000f,
    val playerX: Float = 8.5f,
    val playerY: Float = 36f,
    val playerZ: Float = 8.5f,
    val playerYaw: Float = 0f,
    val playerPitch: Float = 0f,
    val health: Float = 20f,
    val hunger: Float = 20f,
    val level: Int = 1,
    val xp: Int = 0,
    val hotbarJson: String = "",
    val inventoryJson: String = "",
    val armorJson: String = "",
    val modifiedBlocksJson: String = "",
    val lastPlayedTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "mods")
data class ModEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val isEnabled: Boolean = true,
    val customBlocksJson: String = "",
    val customMobsJson: String = "",
    val customRecipesJson: String = ""
)
