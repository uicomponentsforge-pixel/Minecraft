/**
 * Voxel Models & Registries (Blocks, Items, Recipes)
 * Converted from VoxelType.kt, Item.kt, Recipe.kt
 */

const ToolType = {
  NONE: 'NONE',
  PICKAXE: 'PICKAXE',
  AXE: 'AXE',
  SHOVEL: 'SHOVEL',
  SWORD: 'SWORD'
};

const ItemCategory = {
  BLOCKS: 'BLOCKS',
  TOOLS: 'TOOLS',
  WEAPONS: 'WEAPONS',
  ARMOR: 'ARMOR',
  FOOD: 'FOOD',
  MATERIALS: 'MATERIALS',
  SPECIAL: 'SPECIAL'
};

const ArmorSlot = {
  HEAD: 'HEAD',
  CHEST: 'CHEST',
  LEGS: 'LEGS',
  FEET: 'FEET',
  NONE: 'NONE'
};

class BlockProperties {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.displayName = config.displayName;
    this.isSolid = config.isSolid !== undefined ? config.isSolid : true;
    this.isTransparent = config.isTransparent || false;
    this.isLiquid = config.isLiquid || false;
    this.lightEmission = config.lightEmission || 0;
    this.hardness = config.hardness !== undefined ? config.hardness : 1.0;
    this.preferredTool = config.preferredTool || ToolType.NONE;
    this.topColor = config.topColor;
    this.sideColor = config.sideColor;
    this.bottomColor = config.bottomColor !== undefined ? config.bottomColor : config.sideColor;
    this.dropItemId = config.dropItemId || config.name.toLowerCase();
    this.dropCount = config.dropCount || 1;
    this.category = config.category || 'BUILDING';
    this.texturePattern = config.texturePattern || 0;
  }
}

class BlockRegistry {
  static blocks = {};

  static register(block) {
    this.blocks[block.id] = block;
    this.blocks[block.name.toLowerCase()] = block;
    return block;
  }

  static get(idOrName) {
    if (typeof idOrName === 'number') {
      return this.blocks[idOrName] || this.AIR;
    }
    if (typeof idOrName === 'string') {
      return this.blocks[idOrName.toLowerCase()] || this.AIR;
    }
    return this.AIR;
  }

  static registerCustomBlock(block) {
    this.register(block);
  }
}

// Register default blocks
BlockRegistry.AIR = BlockRegistry.register(new BlockProperties({
  id: 0, name: 'AIR', displayName: 'Air', isSolid: false, isTransparent: true, topColor: 0x00000000, sideColor: 0x00000000, dropItemId: 'air'
}));

BlockRegistry.GRASS = BlockRegistry.register(new BlockProperties({
  id: 1, name: 'GRASS', displayName: 'Grass Block', hardness: 0.6, preferredTool: ToolType.SHOVEL,
  topColor: 0x5B8C3A, sideColor: 0x7A5C37, bottomColor: 0x5C3E1E, dropItemId: 'dirt', texturePattern: 1
}));

BlockRegistry.DIRT = BlockRegistry.register(new BlockProperties({
  id: 2, name: 'DIRT', displayName: 'Dirt', hardness: 0.5, preferredTool: ToolType.SHOVEL,
  topColor: 0x5C3E1E, sideColor: 0x5C3E1E, dropItemId: 'dirt'
}));

BlockRegistry.STONE = BlockRegistry.register(new BlockProperties({
  id: 3, name: 'STONE', displayName: 'Stone', hardness: 1.5, preferredTool: ToolType.PICKAXE,
  topColor: 0x787878, sideColor: 0x787878, dropItemId: 'cobblestone'
}));

BlockRegistry.COBBLESTONE = BlockRegistry.register(new BlockProperties({
  id: 4, name: 'COBBLESTONE', displayName: 'Cobblestone', hardness: 2.0, preferredTool: ToolType.PICKAXE,
  topColor: 0x606060, sideColor: 0x606060, dropItemId: 'cobblestone', texturePattern: 5
}));

