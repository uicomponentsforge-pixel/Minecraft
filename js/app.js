/**
 * Application State Controller & UI Event Dispatcher
 * Converted from MainActivity.kt & Compose UI Screens
 */

class VoxelCraftApp {
  constructor() {
    this.currentScreen = 'WORLD_SELECT';
    this.activeWorld = null;
    this.activeWorldEntity = null;
    this.renderer = null;

    this.isAttackingOrBreaking = false;
    this.joystickVector = { x: 0, y: 0 };
    this.isLookDragging = false;
    this.lastTouchPos = { x: 0, y: 0 };

    this.initDOMReferences();
    this.bindEvents();
    this.renderWorldList();
    this.renderModsList();
  }

  initDOMReferences() {
    this.worldSelectScreen = document.getElementById('world-select-screen');
    this.gameScreen = document.getElementById('game-screen');
    this.canvas = document.getElementById('gl-canvas');

    // Modals
    this.createWorldModal = document.getElementById('create-world-modal');
    this.inventoryModal = document.getElementById('inventory-modal');
    this.craftingModal = document.getElementById('crafting-modal');
    this.furnaceModal = document.getElementById('furnace-modal');
    this.pauseModal = document.getElementById('pause-modal');
    this.marketplaceModal = document.getElementById('marketplace-modal');
    this.modManagerModal = document.getElementById('mod-manager-modal');
    this.customBlockModal = document.getElementById('custom-block-modal');
    this.multiplayerModal = document.getElementById('multiplayer-modal');
    this.exportModal = document.getElementById('export-modal');
    this.importModal = document.getElementById('import-modal');
    this.deathScreen = document.getElementById('death-screen');

    // HUD Elements
    this.dayNightIcon = document.getElementById('day-night-icon');
    this.worldInfoText = document.getElementById('world-info-text');
    this.heartsContainer = document.getElementById('hearts-container');
    this.foodContainer = document.getElementById('food-container');
    this.oxygenContainer = document.getElementById('oxygen-container');
    this.xpBarFill = document.getElementById('xp-bar-fill');
    this.levelBadge = document.getElementById('level-badge');
    this.hotbarContainer = document.getElementById('hotbar');
    this.breakProgressBar = document.getElementById('break-progress-bar');
    this.breakProgressContainer = document.getElementById('break-progress-container');
    this.hurtOverlay = document.getElementById('hurt-overlay');
    this.underwaterOverlay = document.getElementById('underwater-overlay');
  }

