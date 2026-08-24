package com.example.model

enum class ItemCategory {
    BLOCKS,
    TOOLS,
    WEAPONS,
    ARMOR,
    FOOD,
    MATERIALS,
    SPECIAL
}

enum class ArmorSlot {
    HEAD,
    CHEST,
    LEGS,
    FEET,
    NONE
}

data class ItemDefinition(
    val id: String,
    val name: String,
    val maxStack: Int = 64,
    val category: ItemCategory = ItemCategory.MATERIALS,
    val blockId: Int? = null, // if this item places a block
    val attackDamage: Float = 1.0f,
    val miningSpeed: Float = 1.0f,
    val toolType: ToolType = ToolType.NONE,
    val durability: Int = -1, // -1 is infinite
    val foodHeal: Int = 0,
    val foodSaturation: Float = 0f,
    val armorDefense: Int = 0,
    val armorSlot: ArmorSlot = ArmorSlot.NONE,
    val iconColor: Int = 0xFFFFFFFF.toInt(),
    val iconSymbol: String = "■",
    val description: String = ""
)

data class ItemStack(
    val item: ItemDefinition,
    var count: Int = 1,
    var currentDurability: Int = item.durability
) {
    val isEmpty: Boolean get() = count <= 0 || item == ItemRegistry.EMPTY

    fun copy(count: Int = this.count): ItemStack {
        return ItemStack(item, count, currentDurability)
    }

    companion object {
        val EMPTY = ItemStack(ItemRegistry.EMPTY, 0)
    }
}

object ItemRegistry {
    val EMPTY = ItemDefinition(
        id = "empty",
        name = "Empty",
        maxStack = 0,
        iconSymbol = ""
    )

    // Blocks as items
    val DIRT = ItemDefinition(
        id = "dirt",
        name = "Dirt",
        category = ItemCategory.BLOCKS,
        blockId = 2,
        iconColor = 0xFF5C3E1E.toInt(),
        iconSymbol = "🟫"
    )

    val GRASS_BLOCK = ItemDefinition(
        id = "grass_block",
        name = "Grass Block",
        category = ItemCategory.BLOCKS,
        blockId = 1,
        iconColor = 0xFF5B8C3A.toInt(),
        iconSymbol = "🟩"
    )

    val STONE = ItemDefinition(
        id = "stone",
        name = "Stone",
        category = ItemCategory.BLOCKS,
        blockId = 3,
        iconColor = 0xFF787878.toInt(),
        iconSymbol = "⬜"
    )

    val COBBLESTONE = ItemDefinition(
        id = "cobblestone",
        name = "Cobblestone",
        category = ItemCategory.BLOCKS,
        blockId = 4,
        iconColor = 0xFF606060.toInt(),
        iconSymbol = "🔲"
    )

    val OAK_LOG = ItemDefinition(
        id = "oak_log",
        name = "Oak Wood",
        category = ItemCategory.BLOCKS,
        blockId = 5,
        iconColor = 0xFF4A3728.toInt(),
        iconSymbol = "🪵"
    )

    val OAK_PLANKS = ItemDefinition(
        id = "oak_planks",
        name = "Oak Planks",
        category = ItemCategory.BLOCKS,
        blockId = 7,
        iconColor = 0xFFB88748.toInt(),
        iconSymbol = "🟫"
    )

    val SAND = ItemDefinition(
        id = "sand",
        name = "Sand",
        category = ItemCategory.BLOCKS,
        blockId = 8,
        iconColor = 0xFFD8CC8C.toInt(),
        iconSymbol = "🟨"
    )

    val GLASS = ItemDefinition(
        id = "glass",
        name = "Glass",
        category = ItemCategory.BLOCKS,
        blockId = 10,
        iconColor = 0xFFD4F1F9.toInt(),
        iconSymbol = "🪟"
    )

