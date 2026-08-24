package com.example.model

data class CraftingRecipe(
    val id: String,
    val result: ItemStack,
    val width: Int,
    val height: Int,
    val ingredients: List<String?>, // IDs of items or null for empty slots (row-major)
    val isShapeless: Boolean = false,
    val description: String = ""
)

data class SmeltingRecipe(
    val inputId: String,
    val result: ItemStack,
    val cookTimeSeconds: Float = 6.0f,
    val xpGain: Float = 0.7f
)

object RecipeRegistry {
    val craftingRecipes = mutableListOf<CraftingRecipe>()
    val smeltingRecipes = mutableListOf<SmeltingRecipe>()

    init {
        registerDefaultRecipes()
    }

    private fun registerDefaultRecipes() {
        craftingRecipes.clear()
        smeltingRecipes.clear()

        // 1 Wood Log -> 4 Planks (Shapeless)
        craftingRecipes.add(
            CraftingRecipe(
                id = "planks_from_log",
                result = ItemStack(ItemRegistry.OAK_PLANKS, 4),
                width = 1,
                height = 1,
                ingredients = listOf("oak_log"),
                isShapeless = true,
                description = "4 Oak Planks from 1 Wood Log"
            )
        )

        // 2 Planks -> 4 Sticks
        craftingRecipes.add(
            CraftingRecipe(
                id = "sticks",
                result = ItemStack(ItemRegistry.STICK, 4),
                width = 1,
                height = 2,
                ingredients = listOf("oak_planks", "oak_planks"),
                description = "4 Sticks from 2 Planks"
            )
        )

        // 4 Planks -> 1 Crafting Table
        craftingRecipes.add(
            CraftingRecipe(
                id = "crafting_table",
                result = ItemStack(ItemRegistry.CRAFTING_TABLE, 1),
                width = 2,
                height = 2,
                ingredients = listOf(
                    "oak_planks", "oak_planks",
                    "oak_planks", "oak_planks"
                ),
                description = "Crafting Table from 4 Planks"
            )
        )

        // 8 Planks -> 1 Chest (3x3 outer ring)
        craftingRecipes.add(
            CraftingRecipe(
                id = "chest",
                result = ItemStack(ItemRegistry.CHEST, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "oak_planks", "oak_planks", "oak_planks",
                    "oak_planks", null,         "oak_planks",
                    "oak_planks", "oak_planks", "oak_planks"
                ),
                description = "Chest from 8 Planks"
            )
        )

        // 8 Cobblestone -> 1 Furnace (3x3 outer ring)
        craftingRecipes.add(
            CraftingRecipe(
                id = "furnace",
                result = ItemStack(ItemRegistry.FURNACE, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "cobblestone", "cobblestone", "cobblestone",
                    "cobblestone", null,          "cobblestone",
                    "cobblestone", "cobblestone", "cobblestone"
                ),
                description = "Furnace from 8 Cobblestone"
            )
        )

        // 1 Coal + 1 Stick -> 4 Torches
        craftingRecipes.add(
            CraftingRecipe(
                id = "torch",
                result = ItemStack(ItemRegistry.TORCH, 4),
                width = 1,
                height = 2,
                ingredients = listOf("coal", "stick"),
                description = "4 Torches from Coal and Stick"
            )
        )

        // Wooden Pickaxe
        craftingRecipes.add(
            CraftingRecipe(
                id = "wooden_pickaxe",
                result = ItemStack(ItemRegistry.WOODEN_PICKAXE, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "oak_planks", "oak_planks", "oak_planks",
                    null,         "stick",      null,
                    null,         "stick",      null
                ),
                description = "Wooden Pickaxe"
            )
        )

        // Wooden Sword
        craftingRecipes.add(
            CraftingRecipe(
                id = "wooden_sword",
                result = ItemStack(ItemRegistry.WOODEN_SWORD, 1),
                width = 1,
                height = 3,
                ingredients = listOf("oak_planks", "oak_planks", "stick"),
                description = "Wooden Sword"
            )
        )

        // Wooden Axe
        craftingRecipes.add(
            CraftingRecipe(
                id = "wooden_axe",
                result = ItemStack(ItemRegistry.WOODEN_AXE, 1),
                width = 2,
                height = 3,
                ingredients = listOf(
                    "oak_planks", "oak_planks",
                    "oak_planks", "stick",
                    null,         "stick"
                ),
                description = "Wooden Axe"
            )
        )

        // Stone Pickaxe
        craftingRecipes.add(
            CraftingRecipe(
                id = "stone_pickaxe",
                result = ItemStack(ItemRegistry.STONE_PICKAXE, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "cobblestone", "cobblestone", "cobblestone",
                    null,          "stick",       null,
                    null,          "stick",       null
                ),
                description = "Stone Pickaxe"
            )
        )

        // Stone Sword
        craftingRecipes.add(
            CraftingRecipe(
                id = "stone_sword",
                result = ItemStack(ItemRegistry.STONE_SWORD, 1),
                width = 1,
                height = 3,
                ingredients = listOf("cobblestone", "cobblestone", "stick"),
                description = "Stone Sword"
            )
        )

        // Iron Pickaxe
        craftingRecipes.add(
            CraftingRecipe(
                id = "iron_pickaxe",
                result = ItemStack(ItemRegistry.IRON_PICKAXE, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "iron_ingot", "iron_ingot", "iron_ingot",
                    null,         "stick",      null,
                    null,         "stick",      null
                ),
                description = "Iron Pickaxe"
            )
        )

        // Iron Sword
        craftingRecipes.add(
            CraftingRecipe(
                id = "iron_sword",
                result = ItemStack(ItemRegistry.IRON_SWORD, 1),
                width = 1,
                height = 3,
                ingredients = listOf("iron_ingot", "iron_ingot", "stick"),
                description = "Iron Sword"
            )
        )

        // Diamond Pickaxe
        craftingRecipes.add(
            CraftingRecipe(
                id = "diamond_pickaxe",
                result = ItemStack(ItemRegistry.DIAMOND_PICKAXE, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "diamond", "diamond", "diamond",
                    null,      "stick",   null,
                    null,      "stick",   null
                ),
                description = "Diamond Pickaxe"
            )
        )

        // Diamond Sword
        craftingRecipes.add(
            CraftingRecipe(
                id = "diamond_sword",
                result = ItemStack(ItemRegistry.DIAMOND_SWORD, 1),
                width = 1,
                height = 3,
                ingredients = listOf("diamond", "diamond", "stick"),
                description = "Diamond Sword"
            )
        )

        // Iron Armor: Helmet
        craftingRecipes.add(
            CraftingRecipe(
                id = "iron_helmet",
                result = ItemStack(ItemRegistry.IRON_HELMET, 1),
                width = 3,
                height = 2,
                ingredients = listOf(
                    "iron_ingot", "iron_ingot", "iron_ingot",
                    "iron_ingot", null,         "iron_ingot"
                ),
                description = "Iron Helmet"
            )
        )

        // Iron Armor: Chestplate
        craftingRecipes.add(
            CraftingRecipe(
                id = "iron_chestplate",
                result = ItemStack(ItemRegistry.IRON_CHESTPLATE, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "iron_ingot", null,         "iron_ingot",
                    "iron_ingot", "iron_ingot", "iron_ingot",
                    "iron_ingot", "iron_ingot", "iron_ingot"
                ),
                description = "Iron Chestplate"
            )
        )

        // Iron Armor: Leggings
        craftingRecipes.add(
            CraftingRecipe(
                id = "iron_leggings",
                result = ItemStack(ItemRegistry.IRON_LEGGINGS, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "iron_ingot", "iron_ingot", "iron_ingot",
                    "iron_ingot", null,         "iron_ingot",
                    "iron_ingot", null,         "iron_ingot"
                ),
                description = "Iron Leggings"
            )
        )

        // Iron Armor: Boots
        craftingRecipes.add(
            CraftingRecipe(
                id = "iron_boots",
                result = ItemStack(ItemRegistry.IRON_BOOTS, 1),
                width = 3,
                height = 2,
                ingredients = listOf(
                    "iron_ingot", null, "iron_ingot",
                    "iron_ingot", null, "iron_ingot"
                ),
                description = "Iron Boots"
            )
        )

        // TNT (4 Sand + 5 Gunpowder)
        craftingRecipes.add(
            CraftingRecipe(
                id = "tnt",
                result = ItemStack(ItemRegistry.TNT, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "gunpowder", "sand",      "gunpowder",
                    "sand",      "gunpowder", "sand",
                    "gunpowder", "sand",      "gunpowder"
                ),
                description = "TNT Block"
            )
        )

        // Bow (3 Sticks + 3 Strings)
        craftingRecipes.add(
            CraftingRecipe(
                id = "bow",
                result = ItemStack(ItemRegistry.BOW, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    null,    "stick",  "string",
                    "stick", null,     "string",
                    null,    "stick",  "string"
                ),
                description = "Hunting Bow"
            )
        )

        // Arrow (1 Stick + 1 Feather + 1 Cobblestone)
        craftingRecipes.add(
            CraftingRecipe(
                id = "arrow",
                result = ItemStack(ItemRegistry.ARROW, 4),
                width = 1,
                height = 3,
                ingredients = listOf("cobblestone", "stick", "feather"),
                description = "4 Arrows"
            )
        )

        // Golden Apple (1 Apple + 8 Gold Ingots)
        craftingRecipes.add(
            CraftingRecipe(
                id = "golden_apple",
                result = ItemStack(ItemRegistry.GOLDEN_APPLE, 1),
                width = 3,
                height = 3,
                ingredients = listOf(
                    "gold_ingot", "gold_ingot", "gold_ingot",
                    "gold_ingot", "apple",      "gold_ingot",
                    "gold_ingot", "gold_ingot", "gold_ingot"
                ),
                description = "Enchanted Golden Apple"
            )
        )

        // Smelting Recipes
        smeltingRecipes.add(SmeltingRecipe("iron_ore", ItemStack(ItemRegistry.IRON_INGOT, 1), 6f, 0.7f))
        smeltingRecipes.add(SmeltingRecipe("gold_ore", ItemStack(ItemRegistry.GOLD_INGOT, 1), 6f, 1.0f))
        smeltingRecipes.add(SmeltingRecipe("sand", ItemStack(ItemRegistry.GLASS, 1), 5f, 0.2f))
        smeltingRecipes.add(SmeltingRecipe("raw_porkchop", ItemStack(ItemRegistry.COOKED_PORKCHOP, 1), 5f, 0.35f))
        smeltingRecipes.add(SmeltingRecipe("cobblestone", ItemStack(ItemRegistry.STONE, 1), 5f, 0.1f))
    }

