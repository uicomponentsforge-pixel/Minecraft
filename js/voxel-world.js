/**
 * Procedural Voxel World Engine, Physics & Entities Simulation
 * Converted from VoxelWorld.kt & VoxelChunk.kt
 */

const GameMode = {
  SURVIVAL: 'SURVIVAL',
  CREATIVE: 'CREATIVE'
};

const Difficulty = {
  PEACEFUL: 'PEACEFUL',
  EASY: 'EASY',
  NORMAL: 'NORMAL',
  HARD: 'HARD'
};

const EntityType = {
  PLAYER: 'PLAYER',
  CREEPER: 'CREEPER',
  SKELETON: 'SKELETON',
  ZOMBIE: 'ZOMBIE',
  PIG: 'PIG',
  COW: 'COW'
};

class VoxelChunk {
  static SIZE_X = 16;
  static SIZE_Y = 64;
  static SIZE_Z = 16;
  static TOTAL_BLOCKS = 16 * 64 * 16;

  static DIR_VECTORS = [
    [0, 1, 0],   // Top
    [0, -1, 0],  // Bottom
    [0, 0, 1],   // North
    [0, 0, -1],  // South
    [1, 0, 0],   // East
    [-1, 0, 0]   // West
  ];

  static FACE_SHADING = [1.0, 0.5, 0.8, 0.8, 0.7, 0.7];

  constructor(chunkX, chunkZ) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;
    this.blocks = new Uint8Array(VoxelChunk.TOTAL_BLOCKS);
    this.isDirty = true;
    this.isGenerated = false;
  }

  getBlock(x, y, z) {
    if (x < 0 || x >= VoxelChunk.SIZE_X || y < 0 || y >= VoxelChunk.SIZE_Y || z < 0 || z >= VoxelChunk.SIZE_Z) return 0;
    return this.blocks[y * VoxelChunk.SIZE_X * VoxelChunk.SIZE_Z + z * VoxelChunk.SIZE_X + x];
  }

  setBlock(x, y, z, blockId) {
    if (x < 0 || x >= VoxelChunk.SIZE_X || y < 0 || y >= VoxelChunk.SIZE_Y || z < 0 || z >= VoxelChunk.SIZE_Z) return;
    this.blocks[y * VoxelChunk.SIZE_X * VoxelChunk.SIZE_Z + z * VoxelChunk.SIZE_X + x] = blockId;
    this.isDirty = true;
  }
}

class MobEntity {
  constructor(id, type, x, y, z) {
    this.id = id;
    this.type = type;
    this.x = x;
    this.y = y;
    this.z = z;
    this.vx = 0; this.vy = 0; this.vz = 0;
    this.yaw = 0;
    this.health = 20;
    this.maxHealth = 20;
    this.isDead = false;
    this.hurtTime = 0;
    this.animTime = 0;
    this.attackCooldown = 0;
    this.creeperFuse = 0;
    this.state = 'IDLE';

    switch (type) {
      case EntityType.CREEPER:
        this.moveSpeed = 2.0;
        this.attackDamage = 20;
        break;
      case EntityType.SKELETON:
        this.moveSpeed = 2.0;
        this.attackDamage = 3;
        break;
      case EntityType.ZOMBIE:
        this.moveSpeed = 2.2;
        this.attackDamage = 4;
        break;
      case EntityType.PIG:
      case EntityType.COW:
        this.moveSpeed = 1.2;
        this.attackDamage = 0;
        break;
      default:
        this.moveSpeed = 2.0;
        this.attackDamage = 2;
    }
  }
}

class ArrowEntity {
  constructor(id, x, y, z, vx, vy, vz, shooterId = 'player', damage = 6.0) {
    this.id = id;
    this.x = x; this.y = y; this.z = z;
    this.vx = vx; this.vy = vy; this.vz = vz;
    this.shooterId = shooterId;
    this.damage = damage;
    this.inGround = false;
    this.groundTimer = 0;
  }
}

class ItemDropEntity {
  constructor(id, x, y, z, itemStack) {
    this.id = id;
    this.x = x; this.y = y; this.z = z;
    this.vx = (Math.random() - 0.5) * 2;
    this.vy = 3 + Math.random() * 2;
    this.vz = (Math.random() - 0.5) * 2;
    this.itemStack = itemStack;
    this.spinAngle = 0;
    this.hoverOffset = 0;
    this.age = 0;
  }
}

class Particle {
  constructor(x, y, z, vx, vy, vz, color, size = 0.1, maxLife = 0.6) {
    this.x = x; this.y = y; this.z = z;
    this.vx = vx; this.vy = vy; this.vz = vz;
    this.color = color;
    this.size = size;
    this.life = 0;
    this.maxLife = maxLife;
    this.gravity = 14.0;
  }
  get isDead() { return this.life >= this.maxLife; }
}

class VoxelWorld {
  constructor(seed = Date.now(), gameMode = GameMode.SURVIVAL, difficulty = Difficulty.NORMAL) {
    this.seed = seed;
    this.gameMode = gameMode;
    this.difficulty = difficulty;
    this.soundEngine = new SoundEngine();
    this.noise = new SimplexNoise(seed);
    this.chunks = new Map();
    this.modifiedBlocks = new Map();

    // Player State
    this.playerX = 8.5;
    this.playerY = 36.0;
    this.playerZ = 8.5;
    this.playerVx = 0; this.playerVy = 0; this.playerVz = 0;
    this.playerYaw = 0;
    this.playerPitch = 0;
    this.isGrounded = false;
    this.isSneaking = false;
    this.isSprinting = false;
    this.isInWater = false;
    this.fallDistance = 0;

    // Survival Stats
    this.health = 20.0;
    this.maxHealth = 20.0;
    this.hunger = 20.0;
    this.maxHunger = 20.0;
    this.saturation = 5.0;
    this.oxygen = 10.0;
    this.maxOxygen = 10.0;
    this.level = 1;
    this.xp = 0;
    this.xpForNextLevel = 100;
    this.hurtFlash = 0;
    this.isPlayerDead = false;

    // Hotbar & Inventory
    this.hotbar = Array.from({ length: 9 }, () => new ItemStack(ItemRegistry.EMPTY, 0));
    this.inventory = Array.from({ length: 27 }, () => new ItemStack(ItemRegistry.EMPTY, 0));
    this.armor = Array.from({ length: 4 }, () => new ItemStack(ItemRegistry.EMPTY, 0));
    this.selectedHotbarIndex = 0;

    // Day-Night Cycle (0 = dawn, 6000 = noon, 12000 = sunset, 18000 = midnight, 24000 = wrap)
    this.timeOfDay = 6000.0;
    this.dayLengthSeconds = 300.0;
    this.skyLight = 1.0;

    // Block Breaking State
    this.breakingBlockX = -1;
    this.breakingBlockY = -1;
    this.breakingBlockZ = -1;
    this.breakProgress = 0;
    this.breakTimeRequired = 1.0;

    // Entities & Lists
    this.mobs = [];
    this.arrows = [];
    this.itemDrops = [];
    this.particles = [];
    this.remotePlayers = new Map();

    this.renderDistanceChunks = 3;
    this.mobSpawnTimer = 0;
    this.hungerTickTimer = 0;
    this.regenTickTimer = 0;

    this.initStarterKit();
    this.ensureChunksAroundPlayer();
    this.findSafeSpawn();
  }

