/**
 * Persistence Storage & Repository Engine
 * Converted from WorldRepository.kt & AppDatabase.kt
 */

class StorageRepository {
  static STORAGE_KEY_WORLDS = 'voxelcraft_worlds_v1';
  static STORAGE_KEY_MODS = 'voxelcraft_mods_v1';

  static getAllWorlds() {
    try {
      const json = localStorage.getItem(this.STORAGE_KEY_WORLDS);
      if (json) {
        return JSON.parse(json);
      }
    } catch (e) {
      console.error(e);
    }
    // Default starter world
    const starterWorld = {
      id: 'starter_world_01',
      name: 'Oak Valley Survival',
      seed: 84920482,
      gameMode: 'SURVIVAL',
      difficulty: 'NORMAL',
      dayTime: 6000,
      playerX: 8.5,
      playerY: 36,
      playerZ: 8.5,
      lastPlayedTime: Date.now()
    };
    this.saveWorldEntity(starterWorld);
    return [starterWorld];
  }

  static saveWorldEntity(entity) {
    let worlds = [];
    try {
      const json = localStorage.getItem(this.STORAGE_KEY_WORLDS);
      if (json) worlds = JSON.parse(json);
    } catch (e) {}
    worlds = worlds.filter(w => w.id !== entity.id);
    worlds.unshift(entity);
    try {
      localStorage.setItem(this.STORAGE_KEY_WORLDS, JSON.stringify(worlds));
    } catch (e) {
      console.error(e);
    }
  }

  static saveWorld(worldId, name, world) {
    const hotbarData = world.hotbar.map(s => ({ itemId: s.item.id, count: s.count, durability: s.currentDurability }));
    const inventoryData = world.inventory.map(s => ({ itemId: s.item.id, count: s.count, durability: s.currentDurability }));
    const armorData = world.armor.map(s => ({ itemId: s.item.id, count: s.count, durability: s.currentDurability }));

    const modifiedBlocksObj = {};
    for (const [k, v] of world.modifiedBlocks.entries()) {
      modifiedBlocksObj[k] = v;
    }

    const entity = {
      id: worldId,
      name: name,
      seed: world.seed,
      gameMode: world.gameMode,
      difficulty: world.difficulty,
      dayTime: world.timeOfDay,
      playerX: world.playerX,
      playerY: world.playerY,
      playerZ: world.playerZ,
      playerYaw: world.playerYaw,
      playerPitch: world.playerPitch,
      health: world.health,
      hunger: world.hunger,
      level: world.level,
      xp: world.xp,
      hotbarJson: JSON.stringify(hotbarData),
      inventoryJson: JSON.stringify(inventoryData),
      armorJson: JSON.stringify(armorData),
      modifiedBlocksJson: JSON.stringify(modifiedBlocksObj),
      lastPlayedTime: Date.now()
    };

    this.saveWorldEntity(entity);
  }

  static loadWorld(entity) {
    const gm = entity.gameMode || GameMode.SURVIVAL;
    const diff = entity.difficulty || Difficulty.NORMAL;
    const world = new VoxelWorld(entity.seed, gm, diff);

    world.timeOfDay = entity.dayTime !== undefined ? entity.dayTime : 6000;
    world.playerX = entity.playerX !== undefined ? entity.playerX : 8.5;
    world.playerY = entity.playerY !== undefined ? entity.playerY : 36;
    world.playerZ = entity.playerZ !== undefined ? entity.playerZ : 8.5;
    world.playerYaw = entity.playerYaw || 0;
    world.playerPitch = entity.playerPitch || 0;
    world.health = entity.health !== undefined ? entity.health : 20;
    world.hunger = entity.hunger !== undefined ? entity.hunger : 20;
    world.level = entity.level || 1;
    world.xp = entity.xp || 0;

    if (entity.hotbarJson) {
      try {
        const arr = JSON.parse(entity.hotbarJson);
        for (let i = 0; i < Math.min(world.hotbar.length, arr.length); i++) {
          const item = ItemRegistry.get(arr[i].itemId);
          if (item !== ItemRegistry.EMPTY && arr[i].count > 0) {
            world.hotbar[i] = new ItemStack(item, arr[i].count, arr[i].durability);
          }
        }
      } catch (e) {}
    }

    if (entity.inventoryJson) {
      try {
        const arr = JSON.parse(entity.inventoryJson);
        for (let i = 0; i < Math.min(world.inventory.length, arr.length); i++) {
          const item = ItemRegistry.get(arr[i].itemId);
          if (item !== ItemRegistry.EMPTY && arr[i].count > 0) {
            world.inventory[i] = new ItemStack(item, arr[i].count, arr[i].durability);
          }
        }
      } catch (e) {}
    }

    if (entity.armorJson) {
      try {
        const arr = JSON.parse(entity.armorJson);
        for (let i = 0; i < Math.min(world.armor.length, arr.length); i++) {
          const item = ItemRegistry.get(arr[i].itemId);
          if (item !== ItemRegistry.EMPTY && arr[i].count > 0) {
            world.armor[i] = new ItemStack(item, arr[i].count, arr[i].durability);
          }
        }
      } catch (e) {}
    }

    if (entity.modifiedBlocksJson) {
      try {
        const obj = JSON.parse(entity.modifiedBlocksJson);
        for (const k in obj) {
          world.modifiedBlocks.set(k, obj[k]);
        }
      } catch (e) {}
    }

    world.ensureChunksAroundPlayer();
    return world;
  }