    fun addCustomRecipe(recipe: CraftingRecipe) {
        craftingRecipes.add(recipe)
    }

    fun find2x2CraftingMatch(grid: List<ItemStack>): ItemStack {
        val ids = grid.map { if (it.isEmpty) null else it.item.id }
        val matched = matchCrafting(ids, 2)
        return matched?.result ?: ItemStack.EMPTY
    }

    fun find3x3CraftingMatch(grid: List<ItemStack>): ItemStack {
        val ids = grid.map { if (it.isEmpty) null else it.item.id }
        val matched = matchCrafting(ids, 3)
        return matched?.result ?: ItemStack.EMPTY
    }

    fun findSmeltingRecipe(itemId: String): SmeltingRecipe? {
        return smeltingRecipes.find { it.inputId.equals(itemId, ignoreCase = true) }
    }

    fun getFuelBurnTime(itemId: String): Float {
        return when (itemId) {
            "coal" -> 16.0f
            "oak_log" -> 6.0f
            "oak_planks" -> 3.0f
            "stick" -> 1.0f
            else -> 0.0f
        }
    }

    /**
     * Checks a 2x2 or 3x3 grid for a matching recipe
     */
    fun matchCrafting(grid: List<String?>, gridSize: Int): CraftingRecipe? {
        val nonNullItems: List<String> = grid.filterNotNull().filter { it.isNotEmpty() }
        if (nonNullItems.isEmpty()) return null

        for (recipe in craftingRecipes) {
            if (recipe.isShapeless) {
                val recipeIngredients: List<String> = recipe.ingredients.filterNotNull().filter { it.isNotEmpty() }
                if (recipeIngredients.sorted() == nonNullItems.sorted()) {
                    return recipe
                }
            } else {
                if (recipe.width > gridSize || recipe.height > gridSize) continue

                // Check all possible offsets in the grid
                for (offY in 0..(gridSize - recipe.height)) {
                    for (offX in 0..(gridSize - recipe.width)) {
                        if (matchesAtOffset(grid, gridSize, recipe, offX, offY)) {
                            return recipe
                        }
                    }
                }
            }
        }
        return null
    }

    private fun matchesAtOffset(
        grid: List<String?>,
        gridSize: Int,
        recipe: CraftingRecipe,
        offX: Int,
        offY: Int
    ): Boolean {
        for (y in 0 until gridSize) {
            for (x in 0 until gridSize) {
                val gridItem = grid.getOrNull(y * gridSize + x)?.ifEmpty { null }
                val recX = x - offX
                val recY = y - offY
                val recipeItem = if (recX in 0 until recipe.width && recY in 0 until recipe.height) {
                    recipe.ingredients.getOrNull(recY * recipe.width + recX)
                } else null

                if (gridItem != recipeItem) {
                    return false
                }
            }
        }
        return true
    }
}
