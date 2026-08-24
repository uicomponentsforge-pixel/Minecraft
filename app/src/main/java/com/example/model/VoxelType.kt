package com.example.model

enum class BlockCategory {
    NATURAL,
    BUILDING,
    ORES,
    UTILITY,
    DECORATION,
    MODDED
}

enum class ToolType {
    NONE,
    PICKAXE,
    AXE,
    SHOVEL,
    SWORD
}

data class BlockProperties(
    val id: Int,
    val name: String,
    val displayName: String,
    val isSolid: Boolean = true,
    val isTransparent: Boolean = false,
    val isLiquid: Boolean = false,
    val lightEmission: Int = 0,
    val hardness: Float = 1.0f,
    val preferredTool: ToolType = ToolType.NONE,
    val topColor: Int,
    val sideColor: Int,
    val bottomColor: Int = sideColor,
    val dropItemId: String = name.lowercase(),
    val dropCount: Int = 1,
    val category: BlockCategory = BlockCategory.BUILDING,
    val texturePattern: Int = 0 // 0=solid/rough, 1=grass_top, 2=wood_rings, 3=leaves, 4=ore, 5=brick, 6=crafting_table, 7=furnace, 8=chest, 9=torch, 10=tnt
)

object BlockRegistry {
    val AIR = BlockProperties(
        id = 0,
        name = "AIR",
        displayName = "Air",
        isSolid = false,
        isTransparent = true,
        topColor = 0x00000000,
        sideColor = 0x00000000,
        dropItemId = "air",
        category = BlockCategory.NATURAL
    )

    val GRASS = BlockProperties(
        id = 1,
        name = "GRASS",
        displayName = "Grass Block",
        isSolid = true,
        isTransparent = false,
        hardness = 0.6f,
        preferredTool = ToolType.SHOVEL,
        topColor = 0xFF5B8C3A.toInt(),      // Lush green
        sideColor = 0xFF7A5C37.toInt(),     // Dirt with green top fringe
        bottomColor = 0xFF5C3E1E.toInt(),   // Dirt brown
        dropItemId = "dirt",
        category = BlockCategory.NATURAL,
        texturePattern = 1
    )

    val DIRT = BlockProperties(
        id = 2,
        name = "DIRT",
        displayName = "Dirt",
        isSolid = true,
        hardness = 0.5f,
        preferredTool = ToolType.SHOVEL,
        topColor = 0xFF5C3E1E.toInt(),
        sideColor = 0xFF5C3E1E.toInt(),
        dropItemId = "dirt",
        category = BlockCategory.NATURAL,
        texturePattern = 0
    )

    val STONE = BlockProperties(
        id = 3,
        name = "STONE",
        displayName = "Stone",
        isSolid = true,
        hardness = 1.5f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFF787878.toInt(),
        sideColor = 0xFF787878.toInt(),
        dropItemId = "cobblestone",
        category = BlockCategory.NATURAL,
        texturePattern = 0
    )

    val COBBLESTONE = BlockProperties(
        id = 4,
        name = "COBBLESTONE",
        displayName = "Cobblestone",
        isSolid = true,
        hardness = 2.0f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFF606060.toInt(),
        sideColor = 0xFF606060.toInt(),
        dropItemId = "cobblestone",
        category = BlockCategory.BUILDING,
        texturePattern = 5
    )

    val OAK_LOG = BlockProperties(
        id = 5,
        name = "OAK_LOG",
        displayName = "Oak Wood",
        isSolid = true,
        hardness = 1.2f,
        preferredTool = ToolType.AXE,
        topColor = 0xFF967347.toInt(),      // Tree ring center
        sideColor = 0xFF4A3728.toInt(),     // Dark bark
        bottomColor = 0xFF967347.toInt(),
        dropItemId = "oak_log",
        category = BlockCategory.NATURAL,
        texturePattern = 2
    )

    val OAK_LEAVES = BlockProperties(
        id = 6,
        name = "OAK_LEAVES",
        displayName = "Oak Leaves",
        isSolid = true,
        isTransparent = true,
        hardness = 0.2f,
        topColor = 0xFF356E26.toInt(),
        sideColor = 0xFF356E26.toInt(),
        dropItemId = "apple",
        category = BlockCategory.NATURAL,
        texturePattern = 3
    )

    val OAK_PLANKS = BlockProperties(
        id = 7,
        name = "OAK_PLANKS",
        displayName = "Oak Planks",
        isSolid = true,
        hardness = 1.0f,
        preferredTool = ToolType.AXE,
        topColor = 0xFFB88748.toInt(),
        sideColor = 0xFFB88748.toInt(),
        dropItemId = "oak_planks",
        category = BlockCategory.BUILDING,
        texturePattern = 5
    )

