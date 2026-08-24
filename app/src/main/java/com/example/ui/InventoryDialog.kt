package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shield
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
fun InventoryDialog(
    world: VoxelWorld,
    onDismiss: () -> Unit
) {
    // 2x2 Crafting Grid
    val craftGrid = remember { mutableStateListOf(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY) }
    var craftResult by remember { mutableStateOf(ItemStack.EMPTY) }

    fun checkCrafting() {
        craftResult = RecipeRegistry.find2x2CraftingMatch(
            listOf(craftGrid[0], craftGrid[1], craftGrid[2], craftGrid[3])
        )
    }

    var selectedItemIndex by remember { mutableStateOf(-1) }
    var isHotbarSelected by remember { mutableStateOf(false) }

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
                        text = "🎒 Inventory & Equipment",
                        color = Color(0xFFFFD54F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).testTag("close_inv_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Armor & 2x2 Crafting Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Armor Slots (4)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Armor", color = Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val armorIcons = listOf("🪖", "🦺", "👖", "🥾")
                        for (i in 0..3) {
                            ItemSlotBox(
                                stack = world.armor[i],
                                isSelected = false,
                                placeholder = armorIcons[i],
                                onClick = {
                                    // Unequip armor back to inventory
                                    if (!world.armor[i].isEmpty) {
                                        world.addItemToInventory(world.armor[i])
                                        world.armor[i] = ItemStack.EMPTY
                                        world.soundEngine.playDig()
                                    }
                                }
                            )
                        }
                    }

                    // 2x2 Crafting Grid
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Crafting (2x2)", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ItemSlotBox(craftGrid[0], false) {
                                transferToCraftSlot(world, craftGrid, 0); checkCrafting()
                            }
                            ItemSlotBox(craftGrid[1], false) {
                                transferToCraftSlot(world, craftGrid, 1); checkCrafting()
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            ItemSlotBox(craftGrid[2], false) {
                                transferToCraftSlot(world, craftGrid, 2); checkCrafting()
                            }
                            ItemSlotBox(craftGrid[3], false) {
                                transferToCraftSlot(world, craftGrid, 3); checkCrafting()
                            }
                        }
                    }

                    // Crafting Result Arrow & Slot
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("Result", color = Color(0xFFFFB74D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("➡️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        ItemSlotBox(
                            stack = craftResult,
                            isSelected = !craftResult.isEmpty,
                            borderColor = Color(0xFF76FF03),
                            onClick = {
                                if (!craftResult.isEmpty) {
                                    // Collect crafted item
                                    world.addItemToInventory(craftResult.copy())
                                    world.soundEngine.playCraftSuccess()
                                    // Decrement craft ingredients
                                    for (i in 0..3) {
                                        if (!craftGrid[i].isEmpty) {
                                            craftGrid[i].count--
                                            if (craftGrid[i].count <= 0) craftGrid[i] = ItemStack.EMPTY
                                        }
                                    }
                                    checkCrafting()
                                }
                            }
                        )
                    }
                }

                Divider(color = Color(0xFF455A64), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                // 27-Slot Main Inventory Grid
                Text(
                    text = "Inventory Storage",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(9),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxWidth().height(125.dp)
                ) {
                    items(27) { idx ->
                        val isSelected = (!isHotbarSelected && selectedItemIndex == idx)
                        ItemSlotBox(
                            stack = world.inventory[idx],
                            isSelected = isSelected,
                            onClick = {
                                handleInventorySlotClick(world, idx, isHotbar = false, selectedItemIndex, isHotbarSelected) { newIdx, isHb ->
                                    selectedItemIndex = newIdx
                                    isHotbarSelected = isHb
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 9-Slot Hotbar Grid
                Text(
                    text = "Equipped Hotbar",
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (idx in 0..8) {
                        val isSelected = (isHotbarSelected && selectedItemIndex == idx)
                        ItemSlotBox(
                            stack = world.hotbar[idx],
                            isSelected = isSelected,
                            borderColor = if (isSelected) Color(0xFFFFD54F) else Color(0x66FFFFFF),
                            onClick = {
                                handleInventorySlotClick(world, idx, isHotbar = true, selectedItemIndex, isHotbarSelected) { newIdx, isHb ->
                                    selectedItemIndex = newIdx
                                    isHotbarSelected = isHb
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun transferToCraftSlot(
    world: VoxelWorld,
    craftGrid: MutableList<ItemStack>,
    gridIndex: Int
) {
    val equipped = world.getEquippedItem()
    if (!equipped.isEmpty) {
        if (craftGrid[gridIndex].isEmpty) {
            craftGrid[gridIndex] = ItemStack(equipped.item, 1)
            equipped.count--
            if (equipped.count <= 0) world.hotbar[world.selectedHotbarIndex] = ItemStack.EMPTY
            world.soundEngine.playDig()
        } else if (craftGrid[gridIndex].item.id == equipped.item.id) {
            craftGrid[gridIndex].count++
            equipped.count--
            if (equipped.count <= 0) world.hotbar[world.selectedHotbarIndex] = ItemStack.EMPTY
            world.soundEngine.playDig()
        }
    } else if (!craftGrid[gridIndex].isEmpty) {
        // Return to inventory
        world.addItemToInventory(craftGrid[gridIndex])
        craftGrid[gridIndex] = ItemStack.EMPTY
    }
}

private fun handleInventorySlotClick(
    world: VoxelWorld,
    clickedIndex: Int,
    isHotbar: Boolean,
    selectedIndex: Int,
    isHotbarSelected: Boolean,
    onSelectionChange: (Int, Boolean) -> Unit
) {
    if (selectedIndex == -1) {
        // First selection
        val item = if (isHotbar) world.hotbar[clickedIndex] else world.inventory[clickedIndex]
        if (!item.isEmpty) {
            // Check if wearable armor item
            if (item.item.category == ItemCategory.ARMOR) {
                val armorSlot = when {
                    item.item.id.contains("helmet") -> 0
                    item.item.id.contains("chestplate") -> 1
                    item.item.id.contains("leggings") -> 2
                    item.item.id.contains("boots") -> 3
                    else -> -1
                }
                if (armorSlot != -1 && world.armor[armorSlot].isEmpty) {
                    world.armor[armorSlot] = item.copy()
                    if (isHotbar) world.hotbar[clickedIndex] = ItemStack.EMPTY else world.inventory[clickedIndex] = ItemStack.EMPTY
                    world.soundEngine.playPlace()
                    return
                }
            }

            onSelectionChange(clickedIndex, isHotbar)
            world.soundEngine.playDig()
        }
    } else {
        // Swap or Merge
        val sourceStack = if (isHotbarSelected) world.hotbar[selectedIndex] else world.inventory[selectedIndex]
        val targetStack = if (isHotbar) world.hotbar[clickedIndex] else world.inventory[clickedIndex]

        if (selectedIndex == clickedIndex && isHotbarSelected == isHotbar) {
            // Deselect
            onSelectionChange(-1, false)
        } else if (sourceStack.item.id == targetStack.item.id && !sourceStack.isEmpty) {
            // Merge stacks
            val available = targetStack.item.maxStack - targetStack.count
            val move = minOf(available, sourceStack.count)
            targetStack.count += move
            sourceStack.count -= move
            if (sourceStack.count <= 0) {
                if (isHotbarSelected) world.hotbar[selectedIndex] = ItemStack.EMPTY else world.inventory[selectedIndex] = ItemStack.EMPTY
            }
            onSelectionChange(-1, false)
            world.soundEngine.playPlace()
        } else {
            // Swap stacks
            if (isHotbarSelected) world.hotbar[selectedIndex] = targetStack else world.inventory[selectedIndex] = targetStack
            if (isHotbar) world.hotbar[clickedIndex] = sourceStack else world.inventory[clickedIndex] = sourceStack
            onSelectionChange(-1, false)
            world.soundEngine.playPlace()
        }
    }
}

@Composable
fun ItemSlotBox(
    stack: ItemStack,
    isSelected: Boolean,
    placeholder: String = "",
    borderColor: Color = if (isSelected) Color(0xFFFFD54F) else Color(0x44FFFFFF),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF455A64) else Color(0xFF1E272C))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!stack.isEmpty) {
            Text(text = stack.item.iconSymbol, fontSize = 16.sp)
            if (stack.count > 1) {
                Text(
                    text = "${stack.count}",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(1.dp)
                )
            }
        } else if (placeholder.isNotEmpty()) {
            Text(text = placeholder, fontSize = 14.sp, color = Color(0x66FFFFFF))
        }
    }
}