BlockRegistry.BEDROCK = BlockRegistry.register(new BlockProperties({
  id: 5, name: 'BEDROCK', displayName: 'Bedrock', hardness: 999.0, topColor: 0x222222, sideColor: 0x222222, dropItemId: ''
}));

BlockRegistry.OAK_LOG = BlockRegistry.register(new BlockProperties({
  id: 6, name: 'OAK_LOG', displayName: 'Oak Log', hardness: 2.0, preferredTool: ToolType.AXE,
  topColor: 0x8D6E63, sideColor: 0x5D4037, dropItemId: 'oak_log', texturePattern: 2
}));

BlockRegistry.OAK_LEAVES = BlockRegistry.register(new BlockProperties({
  id: 7, name: 'OAK_LEAVES', displayName: 'Oak Leaves', hardness: 0.2, isTransparent: true,
  topColor: 0x388E3C, sideColor: 0x2E7D32, dropItemId: 'oak_sapling', texturePattern: 3
}));

BlockRegistry.OAK_PLANKS = BlockRegistry.register(new BlockProperties({
  id: 8, name: 'OAK_PLANKS', displayName: 'Oak Planks', hardness: 2.0, preferredTool: ToolType.AXE,
  topColor: 0xA1887F, sideColor: 0x8D6E63, dropItemId: 'oak_planks'
}));

BlockRegistry.SAND = BlockRegistry.register(new BlockProperties({
  id: 9, name: 'SAND', displayName: 'Sand', hardness: 0.5, preferredTool: ToolType.SHOVEL,
  topColor: 0xFBC02D, sideColor: 0xFBC02D, dropItemId: 'sand'
}));

BlockRegistry.WATER = BlockRegistry.register(new BlockProperties({
  id: 10, name: 'WATER', displayName: 'Water', isSolid: false, isTransparent: true, isLiquid: true,
  topColor: 0x1976D2, sideColor: 0x1565C0, dropItemId: ''
}));

BlockRegistry.COAL_ORE = BlockRegistry.register(new BlockProperties({
  id: 11, name: 'COAL_ORE', displayName: 'Coal Ore', hardness: 3.0, preferredTool: ToolType.PICKAXE,
  topColor: 0x616161, sideColor: 0x616161, dropItemId: 'coal', texturePattern: 4
}));

BlockRegistry.IRON_ORE = BlockRegistry.register(new BlockProperties({
  id: 12, name: 'IRON_ORE', displayName: 'Iron Ore', hardness: 3.0, preferredTool: ToolType.PICKAXE,
  topColor: 0x757575, sideColor: 0x757575, dropItemId: 'raw_iron', texturePattern: 4
}));

BlockRegistry.GOLD_ORE = BlockRegistry.register(new BlockProperties({
  id: 13, name: 'GOLD_ORE', displayName: 'Gold Ore', hardness: 3.0, preferredTool: ToolType.PICKAXE,
  topColor: 0x757575, sideColor: 0x757575, dropItemId: 'raw_gold', texturePattern: 4
}));

BlockRegistry.DIAMOND_ORE = BlockRegistry.register(new BlockProperties({
  id: 14, name: 'DIAMOND_ORE', displayName: 'Diamond Ore', hardness: 3.5, preferredTool: ToolType.PICKAXE,
  topColor: 0x616161, sideColor: 0x616161, dropItemId: 'diamond', texturePattern: 4
}));

BlockRegistry.CRAFTING_TABLE = BlockRegistry.register(new BlockProperties({
  id: 15, name: 'CRAFTING_TABLE', displayName: 'Crafting Table', hardness: 2.5, preferredTool: ToolType.AXE,
  topColor: 0x8D6E63, sideColor: 0x6D4C41, dropItemId: 'crafting_table'
}));

BlockRegistry.FURNACE = BlockRegistry.register(new BlockProperties({
  id: 16, name: 'FURNACE', displayName: 'Furnace', hardness: 3.5, preferredTool: ToolType.PICKAXE,
  topColor: 0x546E7A, sideColor: 0x455A64, dropItemId: 'furnace'
}));

BlockRegistry.CHEST = BlockRegistry.register(new BlockProperties({
  id: 17, name: 'CHEST', displayName: 'Chest', hardness: 2.5, preferredTool: ToolType.AXE,
  topColor: 0x8D6E63, sideColor: 0x6D4C41, dropItemId: 'chest'
}));