    val SAND = BlockProperties(
        id = 8,
        name = "SAND",
        displayName = "Sand",
        isSolid = true,
        hardness = 0.5f,
        preferredTool = ToolType.SHOVEL,
        topColor = 0xFFD8CC8C.toInt(),
        sideColor = 0xFFD8CC8C.toInt(),
        dropItemId = "sand",
        category = BlockCategory.NATURAL,
        texturePattern = 0
    )

    val WATER = BlockProperties(
        id = 9,
        name = "WATER",
        displayName = "Water",
        isSolid = false,
        isLiquid = true,
        isTransparent = true,
        hardness = 100.0f,
        topColor = 0xAA2B5BB8.toInt(),
        sideColor = 0xAA2B5BB8.toInt(),
        dropItemId = "water",
        category = BlockCategory.NATURAL,
        texturePattern = 0
    )

    val GLASS = BlockProperties(
        id = 10,
        name = "GLASS",
        displayName = "Glass",
        isSolid = true,
        isTransparent = true,
        hardness = 0.3f,
        topColor = 0x88D4F1F9.toInt(),
        sideColor = 0x88D4F1F9.toInt(),
        dropItemId = "",
        category = BlockCategory.BUILDING,
        texturePattern = 0
    )

    val COAL_ORE = BlockProperties(
        id = 11,
        name = "COAL_ORE",
        displayName = "Coal Ore",
        isSolid = true,
        hardness = 2.0f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFF303030.toInt(),
        sideColor = 0xFF707070.toInt(),
        dropItemId = "coal",
        category = BlockCategory.ORES,
        texturePattern = 4
    )

    val IRON_ORE = BlockProperties(
        id = 12,
        name = "IRON_ORE",
        displayName = "Iron Ore",
        isSolid = true,
        hardness = 2.5f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFFD4B192.toInt(),
        sideColor = 0xFF707070.toInt(),
        dropItemId = "iron_ore",
        category = BlockCategory.ORES,
        texturePattern = 4
    )

    val GOLD_ORE = BlockProperties(
        id = 13,
        name = "GOLD_ORE",
        displayName = "Gold Ore",
        isSolid = true,
        hardness = 3.0f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFFFCD835.toInt(),
        sideColor = 0xFF707070.toInt(),
        dropItemId = "gold_ore",
        category = BlockCategory.ORES,
        texturePattern = 4
    )

    val DIAMOND_ORE = BlockProperties(
        id = 14,
        name = "DIAMOND_ORE",
        displayName = "Diamond Ore",
        isSolid = true,
        hardness = 3.5f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFF5DECF2.toInt(),
        sideColor = 0xFF707070.toInt(),
        dropItemId = "diamond",
        category = BlockCategory.ORES,
        texturePattern = 4
    )

    val CRAFTING_TABLE = BlockProperties(
        id = 15,
        name = "CRAFTING_TABLE",
        displayName = "Crafting Table",
        isSolid = true,
        hardness = 1.2f,
        preferredTool = ToolType.AXE,
        topColor = 0xFFB3733B.toInt(),
        sideColor = 0xFF8A5528.toInt(),
        dropItemId = "crafting_table",
        category = BlockCategory.UTILITY,
        texturePattern = 6
    )

    val FURNACE = BlockProperties(
        id = 16,
        name = "FURNACE",
        displayName = "Furnace",
        isSolid = true,
        hardness = 2.5f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFF656565.toInt(),
        sideColor = 0xFF4F4F4F.toInt(),
        dropItemId = "furnace",
        category = BlockCategory.UTILITY,
        texturePattern = 7
    )

    val CHEST = BlockProperties(
        id = 17,
        name = "CHEST",
        displayName = "Chest",
        isSolid = true,
        hardness = 1.5f,
        preferredTool = ToolType.AXE,
        topColor = 0xFF9E682C.toInt(),
        sideColor = 0xFF8C5C26.toInt(),
        dropItemId = "chest",
        category = BlockCategory.UTILITY,
        texturePattern = 8
    )

    val TORCH = BlockProperties(
        id = 18,
        name = "TORCH",
        displayName = "Torch",
        isSolid = false,
        isTransparent = true,
        lightEmission = 14,
        hardness = 0.0f,
        topColor = 0xFFFFD700.toInt(),
        sideColor = 0xFF754B24.toInt(),
        dropItemId = "torch",
        category = BlockCategory.DECORATION,
        texturePattern = 9
    )

