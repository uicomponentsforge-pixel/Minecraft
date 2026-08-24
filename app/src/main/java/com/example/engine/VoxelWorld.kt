package com.example.engine

import com.example.model.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.*

enum class GameMode {
    SURVIVAL,
    CREATIVE
}

enum class Difficulty {
    PEACEFUL,
    EASY,
    NORMAL,
    HARD
}

data class RaycastResult(
    val hit: Boolean,
    val blockX: Int = 0,
    val blockY: Int = 0,
    val blockZ: Int = 0,
    val faceX: Int = 0, // adjacent empty coordinate
    val faceY: Int = 0,
    val faceZ: Int = 0,
    val blockId: Int = 0,
    val normalX: Int = 0,
    val normalY: Int = 0,
    val normalZ: Int = 0,
    val distance: Float = 0f
)

class VoxelWorld(
    val seed: Long = System.currentTimeMillis(),
    var gameMode: GameMode = GameMode.SURVIVAL,
    var difficulty: Difficulty = Difficulty.NORMAL,
    val soundEngine: SoundEngine = SoundEngine()
) {
    val noise = SimplexNoise(seed)
    val chunks = ConcurrentHashMap<Long, VoxelChunk>()
    val modifiedBlocks = ConcurrentHashMap<Long, Int>() // persistent delta overrides

    // Player State
    var playerX = 8.0f
    var playerY = 36.0f
    var playerZ = 8.0f
    var playerVx = 0f
    var playerVy = 0f
    var playerVz = 0f
    var playerYaw = 0f // degrees
    var playerPitch = 0f // degrees
    var isGrounded = false
    var isSneaking = false
    var isSprinting = false
    var isInWater = false
    var fallDistance = 0f

    // Survival Stats
    var health = 20.0f
    var maxHealth = 20.0f
    var hunger = 20.0f
    var maxHunger = 20.0f
    var saturation = 5.0f
    var oxygen = 10.0f
    var maxOxygen = 10.0f
    var level = 1
    var xp = 0
    var xpForNextLevel = 100
    var hurtFlash = 0f
    var isPlayerDead = false

    // Hotbar & Inventory
    val hotbar = Array(9) { ItemStack.EMPTY }
    val inventory = Array(27) { ItemStack.EMPTY }
    var selectedHotbarIndex = 0
    val armor = Array(4) { ItemStack.EMPTY } // Head, Chest, Legs, Feet

    // Day-Night Cycle: 0 = dawn (6am), 6000 = noon (12pm), 12000 = sunset (6pm), 18000 = midnight (12am), 24000 = wrap
    var timeOfDay = 6000.0f
    val dayLengthSeconds = 300.0f // 5 minutes real-time full cycle
    var skyLight = 1.0f

    // Breaking State
    var breakingBlockX = -1
    var breakingBlockY = -1
    var breakingBlockZ = -1
    var breakProgress = 0f // 0 to 1
    var breakTimeRequired = 1.0f

    // Entities & Projectiles
    val mobs = CopyOnWriteArrayList<MobEntity>()
    val arrows = CopyOnWriteArrayList<ArrowEntity>()
    val itemDrops = CopyOnWriteArrayList<ItemDropEntity>()
    val particles = CopyOnWriteArrayList<Particle>()
    val remotePlayers = ConcurrentHashMap<String, RemotePlayer>()

    // World Parameters
    val renderDistanceChunks = 3 // 7x7 chunks around player
    private val rand = java.util.Random(seed)
    private var mobSpawnTimer = 0f
    private var hungerTickTimer = 0f
    private var regenTickTimer = 0f

    init {
        // Initialize default starter kit in inventory
        hotbar[0] = ItemStack(ItemRegistry.WOODEN_PICKAXE, 1)
        hotbar[1] = ItemStack(ItemRegistry.WOODEN_SWORD, 1)
        hotbar[2] = ItemStack(ItemRegistry.WOODEN_AXE, 1)
        hotbar[3] = ItemStack(ItemRegistry.TORCH, 16)
        hotbar[4] = ItemStack(ItemRegistry.BREAD, 8)
        hotbar[5] = ItemStack(ItemRegistry.OAK_PLANKS, 32)
        hotbar[6] = ItemStack(ItemRegistry.DIRT, 64)

        ensureChunksAroundPlayer()
        findSafeSpawn()
    }

    private fun chunkKey(cx: Int, cz: Int): Long = (cx.toLong() and 0xFFFFFFFFL) or ((cz.toLong() and 0xFFFFFFFFL) shl 32)
    private fun blockKey(x: Int, y: Int, z: Int): Long {
        return (x.toLong() and 0x3FFFFFFL) or
                ((z.toLong() and 0x3FFFFFFL) shl 26) or
                ((y.toLong() and 0x3FL) shl 52)
    }

    fun getChunk(cx: Int, cz: Int): VoxelChunk? = chunks[chunkKey(cx, cz)]

    fun getBlock(x: Int, y: Int, z: Int): Int {
        if (y !in 0 until VoxelChunk.SIZE_Y) return 0
        val key = blockKey(x, y, z)
        modifiedBlocks[key]?.let { return it }

        val cx = floor(x.toFloat() / VoxelChunk.SIZE_X).toInt()
        val cz = floor(z.toFloat() / VoxelChunk.SIZE_Z).toInt()
        val chunk = getChunk(cx, cz) ?: return 0

        val localX = ((x % VoxelChunk.SIZE_X) + VoxelChunk.SIZE_X) % VoxelChunk.SIZE_X
        val localZ = ((z % VoxelChunk.SIZE_Z) + VoxelChunk.SIZE_Z) % VoxelChunk.SIZE_Z
        return chunk.getBlock(localX, y, localZ)
    }

    fun setBlock(x: Int, y: Int, z: Int, blockId: Int, updateMesh: Boolean = true) {
        if (y !in 0 until VoxelChunk.SIZE_Y) return
        val key = blockKey(x, y, z)
        modifiedBlocks[key] = blockId

        val cx = floor(x.toFloat() / VoxelChunk.SIZE_X).toInt()
        val cz = floor(z.toFloat() / VoxelChunk.SIZE_Z).toInt()
        val chunk = chunks.getOrPut(chunkKey(cx, cz)) {
            val c = VoxelChunk(cx, cz)
            generateChunkTerrain(c)
            c
        }

        val localX = ((x % VoxelChunk.SIZE_X) + VoxelChunk.SIZE_X) % VoxelChunk.SIZE_X
        val localZ = ((z % VoxelChunk.SIZE_Z) + VoxelChunk.SIZE_Z) % VoxelChunk.SIZE_Z
        chunk.setBlock(localX, y, localZ, blockId)

        if (updateMesh) {
            chunk.isDirty = true
            // Also dirty neighboring chunks if placed on chunk boundary
            if (localX == 0) getChunk(cx - 1, cz)?.isDirty = true
            if (localX == VoxelChunk.SIZE_X - 1) getChunk(cx + 1, cz)?.isDirty = true
            if (localZ == 0) getChunk(cx, cz - 1)?.isDirty = true
            if (localZ == VoxelChunk.SIZE_Z - 1) getChunk(cx, cz + 1)?.isDirty = true
        }
    }

    fun getLight(x: Int, y: Int, z: Int): Int {
        if (y >= VoxelChunk.SIZE_Y) return 15
        if (y < 0) return 0
        val cx = floor(x.toFloat() / VoxelChunk.SIZE_X).toInt()
        val cz = floor(z.toFloat() / VoxelChunk.SIZE_Z).toInt()
        val chunk = getChunk(cx, cz) ?: return 15
        val localX = ((x % VoxelChunk.SIZE_X) + VoxelChunk.SIZE_X) % VoxelChunk.SIZE_X
        val localZ = ((z % VoxelChunk.SIZE_Z) + VoxelChunk.SIZE_Z) % VoxelChunk.SIZE_Z
        return chunk.getLight(localX, y, localZ)
    }

    fun getHighestBlockY(x: Int, z: Int): Int {
        for (y in (VoxelChunk.SIZE_Y - 1) downTo 0) {
            val b = getBlock(x, y, z)
            if (b != 0 && b != BlockRegistry.WATER.id && b != BlockRegistry.OAK_LEAVES.id) {
                return y
            }
        }
        return 20
    }

    private fun findSafeSpawn() {
        val groundY = getHighestBlockY(8, 8)
        playerX = 8.5f
        playerY = (groundY + 2).toFloat()
        playerZ = 8.5f
    }

    fun ensureChunksAroundPlayer() {
        val playerChunkX = floor(playerX / VoxelChunk.SIZE_X).toInt()
        val playerChunkZ = floor(playerZ / VoxelChunk.SIZE_Z).toInt()

        for (dx in -renderDistanceChunks..renderDistanceChunks) {
            for (dz in -renderDistanceChunks..renderDistanceChunks) {
                val cx = playerChunkX + dx
                val cz = playerChunkZ + dz
                val key = chunkKey(cx, cz)
                if (!chunks.containsKey(key)) {
                    val chunk = VoxelChunk(cx, cz)
                    generateChunkTerrain(chunk)
                    chunks[key] = chunk
                }
            }
        }
    }

    private fun generateChunkTerrain(chunk: VoxelChunk) {
        val worldOffsetX = chunk.chunkX * VoxelChunk.SIZE_X
        val worldOffsetZ = chunk.chunkZ * VoxelChunk.SIZE_Z
        val waterLevel = 24

        for (lz in 0 until VoxelChunk.SIZE_Z) {
            for (lx in 0 until VoxelChunk.SIZE_X) {
                val wx = worldOffsetX + lx
                val wz = worldOffsetZ + lz

                // Multi-octave elevation
                val base = noise.fractal2D(wx * 0.015f, wz * 0.015f, octaves = 3)
                val mountain = noise.fractal2D(wx * 0.04f, wz * 0.04f, octaves = 2).coerceAtLeast(0f).pow(1.8f)
                val height = (28 + base * 10 + mountain * 14).toInt().coerceIn(4, 58)

                // Fill column
                for (y in 0 until VoxelChunk.SIZE_Y) {
                    val block = when {
                        y == 0 -> BlockRegistry.BEDROCK.id
                        y < height - 4 -> {
                            // Cave 3D noise check
                            val caveNoise = noise.eval3D(wx * 0.06f, y * 0.08f, wz * 0.06f)
                            if (caveNoise > 0.45f && y > 3) {
                                0 // Cave air
                            } else {
                                // Ore veins
                                val oreNoise = noise.eval3D(wx * 0.12f, y * 0.12f, wz * 0.12f)
                                when {
                                    y < 12 && oreNoise > 0.55f -> BlockRegistry.DIAMOND_ORE.id
                                    y < 22 && oreNoise > 0.48f -> BlockRegistry.GOLD_ORE.id
                                    y < 35 && oreNoise > 0.42f -> BlockRegistry.IRON_ORE.id
                                    oreNoise > 0.38f -> BlockRegistry.COAL_ORE.id
                                    else -> BlockRegistry.STONE.id
                                }
                            }
                        }
                        y < height -> {
                            if (height <= waterLevel + 1) BlockRegistry.SAND.id else BlockRegistry.DIRT.id
                        }
                        y == height -> {
                            if (height < waterLevel) {
                                BlockRegistry.DIRT.id
                            } else if (height <= waterLevel + 1) {
                                BlockRegistry.SAND.id
                            } else {
                                BlockRegistry.GRASS.id
                            }
                        }
                        y <= waterLevel -> BlockRegistry.WATER.id
                        else -> 0
                    }
                    chunk.setBlock(lx, y, lz, block)
                }

                // Surface decorations (Trees, Flowers)
                if (height > waterLevel + 1 && chunk.getBlock(lx, height, lz) == BlockRegistry.GRASS.id) {
                    val vegVal = noise.eval2D(wx * 0.25f, wz * 0.25f)
                    if (vegVal > 0.65f && lx in 2..13 && lz in 2..13) {
                        // Place Oak Tree
                        generateTree(chunk, lx, height + 1, lz)
                    } else if (vegVal > 0.45f && height + 1 < VoxelChunk.SIZE_Y) {
                        chunk.setBlock(lx, height + 1, lz, BlockRegistry.RED_FLOWER.id)
                    } else if (vegVal < -0.45f && height + 1 < VoxelChunk.SIZE_Y) {
                        chunk.setBlock(lx, height + 1, lz, BlockRegistry.YELLOW_FLOWER.id)
                    }
                }
            }
        }

        // Apply any modified block delta overrides
        for (y in 0 until VoxelChunk.SIZE_Y) {
            for (lz in 0 until VoxelChunk.SIZE_Z) {
                for (lx in 0 until VoxelChunk.SIZE_X) {
                    val key = blockKey(worldOffsetX + lx, y, worldOffsetZ + lz)
                    modifiedBlocks[key]?.let { overrideId ->
                        chunk.setBlock(lx, y, lz, overrideId)
                    }
                }
            }
        }

        chunk.isGenerated = true
        chunk.isDirty = true
    }

    private fun generateTree(chunk: VoxelChunk, lx: Int, startY: Int, lz: Int) {
        val trunkHeight = 4 + (rand.nextInt(2))
        if (startY + trunkHeight + 2 >= VoxelChunk.SIZE_Y) return

        // Trunk
        for (dy in 0 until trunkHeight) {
            chunk.setBlock(lx, startY + dy, lz, BlockRegistry.OAK_LOG.id)
        }

        // Leaves canopy
        val leafBottom = startY + trunkHeight - 2
        val leafTop = startY + trunkHeight + 1
        for (ly in leafBottom..leafTop) {
            val radius = if (ly >= leafTop) 1 else 2
            for (ox in -radius..radius) {
                for (oz in -radius..radius) {
                    if (abs(ox) == radius && abs(oz) == radius && rand.nextBoolean()) continue
                    val tx = lx + ox
                    val tz = lz + oz
                    if (tx in 0 until VoxelChunk.SIZE_X && tz in 0 until VoxelChunk.SIZE_Z) {
                        if (chunk.getBlock(tx, ly, tz) == 0) {
                            chunk.setBlock(tx, ly, tz, BlockRegistry.OAK_LEAVES.id)
                        }
                    }
                }
            }
        }
    }

    /**
     * Fast Voxel Traversal Raycasting (Amanatides & Woo DDA)
     */
    fun raycast(
        startX: Float, startY: Float, startZ: Float,
        dirX: Float, dirY: Float, dirZ: Float,
        maxDistance: Float = 5.5f
    ): RaycastResult {
        var currentX = floor(startX).toInt()
        var currentY = floor(startY).toInt()
        var currentZ = floor(startZ).toInt()

        val stepX = if (dirX > 0) 1 else if (dirX < 0) -1 else 0
        val stepY = if (dirY > 0) 1 else if (dirY < 0) -1 else 0
        val stepZ = if (dirZ > 0) 1 else if (dirZ < 0) -1 else 0

        val deltaX = if (dirX != 0f) abs(1f / dirX) else Float.MAX_VALUE
        val deltaY = if (dirY != 0f) abs(1f / dirY) else Float.MAX_VALUE
        val deltaZ = if (dirZ != 0f) abs(1f / dirZ) else Float.MAX_VALUE

        var nextTMaxX = if (dirX > 0) (currentX + 1 - startX) * deltaX else (startX - currentX) * deltaX
        var nextTMaxY = if (dirY > 0) (currentY + 1 - startY) * deltaY else (startY - currentY) * deltaY
        var nextTMaxZ = if (dirZ > 0) (currentZ + 1 - startZ) * deltaZ else (startZ - currentZ) * deltaZ

        var normalX = 0
        var normalY = 0
        var normalZ = 0
        var t = 0f

        while (t <= maxDistance) {
            val blockId = getBlock(currentX, currentY, currentZ)
            if (blockId != 0 && blockId != BlockRegistry.WATER.id) {
                return RaycastResult(
                    hit = true,
                    blockX = currentX,
                    blockY = currentY,
                    blockZ = currentZ,
                    faceX = currentX + normalX,
                    faceY = currentY + normalY,
                    faceZ = currentZ + normalZ,
                    blockId = blockId,
                    normalX = normalX,
                    normalY = normalY,
                    normalZ = normalZ,
                    distance = t
                )
            }

            if (nextTMaxX < nextTMaxY) {
                if (nextTMaxX < nextTMaxZ) {
                    currentX += stepX
                    t = nextTMaxX
                    nextTMaxX += deltaX
                    normalX = -stepX; normalY = 0; normalZ = 0
                } else {
                    currentZ += stepZ
                    t = nextTMaxZ
                    nextTMaxZ += deltaZ
                    normalX = 0; normalY = 0; normalZ = -stepZ
                }
            } else {
                if (nextTMaxY < nextTMaxZ) {
                    currentY += stepY
                    t = nextTMaxY
                    nextTMaxY += deltaY
                    normalX = 0; normalY = -stepY; normalZ = 0
                } else {
                    currentZ += stepZ
                    t = nextTMaxZ
                    nextTMaxZ += deltaZ
                    normalX = 0; normalY = 0; normalZ = -stepZ
                }
            }
        }

        return RaycastResult(hit = false)
    }

    /**
     * Start / update breaking block with equipped tool bonus
     */
    fun updateBlockBreaking(dt: Float, ray: RaycastResult): Boolean {
        if (!ray.hit) {
            resetBlockBreaking()
            return false
        }

        if (breakingBlockX != ray.blockX || breakingBlockY != ray.blockY || breakingBlockZ != ray.blockZ) {
            breakingBlockX = ray.blockX
            breakingBlockY = ray.blockY
            breakingBlockZ = ray.blockZ
            breakProgress = 0f

            val block = BlockRegistry.get(ray.blockId)
            val equipped = getEquippedItem()
            var speed = 1.0f
            if (equipped.item.toolType == block.preferredTool && block.preferredTool != ToolType.NONE) {
                speed = equipped.item.miningSpeed
            }
            if (gameMode == GameMode.CREATIVE) {
                speed = 100.0f
            }
            breakTimeRequired = (block.hardness / speed).coerceAtLeast(0.05f)
        }

        breakProgress += dt / breakTimeRequired
        soundEngine.playDig()

        // Spawn digging particles
        val block = BlockRegistry.get(ray.blockId)
        spawnBlockParticles(ray.blockX + 0.5f, ray.blockY + 0.5f, ray.blockZ + 0.5f, block.sideColor, 2)

        if (breakProgress >= 1.0f) {
            breakBlock(ray.blockX, ray.blockY, ray.blockZ, ray.blockId)
            resetBlockBreaking()
            return true
        }
        return false
    }

    fun resetBlockBreaking() {
        breakingBlockX = -1
        breakingBlockY = -1
        breakingBlockZ = -1
        breakProgress = 0f
    }

    fun breakBlock(x: Int, y: Int, z: Int, blockId: Int) {
        val block = BlockRegistry.get(blockId)
        setBlock(x, y, z, 0)
        soundEngine.playBreak()
        spawnBlockParticles(x + 0.5f, y + 0.5f, z + 0.5f, block.sideColor, 12)

        if (gameMode == GameMode.SURVIVAL) {
            // Drop item
            if (block.dropItemId.isNotEmpty()) {
                val dropItem = ItemRegistry.get(block.dropItemId)
                if (dropItem != ItemRegistry.EMPTY) {
                    spawnItemDrop(x + 0.5f, y + 0.5f, z + 0.5f, ItemStack(dropItem, block.dropCount))
                }
            }

            // Damage equipped tool
            val equipped = getEquippedItem()
            if (equipped.item.durability > 0) {
                equipped.currentDurability--
                if (equipped.currentDurability <= 0) {
                    hotbar[selectedHotbarIndex] = ItemStack.EMPTY
                    soundEngine.playBreak()
                }
            }
        }
    }

    fun placeBlock(ray: RaycastResult): Boolean {
        if (!ray.hit) return false
        val equipped = getEquippedItem()
        val blockId = equipped.item.blockId ?: return false

        val px = ray.faceX
        val py = ray.faceY
        val pz = ray.faceZ

        // Check if player intersects placing block
        val playerMinX = playerX - 0.3f
        val playerMaxX = playerX + 0.3f
        val playerMinY = playerY
        val playerMaxY = playerY + 1.8f
        val playerMinZ = playerZ - 0.3f
        val playerMaxZ = playerZ + 0.3f

        if (px + 1 > playerMinX && px < playerMaxX &&
            py + 1 > playerMinY && py < playerMaxY &&
            pz + 1 > playerMinZ && pz < playerMaxZ) {
            return false // Player inside block
        }

        setBlock(px, py, pz, blockId)
        soundEngine.playPlace()

        if (gameMode == GameMode.SURVIVAL) {
            equipped.count--
            if (equipped.count <= 0) {
                hotbar[selectedHotbarIndex] = ItemStack.EMPTY
            }
        }
        return true
    }

    fun getEquippedItem(): ItemStack = hotbar[selectedHotbarIndex]

    fun eatFood(): Boolean {
        val equipped = getEquippedItem()
        if (equipped.item.category != ItemCategory.FOOD || equipped.item.foodHeal <= 0) return false
        if (hunger >= maxHunger && health >= maxHealth && equipped.item != ItemRegistry.GOLDEN_APPLE) return false

        hunger = (hunger + equipped.item.foodHeal).coerceAtMost(maxHunger)
        saturation += equipped.item.foodSaturation
        if (equipped.item == ItemRegistry.GOLDEN_APPLE) {
            health = (health + 4).coerceAtMost(maxHealth)
        }

        soundEngine.playEat()
        equipped.count--
        if (equipped.count <= 0) {
            hotbar[selectedHotbarIndex] = ItemStack.EMPTY
        }
        return true
    }

    fun attackMob(mob: MobEntity) {
        val equipped = getEquippedItem()
        val damage = if (gameMode == GameMode.CREATIVE) 50f else equipped.item.attackDamage
        mob.health -= damage
        mob.hurtTime = 0.3f
        soundEngine.playHurt()
        soundEngine.playSwordSwing()

        // Knockback
        val rad = Math.toRadians(playerYaw.toDouble())
        mob.vx += -sin(rad).toFloat() * 4f
        mob.vy += 2.5f
        mob.vz += cos(rad).toFloat() * 4f

        spawnBlockParticles(mob.x, mob.y + 0.8f, mob.z, 0xFFE53935.toInt(), 8)

        if (mob.health <= 0f) {
            mob.isDead = true
            soundEngine.playBreak()
            addXp(15)

            // Mob drops
            when (mob.type) {
                EntityType.CREEPER -> spawnItemDrop(mob.x, mob.y, mob.z, ItemStack(ItemRegistry.GUNPOWDER, 1 + rand.nextInt(2)))
                EntityType.SKELETON -> {
                    spawnItemDrop(mob.x, mob.y, mob.z, ItemStack(ItemRegistry.BONE, 1 + rand.nextInt(2)))
                    spawnItemDrop(mob.x, mob.y, mob.z, ItemStack(ItemRegistry.ARROW, 1 + rand.nextInt(3)))
                }
                EntityType.ZOMBIE -> spawnItemDrop(mob.x, mob.y, mob.z, ItemStack(ItemRegistry.DIRT, 1))
                EntityType.PIG -> spawnItemDrop(mob.x, mob.y, mob.z, ItemStack(ItemRegistry.RAW_PORKCHOP, 1 + rand.nextInt(2)))
                EntityType.COW -> spawnItemDrop(mob.x, mob.y, mob.z, ItemStack(ItemRegistry.RAW_PORKCHOP, 1 + rand.nextInt(2)))
                else -> {}
            }
        }
    }

    fun shootBow(): Boolean {
        val equipped = getEquippedItem()
        if (equipped.item.id != ItemRegistry.BOW.id) return false

        // Check if player has arrows
        var arrowSlot = -1
        for (i in hotbar.indices) {
            if (hotbar[i].item.id == ItemRegistry.ARROW.id && hotbar[i].count > 0) {
                arrowSlot = i; break
            }
        }
        if (arrowSlot == -1) {
            for (i in inventory.indices) {
                if (inventory[i].item.id == ItemRegistry.ARROW.id && inventory[i].count > 0) {
                    arrowSlot = i + 9; break
                }
            }
        }

        if (gameMode == GameMode.SURVIVAL && arrowSlot == -1) return false

        if (gameMode == GameMode.SURVIVAL && arrowSlot != -1) {
            if (arrowSlot < 9) {
                hotbar[arrowSlot].count--
                if (hotbar[arrowSlot].count <= 0) hotbar[arrowSlot] = ItemStack.EMPTY
            } else {
                inventory[arrowSlot - 9].count--
                if (inventory[arrowSlot - 9].count <= 0) inventory[arrowSlot - 9] = ItemStack.EMPTY
            }
        }

        val yawRad = Math.toRadians(playerYaw.toDouble())
        val pitchRad = Math.toRadians(playerPitch.toDouble())
        val speed = 18.0f

        val vx = (-sin(yawRad) * cos(pitchRad) * speed).toFloat()
        val vy = (-sin(pitchRad) * speed).toFloat()
        val vz = (cos(yawRad) * cos(pitchRad) * speed).toFloat()

        val arrow = ArrowEntity(
            id = "arrow_${System.currentTimeMillis()}",
            x = playerX,
            y = playerY + 1.4f,
            z = playerZ,
            vx = vx,
            vy = vy,
            vz = vz,
            shooterId = "player",
            damage = 6.0f
        )
        arrows.add(arrow)
        soundEngine.playBowShoot()
        return true
    }

    fun addXp(amount: Int) {
        xp += amount
        while (xp >= xpForNextLevel) {
            xp -= xpForNextLevel
            level++
            xpForNextLevel = (xpForNextLevel * 1.25f).toInt()
            soundEngine.playLevelUp()
        }
    }

    fun spawnItemDrop(x: Float, y: Float, z: Float, stack: ItemStack) {
        val drop = ItemDropEntity("drop_${System.currentTimeMillis()}_${rand.nextInt(1000)}", x, y, z, stack)
        drop.vx = (rand.nextFloat() - 0.5f) * 2f
        drop.vy = 3f + rand.nextFloat() * 2f
        drop.vz = (rand.nextFloat() - 0.5f) * 2f
        itemDrops.add(drop)
    }

    fun spawnBlockParticles(x: Float, y: Float, z: Float, color: Int, count: Int) {
        for (i in 0 until count) {
            val p = Particle(
                x = x + (rand.nextFloat() - 0.5f) * 0.6f,
                y = y + (rand.nextFloat() - 0.5f) * 0.6f,
                z = z + (rand.nextFloat() - 0.5f) * 0.6f,
                vx = (rand.nextFloat() - 0.5f) * 4f,
                vy = rand.nextFloat() * 3f + 1f,
                vz = (rand.nextFloat() - 0.5f) * 4f,
                color = color,
                size = 0.08f + rand.nextFloat() * 0.06f,
                maxLife = 0.5f + rand.nextFloat() * 0.4f
            )
            particles.add(p)
        }
    }

    fun explode(ex: Float, ey: Float, ez: Float, power: Float = 3.0f) {
        soundEngine.playExplosion()
        val radius = power.toInt()

        // Destroy blocks in spherical radius
        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    val distSq = dx * dx + dy * dy + dz * dz
                    if (distSq <= radius * radius) {
                        val bx = (ex + dx).toInt()
                        val by = (ey + dy).toInt()
                        val bz = (ez + dz).toInt()
                        val b = getBlock(bx, by, bz)
                        if (b != 0 && b != BlockRegistry.BEDROCK.id && b != BlockRegistry.OBSIDIAN.id) {
                            setBlock(bx, by, bz, 0)
                        }
                    }
                }
            }
        }

        // Damage player
        val distToPlayer = sqrt((playerX - ex).pow(2) + (playerY - ey).pow(2) + (playerZ - ez).pow(2))
        if (distToPlayer < power * 2.5f) {
            val dmg = ((1.0f - distToPlayer / (power * 2.5f)) * 24.0f).coerceAtLeast(2.0f)
            damagePlayer(dmg)
            val push = (1.0f - distToPlayer / (power * 2.5f)) * 12.0f
            playerVx += (playerX - ex) / distToPlayer * push
            playerVy += 6.0f
            playerVz += (playerZ - ez) / distToPlayer * push
        }

        // Spawn explosion particles
        for (i in 0 until 35) {
            val color = if (i % 2 == 0) 0xFFFF5722.toInt() else 0xFF212121.toInt()
            particles.add(
                Particle(
                    x = ex, y = ey, z = ez,
                    vx = (rand.nextFloat() - 0.5f) * 10f,
                    vy = (rand.nextFloat() - 0.5f) * 10f + 3f,
                    vz = (rand.nextFloat() - 0.5f) * 10f,
                    color = color,
                    size = 0.2f + rand.nextFloat() * 0.2f,
                    maxLife = 0.8f + rand.nextFloat() * 0.5f
                )
            )
        }
    }

    fun damagePlayer(amount: Float) {
        if (gameMode == GameMode.CREATIVE || isPlayerDead) return

        // Calculate armor defense
        var totalDefense = 0
        for (a in armor) {
            if (!a.isEmpty) totalDefense += a.item.armorDefense
        }
        val reducedDamage = amount * (1.0f - (totalDefense * 0.035f)).coerceAtLeast(0.15f)

        health = (health - reducedDamage).coerceAtLeast(0f)
        hurtFlash = 0.4f
        soundEngine.playHurt()

        if (health <= 0f) {
            isPlayerDead = true
            soundEngine.playBreak()
        }
    }

    fun respawn() {
        findSafeSpawn()
        health = maxHealth
        hunger = maxHunger
        oxygen = maxOxygen
        isPlayerDead = false
        playerVx = 0f; playerVy = 0f; playerVz = 0f
    }

    /**
     * Main Physics & AI tick loop
     */
    fun update(dt: Float) {
        // Clamp dt to avoid tunneling
        val stepDt = dt.coerceIn(0.001f, 0.05f)

        // Day-Night progression
        timeOfDay = (timeOfDay + (stepDt / dayLengthSeconds) * 24000.0f) % 24000.0f
        // Sky light: 1.0 at day (4000-8000), falls to 0.15 at night (14000-22000)
        skyLight = when {
            timeOfDay in 4000.0f..8000.0f -> 1.0f
            timeOfDay in 14000.0f..22000.0f -> 0.18f
            timeOfDay in 8000.0f..14000.0f -> 1.0f - ((timeOfDay - 8000.0f) / 6000.0f) * 0.82f
            else -> 0.18f + (timeOfDay / 4000.0f) * 0.82f
        }

        updatePlayerPhysics(stepDt)
        updateSurvivalStats(stepDt)
        updateMobs(stepDt)
        updateArrows(stepDt)
        updateItemDrops(stepDt)
        updateParticles(stepDt)
    }

    private fun updatePlayerPhysics(dt: Float) {
        if (isPlayerDead) return

        // In water check
        val blockAtFeet = getBlock(playerX.toInt(), playerY.toInt(), playerZ.toInt())
        val blockAtHead = getBlock(playerX.toInt(), (playerY + 1.5f).toInt(), playerZ.toInt())
        isInWater = (blockAtFeet == BlockRegistry.WATER.id || blockAtHead == BlockRegistry.WATER.id)

        // Gravity & Vertical drag
        if (isInWater) {
            playerVy = (playerVy - 9.8f * dt * 0.3f).coerceIn(-3.0f, 3.0f)
        } else {
            val gravity = 22.0f
            playerVy -= gravity * dt
            if (playerVy < -25.0f) playerVy = -25.0f
        }

        // Apply movement velocity
        val targetX = playerX + playerVx * dt
        val targetY = playerY + playerVy * dt
        val targetZ = playerZ + playerVz * dt

        // Collision detection (AABB)
        val pRadius = 0.28f
        val pHeight = 1.75f

        // Y Collision
        if (playerVy < 0) {
            val groundY = floor(targetY).toInt()
            var collided = false
            for (cx in floor(playerX - pRadius).toInt()..floor(playerX + pRadius).toInt()) {
                for (cz in floor(playerZ - pRadius).toInt()..floor(playerZ + pRadius).toInt()) {
                    val b = getBlock(cx, groundY, cz)
                    if (b != 0 && b != BlockRegistry.WATER.id && BlockRegistry.get(b).isSolid) {
                        collided = true
                        break
                    }
                }
                if (collided) break
            }

            if (collided && targetY <= groundY + 1.0f) {
                playerY = (groundY + 1.0f)
                // Fall damage
                if (fallDistance > 3.5f && !isInWater && gameMode == GameMode.SURVIVAL) {
                    val fallDmg = (fallDistance - 3.0f) * 1.5f
                    damagePlayer(fallDmg)
                }
                fallDistance = 0f
                playerVy = 0f
                isGrounded = true
            } else {
                playerY = targetY
                isGrounded = false
                if (playerVy < 0) fallDistance += abs(playerVy * dt)
            }
        } else if (playerVy > 0) {
            val ceilY = floor(targetY + pHeight).toInt()
            var collided = false
            for (cx in floor(playerX - pRadius).toInt()..floor(playerX + pRadius).toInt()) {
                for (cz in floor(playerZ - pRadius).toInt()..floor(playerZ + pRadius).toInt()) {
                    val b = getBlock(cx, ceilY, cz)
                    if (b != 0 && b != BlockRegistry.WATER.id && BlockRegistry.get(b).isSolid) {
                        collided = true; break
                    }
                }
                if (collided) break
            }
            if (collided) {
                playerY = ceilY - pHeight - 0.01f
                playerVy = 0f
            } else {
                playerY = targetY
                isGrounded = false
            }
        }

        // X Collision with auto-step
        var collidedX = false
        val checkX = if (playerVx > 0) targetX + pRadius else targetX - pRadius
        val ix = floor(checkX).toInt()
        for (y in floor(playerY + 0.1f).toInt()..floor(playerY + pHeight - 0.1f).toInt()) {
            for (cz in floor(playerZ - pRadius).toInt()..floor(playerZ + pRadius).toInt()) {
                val b = getBlock(ix, y, cz)
                if (b != 0 && b != BlockRegistry.WATER.id && BlockRegistry.get(b).isSolid) {
                    collidedX = true; break
                }
            }
            if (collidedX) break
        }

        if (!collidedX) {
            playerX = targetX
        } else {
            // Auto step 0.6 blocks
            val stepBlock = getBlock(ix, (playerY + 1).toInt(), playerZ.toInt())
            if (isGrounded && stepBlock == 0) {
                playerY += 0.5f
                playerX = targetX
            } else {
                playerVx = 0f
            }
        }

        // Z Collision with auto-step
        var collidedZ = false
        val checkZ = if (playerVz > 0) targetZ + pRadius else targetZ - pRadius
        val iz = floor(checkZ).toInt()
        for (y in floor(playerY + 0.1f).toInt()..floor(playerY + pHeight - 0.1f).toInt()) {
            for (cx in floor(playerX - pRadius).toInt()..floor(playerX + pRadius).toInt()) {
                val b = getBlock(cx, y, iz)
                if (b != 0 && b != BlockRegistry.WATER.id && BlockRegistry.get(b).isSolid) {
                    collidedZ = true; break
                }
            }
            if (collidedZ) break
        }

        if (!collidedZ) {
            playerZ = targetZ
        } else {
            val stepBlock = getBlock(playerX.toInt(), (playerY + 1).toInt(), iz)
            if (isGrounded && stepBlock == 0) {
                playerY += 0.5f
                playerZ = targetZ
            } else {
                playerVz = 0f
            }
        }

        // Ground friction
        val friction = if (isGrounded) 0.7f else 0.92f
        playerVx *= friction
        playerVz *= friction

        if (hurtFlash > 0) hurtFlash -= dt
    }

    private fun updateSurvivalStats(dt: Float) {
        if (gameMode == GameMode.CREATIVE || isPlayerDead) return

        // Drowning check
        val headBlock = getBlock(playerX.toInt(), (playerY + 1.5f).toInt(), playerZ.toInt())
        if (headBlock == BlockRegistry.WATER.id) {
            oxygen = (oxygen - dt).coerceAtLeast(0f)
            if (oxygen <= 0f) {
                damagePlayer(2f * dt)
            }
        } else {
            oxygen = (oxygen + dt * 4f).coerceAtMost(maxOxygen)
        }

        // Hunger decay
        hungerTickTimer += dt
        if (hungerTickTimer >= 4.0f) {
            hungerTickTimer = 0f
            val decay = if (isSprinting) 0.3f else 0.05f
            if (saturation > 0) {
                saturation = (saturation - decay).coerceAtLeast(0f)
            } else {
                hunger = (hunger - decay).coerceAtLeast(0f)
            }
        }

        // Health regeneration / Starvation
        regenTickTimer += dt
        if (regenTickTimer >= 3.0f) {
            regenTickTimer = 0f
            if (hunger >= 18f && health < maxHealth) {
                health = (health + 1.0f).coerceAtMost(maxHealth)
                hunger = (hunger - 0.4f).coerceAtLeast(0f)
            } else if (hunger <= 0f && difficulty != Difficulty.PEACEFUL) {
                damagePlayer(1.0f)
            }
        }
    }

    private fun updateMobs(dt: Float) {
        // Spawn mobs
        mobSpawnTimer += dt
        if (mobSpawnTimer >= 5.0f && difficulty != Difficulty.PEACEFUL && mobs.size < 12) {
            mobSpawnTimer = 0f
            val spawnAngle = rand.nextFloat() * Math.PI.toFloat() * 2f
            val spawnDist = 14f + rand.nextFloat() * 12f
            val sx = playerX + sin(spawnAngle) * spawnDist
            val sz = playerZ + cos(spawnAngle) * spawnDist
            val sy = getHighestBlockY(sx.toInt(), sz.toInt()) + 1

            if (sy in 1 until VoxelChunk.SIZE_Y - 2) {
                val isNight = skyLight < 0.35f
                val type = when {
                    isNight && rand.nextFloat() < 0.35f -> EntityType.CREEPER
                    isNight && rand.nextFloat() < 0.70f -> EntityType.SKELETON
                    isNight -> EntityType.ZOMBIE
                    rand.nextBoolean() -> EntityType.PIG
                    else -> EntityType.COW
                }
                mobs.add(MobEntity("mob_${System.currentTimeMillis()}_${rand.nextInt(1000)}", type, sx, sy.toFloat(), sz))
            }
        }

        // Update each mob
        val iter = mobs.iterator()
        while (iter.hasNext()) {
            val mob = iter.next()
            if (mob.isDead) {
                mobs.remove(mob)
                continue
            }

            if (mob.hurtTime > 0) mob.hurtTime -= dt
            mob.animTime += dt

            // Gravity
            mob.vy -= 18.0f * dt
            mob.y += mob.vy * dt
            val groundY = getHighestBlockY(mob.x.toInt(), mob.z.toInt()) + 1.0f
            if (mob.y <= groundY) {
                mob.y = groundY
                mob.vy = 0f
                mob.isGrounded = true
            }

            // Distance to player
            val dx = playerX - mob.x
            val dy = playerY - mob.y
            val dz = playerZ - mob.z
            val dist = sqrt(dx * dx + dz * dz)

            // Turn mob towards player/target
            mob.yaw = (Math.toDegrees(atan2(-dx.toDouble(), dz.toDouble()))).toFloat()

            when (mob.type) {
                EntityType.ZOMBIE -> {
                    // Burn in sunlight
                    if (skyLight > 0.75f && mob.y >= groundY - 1) {
                        mob.health -= dt * 2f
                        spawnBlockParticles(mob.x, mob.y + 1f, mob.z, 0xFFFF5722.toInt(), 1)
                    }

                    if (dist < mob.followRange && dist > 1.2f && !isPlayerDead) {
                        mob.vx = (dx / dist) * mob.moveSpeed
                        mob.vz = (dz / dist) * mob.moveSpeed
                    } else if (dist <= 1.2f && !isPlayerDead) {
                        mob.attackCooldown += dt
                        if (mob.attackCooldown >= 1.2f) {
                            mob.attackCooldown = 0f
                            damagePlayer(mob.attackDamage)
                            soundEngine.playHurt()
                        }
                    } else {
                        mob.vx *= 0.8f; mob.vz *= 0.8f
                    }
                }
                EntityType.CREEPER -> {
                    if (dist < mob.followRange && dist > 2.0f && !isPlayerDead) {
                        mob.vx = (dx / dist) * mob.moveSpeed
                        mob.vz = (dz / dist) * mob.moveSpeed
                        if (mob.state == MobState.HISSING) {
                            mob.state = MobState.CHASE
                            mob.creeperFuse = 0f
                        }
                    } else if (dist <= 2.2f && !isPlayerDead) {
                        mob.vx = 0f; mob.vz = 0f
                        if (mob.state != MobState.HISSING) {
                            mob.state = MobState.HISSING
                            soundEngine.playCreeperHiss()
                        }
                        mob.creeperFuse += dt
                        // Flash white particles
                        spawnBlockParticles(mob.x, mob.y + 1f, mob.z, 0xFFFFFFFF.toInt(), 2)

                        if (mob.creeperFuse >= 1.5f) {
                            explode(mob.x, mob.y + 0.8f, mob.z, power = 3.2f)
                            mob.isDead = true
                        }
                    } else {
                        mob.vx *= 0.8f; mob.vz *= 0.8f
                    }
                }
                EntityType.SKELETON -> {
                    if (skyLight > 0.75f && mob.y >= groundY - 1) {
                        mob.health -= dt * 2f
                        spawnBlockParticles(mob.x, mob.y + 1f, mob.z, 0xFFFF5722.toInt(), 1)
                    }

                    if (dist < mob.followRange && dist > 7.0f && !isPlayerDead) {
                        mob.vx = (dx / dist) * mob.moveSpeed
                        mob.vz = (dz / dist) * mob.moveSpeed
                    } else if (dist <= 10.0f && !isPlayerDead) {
                        mob.vx = 0f; mob.vz = 0f
                        mob.attackCooldown += dt
                        if (mob.attackCooldown >= 2.5f) {
                            mob.attackCooldown = 0f
                            // Shoot arrow at player
                            val speed = 14.0f
                            val arrowVx = (dx / dist) * speed
                            val arrowVy = (dy / dist) * speed + 2.0f
                            val arrowVz = (dz / dist) * speed
                            arrows.add(ArrowEntity("mob_arrow_${System.currentTimeMillis()}", mob.x, mob.y + 1.4f, mob.z, arrowVx, arrowVy, arrowVz, mob.id, mob.attackDamage))
                            soundEngine.playBowShoot()
                        }
                    }
                }
                EntityType.PIG, EntityType.COW -> {
                    mob.stateTimer += dt
                    if (mob.stateTimer > 4.0f) {
                        mob.stateTimer = 0f
                        val angle = rand.nextFloat() * Math.PI.toFloat() * 2f
                        mob.vx = sin(angle) * mob.moveSpeed * 0.5f
                        mob.vz = cos(angle) * mob.moveSpeed * 0.5f
                    }
                }
                else -> {}
            }

            // Apply mob displacement
            mob.x += mob.vx * dt
            mob.z += mob.vz * dt
        }
    }

    private fun updateArrows(dt: Float) {
        val iter = arrows.iterator()
        while (iter.hasNext()) {
            val arrow = iter.next()
            if (arrow.inGround) {
                arrow.groundTimer += dt
                if (arrow.groundTimer > 8.0f) arrows.remove(arrow)
                continue
            }

            arrow.vy -= 14.0f * dt
            arrow.x += arrow.vx * dt
            arrow.y += arrow.vy * dt
            arrow.z += arrow.vz * dt

            // Check hit block
            val block = getBlock(arrow.x.toInt(), arrow.y.toInt(), arrow.z.toInt())
            if (block != 0 && BlockRegistry.get(block).isSolid) {
                arrow.inGround = true
                soundEngine.playDig()
                continue
            }

            // Check hit player
            if (arrow.shooterId != "player" && !isPlayerDead) {
                val pdist = sqrt((arrow.x - playerX).pow(2) + (arrow.y - (playerY + 0.9f)).pow(2) + (arrow.z - playerZ).pow(2))
                if (pdist < 0.8f) {
                    damagePlayer(arrow.damage)
                    arrows.remove(arrow)
                    continue
                }
            }

            // Check hit mob
            for (mob in mobs) {
                if (mob.id != arrow.shooterId && !mob.isDead) {
                    val mdist = sqrt((arrow.x - mob.x).pow(2) + (arrow.y - (mob.y + 0.8f)).pow(2) + (arrow.z - mob.z).pow(2))
                    if (mdist < 0.85f) {
                        mob.health -= arrow.damage
                        mob.hurtTime = 0.3f
                        soundEngine.playHurt()
                        if (mob.health <= 0f) {
                            mob.isDead = true
                            addXp(15)
                        }
                        arrows.remove(arrow)
                        break
                    }
                }
            }
        }
    }

    private fun updateItemDrops(dt: Float) {
        val iter = itemDrops.iterator()
        while (iter.hasNext()) {
            val drop = iter.next()
            drop.age += dt
            drop.spinAngle = (drop.spinAngle + dt * 90f) % 360f
            drop.hoverOffset = sin(drop.age * 3.5f) * 0.08f

            // Gravity
            drop.vy -= 14f * dt
            drop.y += drop.vy * dt
            val groundY = getHighestBlockY(drop.x.toInt(), drop.z.toInt()) + 1.15f
            if (drop.y <= groundY) {
                drop.y = groundY
                drop.vy = 0f
            }
            drop.x += drop.vx * dt
            drop.z += drop.vz * dt
            drop.vx *= 0.85f; drop.vz *= 0.85f

            // Player pickup
            val dist = sqrt((drop.x - playerX).pow(2) + (drop.y - (playerY + 0.5f)).pow(2) + (drop.z - playerZ).pow(2))
            if (dist < 1.6f && !isPlayerDead) {
                if (addItemToInventory(drop.itemStack)) {
                    soundEngine.playDig()
                    itemDrops.remove(drop)
                }
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life += dt
            if (p.isDead) {
                particles.remove(p)
                continue
            }
            p.vy -= p.gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
        }
    }

    fun addItemToInventory(stack: ItemStack): Boolean {
        // Try hotbar stack merge
        for (i in hotbar.indices) {
            if (hotbar[i].item.id == stack.item.id && hotbar[i].count < hotbar[i].item.maxStack) {
                val available = hotbar[i].item.maxStack - hotbar[i].count
                val toAdd = min(available, stack.count)
                hotbar[i].count += toAdd
                stack.count -= toAdd
                if (stack.count <= 0) return true
            }
        }
        // Try inventory stack merge
        for (i in inventory.indices) {
            if (inventory[i].item.id == stack.item.id && inventory[i].count < inventory[i].item.maxStack) {
                val available = inventory[i].item.maxStack - inventory[i].count
                val toAdd = min(available, stack.count)
                inventory[i].count += toAdd
                stack.count -= toAdd
                if (stack.count <= 0) return true
            }
        }
        // Place in empty hotbar
        for (i in hotbar.indices) {
            if (hotbar[i].isEmpty) {
                hotbar[i] = stack.copy()
                stack.count = 0
                return true
            }
        }
        // Place in empty inventory
        for (i in inventory.indices) {
            if (inventory[i].isEmpty) {
                inventory[i] = stack.copy()
                stack.count = 0
                return true
            }
        }
        return false
    }
}