BlockRegistry.TORCH = BlockRegistry.register(new BlockProperties({
  id: 18, name: 'TORCH', displayName: 'Torch', isSolid: false, isTransparent: true, lightEmission: 14, hardness: 0.1,
  topColor: 0xFFB300, sideColor: 0xFF8F00, dropItemId: 'torch'
}));

BlockRegistry.GLASS = BlockRegistry.register(new BlockProperties({
  id: 19, name: 'GLASS', displayName: 'Glass', hardness: 0.3, isTransparent: true,
  topColor: 0xB3E5FC, sideColor: 0x81D4FA, dropItemId: ''
}));

BlockRegistry.TNT = BlockRegistry.register(new BlockProperties({
  id: 20, name: 'TNT', displayName: 'TNT', hardness: 0.0,
  topColor: 0xD32F2F, sideColor: 0xC62828, dropItemId: 'tnt'
}));

BlockRegistry.GLOWSTONE = BlockRegistry.register(new BlockProperties({
  id: 21, name: 'GLOWSTONE', displayName: 'Glowstone', hardness: 0.3, lightEmission: 15,
  topColor: 0xFFE082, sideColor: 0xFFD54F, dropItemId: 'glowstone'
}));

BlockRegistry.OBSIDIAN = BlockRegistry.register(new BlockProperties({
  id: 22, name: 'OBSIDIAN', displayName: 'Obsidian', hardness: 50.0, preferredTool: ToolType.PICKAXE,
  topColor: 0x1A237E, sideColor: 0x0D47A1, dropItemId: 'obsidian'
}));

BlockRegistry.RED_FLOWER = BlockRegistry.register(new BlockProperties({
  id: 23, name: 'RED_FLOWER', displayName: 'Red Flower', isSolid: false, isTransparent: true, hardness: 0.0,
  topColor: 0xE53935, sideColor: 0xD32F2F, dropItemId: 'red_flower'
}));

BlockRegistry.YELLOW_FLOWER = BlockRegistry.register(new BlockProperties({
  id: 24, name: 'YELLOW_FLOWER', displayName: 'Yellow Flower', isSolid: false, isTransparent: true, hardness: 0.0,
  topColor: 0xFDD835, sideColor: 0xFBC02D, dropItemId: 'yellow_flower'
}));


class ItemDefinition {
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.maxStack = config.maxStack !== undefined ? config.maxStack : 64;
    this.category = config.category || ItemCategory.MATERIALS;
    this.blockId = config.blockId || null;
    this.attackDamage = config.attackDamage || 1.0;
    this.miningSpeed = config.miningSpeed || 1.0;
    this.toolType = config.toolType || ToolType.NONE;
    this.durability = config.durability !== undefined ? config.durability : -1;
    this.foodHeal = config.foodHeal || 0;
    this.foodSaturation = config.foodSaturation || 0;
    this.armorDefense = config.armorDefense || 0;
    this.armorSlot = config.armorSlot || ArmorSlot.NONE;
    this.iconColor = config.iconColor || 0xFFFFFFFF;
    this.iconSymbol = config.iconSymbol || '■';
    this.description = config.description || '';
  }
}

class ItemStack {
  constructor(item, count = 1, currentDurability = -1) {
    this.item = item;
    this.count = count;
    this.currentDurability = currentDurability === -1 ? item.durability : currentDurability;
  }

  get isEmpty() {
    return this.count <= 0 || this.item.id === 'empty';
  }

  copy(count = this.count) {
    return new ItemStack(this.item, count, this.currentDurability);
  }
}

class ItemRegistry {
  static items = {};

  static register(item) {
    this.items[item.id.toLowerCase()] = item;
    return item;
  }

  static get(id) {
    if (!id) return this.EMPTY;
    return this.items[id.toLowerCase()] || this.EMPTY;
  }

  static registerCustomItem(item) {
    this.register(item);
  }
}

ItemRegistry.EMPTY = ItemRegistry.register(new ItemDefinition({ id: 'empty', name: 'Empty', maxStack: 0, iconSymbol: '' }));