    val TNT = BlockProperties(
        id = 19,
        name = "TNT",
        displayName = "TNT",
        isSolid = true,
        hardness = 0.0f,
        topColor = 0xFFC63423.toInt(),
        sideColor = 0xFFD83B29.toInt(),
        dropItemId = "tnt",
        category = BlockCategory.UTILITY,
        texturePattern = 10
    )

    val BRICK = BlockProperties(
        id = 20,
        name = "BRICK",
        displayName = "Bricks",
        isSolid = true,
        hardness = 2.0f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFF9A4232.toInt(),
        sideColor = 0xFF9A4232.toInt(),
        dropItemId = "brick",
        category = BlockCategory.BUILDING,
        texturePattern = 5
    )

    val OBSIDIAN = BlockProperties(
        id = 21,
        name = "OBSIDIAN",
        displayName = "Obsidian",
        isSolid = true,
        hardness = 10.0f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFF19102A.toInt(),
        sideColor = 0xFF19102A.toInt(),
        dropItemId = "obsidian",
        category = BlockCategory.BUILDING,
        texturePattern = 0
    )

    val BEDROCK = BlockProperties(
        id = 22,
        name = "BEDROCK",
        displayName = "Bedrock",
        isSolid = true,
        hardness = 1000.0f,
        topColor = 0xFF212121.toInt(),
        sideColor = 0xFF212121.toInt(),
        dropItemId = "",
        category = BlockCategory.NATURAL,
        texturePattern = 0
    )

    val GLOWSTONE = BlockProperties(
        id = 23,
        name = "GLOWSTONE",
        displayName = "Glowstone",
        isSolid = true,
        lightEmission = 15,
        hardness = 0.5f,
        topColor = 0xFFFBE48B.toInt(),
        sideColor = 0xFFFBE48B.toInt(),
        dropItemId = "glowstone",
        category = BlockCategory.DECORATION,
        texturePattern = 0
    )

    val EMERALD_BLOCK = BlockProperties(
        id = 24,
        name = "EMERALD_BLOCK",
        displayName = "Emerald Block",
        isSolid = true,
        hardness = 3.0f,
        preferredTool = ToolType.PICKAXE,
        topColor = 0xFF1FC85C.toInt(),
        sideColor = 0xFF1FC85C.toInt(),
        dropItemId = "emerald_block",
        category = BlockCategory.BUILDING,
        texturePattern = 5
    )

    val RED_FLOWER = BlockProperties(
        id = 25,
        name = "RED_FLOWER",
        displayName = "Poppy Flower",
        isSolid = false,
        isTransparent = true,
        hardness = 0.0f,
        topColor = 0xFFE53935.toInt(),
        sideColor = 0xFFE53935.toInt(),
        dropItemId = "red_flower",
        category = BlockCategory.DECORATION,
        texturePattern = 3
    )

    val YELLOW_FLOWER = BlockProperties(
        id = 26,
        name = "YELLOW_FLOWER",
        displayName = "Dandelion",
        isSolid = false,
        isTransparent = true,
        hardness = 0.0f,
        topColor = 0xFFFFEB3B.toInt(),
        sideColor = 0xFFFFEB3B.toInt(),
        dropItemId = "yellow_flower",
        category = BlockCategory.DECORATION,
        texturePattern = 3
    )

    private val customBlocks = mutableMapOf<Int, BlockProperties>()

    val allStandardBlocks = listOf(
        AIR, GRASS, DIRT, STONE, COBBLESTONE, OAK_LOG, OAK_LEAVES, OAK_PLANKS,
        SAND, WATER, GLASS, COAL_ORE, IRON_ORE, GOLD_ORE, DIAMOND_ORE,
        CRAFTING_TABLE, FURNACE, CHEST, TORCH, TNT, BRICK, OBSIDIAN, BEDROCK,
        GLOWSTONE, EMERALD_BLOCK, RED_FLOWER, YELLOW_FLOWER
    )

    fun registerCustomBlock(block: BlockProperties) {
        customBlocks[block.id] = block
    }

    fun clearCustomBlocks() {
        customBlocks.clear()
    }

    fun get(id: Int): BlockProperties {
        if (id in 0 until allStandardBlocks.size) {
            val standard = allStandardBlocks.find { it.id == id }
            if (standard != null) return standard
        }
        return customBlocks[id] ?: AIR
    }

    fun getByName(name: String): BlockProperties {
        val standard = allStandardBlocks.find { it.name.equals(name, ignoreCase = true) }
        if (standard != null) return standard
        return customBlocks.values.find { it.name.equals(name, ignoreCase = true) } ?: AIR
    }
}