  initStarterKit() {
    this.hotbar[0] = new ItemStack(ItemRegistry.WOODEN_PICKAXE, 1);
    this.hotbar[1] = new ItemStack(ItemRegistry.WOODEN_SWORD, 1);
    this.hotbar[2] = new ItemStack(ItemRegistry.WOODEN_AXE, 1);
    this.hotbar[3] = new ItemStack(ItemRegistry.TORCH, 16);
    this.hotbar[4] = new ItemStack(ItemRegistry.BREAD, 8);
    this.hotbar[5] = new ItemStack(ItemRegistry.OAK_PLANKS, 32);
    this.hotbar[6] = new ItemStack(ItemRegistry.DIRT, 64);
  }

  chunkKey(cx, cz) {
    return `${cx}_${cz}`;
  }

  blockKey(x, y, z) {
    return `${x}_${y}_${z}`;
  }

  getChunk(cx, cz) {
    return this.chunks.get(this.chunkKey(cx, cz)) || null;
  }

  getBlock(x, y, z) {
    if (y < 0 || y >= VoxelChunk.SIZE_Y) return 0;
    const key = this.blockKey(x, y, z);
    if (this.modifiedBlocks.has(key)) {
      return this.modifiedBlocks.get(key);
    }

    const cx = Math.floor(x / VoxelChunk.SIZE_X);
    const cz = Math.floor(z / VoxelChunk.SIZE_Z);
    const chunk = this.getChunk(cx, cz);
    if (!chunk) return 0;

    const localX = ((x % VoxelChunk.SIZE_X) + VoxelChunk.SIZE_X) % VoxelChunk.SIZE_X;
    const localZ = ((z % VoxelChunk.SIZE_Z) + VoxelChunk.SIZE_Z) % VoxelChunk.SIZE_Z;
    return chunk.getBlock(localX, y, localZ);
  }

  setBlock(x, y, z, blockId, updateMesh = true) {
    if (y < 0 || y >= VoxelChunk.SIZE_Y) return;
    const key = this.blockKey(x, y, z);
    this.modifiedBlocks.set(key, blockId);

    const cx = Math.floor(x / VoxelChunk.SIZE_X);
    const cz = Math.floor(z / VoxelChunk.SIZE_Z);
    let chunk = this.getChunk(cx, cz);
    if (!chunk) {
      chunk = new VoxelChunk(cx, cz);
      this.generateChunkTerrain(chunk);
      this.chunks.set(this.chunkKey(cx, cz), chunk);
    }

    const localX = ((x % VoxelChunk.SIZE_X) + VoxelChunk.SIZE_X) % VoxelChunk.SIZE_X;
    const localZ = ((z % VoxelChunk.SIZE_Z) + VoxelChunk.SIZE_Z) % VoxelChunk.SIZE_Z;
    chunk.setBlock(localX, y, localZ, blockId);

    if (updateMesh) {
      chunk.isDirty = true;
      if (localX === 0) { const neighbor = this.getChunk(cx - 1, cz); if (neighbor) neighbor.isDirty = true; }
      if (localX === VoxelChunk.SIZE_X - 1) { const neighbor = this.getChunk(cx + 1, cz); if (neighbor) neighbor.isDirty = true; }
      if (localZ === 0) { const neighbor = this.getChunk(cx, cz - 1); if (neighbor) neighbor.isDirty = true; }
      if (localZ === VoxelChunk.SIZE_Z - 1) { const neighbor = this.getChunk(cx, cz + 1); if (neighbor) neighbor.isDirty = true; }
    }
  }

  getHighestBlockY(x, z) {
    for (let y = VoxelChunk.SIZE_Y - 1; y >= 0; y--) {
      const b = this.getBlock(x, y, z);
      if (b !== 0 && b !== BlockRegistry.WATER.id && b !== BlockRegistry.OAK_LEAVES.id) {
        return y;
      }
    }
    return 20;
  }

  findSafeSpawn() {
    const groundY = this.getHighestBlockY(8, 8);
    this.playerX = 8.5;
    this.playerY = groundY + 2;
    this.playerZ = 8.5;
  }

  ensureChunksAroundPlayer() {
    const playerChunkX = Math.floor(this.playerX / VoxelChunk.SIZE_X);
    const playerChunkZ = Math.floor(this.playerZ / VoxelChunk.SIZE_Z);

    for (let dx = -this.renderDistanceChunks; dx <= this.renderDistanceChunks; dx++) {
      for (let dz = -this.renderDistanceChunks; dz <= this.renderDistanceChunks; dz++) {
        const cx = playerChunkX + dx;
        const cz = playerChunkZ + dz;
        const key = this.chunkKey(cx, cz);
        if (!this.chunks.has(key)) {
          const chunk = new VoxelChunk(cx, cz);
          this.generateChunkTerrain(chunk);
          this.chunks.set(key, chunk);
        }
      }
    }
  }