// Blocks as items
ItemRegistry.DIRT = ItemRegistry.register(new ItemDefinition({ id: 'dirt', name: 'Dirt', category: ItemCategory.BLOCKS, blockId: 2, iconSymbol: '🟫' }));
ItemRegistry.GRASS_BLOCK = ItemRegistry.register(new ItemDefinition({ id: 'grass_block', name: 'Grass Block', category: ItemCategory.BLOCKS, blockId: 1, iconSymbol: '🟩' }));
ItemRegistry.STONE = ItemRegistry.register(new ItemDefinition({ id: 'stone', name: 'Stone', category: ItemCategory.BLOCKS, blockId: 3, iconSymbol: '⬜' }));
ItemRegistry.COBBLESTONE = ItemRegistry.register(new ItemDefinition({ id: 'cobblestone', name: 'Cobblestone', category: ItemCategory.BLOCKS, blockId: 4, iconSymbol: '🔲' }));
ItemRegistry.OAK_LOG = ItemRegistry.register(new ItemDefinition({ id: 'oak_log', name: 'Oak Log', category: ItemCategory.BLOCKS, blockId: 6, iconSymbol: '🪵' }));
ItemRegistry.OAK_PLANKS = ItemRegistry.register(new ItemDefinition({ id: 'oak_planks', name: 'Oak Planks', category: ItemCategory.BLOCKS, blockId: 8, iconSymbol: '🪵' }));
ItemRegistry.SAND = ItemRegistry.register(new ItemDefinition({ id: 'sand', name: 'Sand', category: ItemCategory.BLOCKS, blockId: 9, iconSymbol: '🟨' }));
ItemRegistry.GLASS = ItemRegistry.register(new ItemDefinition({ id: 'glass', name: 'Glass', category: ItemCategory.BLOCKS, blockId: 19, iconSymbol: '🔲' }));
ItemRegistry.CRAFTING_TABLE = ItemRegistry.register(new ItemDefinition({ id: 'crafting_table', name: 'Crafting Table', category: ItemCategory.BLOCKS, blockId: 15, iconSymbol: '📦' }));
ItemRegistry.FURNACE = ItemRegistry.register(new ItemDefinition({ id: 'furnace', name: 'Furnace', category: ItemCategory.BLOCKS, blockId: 16, iconSymbol: '🔥' }));
ItemRegistry.CHEST = ItemRegistry.register(new ItemDefinition({ id: 'chest', name: 'Chest', category: ItemCategory.BLOCKS, blockId: 17, iconSymbol: '🧰' }));
ItemRegistry.TORCH = ItemRegistry.register(new ItemDefinition({ id: 'torch', name: 'Torch', category: ItemCategory.BLOCKS, blockId: 18, iconSymbol: '🕯️' }));
ItemRegistry.TNT = ItemRegistry.register(new ItemDefinition({ id: 'tnt', name: 'TNT', category: ItemCategory.BLOCKS, blockId: 20, iconSymbol: '🧨' }));
ItemRegistry.GLOWSTONE = ItemRegistry.register(new ItemDefinition({ id: 'glowstone', name: 'Glowstone', category: ItemCategory.BLOCKS, blockId: 21, iconSymbol: '💡' }));
ItemRegistry.OBSIDIAN = ItemRegistry.register(new ItemDefinition({ id: 'obsidian', name: 'Obsidian', category: ItemCategory.BLOCKS, blockId: 22, iconSymbol: '⬛' }));