    val CRAFTING_TABLE = ItemDefinition(
        id = "crafting_table",
        name = "Crafting Table",
        category = ItemCategory.BLOCKS,
        blockId = 15,
        iconColor = 0xFFB3733B.toInt(),
        iconSymbol = "🛠️"
    )

    val FURNACE = ItemDefinition(
        id = "furnace",
        name = "Furnace",
        category = ItemCategory.BLOCKS,
        blockId = 16,
        iconColor = 0xFF4F4F4F.toInt(),
        iconSymbol = "🔥"
    )

    val CHEST = ItemDefinition(
        id = "chest",
        name = "Chest",
        category = ItemCategory.BLOCKS,
        blockId = 17,
        iconColor = 0xFF9E682C.toInt(),
        iconSymbol = "📦"
    )

    val TORCH = ItemDefinition(
        id = "torch",
        name = "Torch",
        category = ItemCategory.BLOCKS,
        blockId = 18,
        iconColor = 0xFFFFD700.toInt(),
        iconSymbol = "🕯️"
    )

    val TNT = ItemDefinition(
        id = "tnt",
        name = "TNT",
        category = ItemCategory.BLOCKS,
        blockId = 19,
        iconColor = 0xFFD83B29.toInt(),
        iconSymbol = "🧨"
    )

    val BRICK = ItemDefinition(
        id = "brick",
        name = "Bricks",
        category = ItemCategory.BLOCKS,
        blockId = 20,
        iconColor = 0xFF9A4232.toInt(),
        iconSymbol = "🧱"
    )

    val OBSIDIAN = ItemDefinition(
        id = "obsidian",
        name = "Obsidian",
        category = ItemCategory.BLOCKS,
        blockId = 21,
        iconColor = 0xFF19102A.toInt(),
        iconSymbol = "⬛"
    )

    val GLOWSTONE = ItemDefinition(
        id = "glowstone",
        name = "Glowstone",
        category = ItemCategory.BLOCKS,
        blockId = 23,
        iconColor = 0xFFFBE48B.toInt(),
        iconSymbol = "✨"
    )

    val EMERALD_BLOCK = ItemDefinition(
        id = "emerald_block",
        name = "Emerald Block",
        category = ItemCategory.BLOCKS,
        blockId = 24,
        iconColor = 0xFF1FC85C.toInt(),
        iconSymbol = "🟩"
    )

    val RED_FLOWER = ItemDefinition(
        id = "red_flower",
        name = "Poppy Flower",
        category = ItemCategory.BLOCKS,
        blockId = 25,
        iconColor = 0xFFE53935.toInt(),
        iconSymbol = "🌹"
    )

    val YELLOW_FLOWER = ItemDefinition(
        id = "yellow_flower",
        name = "Dandelion",
        category = ItemCategory.BLOCKS,
        blockId = 26,
        iconColor = 0xFFFFEB3B.toInt(),
        iconSymbol = "🌼"
    )

    // Materials
    val STICK = ItemDefinition(
        id = "stick",
        name = "Stick",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFF7A5229.toInt(),
        iconSymbol = "🥢"
    )

    val COAL = ItemDefinition(
        id = "coal",
        name = "Coal",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFF212121.toInt(),
        iconSymbol = "⚫"
    )

    val IRON_ORE = ItemDefinition(
        id = "iron_ore",
        name = "Iron Ore",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFFD4B192.toInt(),
        iconSymbol = "🪨"
    )

    val IRON_INGOT = ItemDefinition(
        id = "iron_ingot",
        name = "Iron Ingot",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFFE0E0E0.toInt(),
        iconSymbol = "⚪"
    )

    val GOLD_ORE = ItemDefinition(
        id = "gold_ore",
        name = "Gold Ore",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFFFCD835.toInt(),
        iconSymbol = "🟡"
    )

    val GOLD_INGOT = ItemDefinition(
        id = "gold_ingot",
        name = "Gold Ingot",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFFFFD700.toInt(),
        iconSymbol = "🧈"
    )