  generateChunkTerrain(chunk) {
    const worldOffsetX = chunk.chunkX * VoxelChunk.SIZE_X;
    const worldOffsetZ = chunk.chunkZ * VoxelChunk.SIZE_Z;
    const waterLevel = 24;

    for (let lz = 0; lz < VoxelChunk.SIZE_Z; lz++) {
      for (let lx = 0; lx < VoxelChunk.SIZE_X; lx++) {
        const wx = worldOffsetX + lx;
        const wz = worldOffsetZ + lz;

        const base = this.noise.fractal2D(wx * 0.015, wz * 0.015, 3);
        const mountain = Math.pow(Math.max(0, this.noise.fractal2D(wx * 0.04, wz * 0.04, 2)), 1.8);
        const height = Math.min(58, Math.max(4, Math.floor(28 + base * 10 + mountain * 14)));

        for (let y = 0; y < VoxelChunk.SIZE_Y; y++) {
          let block = 0;
          if (y === 0) {
            block = BlockRegistry.BEDROCK.id;
          } else if (y < height - 4) {
            const caveNoise = this.noise.eval3D(wx * 0.06, y * 0.08, wz * 0.06);
            if (caveNoise > 0.45 && y > 3) {
              block = 0;
            } else {
              const oreNoise = this.noise.eval3D(wx * 0.12, y * 0.12, wz * 0.12);
              if (y < 12 && oreNoise > 0.55) block = BlockRegistry.DIAMOND_ORE.id;
              else if (y < 22 && oreNoise > 0.48) block = BlockRegistry.GOLD_ORE.id;
              else if (y < 35 && oreNoise > 0.42) block = BlockRegistry.IRON_ORE.id;
              else if (oreNoise > 0.38) block = BlockRegistry.COAL_ORE.id;
              else block = BlockRegistry.STONE.id;
            }
          } else if (y < height) {
            block = height <= waterLevel + 1 ? BlockRegistry.SAND.id : BlockRegistry.DIRT.id;
          } else if (y === height) {
            if (height < waterLevel) block = BlockRegistry.DIRT.id;
            else if (height <= waterLevel + 1) block = BlockRegistry.SAND.id;
            else block = BlockRegistry.GRASS.id;
          } else if (y <= waterLevel) {
            block = BlockRegistry.WATER.id;
          }
          chunk.setBlock(lx, y, lz, block);
        }

        if (height > waterLevel + 1 && chunk.getBlock(lx, height, lz) === BlockRegistry.GRASS.id) {
          const vegVal = this.noise.eval2D(wx * 0.25, wz * 0.25);
          if (vegVal > 0.65 && lx >= 2 && lx <= 13 && lz >= 2 && lz <= 13) {
            this.generateTree(chunk, lx, height + 1, lz);
          } else if (vegVal > 0.45 && height + 1 < VoxelChunk.SIZE_Y) {
            chunk.setBlock(lx, height + 1, lz, BlockRegistry.RED_FLOWER.id);
          } else if (vegVal < -0.45 && height + 1 < VoxelChunk.SIZE_Y) {
            chunk.setBlock(lx, height + 1, lz, BlockRegistry.YELLOW_FLOWER.id);
          }
        }
      }
    }

    // Apply persistent modified blocks
    for (let y = 0; y < VoxelChunk.SIZE_Y; y++) {
      for (let lz = 0; lz < VoxelChunk.SIZE_Z; lz++) {
        for (let lx = 0; lx < VoxelChunk.SIZE_X; lx++) {
          const key = this.blockKey(worldOffsetX + lx, y, worldOffsetZ + lz);
          if (this.modifiedBlocks.has(key)) {
            chunk.setBlock(lx, y, lz, this.modifiedBlocks.get(key));
          }
        }
      }
    }

    chunk.isGenerated = true;
    chunk.isDirty = true;
  }

  generateTree(chunk, lx, startY, lz) {
    const trunkHeight = 4 + Math.floor(Math.random() * 2);
    if (startY + trunkHeight + 2 >= VoxelChunk.SIZE_Y) return;

    for (let dy = 0; dy < trunkHeight; dy++) {
      chunk.setBlock(lx, startY + dy, lz, BlockRegistry.OAK_LOG.id);
    }

    const leafBottom = startY + trunkHeight - 2;
    const leafTop = startY + trunkHeight + 1;
    for (let ly = leafBottom; ly <= leafTop; ly++) {
      const radius = ly >= leafTop ? 1 : 2;
      for (let ox = -radius; ox <= radius; ox++) {
        for (let oz = -radius; oz <= radius; oz++) {
          if (Math.abs(ox) === radius && Math.abs(oz) === radius && Math.random() < 0.5) continue;
          const tx = lx + ox;
          const tz = lz + oz;
          if (tx >= 0 && tx < VoxelChunk.SIZE_X && tz >= 0 && tz < VoxelChunk.SIZE_Z) {
            if (chunk.getBlock(tx, ly, tz) === 0) {
              chunk.setBlock(tx, ly, tz, BlockRegistry.OAK_LEAVES.id);
            }
          }
        }
      }
    }
  }

  raycast(startX, startY, startZ, dirX, dirY, dirZ, maxDistance = 5.5) {
    let currentX = Math.floor(startX);
    let currentY = Math.floor(startY);
    let currentZ = Math.floor(startZ);

    const stepX = dirX > 0 ? 1 : (dirX < 0 ? -1 : 0);
    const stepY = dirY > 0 ? 1 : (dirY < 0 ? -1 : 0);
    const stepZ = dirZ > 0 ? 1 : (dirZ < 0 ? -1 : 0);

    const deltaX = dirX !== 0 ? Math.abs(1 / dirX) : Number.MAX_VALUE;
    const deltaY = dirY !== 0 ? Math.abs(1 / dirY) : Number.MAX_VALUE;
    const deltaZ = dirZ !== 0 ? Math.abs(1 / dirZ) : Number.MAX_VALUE;

    let nextTMaxX = dirX > 0 ? (currentX + 1 - startX) * deltaX : (startX - currentX) * deltaX;
    let nextTMaxY = dirY > 0 ? (currentY + 1 - startY) * deltaY : (startY - currentY) * deltaY;
    let nextTMaxZ = dirZ > 0 ? (currentZ + 1 - startZ) * deltaZ : (startZ - currentZ) * deltaZ;

    let normalX = 0, normalY = 0, normalZ = 0;
    let t = 0;

    while (t <= maxDistance) {
      const blockId = this.getBlock(currentX, currentY, currentZ);
      if (blockId !== 0 && blockId !== BlockRegistry.WATER.id) {
        return {
          hit: true,
          blockX: currentX, blockY: currentY, blockZ: currentZ,
          faceX: currentX + normalX, faceY: currentY + normalY, faceZ: currentZ + normalZ,
          blockId: blockId,
          normalX: normalX, normalY: normalY, normalZ: normalZ,
          distance: t
        };
      }

      if (nextTMaxX < nextTMaxY) {
        if (nextTMaxX < nextTMaxZ) {
          currentX += stepX; t = nextTMaxX; nextTMaxX += deltaX;
          normalX = -stepX; normalY = 0; normalZ = 0;
        } else {
          currentZ += stepZ; t = nextTMaxZ; nextTMaxZ += deltaZ;
          normalX = 0; normalY = 0; normalZ = -stepZ;
        }
      } else {
        if (nextTMaxY < nextTMaxZ) {
          currentY += stepY; t = nextTMaxY; nextTMaxY += deltaY;
          normalX = 0; normalY = -stepY; normalZ = 0;
        } else {
          currentZ += stepZ; t = nextTMaxZ; nextTMaxZ += deltaZ;
          normalX = 0; normalY = 0; normalZ = -stepZ;
        }
      }
    }

    return { hit: false };
  }