// Tools & Weapons
ItemRegistry.WOODEN_PICKAXE = ItemRegistry.register(new ItemDefinition({ id: 'wooden_pickaxe', name: 'Wooden Pickaxe', category: ItemCategory.TOOLS, miningSpeed: 2.0, toolType: ToolType.PICKAXE, durability: 60, iconSymbol: '⛏️' }));
ItemRegistry.STONE_PICKAXE = ItemRegistry.register(new ItemDefinition({ id: 'stone_pickaxe', name: 'Stone Pickaxe', category: ItemCategory.TOOLS, miningSpeed: 4.0, toolType: ToolType.PICKAXE, durability: 132, iconSymbol: '⛏️' }));
ItemRegistry.IRON_PICKAXE = ItemRegistry.register(new ItemDefinition({ id: 'iron_pickaxe', name: 'Iron Pickaxe', category: ItemCategory.TOOLS, miningSpeed: 6.0, toolType: ToolType.PICKAXE, durability: 250, iconSymbol: '⛏️' }));
ItemRegistry.DIAMOND_PICKAXE = ItemRegistry.register(new ItemDefinition({ id: 'diamond_pickaxe', name: 'Diamond Pickaxe', category: ItemCategory.TOOLS, miningSpeed: 10.0, toolType: ToolType.PICKAXE, durability: 1561, iconSymbol: '💎' }));

ItemRegistry.WOODEN_SWORD = ItemRegistry.register(new ItemDefinition({ id: 'wooden_sword', name: 'Wooden Sword', category: ItemCategory.WEAPONS, attackDamage: 4.0, toolType: ToolType.SWORD, durability: 60, iconSymbol: '🗡️' }));
ItemRegistry.STONE_SWORD = ItemRegistry.register(new ItemDefinition({ id: 'stone_sword', name: 'Stone Sword', category: ItemCategory.WEAPONS, attackDamage: 5.0, toolType: ToolType.SWORD, durability: 132, iconSymbol: '🗡️' }));
ItemRegistry.IRON_SWORD = ItemRegistry.register(new ItemDefinition({ id: 'iron_sword', name: 'Iron Sword', category: ItemCategory.WEAPONS, attackDamage: 6.0, toolType: ToolType.SWORD, durability: 250, iconSymbol: '⚔️' }));
ItemRegistry.DIAMOND_SWORD = ItemRegistry.register(new ItemDefinition({ id: 'diamond_sword', name: 'Diamond Sword', category: ItemCategory.WEAPONS, attackDamage: 8.0, toolType: ToolType.SWORD, durability: 1561, iconSymbol: '⚔️' }));

ItemRegistry.WOODEN_AXE = ItemRegistry.register(new ItemDefinition({ id: 'wooden_axe', name: 'Wooden Axe', category: ItemCategory.TOOLS, miningSpeed: 2.0, toolType: ToolType.AXE, durability: 60, iconSymbol: '🪓' }));
ItemRegistry.BOW = ItemRegistry.register(new ItemDefinition({ id: 'bow', name: 'Bow', category: ItemCategory.WEAPONS, durability: 384, iconSymbol: '🏹' }));
ItemRegistry.ARROW = ItemRegistry.register(new ItemDefinition({ id: 'arrow', name: 'Arrow', category: ItemCategory.WEAPONS, iconSymbol: '🏹' }));

// Armor
ItemRegistry.IRON_HELMET = ItemRegistry.register(new ItemDefinition({ id: 'iron_helmet', name: 'Iron Helmet', category: ItemCategory.ARMOR, armorDefense: 2, armorSlot: ArmorSlot.HEAD, durability: 165, iconSymbol: '🪖' }));
ItemRegistry.IRON_CHESTPLATE = ItemRegistry.register(new ItemDefinition({ id: 'iron_chestplate', name: 'Iron Chestplate', category: ItemCategory.ARMOR, armorDefense: 6, armorSlot: ArmorSlot.CHEST, durability: 240, iconSymbol: '🛡️' }));
ItemRegistry.DIAMOND_CHESTPLATE = ItemRegistry.register(new ItemDefinition({ id: 'diamond_chestplate', name: 'Diamond Chestplate', category: ItemCategory.ARMOR, armorDefense: 8, armorSlot: ArmorSlot.CHEST, durability: 528, iconSymbol: '🛡️' }));

