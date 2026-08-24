package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.example.engine.VoxelWorld
import com.example.model.*

@Composable
fun CraftingDialog(
    world: VoxelWorld,
    onDismiss: () -> Unit
) {
    val grid = remember { mutableStateListOf(*Array(9) { ItemStack.EMPTY }) }
    var outputStack by remember { mutableStateOf(ItemStack.EMPTY) }

    fun check3x3Crafting() {
        outputStack = RecipeRegistry.find3x3CraftingMatch(grid.toList())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF263238),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF455A64)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛠️ Crafting Table (3x3)",
                        color = Color(0xFFFFD54F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).testTag("close_crafting_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3x3 Grid & Result Output
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 3x3 Input Grid
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (row in 0..2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                for (col in 0..2) {
                                    val idx = row * 3 + col
                                    ItemSlotBox(
                                        stack = grid[idx],
                                        isSelected = false,
                                        onClick = {
                                            val equipped = world.getEquippedItem()
                                            if (!equipped.isEmpty) {
                                                if (grid[idx].isEmpty) {
                                                    grid[idx] = ItemStack(equipped.item, 1)
                                                    equipped.count--
                                                    if (equipped.count <= 0) world.hotbar[world.selectedHotbarIndex] = ItemStack.EMPTY
                                                } else if (grid[idx].item.id == equipped.item.id) {
                                                    grid[idx].count++
                                                    equipped.count--
                                                    if (equipped.count <= 0) world.hotbar[world.selectedHotbarIndex] = ItemStack.EMPTY
                                                }
                                            } else if (!grid[idx].isEmpty) {
                                                world.addItemToInventory(grid[idx])
                                                grid[idx] = ItemStack.EMPTY
                                            }
                                            check3x3Crafting()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                    Text("➡️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(16.dp))

                    // Output Slot
                    ItemSlotBox(
                        stack = outputStack,
                        isSelected = !outputStack.isEmpty,
                        borderColor = Color(0xFF76FF03),
                        onClick = {
                            if (!outputStack.isEmpty) {
                                world.addItemToInventory(outputStack.copy())
                                world.soundEngine.playCraftSuccess()
                                for (i in 0..8) {
                                    if (!grid[i].isEmpty) {
                                        grid[i].count--
                                        if (grid[i].count <= 0) grid[i] = ItemStack.EMPTY
                                    }
                                }
                                check3x3Crafting()
                            }
                        }
                    )
                }

                Divider(color = Color(0xFF455A64), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // Instant Recipe Book Section
                Text(
                    text = "📖 Recipe Book (1-Touch Craft)",
                    color = Color(0xFF90CAF9),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Recipe List
                val allRecipes = RecipeRegistry.craftingRecipes
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(allRecipes) { recipe ->
                        val canCraft = canPlayerCraft(world, recipe)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (canCraft) Color(0xFF1E272C) else Color(0x661E272C),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (canCraft) Color(0xFF43A047) else Color(0x33FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = canCraft) {
                                    executeInstantCraft(world, recipe)
                                    world.soundEngine.playCraftSuccess()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(recipe.result.item.iconSymbol, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "${recipe.result.item.name} x${recipe.result.count}",
                                            color = if (canCraft) Color.White else Color.Gray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Cost: " + recipe.ingredients.filterNotNull().joinToString(", "),
                                            color = if (canCraft) Color(0xFFA5D6A7) else Color(0xFF757575),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        executeInstantCraft(world, recipe)
                                        world.soundEngine.playCraftSuccess()
                                    },
                                    enabled = canCraft,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Craft", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun canPlayerCraft(world: VoxelWorld, recipe: CraftingRecipe): Boolean {
    if (world.gameMode == com.example.engine.GameMode.CREATIVE) return true

    val requiredCounts = mutableMapOf<String, Int>()
    for (ing in recipe.ingredients.filterNotNull()) {
        requiredCounts[ing] = (requiredCounts[ing] ?: 0) + 1
    }

    val availableCounts = mutableMapOf<String, Int>()
    for (s in world.hotbar) {
        if (!s.isEmpty) {
            availableCounts[s.item.id] = (availableCounts[s.item.id] ?: 0) + s.count
        }
    }
    for (s in world.inventory) {
        if (!s.isEmpty) {
            availableCounts[s.item.id] = (availableCounts[s.item.id] ?: 0) + s.count
        }
    }

    for ((ing, req) in requiredCounts) {
        val avail = availableCounts[ing] ?: 0
        if (avail < req) return false
    }
    return true
}

private fun executeInstantCraft(world: VoxelWorld, recipe: CraftingRecipe) {
    if (world.gameMode != com.example.engine.GameMode.CREATIVE) {
        val requiredCounts = mutableMapOf<String, Int>()
        for (ing in recipe.ingredients.filterNotNull()) {
            requiredCounts[ing] = (requiredCounts[ing] ?: 0) + 1
        }

        for ((ing, countNeeded) in requiredCounts) {
            var rem = countNeeded
            for (i in world.hotbar.indices) {
                if (rem <= 0) break
                if (world.hotbar[i].item.id == ing) {
                    val take = minOf(world.hotbar[i].count, rem)
                    world.hotbar[i].count -= take
                    rem -= take
                    if (world.hotbar[i].count <= 0) world.hotbar[i] = ItemStack.EMPTY
                }
            }
            for (i in world.inventory.indices) {
                if (rem <= 0) break
                if (world.inventory[i].item.id == ing) {
                    val take = minOf(world.inventory[i].count, rem)
                    world.inventory[i].count -= take
                    rem -= take
                    if (world.inventory[i].count <= 0) world.inventory[i] = ItemStack.EMPTY
                }
            }
        }
    }
    world.addItemToInventory(recipe.result.copy())
}
