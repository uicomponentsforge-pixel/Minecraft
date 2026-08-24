package com.example.data

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

data class CustomBlockConfig(
    val id: Int,
    val name: String,
    val displayName: String,
    val colorHex: Int,
    val hardness: Float = 1.0f,
    val isTransparent: Boolean = false,
    val lightEmission: Int = 0,
    val toolType: ToolType = ToolType.PICKAXE
)

data class CustomMobConfig(
    val id: String,
    val name: String,
    val health: Float = 30f,
    val damage: Float = 5f,
    val speed: Float = 2.4f,
    val colorHex: Int = 0xFFFF0055.toInt(),
    val isHostile: Boolean = true
)

object ModManager {
    val installedMods = mutableListOf<ModEntity>()

    fun initDefaultMods(): List<ModEntity> {
        val mods = mutableListOf<ModEntity>()

        // 1. Netherite & Emerald Arsenal Mod
        mods.add(
            ModEntity(
                id = "mod_emerald_arsenal",
                name = "Emerald & Netherite Arsenal",
                version = "v1.4.0",
                author = "VoxelForge Studio",
                description = "Adds craftable Emerald Blades, Netherite Armor sets, and reinforced obsidian blocks with enhanced blast resistance!",
                isEnabled = true
            )
        )

        // 2. Magma Fiend & Nether Hostiles
        mods.add(
            ModEntity(
                id = "mod_magma_fiends",
                name = "Infernal Biomes & Magma Mobs",
                version = "v2.0.1",
                author = "EnderCraft Devs",
                description = "Spawns blazing Magma Fiends in deep caverns and introduces Magma Bricks that glow in the dark.",
                isEnabled = true
            )
        )

        // 3. Cyber Voxel Expansion
        mods.add(
            ModEntity(
                id = "mod_cyber_voxels",
                name = "Cyber Voxel Sci-Fi Pack",
                version = "v1.0.2",
                author = "NeoBlocks",
                description = "Adds Neon Blue Laser Crystals, Anti-Gravity Boots, and high-tech alloy plating blocks.",
                isEnabled = false
            )
        )

        return mods
    }

    fun applyMod(mod: ModEntity) {
        if (!mod.isEnabled) return

        // Register custom block if any
        if (mod.id == "mod_emerald_arsenal") {
            val emeraldSword = ItemDefinition(
                id = "emerald_sword",
                name = "Emerald Blade",
                maxStack = 1,
                category = ItemCategory.WEAPONS,
                attackDamage = 9.0f,
                durability = 2000,
                toolType = ToolType.SWORD,
                iconColor = 0xFF1FC85C.toInt(),
                iconSymbol = "🗡️",
                description = "Ultra sharp reinforced emerald blade"
            )
            ItemRegistry.registerCustomItem(emeraldSword)

            RecipeRegistry.addCustomRecipe(
                CraftingRecipe(
                    id = "mod_emerald_sword",
                    result = ItemStack(emeraldSword, 1),
                    width = 1,
                    height = 3,
                    ingredients = listOf("emerald", "emerald", "stick"),
                    description = "Emerald Blade"
                )
            )
        }
    }

    fun registerCustomUserBlock(config: CustomBlockConfig) {
        val block = BlockProperties(
            id = config.id,
            name = config.name.uppercase().replace(" ", "_"),
            displayName = config.displayName,
            isSolid = true,
            isTransparent = config.isTransparent,
            lightEmission = config.lightEmission,
            hardness = config.hardness,
            preferredTool = config.toolType,
            topColor = config.colorHex,
            sideColor = config.colorHex,
            bottomColor = config.colorHex,
            dropItemId = config.name.lowercase().replace(" ", "_"),
            category = BlockCategory.MODDED
        )
        BlockRegistry.registerCustomBlock(block)

        val item = ItemDefinition(
            id = config.name.lowercase().replace(" ", "_"),
            name = config.displayName,
            category = ItemCategory.BLOCKS,
            blockId = config.id,
            iconColor = config.colorHex,
            iconSymbol = "🟦"
        )
        ItemRegistry.registerCustomItem(item)
    }
}