// Food & Items
ItemRegistry.BREAD = ItemRegistry.register(new ItemDefinition({ id: 'bread', name: 'Bread', category: ItemCategory.FOOD, foodHeal: 5, foodSaturation: 6.0, iconSymbol: '🍞' }));
ItemRegistry.RAW_PORKCHOP = ItemRegistry.register(new ItemDefinition({ id: 'raw_porkchop', name: 'Raw Porkchop', category: ItemCategory.FOOD, foodHeal: 3, foodSaturation: 1.8, iconSymbol: '🥩' }));
ItemRegistry.COOKED_PORKCHOP = ItemRegistry.register(new ItemDefinition({ id: 'cooked_porkchop', name: 'Cooked Porkchop', category: ItemCategory.FOOD, foodHeal: 8, foodSaturation: 12.8, iconSymbol: '🍖' }));
ItemRegistry.GOLDEN_APPLE = ItemRegistry.register(new ItemDefinition({ id: 'golden_apple', name: 'Golden Apple', category: ItemCategory.FOOD, foodHeal: 10, foodSaturation: 9.6, iconSymbol: '🍎' }));

// Materials & Ores
ItemRegistry.STICK = ItemRegistry.register(new ItemDefinition({ id: 'stick', name: 'Stick', iconSymbol: '🥢' }));
ItemRegistry.COAL = ItemRegistry.register(new ItemDefinition({ id: 'coal', name: 'Coal', iconSymbol: '⬛' }));
ItemRegistry.RAW_IRON = ItemRegistry.register(new ItemDefinition({ id: 'raw_iron', name: 'Raw Iron', iconSymbol: '🪨' }));
ItemRegistry.IRON_INGOT = ItemRegistry.register(new ItemDefinition({ id: 'iron_ingot', name: 'Iron Ingot', iconSymbol: '🪙' }));
ItemRegistry.RAW_GOLD = ItemRegistry.register(new ItemDefinition({ id: 'raw_gold', name: 'Raw Gold', iconSymbol: '🪨' }));
ItemRegistry.GOLD_INGOT = ItemRegistry.register(new ItemDefinition({ id: 'gold_ingot', name: 'Gold Ingot', iconSymbol: '🪙' }));
ItemRegistry.DIAMOND = ItemRegistry.register(new ItemDefinition({ id: 'diamond', name: 'Diamond', iconSymbol: '💎' }));
ItemRegistry.EMERALD = ItemRegistry.register(new ItemDefinition({ id: 'emerald', name: 'Emerald', iconSymbol: '💚' }));
ItemRegistry.GUNPOWDER = ItemRegistry.register(new ItemDefinition({ id: 'gunpowder', name: 'Gunpowder', iconSymbol: '🎆' }));
ItemRegistry.BONE = ItemRegistry.register(new ItemDefinition({ id: 'bone', name: 'Bone', iconSymbol: '🦴' }));


class RecipeRegistry {
  static craftingRecipes = [];
  static smeltingRecipes = [];