  updateBlockBreaking(dt, ray) {
    if (!ray.hit) {
      this.resetBlockBreaking();
      return false;
    }

    if (this.breakingBlockX !== ray.blockX || this.breakingBlockY !== ray.blockY || this.breakingBlockZ !== ray.blockZ) {
      this.breakingBlockX = ray.blockX;
      this.breakingBlockY = ray.blockY;
      this.breakingBlockZ = ray.blockZ;
      this.breakProgress = 0;

      const block = BlockRegistry.get(ray.blockId);
      const equipped = this.getEquippedItem();
      let speed = 1.0;
      if (equipped.item.toolType === block.preferredTool && block.preferredTool !== ToolType.NONE) {
        speed = equipped.item.miningSpeed;
      }
      if (this.gameMode === GameMode.CREATIVE) {
        speed = 100.0;
      }
      this.breakTimeRequired = Math.max(0.05, block.hardness / speed);
    }

    this.breakProgress += dt / this.breakTimeRequired;
    this.soundEngine.playDig();

    const block = BlockRegistry.get(ray.blockId);
    this.spawnBlockParticles(ray.blockX + 0.5, ray.blockY + 0.5, ray.blockZ + 0.5, block.sideColor, 2);

    if (this.breakProgress >= 1.0) {
      this.breakBlock(ray.blockX, ray.blockY, ray.blockZ, ray.blockId);
      this.resetBlockBreaking();
      return true;
    }
    return false;
  }

  resetBlockBreaking() {
    this.breakingBlockX = -1;
    this.breakingBlockY = -1;
    this.breakingBlockZ = -1;
    this.breakProgress = 0;
  }

  breakBlock(x, y, z, blockId) {
    const block = BlockRegistry.get(blockId);
    this.setBlock(x, y, z, 0);
    this.soundEngine.playBreak();
    this.spawnBlockParticles(x + 0.5, y + 0.5, z + 0.5, block.sideColor, 12);

    if (this.gameMode === GameMode.SURVIVAL) {
      if (block.dropItemId) {
        const dropItem = ItemRegistry.get(block.dropItemId);
        if (dropItem !== ItemRegistry.EMPTY) {
          this.spawnItemDrop(x + 0.5, y + 0.5, z + 0.5, new ItemStack(dropItem, block.dropCount));
        }
      }

      const equipped = this.getEquippedItem();
      if (equipped.item.durability > 0) {
        equipped.currentDurability--;
        if (equipped.currentDurability <= 0) {
          this.hotbar[this.selectedHotbarIndex] = new ItemStack(ItemRegistry.EMPTY, 0);
          this.soundEngine.playBreak();
        }
      }
    }
  }

  placeBlock(ray) {
    if (!ray.hit) return false;
    const equipped = this.getEquippedItem();
    if (!equipped.item.blockId) return false;

    const px = ray.faceX;
    const py = ray.faceY;
    const pz = ray.faceZ;

    const playerMinX = this.playerX - 0.3;
    const playerMaxX = this.playerX + 0.3;
    const playerMinY = this.playerY;
    const playerMaxY = this.playerY + 1.8;
    const playerMinZ = this.playerZ - 0.3;
    const playerMaxZ = this.playerZ + 0.3;

    if (px + 1 > playerMinX && px < playerMaxX &&
        py + 1 > playerMinY && py < playerMaxY &&
        pz + 1 > playerMinZ && pz < playerMaxZ) {
      return false;
    }

    this.setBlock(px, py, pz, equipped.item.blockId);
    this.soundEngine.playPlace();

    if (this.gameMode === GameMode.SURVIVAL) {
      equipped.count--;
      if (equipped.count <= 0) {
        this.hotbar[this.selectedHotbarIndex] = new ItemStack(ItemRegistry.EMPTY, 0);
      }
    }
    return true;
  }

  getEquippedItem() {
    return this.hotbar[this.selectedHotbarIndex];
  }

  eatFood() {
    const equipped = this.getEquippedItem();
    if (equipped.item.category !== ItemCategory.FOOD || equipped.item.foodHeal <= 0) return false;
    if (this.hunger >= this.maxHunger && this.health >= this.maxHealth && equipped.item.id !== 'golden_apple') return false;

    this.hunger = Math.min(this.maxHunger, this.hunger + equipped.item.foodHeal);
    this.saturation += equipped.item.foodSaturation;
    if (equipped.item.id === 'golden_apple') {
      this.health = Math.min(this.maxHealth, this.health + 4);
    }

    this.soundEngine.playEat();
    equipped.count--;
    if (equipped.count <= 0) {
      this.hotbar[this.selectedHotbarIndex] = new ItemStack(ItemRegistry.EMPTY, 0);
    }
    return true;
  }

