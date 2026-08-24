package com.example.data

import com.example.engine.VoxelWorld
import com.example.model.ItemRegistry
import com.example.model.ItemStack

data class TradeOffer(
    val id: String,
    val title: String,
    val description: String,
    val costItemId: String,
    val costCount: Int,
    val rewardItemId: String,
    val rewardCount: Int,
    val category: String = "TOOLS"
)

object MarketplaceManager {
    val tradeOffers = listOf(
        TradeOffer(
            id = "trade_diamond_pickaxe",
            title = "Enchanted Diamond Pickaxe",
            description = "Mastercrafted pickaxe that slices through obsidian with ease.",
            costItemId = "emerald",
            costCount = 5,
            rewardItemId = "diamond_pickaxe",
            rewardCount = 1,
            category = "TOOLS"
        ),
        TradeOffer(
            id = "trade_diamond_sword",
            title = "Vorpal Diamond Blade",
            description = "Inflicts devastating critical damage against Creepers and Skeletons.",
            costItemId = "emerald",
            costCount = 6,
            rewardItemId = "diamond_sword",
            rewardCount = 1,
            category = "WEAPONS"
        ),
        TradeOffer(
            id = "trade_golden_apples",
            title = "Enchanted Golden Apples (x3)",
            description = "Instantly restores full health, hunger, and provides strong absorption.",
            costItemId = "gold_ingot",
            costCount = 12,
            rewardItemId = "golden_apple",
            rewardCount = 3,
            category = "FOOD"
        ),
        TradeOffer(
            id = "trade_tnt_bundle",
            title = "Demolition TNT Bundle (x8)",
            description = "High explosive blocks for instant cave excavation and mining.",
            costItemId = "iron_ingot",
            costCount = 8,
            rewardItemId = "tnt",
            rewardCount = 8,
            category = "UTILITY"
        ),
        TradeOffer(
            id = "trade_diamond_armor",
            title = "Reinforced Diamond Chestplate",
            description = "Heavy diamond armor that absorbs 80% of incoming hostile mob attacks.",
            costItemId = "diamond",
            costCount = 8,
            rewardItemId = "diamond_chestplate",
            rewardCount = 1,
            category = "ARMOR"
        ),
        TradeOffer(
            id = "trade_glowstone_crate",
            title = "Glowstone Luminary Pack (x16)",
            description = "Permanent radiant lighting blocks for illumination during pitch dark nights.",
            costItemId = "coal",
            costCount = 24,
            rewardItemId = "glowstone",
            rewardCount = 16,
            category = "BUILDING"
        ),
        TradeOffer(
            id = "trade_emeralds_for_gold",
            title = "Bank Exchange: 5 Emeralds",
            description = "Trade raw smelted gold bars for genuine village emeralds.",
            costItemId = "gold_ingot",
            costCount = 10,
            rewardItemId = "emerald",
            rewardCount = 5,
            category = "CURRENCY"
        )
    )

    fun executeTrade(world: VoxelWorld, offer: TradeOffer): Boolean {
        // Count how many cost items player has
        var totalFound = 0
        for (slot in world.hotbar) {
            if (slot.item.id.equals(offer.costItemId, ignoreCase = true)) {
                totalFound += slot.count
            }
        }
        for (slot in world.inventory) {
            if (slot.item.id.equals(offer.costItemId, ignoreCase = true)) {
                totalFound += slot.count
            }
        }

        if (totalFound < offer.costCount && world.gameMode != com.example.engine.GameMode.CREATIVE) {
            return false // Insufficient funds
        }

        // Deduct cost
        if (world.gameMode != com.example.engine.GameMode.CREATIVE) {
            var remainingToRemove = offer.costCount
            for (i in world.hotbar.indices) {
                if (remainingToRemove <= 0) break
                if (world.hotbar[i].item.id.equals(offer.costItemId, ignoreCase = true)) {
                    val take = minOf(world.hotbar[i].count, remainingToRemove)
                    world.hotbar[i].count -= take
                    remainingToRemove -= take
                    if (world.hotbar[i].count <= 0) world.hotbar[i] = ItemStack.EMPTY
                }
            }
            for (i in world.inventory.indices) {
                if (remainingToRemove <= 0) break
                if (world.inventory[i].item.id.equals(offer.costItemId, ignoreCase = true)) {
                    val take = minOf(world.inventory[i].count, remainingToRemove)
                    world.inventory[i].count -= take
                    remainingToRemove -= take
                    if (world.inventory[i].count <= 0) world.inventory[i] = ItemStack.EMPTY
                }
            }
        }

        // Give reward item
        val rewardItem = ItemRegistry.get(offer.rewardItemId)
        world.addItemToInventory(ItemStack(rewardItem, offer.rewardCount))
        world.soundEngine.playCraftSuccess()
        return true
    }
}