    val DIAMOND = ItemDefinition(
        id = "diamond",
        name = "Diamond",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFF5DECF2.toInt(),
        iconSymbol = "💎"
    )

    val EMERALD = ItemDefinition(
        id = "emerald",
        name = "Emerald",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFF1FC85C.toInt(),
        iconSymbol = "🟢"
    )

    val GUNPOWDER = ItemDefinition(
        id = "gunpowder",
        name = "Gunpowder",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFF555555.toInt(),
        iconSymbol = "💨"
    )

    val BONE = ItemDefinition(
        id = "bone",
        name = "Bone",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFFEEEEEE.toInt(),
        iconSymbol = "🦴"
    )

    val STRING = ItemDefinition(
        id = "string",
        name = "String",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFFDDDDDD.toInt(),
        iconSymbol = "🧵"
    )

    val FEATHER = ItemDefinition(
        id = "feather",
        name = "Feather",
        category = ItemCategory.MATERIALS,
        iconColor = 0xFFFFFFFF.toInt(),
        iconSymbol = "🪶"
    )

    val ARROW = ItemDefinition(
        id = "arrow",
        name = "Arrow",
        category = ItemCategory.WEAPONS,
        iconColor = 0xFFB88748.toInt(),
        iconSymbol = "🏹"
    )

    // Weapons & Tools
    val WOODEN_SWORD = ItemDefinition(
        id = "wooden_sword",
        name = "Wooden Sword",
        maxStack = 1,
        category = ItemCategory.WEAPONS,
        attackDamage = 4.0f,
        durability = 60,
        toolType = ToolType.SWORD,
        iconColor = 0xFFB88748.toInt(),
        iconSymbol = "🗡️"
    )

    val STONE_SWORD = ItemDefinition(
        id = "stone_sword",
        name = "Stone Sword",
        maxStack = 1,
        category = ItemCategory.WEAPONS,
        attackDamage = 5.0f,
        durability = 132,
        toolType = ToolType.SWORD,
        iconColor = 0xFF888888.toInt(),
        iconSymbol = "🗡️"
    )

    val IRON_SWORD = ItemDefinition(
        id = "iron_sword",
        name = "Iron Sword",
        maxStack = 1,
        category = ItemCategory.WEAPONS,
        attackDamage = 6.0f,
        durability = 250,
        toolType = ToolType.SWORD,
        iconColor = 0xFFE0E0E0.toInt(),
        iconSymbol = "🗡️"
    )

    val DIAMOND_SWORD = ItemDefinition(
        id = "diamond_sword",
        name = "Diamond Sword",
        maxStack = 1,
        category = ItemCategory.WEAPONS,
        attackDamage = 7.5f,
        durability = 1561,
        toolType = ToolType.SWORD,
        iconColor = 0xFF5DECF2.toInt(),
        iconSymbol = "🗡️"
    )

    val WOODEN_PICKAXE = ItemDefinition(
        id = "wooden_pickaxe",
        name = "Wooden Pickaxe",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 2.0f,
        durability = 60,
        toolType = ToolType.PICKAXE,
        iconColor = 0xFFB88748.toInt(),
        iconSymbol = "⛏️"
    )

    val STONE_PICKAXE = ItemDefinition(
        id = "stone_pickaxe",
        name = "Stone Pickaxe",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 4.0f,
        durability = 132,
        toolType = ToolType.PICKAXE,
        iconColor = 0xFF888888.toInt(),
        iconSymbol = "⛏️"
    )

    val IRON_PICKAXE = ItemDefinition(
        id = "iron_pickaxe",
        name = "Iron Pickaxe",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 6.0f,
        durability = 250,
        toolType = ToolType.PICKAXE,
        iconColor = 0xFFE0E0E0.toInt(),
        iconSymbol = "⛏️"
    )