  bindEvents() {
    // World Select Buttons
    document.getElementById('create-world-btn').addEventListener('click', () => this.showModal(this.createWorldModal));
    document.getElementById('confirm-create-world').addEventListener('click', () => this.handleCreateWorld());
    document.getElementById('cancel-create-world').addEventListener('click', () => this.hideModal(this.createWorldModal));

    document.getElementById('import-world-btn').addEventListener('click', () => this.showModal(this.importModal));
    document.getElementById('confirm-import-json-btn').addEventListener('click', () => this.handleImportJson());
    document.getElementById('close-import-btn').addEventListener('click', () => this.hideModal(this.importModal));

    document.getElementById('multiplayer-btn').addEventListener('click', () => this.openMultiplayerLobby());
    document.getElementById('close-multiplayer-btn').addEventListener('click', () => this.hideModal(this.multiplayerModal));

    document.getElementById('mods-btn').addEventListener('click', () => this.showModal(this.modManagerModal));
    document.getElementById('close-mods-btn').addEventListener('click', () => this.hideModal(this.modManagerModal));

    document.getElementById('create-custom-block-btn').addEventListener('click', () => this.showModal(this.customBlockModal));
    document.getElementById('cancel-custom-block').addEventListener('click', () => this.hideModal(this.customBlockModal));
    document.getElementById('confirm-custom-block').addEventListener('click', () => this.handleRegisterCustomBlock());

    // In-Game Top HUD
    document.getElementById('camera-toggle-btn').addEventListener('click', () => this.toggleCameraMode());
    document.getElementById('marketplace-hud-btn').addEventListener('click', () => this.openMarketplace());
    document.getElementById('close-marketplace-btn').addEventListener('click', () => this.hideModal(this.marketplaceModal));

    document.getElementById('pause-hud-btn').addEventListener('click', () => this.showModal(this.pauseModal));
    document.getElementById('resume-game-btn').addEventListener('click', () => this.hideModal(this.pauseModal));
    document.getElementById('toggle-sound-btn').addEventListener('click', (e) => this.toggleSound(e.target));
    document.getElementById('toggle-gamemode-btn').addEventListener('click', (e) => this.toggleGameMode(e.target));
    document.getElementById('save-quit-btn').addEventListener('click', () => this.saveAndQuitToMenu());

    // In-Game Bottom HUD
    document.getElementById('inventory-btn').addEventListener('click', () => this.openInventory());
    document.getElementById('close-inventory-btn').addEventListener('click', () => this.hideModal(this.inventoryModal));
    document.getElementById('close-crafting-btn').addEventListener('click', () => this.hideModal(this.craftingModal));
    document.getElementById('close-furnace-btn').addEventListener('click', () => this.hideModal(this.furnaceModal));
    document.getElementById('close-export-btn').addEventListener('click', () => this.hideModal(this.exportModal));
    document.getElementById('copy-export-json-btn').addEventListener('click', () => this.copyExportJson());
    document.getElementById('respawn-btn').addEventListener('click', () => this.respawnPlayer());

    // Keyboard Input
    window.addEventListener('keydown', (e) => this.handleKeyDown(e));

    // Look Area Dragging (Touch/Mouse)
    const lookArea = document.getElementById('look-touch-area');
    const startLook = (x, y) => {
      this.isLookDragging = true;
      this.lastTouchPos = { x, y };
    };
    const moveLook = (x, y) => {
      if (!this.isLookDragging || !this.activeWorld) return;
      const dx = x - this.lastTouchPos.x;
      const dy = y - this.lastTouchPos.y;
      this.lastTouchPos = { x, y };

      const sens = 0.22;
      this.activeWorld.playerYaw = (this.activeWorld.playerYaw + dx * sens) % 360;
      this.activeWorld.playerPitch = Math.max(-89, Math.min(89, this.activeWorld.playerPitch + dy * sens));
    };
    const endLook = () => { this.isLookDragging = false; };

    lookArea.addEventListener('mousedown', (e) => startLook(e.clientX, e.clientY));
    window.addEventListener('mousemove', (e) => moveLook(e.clientX, e.clientY));
    window.addEventListener('mouseup', endLook);

    lookArea.addEventListener('touchstart', (e) => {
      if (e.touches.length > 0) startLook(e.touches[0].clientX, e.touches[0].clientY);
    });
    window.addEventListener('touchmove', (e) => {
      if (e.touches.length > 0) moveLook(e.touches[0].clientX, e.touches[0].clientY);
    });
    window.addEventListener('touchend', endLook);

    // Joystick Touch Control
    const joystick = document.getElementById('joystick-container');
    const knob = document.getElementById('joystick-knob');
    let joystickActive = false;

    const handleJoystickMove = (clientX, clientY) => {
      const rect = joystick.getBoundingClientRect();
      const centerX = rect.left + rect.width / 2;
      const centerY = rect.top + rect.height / 2;
      const dx = clientX - centerX;
      const dy = clientY - centerY;
      const dist = Math.hypot(dx, dy);
      const maxDist = 45;

      const normX = Math.max(-1, Math.min(1, dx / maxDist));
      const normY = Math.max(-1, Math.min(1, dy / maxDist));
      this.joystickVector = { x: normX, y: normY };

      const clampedDist = Math.min(dist, maxDist);
      const angle = Math.atan2(dy, dx);
      knob.style.transform = `translate(${Math.cos(angle) * clampedDist}px, ${Math.sin(angle) * clampedDist}px)`;
    };

    const resetJoystick = () => {
      joystickActive = false;
      this.joystickVector = { x: 0, y: 0 };
      knob.style.transform = `translate(0px, 0px)`;
    };

    joystick.addEventListener('mousedown', (e) => { joystickActive = true; handleJoystickMove(e.clientX, e.clientY); });
    window.addEventListener('mousemove', (e) => { if (joystickActive) handleJoystickMove(e.clientX, e.clientY); });
    window.addEventListener('mouseup', resetJoystick);

    joystick.addEventListener('touchstart', (e) => {
      joystickActive = true;
      if (e.touches.length > 0) handleJoystickMove(e.touches[0].clientX, e.touches[0].clientY);
    });
    window.addEventListener('touchmove', (e) => {
      if (joystickActive && e.touches.length > 0) handleJoystickMove(e.touches[0].clientX, e.touches[0].clientY);
    });
    window.addEventListener('touchend', resetJoystick);

    // Mobile Action Buttons
    const mineBtn = document.getElementById('mine-btn');
    mineBtn.addEventListener('mousedown', () => { this.isAttackingOrBreaking = true; });
    window.addEventListener('mouseup', () => { this.isAttackingOrBreaking = false; });
    mineBtn.addEventListener('touchstart', (e) => { e.preventDefault(); this.isAttackingOrBreaking = true; });
    window.addEventListener('touchend', () => { this.isAttackingOrBreaking = false; });

    document.getElementById('place-btn').addEventListener('click', () => this.handlePlaceAction());
    document.getElementById('jump-btn').addEventListener('click', () => this.handleJumpAction());
    document.getElementById('sneak-sprint-btn').addEventListener('click', (e) => this.toggleSneakSprint(e.target));
  }

