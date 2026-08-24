package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MarketplaceManager
import com.example.data.TradeOffer
import com.example.engine.VoxelWorld
import com.example.model.ItemRegistry

@Composable
fun MarketplaceDialog(
    world: VoxelWorld,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("ALL") }
    var statusMessage by remember { mutableStateOf("") }

    // Count player currencies
    fun countItem(id: String): Int {
        var count = 0
        for (s in world.hotbar) if (s.item.id.equals(id, ignoreCase = true)) count += s.count
        for (s in world.inventory) if (s.item.id.equals(id, ignoreCase = true)) count += s.count
        return count
    }

    val emeraldCount = countItem("emerald")
    val diamondCount = countItem("diamond")
    val goldCount = countItem("gold_ingot")
    val ironCount = countItem("iron_ingot")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E272C),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF37474F)),
            modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFFFD54F))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Global Outpost Marketplace",
                            color = Color(0xFFFFD54F),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }

                // Balance Bar
                Surface(
                    color = Color(0xFF263238),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF455A64)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("🟢 Emeralds: $emeraldCount", fontSize = 11.sp, color = Color(0xFF69F0AE), fontWeight = FontWeight.Bold)
                        Text("💎 Diamonds: $diamondCount", fontSize = 11.sp, color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold)
                        Text("🟡 Gold: $goldCount", fontSize = 11.sp, color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold)
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    Text(
                        text = statusMessage,
                        color = if (statusMessage.startsWith("Trade Success")) Color(0xFF76FF03) else Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Offers List
                val offers = MarketplaceManager.tradeOffers
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(offers) { offer ->
                        val costDef = ItemRegistry.get(offer.costItemId)
                        val rewardDef = ItemRegistry.get(offer.rewardItemId)
                        val playerHas = countItem(offer.costItemId)
                        val canAfford = playerHas >= offer.costCount || world.gameMode == com.example.engine.GameMode.CREATIVE

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF263238),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (canAfford) Color(0xFF4CAF50) else Color(0xFF455A64)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(rewardDef.iconSymbol, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = offer.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = offer.description,
                                            color = Color(0xFFB0BEC5),
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp
                                        )
                                        Text(
                                            text = "Cost: ${offer.costCount}x ${costDef.name}",
                                            color = if (canAfford) Color(0xFF81C784) else Color(0xFFFF8A80),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (MarketplaceManager.executeTrade(world, offer)) {
                                            statusMessage = "Trade Success! Received ${offer.title}"
                                        } else {
                                            statusMessage = "Not enough ${costDef.name}!"
                                        }
                                    },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        disabledContainerColor = Color(0xFF37474F)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