    val DIAMOND_PICKAXE = ItemDefinition(
        id = "diamond_pickaxe",
        name = "Diamond Pickaxe",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 8.0f,
        durability = 1561,
        toolType = ToolType.PICKAXE,
        iconColor = 0xFF5DECF2.toInt(),
        iconSymbol = "⛏️"
    )

    val WOODEN_AXE = ItemDefinition(
        id = "wooden_axe",
        name = "Wooden Axe",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 2.0f,
        attackDamage = 3.0f,
        durability = 60,
        toolType = ToolType.AXE,
        iconColor = 0xFFB88748.toInt(),
        iconSymbol = "🪓"
    )

    val STONE_AXE = ItemDefinition(
        id = "stone_axe",
        name = "Stone Axe",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 4.0f,
        attackDamage = 4.0f,
        durability = 132,
        toolType = ToolType.AXE,
        iconColor = 0xFF888888.toInt(),
        iconSymbol = "🪓"
    )

    val IRON_AXE = ItemDefinition(
        id = "iron_axe",
        name = "Iron Axe",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 6.0f,
        attackDamage = 5.0f,
        durability = 250,
        toolType = ToolType.AXE,
        iconColor = 0xFFE0E0E0.toInt(),
        iconSymbol = "🪓"
    )

    val WOODEN_SHOVEL = ItemDefinition(
        id = "wooden_shovel",
        name = "Wooden Shovel",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 2.0f,
        durability = 60,
        toolType = ToolType.SHOVEL,
        iconColor = 0xFFB88748.toInt(),
        iconSymbol = "🥄"
    )

    val STONE_SHOVEL = ItemDefinition(
        id = "stone_shovel",
        name = "Stone Shovel",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 4.0f,
        durability = 132,
        toolType = ToolType.SHOVEL,
        iconColor = 0xFF888888.toInt(),
        iconSymbol = "🥄"
    )

    val IRON_SHOVEL = ItemDefinition(
        id = "iron_shovel",
        name = "Iron Shovel",
        maxStack = 1,
        category = ItemCategory.TOOLS,
        miningSpeed = 6.0f,
        durability = 250,
        toolType = ToolType.SHOVEL,
        iconColor = 0xFFE0E0E0.toInt(),
        iconSymbol = "🥄"
    )

    val BOW = ItemDefinition(
        id = "bow",
        name = "Bow",
        maxStack = 1,
        category = ItemCategory.WEAPONS,
        durability = 384,
        iconColor = 0xFF8A5528.toInt(),
        iconSymbol = "🏹"
    )

    // Armor
    val IRON_HELMET = ItemDefinition(
        id = "iron_helmet",
        name = "Iron Helmet",
        maxStack = 1,
        category = ItemCategory.ARMOR,
        armorDefense = 2,
        armorSlot = ArmorSlot.HEAD,
        durability = 165,
        iconColor = 0xFFD8D8D8.toInt(),
        iconSymbol = "🪖"
    )

    val IRON_CHESTPLATE = ItemDefinition(
        id = "iron_chestplate",
        name = "Iron Chestplate",
        maxStack = 1,
        category = ItemCategory.ARMOR,
        armorDefense = 6,
        armorSlot = ArmorSlot.CHEST,
        durability = 240,
        iconColor = 0xFFD8D8D8.toInt(),
        iconSymbol = "🦺"
    )

    val IRON_LEGGINGS = ItemDefinition(
        id = "iron_leggings",
        name = "Iron Leggings",
        maxStack = 1,
        category = ItemCategory.ARMOR,
        armorDefense = 5,
        armorSlot = ArmorSlot.LEGS,
        durability = 225,
        iconColor = 0xFFD8D8D8.toInt(),
        iconSymbol = "👖"
    )

    val IRON_BOOTS = ItemDefinition(
        id = "iron_boots",
        name = "Iron Boots",
        maxStack = 1,
        category = ItemCategory.ARMOR,
        armorDefense = 2,
        armorSlot = ArmorSlot.FEET,
        durability = 195,
        iconColor = 0xFFD8D8D8.toInt(),
        iconSymbol = "🥾"
    )