  attackMob(mob) {
    const equipped = this.getEquippedItem();
    const damage = this.gameMode === GameMode.CREATIVE ? 50 : equipped.item.attackDamage;
    mob.health -= damage;
    mob.hurtTime = 0.3;
    this.soundEngine.playHurt();
    this.soundEngine.playSwordSwing();

    const rad = (this.playerYaw * Math.PI) / 180.0;
    mob.vx += -Math.sin(rad) * 4.0;
    mob.vy += 2.5;
    mob.vz += Math.cos(rad) * 4.0;

    this.spawnBlockParticles(mob.x, mob.y + 0.8, mob.z, 0xE53935, 8);

    if (mob.health <= 0) {
      mob.isDead = true;
      this.soundEngine.playBreak();
      this.addXp(15);

      switch (mob.type) {
        case EntityType.CREEPER:
          this.spawnItemDrop(mob.x, mob.y, mob.z, new ItemStack(ItemRegistry.GUNPOWDER, 1 + Math.floor(Math.random() * 2)));
          break;
        case EntityType.SKELETON:
          this.spawnItemDrop(mob.x, mob.y, mob.z, new ItemStack(ItemRegistry.BONE, 1 + Math.floor(Math.random() * 2)));
          this.spawnItemDrop(mob.x, mob.y, mob.z, new ItemStack(ItemRegistry.ARROW, 1 + Math.floor(Math.random() * 3)));
          break;
        case EntityType.ZOMBIE:
          this.spawnItemDrop(mob.x, mob.y, mob.z, new ItemStack(ItemRegistry.DIRT, 1));
          break;
        case EntityType.PIG:
        case EntityType.COW:
          this.spawnItemDrop(mob.x, mob.y, mob.z, new ItemStack(ItemRegistry.RAW_PORKCHOP, 1 + Math.floor(Math.random() * 2)));
          break;
      }
    }
  }

  shootBow() {
    const equipped = this.getEquippedItem();
    if (equipped.item.id !== 'bow') return false;

    let arrowSlot = -1;
    for (let i = 0; i < this.hotbar.length; i++) {
      if (this.hotbar[i].item.id === 'arrow' && this.hotbar[i].count > 0) {
        arrowSlot = i; break;
      }
    }
    if (arrowSlot === -1) {
      for (let i = 0; i < this.inventory.length; i++) {
        if (this.inventory[i].item.id === 'arrow' && this.inventory[i].count > 0) {
          arrowSlot = i + 9; break;
        }
      }
    }

    if (this.gameMode === GameMode.SURVIVAL && arrowSlot === -1) return false;

    if (this.gameMode === GameMode.SURVIVAL && arrowSlot !== -1) {
      if (arrowSlot < 9) {
        this.hotbar[arrowSlot].count--;
        if (this.hotbar[arrowSlot].count <= 0) this.hotbar[arrowSlot] = new ItemStack(ItemRegistry.EMPTY, 0);
      } else {
        this.inventory[arrowSlot - 9].count--;
        if (this.inventory[arrowSlot - 9].count <= 0) this.inventory[arrowSlot - 9] = new ItemStack(ItemRegistry.EMPTY, 0);
      }
    }

    const yawRad = (this.playerYaw * Math.PI) / 180.0;
    const pitchRad = (this.playerPitch * Math.PI) / 180.0;
    const speed = 18.0;

    const vx = -Math.sin(yawRad) * Math.cos(pitchRad) * speed;
    const vy = -Math.sin(pitchRad) * speed;
    const vz = Math.cos(yawRad) * Math.cos(pitchRad) * speed;

    this.arrows.push(new ArrowEntity(`arrow_${Date.now()}`, this.playerX, this.playerY + 1.4, this.playerZ, vx, vy, vz, 'player', 6.0));
    this.soundEngine.playBowShoot();
    return true;
  }

  addXp(amount) {
    this.xp += amount;
    while (this.xp >= this.xpForNextLevel) {
      this.xp -= this.xpForNextLevel;
      this.level++;
      this.xpForNextLevel = Math.floor(this.xpForNextLevel * 1.25);
      this.soundEngine.playLevelUp();
    }
  }

  spawnItemDrop(x, y, z, stack) {
    this.itemDrops.push(new ItemDropEntity(`drop_${Date.now()}_${Math.floor(Math.random() * 1000)}`, x, y, z, stack));
  }

  spawnBlockParticles(x, y, z, color, count) {
    for (let i = 0; i < count; i++) {
      this.particles.push(new Particle(
        x + (Math.random() - 0.5) * 0.6,
        y + (Math.random() - 0.5) * 0.6,
        z + (Math.random() - 0.5) * 0.6,
        (Math.random() - 0.5) * 4,
        Math.random() * 3 + 1,
        (Math.random() - 0.5) * 4,
        color,
        0.08 + Math.random() * 0.06,
        0.5 + Math.random() * 0.4
      ));
    }
  }

  explode(ex, ey, ez, power = 3.0) {
    this.soundEngine.playExplosion();
    const radius = Math.floor(power);

    for (let dx = -radius; dx <= radius; dx++) {
      for (let dy = -radius; dy <= radius; dy++) {
        for (let dz = -radius; dz <= radius; dz++) {
          if (dx * dx + dy * dy + dz * dz <= radius * radius) {
            const bx = Math.floor(ex + dx);
            const by = Math.floor(ey + dy);
            const bz = Math.floor(ez + dz);
            const b = this.getBlock(bx, by, bz);
            if (b !== 0 && b !== BlockRegistry.BEDROCK.id && b !== BlockRegistry.OBSIDIAN.id) {
              this.setBlock(bx, by, bz, 0);
            }
          }
        }
      }
    }

    const distToPlayer = Math.hypot(this.playerX - ex, this.playerY - ey, this.playerZ - ez);
    if (distToPlayer < power * 2.5) {
      const dmg = Math.max(2.0, (1.0 - distToPlayer / (power * 2.5)) * 24.0);
      this.damagePlayer(dmg);
      const push = (1.0 - distToPlayer / (power * 2.5)) * 12.0;
      this.playerVx += ((this.playerX - ex) / distToPlayer) * push;
      this.playerVy += 6.0;
      this.playerVz += ((this.playerZ - ez) / distToPlayer) * push;
    }

    for (let i = 0; i < 35; i++) {
      const color = i % 2 === 0 ? 0xFF5722 : 0x212121;
      this.particles.push(new Particle(
        ex, ey, ez,
        (Math.random() - 0.5) * 10,
        (Math.random() - 0.5) * 10 + 3,
        (Math.random() - 0.5) * 10,
        color,
        0.2 + Math.random() * 0.2,
        0.8 + Math.random() * 0.5
      ));
    }
  }