  showModal(modal) { modal.classList.remove('hidden'); }
  hideModal(modal) { modal.classList.add('hidden'); }

  renderWorldList() {
    const container = document.getElementById('worlds-list');
    container.innerHTML = '';
    const worlds = StorageRepository.getAllWorlds();

    worlds.forEach((w) => {
      const card = document.createElement('div');
      card.className = 'world-card';
      card.setAttribute('data-testid', `world_card_${w.id}`);

      const dateStr = new Date(w.lastPlayedTime || Date.now()).toLocaleDateString();

      card.innerHTML = `
        <div>
          <div class="world-title">${w.name}</div>
          <div class="world-meta">
            <span>Mode: ${w.gameMode} | Difficulty: ${w.difficulty}</span>
            <span>Seed: ${w.seed}</span>
            <span>Last Played: ${dateStr}</span>
          </div>
        </div>
        <div class="card-actions">
          <button class="btn btn-primary play-btn">▶ Play</button>
          <button class="btn btn-secondary export-btn" data-testid="export_world_${w.id}">📤 Export</button>
          <button class="btn btn-danger delete-btn" data-testid="delete_world_${w.id}">🗑 Delete</button>
        </div>
      `;

      card.querySelector('.play-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        this.startWorld(w);
      });

      card.querySelector('.export-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        const json = StorageRepository.exportWorldJson(w);
        document.getElementById('export-json-text').value = json;
        this.showModal(this.exportModal);
      });

      card.querySelector('.delete-btn').addEventListener('click', (e) => {
        e.stopPropagation();
        StorageRepository.deleteWorld(w.id);
        this.renderWorldList();
      });

      container.appendChild(card);
    });
  }

  startWorld(entity) {
    this.activeWorldEntity = entity;
    this.activeWorld = StorageRepository.loadWorld(entity);

    this.worldSelectScreen.classList.remove('active');
    this.gameScreen.classList.add('active');
    this.currentScreen = 'IN_GAME';

    this.renderer = new VoxelRenderer(this.canvas, this.activeWorld);
    this.startGameLoop();
  }

  handleCreateWorld() {
    const name = document.getElementById('new-world-name').value || 'New Survival World';
    const seed = parseInt(document.getElementById('new-world-seed').value) || 84920482;
    const gm = document.getElementById('new-world-gamemode').value;
    const diff = document.getElementById('new-world-difficulty').value;

    const id = `world_${Math.random().toString(36).substring(2, 10)}`;
    const entity = {
      id, name, seed, gameMode: gm, difficulty: diff, dayTime: 6000,
      playerX: 8.5, playerY: 36, playerZ: 8.5, lastPlayedTime: Date.now()
    };

    StorageRepository.saveWorldEntity(entity);
    this.hideModal(this.createWorldModal);
    this.startWorld(entity);
  }

  handleImportJson() {
    const jsonStr = document.getElementById('import-json-text').value;
    try {
      const entity = StorageRepository.importWorldJson(jsonStr);
      this.hideModal(this.importModal);
      this.startWorld(entity);
    } catch (e) {
      alert("Failed to import save code. Please check JSON format.");
    }
  }

  copyExportJson() {
    const text = document.getElementById('export-json-text').value;
    navigator.clipboard.writeText(text);
    alert("Save code copied to clipboard!");
  }

  saveAndQuitToMenu() {
    if (this.activeWorld && this.activeWorldEntity) {
      StorageRepository.saveWorld(this.activeWorldEntity.id, this.activeWorldEntity.name, this.activeWorld);
    }
    this.activeWorld = null;
    this.activeWorldEntity = null;
    this.hideModal(this.pauseModal);

    this.gameScreen.classList.remove('active');
    this.worldSelectScreen.classList.add('active');
    this.currentScreen = 'WORLD_SELECT';
    this.renderWorldList();
  }

  startGameLoop() {
    let lastTime = performance.now();

    const loop = (now) => {
      if (this.currentScreen !== 'IN_GAME' || !this.activeWorld) return;

      const dt = Math.min(0.05, Math.max(0.001, (now - lastTime) / 1000.0));
      lastTime = now;

      // Joystick Movement
      if (Math.hypot(this.joystickVector.x, this.joystickVector.y) > 0.1) {
        const yawRad = (this.activeWorld.playerYaw * Math.PI) / 180.0;
        const cosY = Math.cos(yawRad);
        const sinY = Math.sin(yawRad);

        const moveSpeed = this.activeWorld.isSprinting ? 7.5 : 4.5;
        const fwd = -this.joystickVector.y;
        const strafe = this.joystickVector.x;

        this.activeWorld.playerVx = (-sinY * fwd + cosY * strafe) * moveSpeed;
        this.activeWorld.playerVz = (cosY * fwd + sinY * strafe) * moveSpeed;
      }

      // Raycast Target
      const yawRad = (this.activeWorld.playerYaw * Math.PI) / 180.0;
      const pitchRad = (this.activeWorld.playerPitch * Math.PI) / 180.0;
      const fwdX = -Math.sin(yawRad) * Math.cos(pitchRad);
      const fwdY = -Math.sin(pitchRad);
      const fwdZ = Math.cos(yawRad) * Math.cos(pitchRad);

      const ray = this.activeWorld.raycast(
        this.activeWorld.playerX, this.activeWorld.playerY + 1.62, this.activeWorld.playerZ,
        fwdX, fwdY, fwdZ, 5.0
      );

      // Mine / Attack Action
      if (this.isAttackingOrBreaking && ray.hit) {
        let hitMob = false;
        for (const mob of this.activeWorld.mobs) {
          const dist = Math.hypot(mob.x - this.activeWorld.playerX, mob.z - this.activeWorld.playerZ);
          if (dist < 3.5 && Math.abs(mob.y - this.activeWorld.playerY) < 2.5) {
            this.activeWorld.attackMob(mob);
            hitMob = true;
            this.isAttackingOrBreaking = false;
            break;
          }
        }
        if (!hitMob) {
          this.activeWorld.updateBlockBreaking(dt, ray);
        }
      } else {
        this.activeWorld.resetBlockBreaking();
      }

      this.activeWorld.update(dt);
      this.updateHUD();

      if (this.renderer) {
        this.renderer.render();
      }

      requestAnimationFrame(loop);
    };

    requestAnimationFrame(loop);
  }

  updateHUD() {
    if (!this.activeWorld) return;
    const w = this.activeWorld;

    const isDay = w.timeOfDay >= 4000 && w.timeOfDay <= 14000;
    this.dayNightIcon.textContent = isDay ? '☀️' : '🌙';
    this.worldInfoText.textContent = `${this.activeWorldEntity.name} (XYZ: ${Math.floor(w.playerX)}, ${Math.floor(w.playerY)}, ${Math.floor(w.playerZ)})`;

    // Hearts (20 HP = 10 Hearts)
    this.heartsContainer.innerHTML = '';
    const fullHearts = Math.floor(w.health / 2);
    const hasHalfHeart = (w.health % 2) >= 1.0;
    for (let i = 0; i < 10; i++) {
      const span = document.createElement('span');
      if (i < fullHearts) span.textContent = '❤️';
      else if (i === fullHearts && hasHalfHeart) span.textContent = '💔';
      else span.textContent = '🖤';
      this.heartsContainer.appendChild(span);
    }

    // Food (20 Hunger = 10 Drumsticks)
    this.foodContainer.innerHTML = '';
    const fullFood = Math.floor(w.hunger / 2);
    const hasHalfFood = (w.hunger % 2) >= 1.0;
    for (let i = 0; i < 10; i++) {
      const span = document.createElement('span');
      if (i < fullFood) span.textContent = '🍗';
      else if (i === fullFood && hasHalfFood) span.textContent = '🍖';
      else span.textContent = '🦴';
      this.foodContainer.appendChild(span);
    }

    // Oxygen
    this.oxygenContainer.innerHTML = '';
    if (w.isInWater && w.oxygen < 10) {
      for (let i = 0; i < 10; i++) {
        const span = document.createElement('span');
        span.textContent = i < Math.floor(w.oxygen) ? '🫧' : '◌';
        this.oxygenContainer.appendChild(span);
      }
    }

    // XP
    const ratio = w.xpForNextLevel > 0 ? (w.xp / w.xpForNextLevel) * 100 : 0;
    this.xpBarFill.style.width = `${ratio}%`;
    this.levelBadge.textContent = w.level;

    // Hotbar
    this.hotbarContainer.innerHTML = '';
    for (let i = 0; i < 9; i++) {
      const stack = w.hotbar[i];
      const slot = document.createElement('div');
      slot.className = `item-slot ${w.selectedHotbarIndex === i ? 'selected' : ''}`;
      slot.setAttribute('data-testid', `hotbar_slot_${i}`);

      if (!stack.isEmpty) {
        slot.textContent = stack.item.iconSymbol;
        if (stack.count > 1) {
          const countBadge = document.createElement('span');
          countBadge.className = 'item-count';
          countBadge.textContent = stack.count;
          slot.appendChild(countBadge);
        }
        if (stack.item.durability > 0 && stack.currentDurability < stack.item.durability) {
          const duraBar = document.createElement('div');
          duraBar.className = 'durability-bar';
          const fill = document.createElement('div');
          fill.className = 'durability-fill';
          fill.style.width = `${(stack.currentDurability / stack.item.durability) * 100}%`;
          duraBar.appendChild(fill);
          slot.appendChild(duraBar);
        }
      }

      slot.addEventListener('click', () => { w.selectedHotbarIndex = i; this.updateHUD(); });
      this.hotbarContainer.appendChild(slot);
    }

    // Mining bar
    if (w.breakProgress > 0) {
      this.breakProgressContainer.style.display = 'block';
      this.breakProgressBar.style.width = `${Math.min(100, w.breakProgress * 100)}%`;
    } else {
      this.breakProgressContainer.style.display = 'none';
    }

    // Hurt & Underwater overlays
    this.hurtOverlay.style.opacity = w.hurtFlash > 0 ? `${Math.min(0.65, w.hurtFlash * 1.5)}` : '0';
    this.underwaterOverlay.style.opacity = w.isInWater ? '1' : '0';

    // Death Screen
    if (w.isPlayerDead) {
      document.getElementById('final-score').textContent = `Score: ${w.xp + w.level * 100}`;
      this.showModal(this.deathScreen);
    } else {
      this.hideModal(this.deathScreen);
    }
  }

  handlePlaceAction() {
    if (!this.activeWorld) return;
    const w = this.activeWorld;

    const yawRad = (w.playerYaw * Math.PI) / 180.0;
    const pitchRad = (w.playerPitch * Math.PI) / 180.0;
    const fwdX = -Math.sin(yawRad) * Math.cos(pitchRad);
    const fwdY = -Math.sin(pitchRad);
    const fwdZ = Math.cos(yawRad) * Math.cos(pitchRad);

    const ray = w.raycast(w.playerX, w.playerY + 1.62, w.playerZ, fwdX, fwdY, fwdZ, 5.0);
    if (ray.hit) {
      const targetBlock = w.getBlock(ray.blockX, ray.blockY, ray.blockZ);
      if (targetBlock === BlockRegistry.CRAFTING_TABLE.id) {
        this.openCrafting();
      } else if (targetBlock === BlockRegistry.FURNACE.id) {
        this.openFurnace();
      } else {
        const equipped = w.getEquippedItem();
        if (equipped.item.category === ItemCategory.FOOD) {
          w.eatFood();
        } else if (equipped.item.id === 'bow') {
          w.shootBow();
        } else {
          w.placeBlock(ray);
        }
      }
    }
  }

  handleJumpAction() {
    if (!this.activeWorld) return;
    if (this.activeWorld.isGrounded || this.activeWorld.isInWater) {
      this.activeWorld.playerVy = this.activeWorld.isInWater ? 5.0 : 7.8;
      this.activeWorld.soundEngine.playJump();
    }
  }

  toggleSneakSprint(btn) {
    if (!this.activeWorld) return;
    if (this.activeWorld.isSneaking) {
      this.activeWorld.isSneaking = false;
      this.activeWorld.isSprinting = true;
      btn.textContent = '🏃 SPRINT';
    } else if (this.activeWorld.isSprinting) {
      this.activeWorld.isSneaking = false;
      this.activeWorld.isSprinting = false;
      btn.textContent = '🚶 WALK';
    } else {
      this.activeWorld.isSneaking = true;
      this.activeWorld.isSprinting = false;
      btn.textContent = '🚶 SNEAK';
    }
  }

  toggleCameraMode() {
    if (!this.renderer) return;
    if (this.renderer.cameraMode === CameraMode.FIRST_PERSON) this.renderer.cameraMode = CameraMode.THIRD_PERSON_BACK;
    else if (this.renderer.cameraMode === CameraMode.THIRD_PERSON_BACK) this.renderer.cameraMode = CameraMode.THIRD_PERSON_FRONT;
    else this.renderer.cameraMode = CameraMode.FIRST_PERSON;
  }

  toggleSound(btn) {
    if (!this.activeWorld) return;
    this.activeWorld.soundEngine.isMuted = !this.activeWorld.soundEngine.isMuted;
    btn.textContent = `🔊 Sound: ${this.activeWorld.soundEngine.isMuted ? 'OFF' : 'ON'}`;
  }

  toggleGameMode(btn) {
    if (!this.activeWorld) return;
    this.activeWorld.gameMode = this.activeWorld.gameMode === GameMode.SURVIVAL ? GameMode.CREATIVE : GameMode.SURVIVAL;
    btn.textContent = `🎮 Mode: ${this.activeWorld.gameMode}`;
  }

  respawnPlayer() {
    if (this.activeWorld) {
      this.activeWorld.respawn();
      this.hideModal(this.deathScreen);
    }
  }

  openInventory() {
    if (!this.activeWorld) return;
    const w = this.activeWorld;

    const renderSlots = (container, array) => {
      container.innerHTML = '';
      array.forEach((stack, idx) => {
        const slot = document.createElement('div');
        slot.className = 'item-slot';
        if (!stack.isEmpty) {
          slot.textContent = stack.item.iconSymbol;
          if (stack.count > 1) {
            const countBadge = document.createElement('span');
            countBadge.className = 'item-count';
            countBadge.textContent = stack.count;
            slot.appendChild(countBadge);
          }
        }
        container.appendChild(slot);
      });
    };

    renderSlots(document.getElementById('armor-slots'), w.armor);
    renderSlots(document.getElementById('inventory-slots'), w.inventory);
    renderSlots(document.getElementById('inv-hotbar-slots'), w.hotbar);

    this.showModal(this.inventoryModal);
  }

  openCrafting() {
    if (!this.activeWorld) return;

    const grid = document.getElementById('crafting-grid');
    grid.innerHTML = '';
    const matrix = Array.from({ length: 9 }, () => null);

    for (let i = 0; i < 9; i++) {
      const slot = document.createElement('div');
      slot.className = 'item-slot';
      grid.appendChild(slot);
    }

    const recipesList = document.getElementById('recipes-list');
    recipesList.innerHTML = '';
    RecipeRegistry.craftingRecipes.forEach(r => {
      const div = document.createElement('div');
      div.className = 'recipe-item';
      div.innerHTML = `<span>${r.result.item.iconSymbol} ${r.result.item.name} (x${r.result.count})</span><strong>Craft</strong>`;
      div.addEventListener('click', () => {
        this.activeWorld.addItemToInventory(r.result.copy());
        this.activeWorld.soundEngine.playCraftSuccess();
        alert(`Crafted ${r.result.item.name}!`);
      });
      recipesList.appendChild(div);
    });

    this.showModal(this.craftingModal);
  }

  openFurnace() {
    this.showModal(this.furnaceModal);
  }

  openMarketplace() {
    if (!this.activeWorld) return;
    const w = this.activeWorld;

    const countItem = (id) => {
      let count = 0;
      w.hotbar.forEach(s => { if (s.item.id.toLowerCase() === id.toLowerCase()) count += s.count; });
      w.inventory.forEach(s => { if (s.item.id.toLowerCase() === id.toLowerCase()) count += s.count; });
      return count;
    };

    document.getElementById('mkt-diamonds').textContent = countItem('diamond');
    document.getElementById('mkt-emeralds').textContent = countItem('emerald');
    document.getElementById('mkt-gold').textContent = countItem('gold_ingot');
    document.getElementById('mkt-iron').textContent = countItem('iron_ingot');

    const offersList = document.getElementById('trade-offers-list');
    offersList.innerHTML = '';

    const trades = [
      { id: 'trade_diamond_pickaxe', title: 'Enchanted Diamond Pickaxe', desc: 'Slices through obsidian with ease', cost: 'emerald', costCount: 5, reward: 'diamond_pickaxe', rewardCount: 1 },
      { id: 'trade_diamond_sword', title: 'Vorpal Diamond Blade', desc: 'Critical damage against Creepers & Skeletons', cost: 'emerald', costCount: 6, reward: 'diamond_sword', rewardCount: 1 },
      { id: 'trade_golden_apples', title: 'Golden Apples (x3)', desc: 'Restores full health and hunger', cost: 'gold_ingot', costCount: 12, reward: 'golden_apple', rewardCount: 3 },
      { id: 'trade_tnt_bundle', title: 'TNT Bundle (x8)', desc: 'High explosive blocks for excavation', cost: 'iron_ingot', costCount: 8, reward: 'tnt', rewardCount: 8 }
    ];

    trades.forEach(t => {
      const card = document.createElement('div');
      card.className = 'offer-card';
      card.innerHTML = `
        <div class="offer-info">
          <h4>${t.title}</h4>
          <p>${t.desc}</p>
          <p>Cost: <strong>${t.costCount} ${t.cost}</strong></p>
        </div>
        <button class="btn btn-primary">Trade</button>
      `;

      card.querySelector('button').addEventListener('click', () => {
        const rewardItem = ItemRegistry.get(t.reward);
        w.addItemToInventory(new ItemStack(rewardItem, t.rewardCount));
        w.soundEngine.playCraftSuccess();
        alert(`Traded for ${t.title}!`);
      });

      offersList.appendChild(card);
    });

    this.showModal(this.marketplaceModal);
  }

  renderModsList() {
    const container = document.getElementById('mods-list');
    container.innerHTML = '';
    const mods = StorageRepository.getAllMods();

    mods.forEach(m => {
      const item = document.createElement('div');
      item.className = 'mod-item';
      item.innerHTML = `
        <div>
          <strong>${m.name} (${m.version})</strong>
          <p style="font-size: 11px; color: #B0BEC5;">${m.description}</p>
        </div>
        <button class="btn ${m.isEnabled ? 'btn-primary' : 'btn-secondary'}">${m.isEnabled ? 'ENABLED' : 'DISABLED'}</button>
      `;

      item.querySelector('button').addEventListener('click', () => {
        m.isEnabled = !m.isEnabled;
        StorageRepository.saveMods(mods);
        this.renderModsList();
      });

      container.appendChild(item);
    });
  }

  handleRegisterCustomBlock() {
    const name = document.getElementById('custom-block-name').value || 'Ruby Ore';
    const colorHex = parseInt(document.getElementById('custom-block-color').value.replace('#', '0x'), 16) || 0xE53935;
    const hardness = parseFloat(document.getElementById('custom-block-hardness').value) || 1.5;
    const tool = document.getElementById('custom-block-tool').value;

    const blockId = 30 + Math.floor(Math.random() * 100);
    const block = new BlockProperties({
      id: blockId, name: name.toUpperCase().replace(/\s+/g, '_'), displayName: name,
      topColor: colorHex, sideColor: colorHex, hardness, preferredTool: tool, category: 'MODDED'
    });
    BlockRegistry.registerCustomBlock(block);

    const item = new ItemDefinition({
      id: name.toLowerCase().replace(/\s+/g, '_'), name, category: ItemCategory.BLOCKS, blockId, iconSymbol: '🟦'
    });
    ItemRegistry.registerCustomItem(item);

    this.hideModal(this.customBlockModal);
    alert(`Registered custom block: ${name}!`);
  }

  openMultiplayerLobby() {
    const container = document.getElementById('mp-rooms-list');
    container.innerHTML = '';

    const rooms = [
      { id: 'room_1', name: 'Oak Valley Survival Server', host: 'Alex_Voxel', players: 3 },
      { id: 'room_2', name: 'Creative Builders Realm', host: 'BuilderPro', players: 5 }
    ];

    rooms.forEach(r => {
      const item = document.createElement('div');
      item.className = 'room-item';
      item.innerHTML = `
        <div>
          <strong>${r.name}</strong>
          <p style="font-size: 11px; color: #B0BEC5;">Host: ${r.host} | Players: ${r.players}/8</p>
        </div>
        <button class="btn btn-primary">Join Room</button>
      `;
      item.querySelector('button').addEventListener('click', () => {
        this.hideModal(this.multiplayerModal);
        const worlds = StorageRepository.getAllWorlds();
        this.startWorld(worlds[0]);
      });
      container.appendChild(item);
    });

    this.showModal(this.multiplayerModal);
  }

  handleKeyDown(e) {
    if (!this.activeWorld || this.currentScreen !== 'IN_GAME') return;
    const w = this.activeWorld;

    const yawRad = (w.playerYaw * Math.PI) / 180.0;
    const cosY = Math.cos(yawRad);
    const sinY = Math.sin(yawRad);
    const speed = w.isSprinting ? 7.5 : 4.5;

    switch (e.code) {
      case 'KeyW': w.playerVx = -sinY * speed; w.playerVz = cosY * speed; break;
      case 'KeyS': w.playerVx = sinY * speed; w.playerVz = -cosY * speed; break;
      case 'KeyA': w.playerVx = -cosY * speed; w.playerVz = -sinY * speed; break;
      case 'KeyD': w.playerVx = cosY * speed; w.playerVz = sinY * speed; break;
      case 'Space': this.handleJumpAction(); break;
      case 'KeyE': this.openInventory(); break;
      case 'Digit1': w.selectedHotbarIndex = 0; break;
      case 'Digit2': w.selectedHotbarIndex = 1; break;
      case 'Digit3': w.selectedHotbarIndex = 2; break;
      case 'Digit4': w.selectedHotbarIndex = 3; break;
      case 'Digit5': w.selectedHotbarIndex = 4; break;
      case 'Digit6': w.selectedHotbarIndex = 5; break;
      case 'Digit7': w.selectedHotbarIndex = 6; break;
      case 'Digit8': w.selectedHotbarIndex = 7; break;
      case 'Digit9': w.selectedHotbarIndex = 8; break;
    }
  }
}

// Instantiate on DOM load
window.addEventListener('DOMContentLoaded', () => {
  window.app = new VoxelCraftApp();
});
