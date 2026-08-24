package com.example.data

import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class WorldRepository(private val dao: WorldDao) {
    val allWorlds: Flow<List<WorldEntity>> = dao.getAllWorlds()
    val allMods: Flow<List<ModEntity>> = dao.getAllMods()

    suspend fun saveWorld(worldId: String, name: String, world: VoxelWorld) {
        val hotbarJson = serializeItemArray(world.hotbar)
        val invJson = serializeItemArray(world.inventory)
        val armorJson = serializeItemArray(world.armor)
        val modBlocksJson = serializeModifiedBlocks(world.modifiedBlocks)

        val entity = WorldEntity(
            id = worldId,
            name = name,
            seed = world.seed,
            gameMode = world.gameMode.name,
            difficulty = world.difficulty.name,
            dayTime = world.timeOfDay,
            playerX = world.playerX,
            playerY = world.playerY,
            playerZ = world.playerZ,
            playerYaw = world.playerYaw,
            playerPitch = world.playerPitch,
            health = world.health,
            hunger = world.hunger,
            level = world.level,
            xp = world.xp,
            hotbarJson = hotbarJson,
            inventoryJson = invJson,
            armorJson = armorJson,
            modifiedBlocksJson = modBlocksJson,
            lastPlayedTime = System.currentTimeMillis()
        )
        dao.insertWorld(entity)
    }

    suspend fun loadWorld(entity: WorldEntity): VoxelWorld {
        val gm = try { GameMode.valueOf(entity.gameMode) } catch (e: Exception) { GameMode.SURVIVAL }
        val diff = try { Difficulty.valueOf(entity.difficulty) } catch (e: Exception) { Difficulty.NORMAL }

        val world = VoxelWorld(
            seed = entity.seed,
            gameMode = gm,
            difficulty = diff
        )

        world.timeOfDay = entity.dayTime
        world.playerX = entity.playerX
        world.playerY = entity.playerY
        world.playerZ = entity.playerZ
        world.playerYaw = entity.playerYaw
        world.playerPitch = entity.playerPitch
        world.health = entity.health
        world.hunger = entity.hunger
        world.level = entity.level
        world.xp = entity.xp

        deserializeItemArray(entity.hotbarJson, world.hotbar)
        deserializeItemArray(entity.inventoryJson, world.inventory)
        deserializeItemArray(entity.armorJson, world.armor)
        deserializeModifiedBlocks(entity.modifiedBlocksJson, world.modifiedBlocks)

        // Ensure chunks generated and re-meshed
        world.ensureChunksAroundPlayer()
        return world
    }

    suspend fun deleteWorld(id: String) {
        dao.deleteWorldById(id)
    }

    suspend fun insertMod(mod: ModEntity) {
        dao.insertMod(mod)
    }

    suspend fun updateMod(mod: ModEntity) {
        dao.updateMod(mod)
    }

    suspend fun deleteMod(mod: ModEntity) {
        dao.deleteMod(mod)
    }

    // Export World to portable JSON String for cross-platform synchronization
    fun exportWorldJson(entity: WorldEntity): String {
        val json = JSONObject()
        json.put("id", entity.id)
        json.put("name", entity.name)
        json.put("seed", entity.seed)
        json.put("gameMode", entity.gameMode)
        json.put("difficulty", entity.difficulty)
        json.put("dayTime", entity.dayTime)
        json.put("playerX", entity.playerX)
        json.put("playerY", entity.playerY)
        json.put("playerZ", entity.playerZ)
        json.put("health", entity.health)
        json.put("hunger", entity.hunger)
        json.put("level", entity.level)
        json.put("xp", entity.xp)
        json.put("hotbarJson", entity.hotbarJson)
        json.put("inventoryJson", entity.inventoryJson)
        json.put("armorJson", entity.armorJson)
        json.put("modifiedBlocksJson", entity.modifiedBlocksJson)
        json.put("exportTimestamp", System.currentTimeMillis())
        return json.toString(2)
    }

    // Import World from portable JSON String
    suspend fun importWorldJson(jsonString: String): WorldEntity {
        val json = JSONObject(jsonString)
        val entity = WorldEntity(
            id = "imported_${System.currentTimeMillis()}",
            name = json.optString("name", "Imported World") + " (Synced)",
            seed = json.optLong("seed", 12345L),
            gameMode = json.optString("gameMode", "SURVIVAL"),
            difficulty = json.optString("difficulty", "NORMAL"),
            dayTime = json.optDouble("dayTime", 6000.0).toFloat(),
            playerX = json.optDouble("playerX", 8.5).toFloat(),
            playerY = json.optDouble("playerY", 36.0).toFloat(),
            playerZ = json.optDouble("playerZ", 8.5).toFloat(),
            health = json.optDouble("health", 20.0).toFloat(),
            hunger = json.optDouble("hunger", 20.0).toFloat(),
            level = json.optInt("level", 1),
            xp = json.optInt("xp", 0),
            hotbarJson = json.optString("hotbarJson", ""),
            inventoryJson = json.optString("inventoryJson", ""),
            armorJson = json.optString("armorJson", ""),
            modifiedBlocksJson = json.optString("modifiedBlocksJson", ""),
            lastPlayedTime = System.currentTimeMillis()
        )
        dao.insertWorld(entity)
        return entity
    }

    private fun serializeItemArray(array: Array<ItemStack>): String {
        val jsonArray = JSONArray()
        for (stack in array) {
            val obj = JSONObject()
            obj.put("itemId", stack.item.id)
            obj.put("count", stack.count)
            obj.put("durability", stack.currentDurability)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun deserializeItemArray(jsonStr: String, array: Array<ItemStack>) {
        if (jsonStr.isEmpty()) return
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until minOf(array.size, jsonArray.length())) {
                val obj = jsonArray.getJSONObject(i)
                val itemId = obj.getString("itemId")
                val count = obj.getInt("count")
                val dura = obj.optInt("durability", -1)
                val def = ItemRegistry.get(itemId)
                if (def != ItemRegistry.EMPTY && count > 0) {
                    array[i] = ItemStack(def, count, dura)
                } else {
                    array[i] = ItemStack.EMPTY
                }
            }
        } catch (e: Exception) {
            // Keep existing
        }
    }

    private fun serializeModifiedBlocks(map: Map<Long, Int>): String {
        val json = JSONObject()
        for ((key, value) in map) {
            json.put(key.toString(), value)
        }
        return json.toString()
    }

    private fun deserializeModifiedBlocks(jsonStr: String, map: MutableMap<Long, Int>) {
        if (jsonStr.isEmpty()) return
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k.toLong()] = json.getInt(k)
            }
        } catch (e: Exception) {
            // ignore
        }
    }
}