  damagePlayer(amount) {
    if (this.gameMode === GameMode.CREATIVE || this.isPlayerDead) return;

    let totalDefense = 0;
    for (const a of this.armor) {
      if (!a.isEmpty) totalDefense += a.item.armorDefense;
    }
    const reducedDamage = amount * Math.max(0.15, 1.0 - totalDefense * 0.035);

    this.health = Math.max(0, this.health - reducedDamage);
    this.hurtFlash = 0.4;
    this.soundEngine.playHurt();

    if (this.health <= 0) {
      this.isPlayerDead = true;
      this.soundEngine.playBreak();
    }
  }

  respawn() {
    this.findSafeSpawn();
    this.health = this.maxHealth;
    this.hunger = this.maxHunger;
    this.oxygen = this.maxOxygen;
    this.isPlayerDead = false;
    this.playerVx = 0; this.playerVy = 0; this.playerVz = 0;
  }

  update(dt) {
    const stepDt = Math.min(0.05, Math.max(0.001, dt));

    this.timeOfDay = (this.timeOfDay + (stepDt / this.dayLengthSeconds) * 24000.0) % 24000.0;
    if (this.timeOfDay >= 4000 && this.timeOfDay <= 8000) {
      this.skyLight = 1.0;
    } else if (this.timeOfDay >= 14000 && this.timeOfDay <= 22000) {
      this.skyLight = 0.18;
    } else if (this.timeOfDay > 8000 && this.timeOfDay < 14000) {
      this.skyLight = 1.0 - ((this.timeOfDay - 8000) / 6000.0) * 0.82;
    } else {
      this.skyLight = 0.18 + (this.timeOfDay / 4000.0) * 0.82;
    }

    this.updatePlayerPhysics(stepDt);
    this.updateSurvivalStats(stepDt);
    this.updateMobs(stepDt);
    this.updateArrows(stepDt);
    this.updateItemDrops(stepDt);
    this.updateParticles(stepDt);
  }

  updatePlayerPhysics(dt) {
    if (this.isPlayerDead) return;

    const blockAtFeet = this.getBlock(Math.floor(this.playerX), Math.floor(this.playerY), Math.floor(this.playerZ));
    const blockAtHead = this.getBlock(Math.floor(this.playerX), Math.floor(this.playerY + 1.5), Math.floor(this.playerZ));
    this.isInWater = (blockAtFeet === BlockRegistry.WATER.id || blockAtHead === BlockRegistry.WATER.id);

    if (this.isInWater) {
      this.playerVy = Math.max(-3.0, Math.min(3.0, this.playerVy - 9.8 * dt * 0.3));
    } else {
      this.playerVy -= 22.0 * dt;
      if (this.playerVy < -25.0) this.playerVy = -25.0;
    }

    const targetX = this.playerX + this.playerVx * dt;
    const targetY = this.playerY + this.playerVy * dt;
    const targetZ = this.playerZ + this.playerVz * dt;

    const pRadius = 0.28;
    const pHeight = 1.75;

    // Y Collision
    if (this.playerVy < 0) {
      const groundY = Math.floor(targetY);
      let collided = false;
      for (let cx = Math.floor(this.playerX - pRadius); cx <= Math.floor(this.playerX + pRadius); cx++) {
        for (let cz = Math.floor(this.playerZ - pRadius); cz <= Math.floor(this.playerZ + pRadius); cz++) {
          const b = this.getBlock(cx, groundY, cz);
          if (b !== 0 && b !== BlockRegistry.WATER.id && BlockRegistry.get(b).isSolid) {
            collided = true; break;
          }
        }
        if (collided) break;
      }

      if (collided && targetY <= groundY + 1.0) {
        this.playerY = groundY + 1.0;
        if (this.fallDistance > 3.5 && !this.isInWater && this.gameMode === GameMode.SURVIVAL) {
          this.damagePlayer((this.fallDistance - 3.0) * 1.5);
        }
        this.fallDistance = 0;
        this.playerVy = 0;
        this.isGrounded = true;
      } else {
        this.playerY = targetY;
        this.isGrounded = false;
        if (this.playerVy < 0) this.fallDistance += Math.abs(this.playerVy * dt);
      }
    } else if (this.playerVy > 0) {
      const ceilY = Math.floor(targetY + pHeight);
      let collided = false;
      for (let cx = Math.floor(this.playerX - pRadius); cx <= Math.floor(this.playerX + pRadius); cx++) {
        for (let cz = Math.floor(this.playerZ - pRadius); cz <= Math.floor(this.playerZ + pRadius); cz++) {
          const b = this.getBlock(cx, ceilY, cz);
          if (b !== 0 && b !== BlockRegistry.WATER.id && BlockRegistry.get(b).isSolid) {
            collided = true; break;
          }
        }
        if (collided) break;
      }
      if (collided) {
        this.playerY = ceilY - pHeight - 0.01;
        this.playerVy = 0;
      } else {
        this.playerY = targetY;
        this.isGrounded = false;
      }
    }

    // X Collision with auto-step
    let collidedX = false;
    const checkX = this.playerVx > 0 ? targetX + pRadius : targetX - pRadius;
    const ix = Math.floor(checkX);
    for (let y = Math.floor(this.playerY + 0.1); y <= Math.floor(this.playerY + pHeight - 0.1); y++) {
      for (let cz = Math.floor(this.playerZ - pRadius); cz <= Math.floor(this.playerZ + pRadius); cz++) {
        const b = this.getBlock(ix, y, cz);
        if (b !== 0 && b !== BlockRegistry.WATER.id && BlockRegistry.get(b).isSolid) {
          collidedX = true; break;
        }
      }
      if (collidedX) break;
    }

    if (!collidedX) {
      this.playerX = targetX;
    } else {
      const stepBlock = this.getBlock(ix, Math.floor(this.playerY + 1), Math.floor(this.playerZ));
      if (this.isGrounded && stepBlock === 0) {
        this.playerY += 0.5;
        this.playerX = targetX;
      } else {
        this.playerVx = 0;
      }
    }

    // Z Collision with auto-step
    let collidedZ = false;
    const checkZ = this.playerVz > 0 ? targetZ + pRadius : targetZ - pRadius;
    const iz = Math.floor(checkZ);
    for (let y = Math.floor(this.playerY + 0.1); y <= Math.floor(this.playerY + pHeight - 0.1); y++) {
      for (let cx = Math.floor(this.playerX - pRadius); cx <= Math.floor(this.playerX + pRadius); cx++) {
        const b = this.getBlock(cx, y, iz);
        if (b !== 0 && b !== BlockRegistry.WATER.id && BlockRegistry.get(b).isSolid) {
          collidedZ = true; break;
        }
      }
      if (collidedZ) break;
    }

    if (!collidedZ) {
      this.playerZ = targetZ;
    } else {
      const stepBlock = this.getBlock(Math.floor(this.playerX), Math.floor(this.playerY + 1), iz);
      if (this.isGrounded && stepBlock === 0) {
        this.playerY += 0.5;
        this.playerZ = targetZ;
      } else {
        this.playerVz = 0;
      }
    }

    const friction = this.isGrounded ? 0.7 : 0.92;
    this.playerVx *= friction;
    this.playerVz *= friction;

    if (this.hurtFlash > 0) this.hurtFlash -= dt;
  }