  static init() {
    this.craftingRecipes = [
      { id: 'planks', result: new ItemStack(ItemRegistry.OAK_PLANKS, 4), width: 1, height: 1, ingredients: ['oak_log'], isShapeless: true, description: '4 Oak Planks from Log' },
      { id: 'sticks', result: new ItemStack(ItemRegistry.STICK, 4), width: 1, height: 2, ingredients: ['oak_planks', 'oak_planks'], description: '4 Sticks from 2 Planks' },
      { id: 'crafting_table', result: new ItemStack(ItemRegistry.CRAFTING_TABLE, 1), width: 2, height: 2, ingredients: ['oak_planks', 'oak_planks', 'oak_planks', 'oak_planks'], description: 'Crafting Table' },
      { id: 'chest', result: new ItemStack(ItemRegistry.CHEST, 1), width: 3, height: 3, ingredients: ['oak_planks', 'oak_planks', 'oak_planks', 'oak_planks', null, 'oak_planks', 'oak_planks', 'oak_planks', 'oak_planks'], description: 'Chest' },
      { id: 'furnace', result: new ItemStack(ItemRegistry.FURNACE, 1), width: 3, height: 3, ingredients: ['cobblestone', 'cobblestone', 'cobblestone', 'cobblestone', null, 'cobblestone', 'cobblestone', 'cobblestone', 'cobblestone'], description: 'Furnace' },
      { id: 'wooden_pickaxe', result: new ItemStack(ItemRegistry.WOODEN_PICKAXE, 1), width: 3, height: 3, ingredients: ['oak_planks', 'oak_planks', 'oak_planks', null, 'stick', null, null, 'stick', null], description: 'Wooden Pickaxe' },
      { id: 'stone_pickaxe', result: new ItemStack(ItemRegistry.STONE_PICKAXE, 1), width: 3, height: 3, ingredients: ['cobblestone', 'cobblestone', 'cobblestone', null, 'stick', null, null, 'stick', null], description: 'Stone Pickaxe' },
      { id: 'iron_pickaxe', result: new ItemStack(ItemRegistry.IRON_PICKAXE, 1), width: 3, height: 3, ingredients: ['iron_ingot', 'iron_ingot', 'iron_ingot', null, 'stick', null, null, 'stick', null], description: 'Iron Pickaxe' },
      { id: 'diamond_pickaxe', result: new ItemStack(ItemRegistry.DIAMOND_PICKAXE, 1), width: 3, height: 3, ingredients: ['diamond', 'diamond', 'diamond', null, 'stick', null, null, 'stick', null], description: 'Diamond Pickaxe' },
      { id: 'wooden_sword', result: new ItemStack(ItemRegistry.WOODEN_SWORD, 1), width: 1, height: 3, ingredients: ['oak_planks', 'oak_planks', 'stick'], description: 'Wooden Sword' },
      { id: 'stone_sword', result: new ItemStack(ItemRegistry.STONE_SWORD, 1), width: 1, height: 3, ingredients: ['cobblestone', 'cobblestone', 'stick'], description: 'Stone Sword' },
      { id: 'iron_sword', result: new ItemStack(ItemRegistry.IRON_SWORD, 1), width: 1, height: 3, ingredients: ['iron_ingot', 'iron_ingot', 'stick'], description: 'Iron Sword' },
      { id: 'diamond_sword', result: new ItemStack(ItemRegistry.DIAMOND_SWORD, 1), width: 1, height: 3, ingredients: ['diamond', 'diamond', 'stick'], description: 'Diamond Sword' },
      { id: 'torch', result: new ItemStack(ItemRegistry.TORCH, 4), width: 1, height: 2, ingredients: ['coal', 'stick'], description: '4 Torches' },
      { id: 'bow', result: new ItemStack(ItemRegistry.BOW, 1), width: 3, height: 3, ingredients: ['stick', 'stick', null, 'stick', null, 'stick', null, null, null], isShapeless: true, description: 'Bow' },
      { id: 'tnt', result: new ItemStack(ItemRegistry.TNT, 1), width: 3, height: 3, ingredients: ['gunpowder', 'sand', 'gunpowder', 'sand', 'gunpowder', 'sand', 'gunpowder', 'sand', 'gunpowder'], description: 'TNT' }
    ];

    this.smeltingRecipes = [
      { inputId: 'raw_iron', result: new ItemStack(ItemRegistry.IRON_INGOT, 1), cookTimeSeconds: 5.0 },
      { inputId: 'raw_gold', result: new ItemStack(ItemRegistry.GOLD_INGOT, 1), cookTimeSeconds: 6.0 },
      { inputId: 'cobblestone', result: new ItemStack(ItemRegistry.STONE, 1), cookTimeSeconds: 4.0 },
      { inputId: 'sand', result: new ItemStack(ItemRegistry.GLASS, 1), cookTimeSeconds: 4.0 },
      { inputId: 'raw_porkchop', result: new ItemStack(ItemRegistry.COOKED_PORKCHOP, 1), cookTimeSeconds: 5.0 }
    ];
  }

  static findSmeltingRecipe(inputId) {
    if (!inputId) return null;
    return this.smeltingRecipes.find(r => r.inputId.toLowerCase() === inputId.toLowerCase()) || null;
  }

  static getFuelBurnTime(fuelId) {
    if (!fuelId) return 0;
    const id = fuelId.toLowerCase();
    if (id === 'coal') return 80.0;
    if (id === 'oak_log' || id === 'oak_planks') return 15.0;
    if (id === 'stick') return 5.0;
    return 0;
  }

  static addCustomRecipe(recipe) {
    this.craftingRecipes.unshift(recipe);
  }
}

RecipeRegistry.init();
