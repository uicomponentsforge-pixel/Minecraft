package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VoxelCraftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF10141E)
                ) {
                    VoxelCraftApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveCurrentWorld()
    }
}

@Composable
fun VoxelCraftApp(viewModel: VoxelCraftViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val worlds by viewModel.worldsList.collectAsState()
    val mods by viewModel.modsList.collectAsState()

    val isInvOpen by viewModel.isInventoryOpen.collectAsState()
    val isCraftOpen by viewModel.isCraftingOpen.collectAsState()
    val isFurnaceOpen by viewModel.isFurnaceOpen.collectAsState()
    val isPauseOpen by viewModel.isPauseOpen.collectAsState()
    val isMarketOpen by viewModel.isMarketplaceOpen.collectAsState()
    val exportJson by viewModel.exportJsonPayload.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.WORLD_SELECT -> {
                WorldSelectScreen(
                    worlds = worlds,
                    onSelectWorld = { viewModel.startWorld(it) },
                    onCreateWorld = { name, seed, gm, diff ->
                        viewModel.createNewWorld(name, seed, gm, diff)
                    },
                    onDeleteWorld = { viewModel.deleteWorld(it) },
                    onExportWorld = { viewModel.exportWorld(it) },
                    onImportWorld = { viewModel.importWorld(it) },
                    onOpenMultiplayer = { viewModel.navigateTo(AppScreen.MULTIPLAYER_LOBBY) },
                    onOpenMods = { viewModel.navigateTo(AppScreen.MOD_MANAGER) }
                )
            }

            AppScreen.MULTIPLAYER_LOBBY -> {
                viewModel.activeWorld?.let {
                    MultiplayerScreen(
                        multiplayerManager = viewModel.multiplayerManager,
                        onBack = { viewModel.navigateTo(AppScreen.WORLD_SELECT) },
                        onStartGame = { viewModel.navigateTo(AppScreen.IN_GAME) }
                    )
                } ?: run {
                    // Fallback to world select if no world active
                    LaunchedEffect(Unit) { viewModel.navigateTo(AppScreen.WORLD_SELECT) }
                }
            }

            AppScreen.MOD_MANAGER -> {
                ModManagerScreen(
                    mods = mods,
                    onToggleMod = { viewModel.toggleMod(it) },
                    onAddCustomBlock = { viewModel.registerCustomBlock(it) },
                    onBack = { viewModel.navigateTo(AppScreen.WORLD_SELECT) }
                )
            }

            AppScreen.IN_GAME -> {
                val world = viewModel.activeWorld
                if (world != null) {
                    GameScreen(
                        world = world,
                        worldName = viewModel.activeWorldName,
                        onOpenInventory = { viewModel.isInventoryOpen.value = true },
                        onOpenCrafting = { viewModel.isCraftingOpen.value = true },
                        onOpenFurnace = { viewModel.isFurnaceOpen.value = true },
                        onOpenPause = { viewModel.isPauseOpen.value = true },
                        onOpenMarketplace = { viewModel.isMarketplaceOpen.value = true }
                    )

                    // Dialogs
                    if (isInvOpen) {
                        InventoryDialog(
                            world = world,
                            onDismiss = { viewModel.isInventoryOpen.value = false }
                        )
                    }

                    if (isCraftOpen) {
                        CraftingDialog(
                            world = world,
                            onDismiss = { viewModel.isCraftingOpen.value = false }
                        )
                    }

                    if (isFurnaceOpen) {
                        FurnaceDialog(
                            world = world,
                            onDismiss = { viewModel.isFurnaceOpen.value = false }
                        )
                    }

                    if (isPauseOpen) {
                        PauseMenuDialog(
                            world = world,
                            onResume = { viewModel.isPauseOpen.value = false },
                            onSaveAndQuit = { viewModel.saveAndQuitToMenu() }
                        )
                    }

                    if (isMarketOpen) {
                        MarketplaceDialog(
                            world = world,
                            onDismiss = { viewModel.isMarketplaceOpen.value = false }
                        )
                    }
                }
            }
        }

        // Export Sync JSON Dialog
        exportJson?.let { payload ->
            ExportSaveDialog(
                jsonPayload = payload,
                onDismiss = { viewModel.exportJsonPayload.value = null }
            )
        }
    }
}