  updateSurvivalStats(dt) {
    if (this.gameMode === GameMode.CREATIVE || this.isPlayerDead) return;

    const headBlock = this.getBlock(Math.floor(this.playerX), Math.floor(this.playerY + 1.5), Math.floor(this.playerZ));
    if (headBlock === BlockRegistry.WATER.id) {
      this.oxygen = Math.max(0, this.oxygen - dt);
      if (this.oxygen <= 0) {
        this.damagePlayer(2.0 * dt);
      }
    } else {
      this.oxygen = Math.min(this.maxOxygen, this.oxygen + dt * 4.0);
    }

    this.hungerTickTimer += dt;
    if (this.hungerTickTimer >= 4.0) {
      this.hungerTickTimer = 0;
      const decay = this.isSprinting ? 0.3 : 0.05;
      if (this.saturation > 0) {
        this.saturation = Math.max(0, this.saturation - decay);
      } else {
        this.hunger = Math.max(0, this.hunger - decay);
      }
    }

    this.regenTickTimer += dt;
    if (this.regenTickTimer >= 3.0) {
      this.regenTickTimer = 0;
      if (this.hunger >= 18 && this.health < this.maxHealth) {
        this.health = Math.min(this.maxHealth, this.health + 1.0);
        this.hunger = Math.max(0, this.hunger - 0.4);
      } else if (this.hunger <= 0 && this.difficulty !== Difficulty.PEACEFUL) {
        this.damagePlayer(1.0);
      }
    }
  }

  updateMobs(dt) {
    this.mobSpawnTimer += dt;
    if (this.mobSpawnTimer >= 5.0 && this.difficulty !== Difficulty.PEACEFUL && this.mobs.length < 12) {
      this.mobSpawnTimer = 0;
      const spawnAngle = Math.random() * Math.PI * 2;
      const spawnDist = 14 + Math.random() * 12;
      const sx = this.playerX + Math.sin(spawnAngle) * spawnDist;
      const sz = this.playerZ + Math.cos(spawnAngle) * spawnDist;
      const sy = this.getHighestBlockY(Math.floor(sx), Math.floor(sz)) + 1;

      if (sy >= 1 && sy < VoxelChunk.SIZE_Y - 2) {
        const isNight = this.skyLight < 0.35;
        let type = EntityType.PIG;
        if (isNight) {
          const r = Math.random();
          if (r < 0.35) type = EntityType.CREEPER;
          else if (r < 0.70) type = EntityType.SKELETON;
          else type = EntityType.ZOMBIE;
        } else {
          type = Math.random() < 0.5 ? EntityType.PIG : EntityType.COW;
        }
        this.mobs.push(new MobEntity(`mob_${Date.now()}_${Math.floor(Math.random() * 1000)}`, type, sx, sy, sz));
      }
    }

    for (let i = this.mobs.length - 1; i >= 0; i--) {
      const mob = this.mobs[i];
      if (mob.isDead) {
        this.mobs.splice(i, 1);
        continue;
      }

      if (mob.hurtTime > 0) mob.hurtTime -= dt;
      mob.animTime += dt;

      mob.vy -= 18.0 * dt;
      mob.y += mob.vy * dt;
      const groundY = this.getHighestBlockY(Math.floor(mob.x), Math.floor(mob.z)) + 1.0;
      if (mob.y <= groundY) {
        mob.y = groundY;
        mob.vy = 0;
      }

      const dx = this.playerX - mob.x;
      const dy = this.playerY - mob.y;
      const dz = this.playerZ - mob.z;
      const dist = Math.hypot(dx, dz);

      mob.yaw = (Math.atan2(-dx, dz) * 180.0) / Math.PI;

      switch (mob.type) {
        case EntityType.ZOMBIE:
          if (this.skyLight > 0.75 && mob.y >= groundY - 1) {
            mob.health -= dt * 2.0;
            this.spawnBlockParticles(mob.x, mob.y + 1, mob.z, 0xFF5722, 1);
          }
          if (dist < 16 && dist > 1.2 && !this.isPlayerDead) {
            mob.vx = (dx / dist) * mob.moveSpeed;
            mob.vz = (dz / dist) * mob.moveSpeed;
          } else if (dist <= 1.2 && !this.isPlayerDead) {
            mob.attackCooldown += dt;
            if (mob.attackCooldown >= 1.2) {
              mob.attackCooldown = 0;
              this.damagePlayer(mob.attackDamage);
            }
          } else {
            mob.vx *= 0.8; mob.vz *= 0.8;
          }
          break;

        case EntityType.CREEPER:
          if (dist < 16 && dist > 2.0 && !this.isPlayerDead) {
            mob.vx = (dx / dist) * mob.moveSpeed;
            mob.vz = (dz / dist) * mob.moveSpeed;
            if (mob.state === 'HISSING') {
              mob.state = 'CHASE';
              mob.creeperFuse = 0;
            }
          } else if (dist <= 2.2 && !this.isPlayerDead) {
            mob.vx = 0; mob.vz = 0;
            if (mob.state !== 'HISSING') {
              mob.state = 'HISSING';
              this.soundEngine.playCreeperHiss();
            }
            mob.creeperFuse += dt;
            this.spawnBlockParticles(mob.x, mob.y + 1, mob.z, 0xFFFFFF, 2);
            if (mob.creeperFuse >= 1.5) {
              this.explode(mob.x, mob.y + 0.8, mob.z, 3.2);
              mob.isDead = true;
            }
          } else {
            mob.vx *= 0.8; mob.vz *= 0.8;
          }
          break;

        case EntityType.SKELETON:
          if (this.skyLight > 0.75 && mob.y >= groundY - 1) {
            mob.health -= dt * 2.0;
            this.spawnBlockParticles(mob.x, mob.y + 1, mob.z, 0xFF5722, 1);
          }
          if (dist < 16 && dist > 7.0 && !this.isPlayerDead) {
            mob.vx = (dx / dist) * mob.moveSpeed;
            mob.vz = (dz / dist) * mob.moveSpeed;
          } else if (dist <= 10.0 && !this.isPlayerDead) {
            mob.vx = 0; mob.vz = 0;
            mob.attackCooldown += dt;
            if (mob.attackCooldown >= 2.5) {
              mob.attackCooldown = 0;
              const speed = 14.0;
              const arrowVx = (dx / dist) * speed;
              const arrowVy = (dy / dist) * speed + 2.0;
              const arrowVz = (dz / dist) * speed;
              this.arrows.push(new ArrowEntity(`mob_arrow_${Date.now()}`, mob.x, mob.y + 1.4, mob.z, arrowVx, arrowVy, arrowVz, mob.id, mob.attackDamage));
              this.soundEngine.playBowShoot();
            }
          }
          break;
      }

      mob.x += mob.vx * dt;
      mob.z += mob.vz * dt;
    }
  }

