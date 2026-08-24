package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay

@Composable
fun FurnaceDialog(
    world: VoxelWorld,
    onDismiss: () -> Unit
) {
    var inputSlot by remember { mutableStateOf(ItemStack(ItemRegistry.IRON_ORE, 4)) }
    var fuelSlot by remember { mutableStateOf(ItemStack(ItemRegistry.COAL, 4)) }
    var outputSlot by remember { mutableStateOf(ItemStack.EMPTY) }

    var burnTimeRemaining by remember { mutableFloatStateOf(0f) }
    var maxBurnTime by remember { mutableFloatStateOf(10f) }
    var cookProgress by remember { mutableFloatStateOf(0f) }
    val cookTimeRequired = 3.0f

    // Furnace Smelting Loop
    LaunchedEffect(Unit) {
        while (true) {
            val recipe = RecipeRegistry.findSmeltingRecipe(inputSlot.item.id)

            if (recipe != null && inputSlot.count > 0) {
                // Check if fuel is burning
                if (burnTimeRemaining <= 0f) {
                    val fuelVal = RecipeRegistry.getFuelBurnTime(fuelSlot.item.id)
                    if (fuelVal > 0 && fuelSlot.count > 0) {
                        fuelSlot.count--
                        if (fuelSlot.count <= 0) fuelSlot = ItemStack.EMPTY
                        burnTimeRemaining = fuelVal
                        maxBurnTime = fuelVal
                    }
                }

                if (burnTimeRemaining > 0f) {
                    cookProgress += 0.1f / cookTimeRequired
                    burnTimeRemaining -= 0.1f

                    if (cookProgress >= 1.0f) {
                        cookProgress = 0f
                        // Output result
                        if (outputSlot.isEmpty) {
                            outputSlot = recipe.result.copy()
                        } else if (outputSlot.item.id == recipe.result.item.id) {
                            outputSlot.count += recipe.result.count
                        }
                        inputSlot.count--
                        if (inputSlot.count <= 0) inputSlot = ItemStack.EMPTY
                        world.soundEngine.playDig()
                    }
                } else {
                    cookProgress = 0f
                }
            } else {
                cookProgress = 0f
                if (burnTimeRemaining > 0f) burnTimeRemaining -= 0.1f
            }

            delay(100)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF263238),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF455A64)),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥 Smelting Furnace",
                        color = Color(0xFFFFD54F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).testTag("close_furnace_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Input + Fuel
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Input", color = Color(0xFF90CAF9), fontSize = 11.sp)
                        ItemSlotBox(inputSlot, false) {
                            val equipped = world.getEquippedItem()
                            if (!equipped.isEmpty) {
                                inputSlot = equipped.copy()
                                world.hotbar[world.selectedHotbarIndex] = ItemStack.EMPTY
                            } else if (!inputSlot.isEmpty) {
                                world.addItemToInventory(inputSlot)
                                inputSlot = ItemStack.EMPTY
                            }
                        }

                        // Fire Animation Icon
                        Text(
                            text = if (burnTimeRemaining > 0) "🔥" else "🕯️",
                            fontSize = 20.sp
                        )

                        Text("Fuel", color = Color(0xFFFFB74D), fontSize = 11.sp)
                        ItemSlotBox(fuelSlot, false) {
                            val equipped = world.getEquippedItem()
                            if (!equipped.isEmpty) {
                                fuelSlot = equipped.copy()
                                world.hotbar[world.selectedHotbarIndex] = ItemStack.EMPTY
                            } else if (!fuelSlot.isEmpty) {
                                world.addItemToInventory(fuelSlot)
                                fuelSlot = ItemStack.EMPTY
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Center Column: Progress Arrow
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Smelting", color = Color(0xFF81C784), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { cookProgress },
                            modifier = Modifier.width(48.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFFF9800),
                            trackColor = Color(0xFF37474F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("➡️", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    // Right Column: Output Result
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Result", color = Color(0xFF76FF03), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(18.dp))
                        ItemSlotBox(
                            stack = outputSlot,
                            isSelected = !outputSlot.isEmpty,
                            borderColor = Color(0xFF76FF03),
                            onClick = {
                                if (!outputSlot.isEmpty) {
                                    world.addItemToInventory(outputSlot.copy())
                                    world.soundEngine.playCraftSuccess()
                                    outputSlot = ItemStack.EMPTY
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
