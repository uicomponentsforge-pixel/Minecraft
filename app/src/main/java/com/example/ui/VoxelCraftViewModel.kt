package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.GameMode
import com.example.engine.VoxelWorld
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    WORLD_SELECT,
    IN_GAME,
    MULTIPLAYER_LOBBY,
    MOD_MANAGER
}

class VoxelCraftViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = WorldRepository(database.worldDao())

    private val _currentScreen = MutableStateFlow(AppScreen.WORLD_SELECT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _worldsList = MutableStateFlow<List<WorldEntity>>(emptyList())
    val worldsList: StateFlow<List<WorldEntity>> = _worldsList.asStateFlow()

    private val _modsList = MutableStateFlow<List<ModEntity>>(emptyList())
    val modsList: StateFlow<List<ModEntity>> = _modsList.asStateFlow()

    var activeWorld: VoxelWorld? = null
        private set
    var activeWorldEntity: WorldEntity? = null
        private set
    var activeWorldName: String = "Voxel World"
        private set

    lateinit var multiplayerManager: MultiplayerManager

    // Dialog flags
    var isInventoryOpen = MutableStateFlow(false)
    var isCraftingOpen = MutableStateFlow(false)
    var isFurnaceOpen = MutableStateFlow(false)
    var isPauseOpen = MutableStateFlow(false)
    var isMarketplaceOpen = MutableStateFlow(false)
    var exportJsonPayload = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.allWorlds.collect { list ->
                _worldsList.value = list
                if (list.isEmpty()) {
                    // Create default sample starter survival world
                    val starterWorld = WorldEntity(
                        id = "starter_world_01",
                        name = "Oak Valley Survival",
                        seed = 84920482L,
                        gameMode = "SURVIVAL",
                        difficulty = "NORMAL",
                        dayTime = 6000f,
                        playerX = 8.5f,
                        playerY = 36f,
                        playerZ = 8.5f,
                        lastPlayedTime = System.currentTimeMillis()
                    )
                    repository.saveWorld(starterWorld.id, starterWorld.name, VoxelWorld(seed = starterWorld.seed))
                }
            }
        }

        viewModelScope.launch {
            repository.allMods.collect { list ->
                if (list.isEmpty()) {
                    val defaults = ModManager.initDefaultMods()
                    for (m in defaults) repository.insertMod(m)
                    _modsList.value = defaults
                } else {
                    _modsList.value = list
                    // Apply mods to registries
                    for (m in list) ModManager.applyMod(m)
                }
            }
        }
    }

    fun startWorld(entity: WorldEntity) {
        viewModelScope.launch {
            activeWorldEntity = entity
            activeWorldName = entity.name
            val world = repository.loadWorld(entity)
            activeWorld = world
            multiplayerManager = MultiplayerManager(world)
            _currentScreen.value = AppScreen.IN_GAME
        }
    }

    fun createNewWorld(name: String, seed: Long, gameMode: String, difficulty: String) {
        viewModelScope.launch {
            val id = "world_${UUID.randomUUID().toString().substring(0, 8)}"
            val gm = try { GameMode.valueOf(gameMode) } catch (e: Exception) { GameMode.SURVIVAL }
            val world = VoxelWorld(seed = seed, gameMode = gm)
            repository.saveWorld(id, name, world)

            val entity = WorldEntity(
                id = id,
                name = name,
                seed = seed,
                gameMode = gameMode,
                difficulty = difficulty,
                lastPlayedTime = System.currentTimeMillis()
            )
            startWorld(entity)
        }
    }

    fun saveCurrentWorld() {
        val world = activeWorld ?: return
        val entity = activeWorldEntity ?: return
        viewModelScope.launch {
            repository.saveWorld(entity.id, entity.name, world)
        }
    }

    fun saveAndQuitToMenu() {
        saveCurrentWorld()
        isPauseOpen.value = false
        isInventoryOpen.value = false
        isCraftingOpen.value = false
        isFurnaceOpen.value = false
        isMarketplaceOpen.value = false
        activeWorld = null
        activeWorldEntity = null
        _currentScreen.value = AppScreen.WORLD_SELECT
    }

    fun deleteWorld(id: String) {
        viewModelScope.launch {
            repository.deleteWorld(id)
        }
    }

    fun exportWorld(entity: WorldEntity) {
        val json = repository.exportWorldJson(entity)
        exportJsonPayload.value = json
    }

    fun importWorld(json: String) {
        viewModelScope.launch {
            try {
                val entity = repository.importWorldJson(json)
                startWorld(entity)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun toggleMod(mod: ModEntity) {
        viewModelScope.launch {
            repository.updateMod(mod)
            ModManager.applyMod(mod)
        }
    }

    fun registerCustomBlock(config: CustomBlockConfig) {
        ModManager.registerCustomUserBlock(config)
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }
}