  static deleteWorld(id) {
    const worlds = this.getAllWorlds().filter(w => w.id !== id);
    try {
      localStorage.setItem(this.STORAGE_KEY_WORLDS, JSON.stringify(worlds));
    } catch (e) {}
  }

  static exportWorldJson(entity) {
    return JSON.stringify(entity, null, 2);
  }

  static importWorldJson(jsonString) {
    try {
      const json = JSON.parse(jsonString);
      const entity = {
        id: `imported_${Date.now()}`,
        name: (json.name || 'Imported World') + ' (Synced)',
        seed: json.seed || 12345,
        gameMode: json.gameMode || 'SURVIVAL',
        difficulty: json.difficulty || 'NORMAL',
        dayTime: json.dayTime || 6000,
        playerX: json.playerX || 8.5,
        playerY: json.playerY || 36,
        playerZ: json.playerZ || 8.5,
        health: json.health || 20,
        hunger: json.hunger || 20,
        level: json.level || 1,
        xp: json.xp || 0,
        hotbarJson: json.hotbarJson || '',
        inventoryJson: json.inventoryJson || '',
        armorJson: json.armorJson || '',
        modifiedBlocksJson: json.modifiedBlocksJson || '',
        lastPlayedTime: Date.now()
      };
      this.saveWorldEntity(entity);
      return entity;
    } catch (e) {
      throw new Error("Invalid save JSON payload");
    }
  }

  static getAllMods() {
    try {
      const json = localStorage.getItem(this.STORAGE_KEY_MODS);
      if (json) return JSON.parse(json);
    } catch (e) {}

    const defaults = [
      {
        id: 'mod_emerald_arsenal',
        name: 'Emerald & Netherite Arsenal',
        version: 'v1.4.0',
        author: 'VoxelForge Studio',
        description: 'Adds craftable Emerald Blades, Netherite Armor sets, and reinforced obsidian blocks with enhanced blast resistance!',
        isEnabled: true
      },
      {
        id: 'mod_magma_fiends',
        name: 'Infernal Biomes & Magma Mobs',
        version: 'v2.0.1',
        author: 'EnderCraft Devs',
        description: 'Spawns blazing Magma Fiends in deep caverns and introduces Magma Bricks that glow in the dark.',
        isEnabled: true
      },
      {
        id: 'mod_cyber_voxels',
        name: 'Cyber Voxel Sci-Fi Pack',
        version: 'v1.0.2',
        author: 'NeoBlocks',
        description: 'Adds Neon Blue Laser Crystals, Anti-Gravity Boots, and high-tech alloy plating blocks.',
        isEnabled: false
      }
    ];
    localStorage.setItem(this.STORAGE_KEY_MODS, JSON.stringify(defaults));
    return defaults;
  }

  static saveMods(mods) {
    try {
      localStorage.setItem(this.STORAGE_KEY_MODS, JSON.stringify(mods));
    } catch (e) {}
  }
}