  updateArrows(dt) {
    for (let i = this.arrows.length - 1; i >= 0; i--) {
      const arrow = this.arrows[i];
      if (arrow.inGround) {
        arrow.groundTimer += dt;
        if (arrow.groundTimer > 8.0) this.arrows.splice(i, 1);
        continue;
      }

      arrow.vy -= 14.0 * dt;
      arrow.x += arrow.vx * dt;
      arrow.y += arrow.vy * dt;
      arrow.z += arrow.vz * dt;

      const block = this.getBlock(Math.floor(arrow.x), Math.floor(arrow.y), Math.floor(arrow.z));
      if (block !== 0 && BlockRegistry.get(block).isSolid) {
        arrow.inGround = true;
        this.soundEngine.playDig();
        continue;
      }

      if (arrow.shooterId !== 'player' && !this.isPlayerDead) {
        const pdist = Math.hypot(arrow.x - this.playerX, arrow.y - (this.playerY + 0.9), arrow.z - this.playerZ);
        if (pdist < 0.8) {
          this.damagePlayer(arrow.damage);
          this.arrows.splice(i, 1);
          continue;
        }
      }

      for (const mob of this.mobs) {
        if (mob.id !== arrow.shooterId && !mob.isDead) {
          const mdist = Math.hypot(arrow.x - mob.x, arrow.y - (mob.y + 0.8), arrow.z - mob.z);
          if (mdist < 0.85) {
            mob.health -= arrow.damage;
            mob.hurtTime = 0.3;
            this.soundEngine.playHurt();
            if (mob.health <= 0) {
              mob.isDead = true;
              this.addXp(15);
            }
            this.arrows.splice(i, 1);
            break;
          }
        }
      }
    }
  }

  updateItemDrops(dt) {
    for (let i = this.itemDrops.length - 1; i >= 0; i--) {
      const drop = this.itemDrops[i];
      drop.age += dt;
      drop.spinAngle = (drop.spinAngle + dt * 90) % 360;
      drop.hoverOffset = Math.sin(drop.age * 3.5) * 0.08;

      drop.vy -= 14 * dt;
      drop.y += drop.vy * dt;
      const groundY = this.getHighestBlockY(Math.floor(drop.x), Math.floor(drop.z)) + 1.15;
      if (drop.y <= groundY) {
        drop.y = groundY;
        drop.vy = 0;
      }
      drop.x += drop.vx * dt;
      drop.z += drop.vz * dt;
      drop.vx *= 0.85; drop.vz *= 0.85;

      const dist = Math.hypot(drop.x - this.playerX, drop.y - (this.playerY + 0.5), drop.z - this.playerZ);
      if (dist < 1.6 && !this.isPlayerDead) {
        if (this.addItemToInventory(drop.itemStack)) {
          this.soundEngine.playDig();
          this.itemDrops.splice(i, 1);
        }
      }
    }
  }

  updateParticles(dt) {
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.life += dt;
      if (p.isDead) {
        this.particles.splice(i, 1);
        continue;
      }
      p.vy -= p.gravity * dt;
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.z += p.vz * dt;
    }
  }

  addItemToInventory(stack) {
    for (let i = 0; i < this.hotbar.length; i++) {
      if (this.hotbar[i].item.id === stack.item.id && this.hotbar[i].count < this.hotbar[i].item.maxStack) {
        const available = this.hotbar[i].item.maxStack - this.hotbar[i].count;
        const toAdd = Math.min(available, stack.count);
        this.hotbar[i].count += toAdd;
        stack.count -= toAdd;
        if (stack.count <= 0) return true;
      }
    }
    for (let i = 0; i < this.inventory.length; i++) {
      if (this.inventory[i].item.id === stack.item.id && this.inventory[i].count < this.inventory[i].item.maxStack) {
        const available = this.inventory[i].item.maxStack - this.inventory[i].count;
        const toAdd = Math.min(available, stack.count);
        this.inventory[i].count += toAdd;
        stack.count -= toAdd;
        if (stack.count <= 0) return true;
      }
    }
    for (let i = 0; i < this.hotbar.length; i++) {
      if (this.hotbar[i].isEmpty) {
        this.hotbar[i] = stack.copy();
        stack.count = 0;
        return true;
      }
    }
    for (let i = 0; i < this.inventory.length; i++) {
      if (this.inventory[i].isEmpty) {
        this.inventory[i] = stack.copy();
        stack.count = 0;
        return true;
      }
    }
    return false;
  }
}