    val DIAMOND_CHESTPLATE = ItemDefinition(
        id = "diamond_chestplate",
        name = "Diamond Chestplate",
        maxStack = 1,
        category = ItemCategory.ARMOR,
        armorDefense = 8,
        armorSlot = ArmorSlot.CHEST,
        durability = 528,
        iconColor = 0xFF5DECF2.toInt(),
        iconSymbol = "🦺"
    )

    // Food
    val APPLE = ItemDefinition(
        id = "apple",
        name = "Apple",
        category = ItemCategory.FOOD,
        foodHeal = 4,
        foodSaturation = 2.4f,
        iconColor = 0xFFE53935.toInt(),
        iconSymbol = "🍎"
    )

    val BREAD = ItemDefinition(
        id = "bread",
        name = "Bread",
        category = ItemCategory.FOOD,
        foodHeal = 5,
        foodSaturation = 6.0f,
        iconColor = 0xFFD4A359.toInt(),
        iconSymbol = "🍞"
    )

    val RAW_PORKCHOP = ItemDefinition(
        id = "raw_porkchop",
        name = "Raw Porkchop",
        category = ItemCategory.FOOD,
        foodHeal = 3,
        foodSaturation = 1.8f,
        iconColor = 0xFFE57373.toInt(),
        iconSymbol = "🥩"
    )

    val COOKED_PORKCHOP = ItemDefinition(
        id = "cooked_porkchop",
        name = "Cooked Porkchop",
        category = ItemCategory.FOOD,
        foodHeal = 8,
        foodSaturation = 12.8f,
        iconColor = 0xFF8D5B4C.toInt(),
        iconSymbol = "🍖"
    )

    val GOLDEN_APPLE = ItemDefinition(
        id = "golden_apple",
        name = "Golden Apple",
        category = ItemCategory.FOOD,
        foodHeal = 8,
        foodSaturation = 14.4f,
        iconColor = 0xFFFFD700.toInt(),
        iconSymbol = "🍏"
    )

    private val customItems = mutableMapOf<String, ItemDefinition>()

    val allItems = listOf(
        DIRT, GRASS_BLOCK, STONE, COBBLESTONE, OAK_LOG, OAK_PLANKS, SAND, GLASS,
        CRAFTING_TABLE, FURNACE, CHEST, TORCH, TNT, BRICK, OBSIDIAN, GLOWSTONE,
        EMERALD_BLOCK, RED_FLOWER, YELLOW_FLOWER,
        STICK, COAL, IRON_ORE, IRON_INGOT, GOLD_ORE, GOLD_INGOT, DIAMOND, EMERALD,
        GUNPOWDER, BONE, STRING, FEATHER, ARROW,
        WOODEN_SWORD, STONE_SWORD, IRON_SWORD, DIAMOND_SWORD,
        WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, DIAMOND_PICKAXE,
        WOODEN_AXE, STONE_AXE, IRON_AXE,
        WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL,
        BOW,
        IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS, DIAMOND_CHESTPLATE,
        APPLE, BREAD, RAW_PORKCHOP, COOKED_PORKCHOP, GOLDEN_APPLE
    )

    fun registerCustomItem(item: ItemDefinition) {
        customItems[item.id] = item
    }

    fun clearCustomItems() {
        customItems.clear()
    }

    fun get(id: String): ItemDefinition {
        val standard = allItems.find { it.id.equals(id, ignoreCase = true) }
        if (standard != null) return standard
        return customItems[id.lowercase()] ?: EMPTY
    }

    fun getByBlockId(blockId: Int): ItemDefinition {
        val standard = allItems.find { it.blockId == blockId }
        if (standard != null) return standard
        return customItems.values.find { it.blockId == blockId } ?: EMPTY
    }
}
