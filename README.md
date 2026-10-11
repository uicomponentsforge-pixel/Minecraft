# ⛏️ VoxelCraft 3D Web

A web-based 3D voxel survival and crafting game engine built with HTML5, CSS3, JavaScript (ES6), WebGL, and Web Audio API.

---

## 📖 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Getting Started](#getting-started)
- [Controls & Gameplay Guide](#controls--gameplay-guide)
- [Project Architecture](#project-architecture)
- [License](#license)

---

## 🔍 Overview

**VoxelCraft 3D Web** is a feature-packed, lightweight voxel sandbox game running natively in the browser without external rendering engine dependencies. Built using raw WebGL shaders and pure JavaScript, VoxelCraft delivers procedural terrain generation, dynamic day/night cycles, survival mechanics, block interaction, inventory and crafting systems, and modular extension capabilities.

---

## ✨ Features

- **Procedural World Generation**: Infinite-feel voxel terrain generation driven by 2D/3D Simplex noise, including biomes, trees, ore deposits, and underground caves.
- **Game Modes**:
  - **Survival**: Experience health, hunger, oxygen, leveling/XP, hostile mobs (Creepers, Zombies, Skeletons), and damage mechanics.
  - **Creative**: Infinite block placement, flying capabilities, and damage immunity.
- **Custom WebGL Rendering Engine**:
  - Custom WebGL shader pipelines supporting dynamic lighting based on time of day.
  - Camera toggling between First-Person, Third-Person Back, and Third-Person Front views.
  - Dynamic visual overlays for underwater diving and taking damage.
- **Synthesized Web Audio Engine**:
  - Real-time procedural audio synthesis using Web Audio API oscillators.
  - Unique sound effects for mining, block placement, footsteps, jumping, damage, eating, and crafting.
- **Crafting & Smelting Systems**:
  - Interactive 3x3 Workbench Crafting Grid with pre-defined recipes.
  - Smelting Furnace with fuel, input, and output slots.
- **Trader Outpost Marketplace**:
  - In-game currency trading system exchanging diamonds, emeralds, gold, and iron for special items and gear.
- **Mod Manager & Custom Blocks**:
  - Register custom blocks dynamically with custom display names, hex colors, hardness values, and preferred tool types.
- **Save State Management & Export/Import**:
  - World persistent storage powered by `localStorage`.
  - Save code export and import via JSON payload for cross-platform play and sharing.
- **Multiplayer Lobby UI**:
  - Local co-op server browser UI for listing and joining multiplayer world rooms.

---

## 🚀 Getting Started

### Prerequisites

- Node.js (v14 or higher recommended)

### Running the Application

1. Clone or navigate to the project directory:
   ```bash
   cd voxelcraft-web
   ```

2. Start the local server:
   ```bash
   npm start
   ```
   *Alternatively, you can run directly with Node:*
   ```bash
   node server.js
   ```

3. Open your browser and navigate to:
   ```
   http://localhost:3000
   ```

---

## 🎮 Controls & Gameplay Guide

### Keyboard & Mouse (Desktop)

| Action | Control |
| :--- | :--- |
| **Move Forward / Backward** | `W` / `S` |
| **Strafe Left / Right** | `A` / `D` |
| **Jump / Swim Up** | `Space` |
| **Open Inventory** | `E` |
| **Select Hotbar Slot** | `1` - `9` |
| **Look / Rotate Camera** | Drag Mouse on Game Canvas |
| **Mine / Attack** | Click & Hold `MINE` or Left Mouse |
| **Place Block / Interact** | Click `PLACE` / Right Click |

### Touch & On-Screen Controls (Mobile/Tablet)

| Element | Description |
| :--- | :--- |
| **Virtual Joystick** | Located on the bottom left for 360° player movement |
| **Right Touch Area** | Drag across the right half of the screen to adjust camera look / pitch / yaw |
| **Action Buttons** | `⛏️ MINE`, `🧱 PLACE`, `⬆️ JUMP`, and `🏃 SPRINT` / `🚶 SNEAK` |

---

## 📁 Project Architecture

```
.
├── index.html           # Main application HTML shell & UI overlays
├── styles.css           # UI layout, HUD components, and modal styling
├── package.json         # Node.js project manifest
├── server.js            # Node.js HTTP server delivering static assets & MIME types
└── js/
    ├── app.js           # Core application state controller & UI event binding
    ├── simplex-noise.js # Fast Simplex Noise implementation for procedural generation
    ├── sound-engine.js  # Web Audio API sound generator
    ├── storage.js       # LocalStorage wrapper & JSON Import/Export repository
    ├── voxel-models.js # Block definitions, items, recipes, stacks, and mob entities
    ├── voxel-renderer.js# WebGL shader compiler, buffer manager, and camera controller
    └── voxel-world.js   # World matrix state, player physics, mob AI, raycasting, & time cycle
```

---

## 📄 License

This project is open-source and available under the MIT License.
