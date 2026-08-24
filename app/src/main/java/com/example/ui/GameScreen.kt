package com.example.ui

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.*
import com.example.model.*
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun GameScreen(
    world: VoxelWorld,
    worldName: String,
    onOpenInventory: () -> Unit,
    onOpenCrafting: () -> Unit,
    onOpenFurnace: () -> Unit,
    onOpenPause: () -> Unit,
    onOpenMarketplace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var renderer by remember { mutableStateOf<VoxelRenderer?>(null) }
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }

    // UI State derived from world
    var health by remember { mutableFloatStateOf(world.health) }
    var hunger by remember { mutableFloatStateOf(world.hunger) }
    var oxygen by remember { mutableFloatStateOf(world.oxygen) }
    var level by remember { mutableIntStateOf(world.level) }
    var xpProgress by remember { mutableFloatStateOf(0f) }
    var hurtFlash by remember { mutableFloatStateOf(0f) }
    var isDead by remember { mutableStateOf(world.isPlayerDead) }
    var selectedSlot by remember { mutableIntStateOf(world.selectedHotbarIndex) }
    var isSneakActive by remember { mutableStateOf(world.isSneaking) }
    var isSprintActive by remember { mutableStateOf(world.isSprinting) }
    var cameraMode by remember { mutableStateOf(CameraMode.FIRST_PERSON) }
    var timeOfDay by remember { mutableFloatStateOf(world.timeOfDay) }
    var breakProgress by remember { mutableFloatStateOf(0f) }

    // Action button states
    var isAttackingOrBreaking by remember { mutableStateOf(false) }

    // Virtual Joystick state
    var joystickOffset by remember { mutableStateOf(Offset.Zero) }

    // Game loop tick
    LaunchedEffect(Unit) {
        var lastTime = System.nanoTime()
        while (true) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
            lastTime = now

            // Apply Joystick movement
            val joyLen = hypot(joystickOffset.x, joystickOffset.y)
            if (joyLen > 10f) {
                val normX = (joystickOffset.x / 60f).coerceIn(-1f, 1f)
                val normY = (joystickOffset.y / 60f).coerceIn(-1f, 1f)

                val yawRad = Math.toRadians(world.playerYaw.toDouble())
                val cosY = cos(yawRad).toFloat()
                val sinY = sin(yawRad).toFloat()

                // Strafe (normX) and Forward/Back (-normY)
                val moveSpeed = if (world.isSprinting) 7.5f else 4.5f
                val fwd = -normY
                val strafe = normX

                world.playerVx = (-sinY * fwd + cosY * strafe) * moveSpeed
                world.playerVz = (cosY * fwd + sinY * strafe) * moveSpeed

                // Footstep sound
                if (world.isGrounded && (world.playerVx != 0f || world.playerVz != 0f)) {
                    if (now % 350_000_000 < 50_000_000) {
                        world.soundEngine.playStep()
                    }
                }
            }

            // Raycast for crosshair target
            val yawRad = Math.toRadians(world.playerYaw.toDouble())
            val pitchRad = Math.toRadians(world.playerPitch.toDouble())
            val fwdX = (-sin(yawRad) * cos(pitchRad)).toFloat()
            val fwdY = (-sin(pitchRad)).toFloat()
            val fwdZ = (cos(yawRad) * cos(pitchRad)).toFloat()

            val ray = world.raycast(
                world.playerX, world.playerY + 1.62f, world.playerZ,
                fwdX, fwdY, fwdZ,
                maxDistance = 5.0f
            )

            // Breaking / Attacking
            if (isAttackingOrBreaking && ray.hit) {
                // Check if aiming at mob first
                var hitMob = false
                for (mob in world.mobs) {
                    val dist = hypot(mob.x - world.playerX, mob.z - world.playerZ)
                    if (dist < 3.5f && abs(mob.y - world.playerY) < 2.5f) {
                        world.attackMob(mob)
                        hitMob = true
                        isAttackingOrBreaking = false
                        break
                    }
                }
                if (!hitMob) {
                    world.updateBlockBreaking(dt, ray)
                }
            } else {
                world.resetBlockBreaking()
            }

            // Update world physics & AI
            world.update(dt)

            // Update UI properties
            health = world.health
            hunger = world.hunger
            oxygen = world.oxygen
            level = world.level
            xpProgress = if (world.xpForNextLevel > 0) world.xp.toFloat() / world.xpForNextLevel else 0f
            hurtFlash = world.hurtFlash
            isDead = world.isPlayerDead
            timeOfDay = world.timeOfDay
            breakProgress = world.breakProgress

            delay(16) // ~60fps UI tick
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // 1. OpenGL 3D Surface View
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    val r = VoxelRenderer(world)
                    renderer = r
                    setRenderer(r)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    glSurfaceView = this

                    // Keyboard input support for Desktop / Chromebook
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                    setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN) {
                            when (keyCode) {
                                KeyEvent.KEYCODE_W -> {
                                    val yawRad = Math.toRadians(world.playerYaw.toDouble())
                                    world.playerVx = -sin(yawRad).toFloat() * 5f
                                    world.playerVz = cos(yawRad).toFloat() * 5f
                                    true
                                }
                                KeyEvent.KEYCODE_S -> {
                                    val yawRad = Math.toRadians(world.playerYaw.toDouble())
                                    world.playerVx = sin(yawRad).toFloat() * 4f
                                    world.playerVz = -cos(yawRad).toFloat() * 4f
                                    true
                                }
                                KeyEvent.KEYCODE_A -> {
                                    val yawRad = Math.toRadians(world.playerYaw.toDouble())
                                    world.playerVx = -cos(yawRad).toFloat() * 4f
                                    world.playerVz = -sin(yawRad).toFloat() * 4f
                                    true
                                }
                                KeyEvent.KEYCODE_D -> {
                                    val yawRad = Math.toRadians(world.playerYaw.toDouble())
                                    world.playerVx = cos(yawRad).toFloat() * 4f
                                    world.playerVz = sin(yawRad).toFloat() * 4f
                                    true
                                }
                                KeyEvent.KEYCODE_SPACE -> {
                                    if (world.isGrounded || world.isInWater) {
                                        world.playerVy = if (world.isInWater) 4.5f else 7.5f
                                        world.soundEngine.playJump()
                                    }
                                    true
                                }
                                KeyEvent.KEYCODE_E -> {
                                    onOpenInventory()
                                    true
                                }
                                KeyEvent.KEYCODE_1 -> { world.selectedHotbarIndex = 0; selectedSlot = 0; true }
                                KeyEvent.KEYCODE_2 -> { world.selectedHotbarIndex = 1; selectedSlot = 1; true }
                                KeyEvent.KEYCODE_3 -> { world.selectedHotbarIndex = 2; selectedSlot = 2; true }
                                KeyEvent.KEYCODE_4 -> { world.selectedHotbarIndex = 3; selectedSlot = 3; true }
                                KeyEvent.KEYCODE_5 -> { world.selectedHotbarIndex = 4; selectedSlot = 4; true }
                                KeyEvent.KEYCODE_6 -> { world.selectedHotbarIndex = 5; selectedSlot = 5; true }
                                KeyEvent.KEYCODE_7 -> { world.selectedHotbarIndex = 6; selectedSlot = 6; true }
                                KeyEvent.KEYCODE_8 -> { world.selectedHotbarIndex = 7; selectedSlot = 7; true }
                                KeyEvent.KEYCODE_9 -> { world.selectedHotbarIndex = 8; selectedSlot = 8; true }
                                else -> false
                            }
                        } else false
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Camera Look Drag Area (Right half of screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Sensitivity adjustment
                        val sens = 0.22f
                        world.playerYaw = (world.playerYaw + dragAmount.x * sens) % 360f
                        world.playerPitch = (world.playerPitch + dragAmount.y * sens).coerceIn(-89f, 89f)
                    }
                }
        )

        // 3. Hurt Screen Flash Effect
        if (hurtFlash > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = (hurtFlash * 0.7f).coerceIn(0.1f, 0.65f)))
            )
        }

        // 4. Underwater Overlay
        if (world.isInWater) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x331976D2))
            )
        }

        // 5. Center Crosshair (+)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(4.dp).background(Color.White, CircleShape))
            }
        }

        // 6. Breaking Progress Radial / Bar Indicator
        if (breakProgress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 28.dp)
            ) {
                LinearProgressIndicator(
                    progress = { breakProgress },
                    modifier = Modifier.width(60.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFFD54F),
                    trackColor = Color(0x66000000)
                )
            }
        }

        // 7. Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // World Info Chip
            Surface(
                color = Color(0x9910141E),
                shape = RoundedCornerShape(8.dp),
                border = borderStroke()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDay = timeOfDay in 4000f..14000f
                    Text(
                        text = if (isDay) "☀️" else "🌙",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$worldName (XYZ: ${world.playerX.toInt()}, ${world.playerY.toInt()}, ${world.playerZ.toInt()})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Quick Top Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Camera Mode Switch
                SmallIconButton(
                    icon = Icons.Default.Visibility,
                    tag = "camera_toggle",
                    onClick = {
                        cameraMode = when (cameraMode) {
                            CameraMode.FIRST_PERSON -> CameraMode.THIRD_PERSON_BACK
                            CameraMode.THIRD_PERSON_BACK -> CameraMode.THIRD_PERSON_FRONT
                            CameraMode.THIRD_PERSON_FRONT -> CameraMode.FIRST_PERSON
                        }
                        renderer?.cameraMode = cameraMode
                    }
                )

                // Marketplace / Trade Outpost Button
                SmallIconButton(
                    icon = Icons.Default.Storefront,
                    tag = "marketplace_btn",
                    onClick = onOpenMarketplace
                )

                // Pause Button
                SmallIconButton(
                    icon = Icons.Default.Pause,
                    tag = "pause_btn",
                    onClick = onOpenPause
                )
            }
        }

        // 8. Virtual Left Joystick
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 24.dp, bottom = 28.dp)
                .size(130.dp)
                .background(Color(0x44000000), CircleShape)
                .border(2.dp, Color(0x66FFFFFF), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { joystickOffset = Offset.Zero },
                        onDragCancel = { joystickOffset = Offset.Zero }
                    ) { change, dragAmount ->
                        change.consume()
                        val newOffset = joystickOffset + dragAmount
                        val maxDist = 50f
                        val dist = hypot(newOffset.x, newOffset.y)
                        joystickOffset = if (dist > maxDist) {
                            Offset(newOffset.x / dist * maxDist, newOffset.y / dist * maxDist)
                        } else newOffset
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(joystickOffset.x.roundToInt(), joystickOffset.y.roundToInt()) }
                    .size(54.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xCC5B8C3A), Color(0x99356E26))),
                        CircleShape
                    )
                    .border(2.dp, Color(0xEEFFFFFF), CircleShape)
            )
        }

        // 9. Mobile Action Floating Buttons (Right side)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Row 1: Attack / Break & Place / Use
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Break / Mine / Attack Button
                ActionButton(
                    text = "⛏️ MINE",
                    tag = "mine_btn",
                    bgBrush = Brush.linearGradient(listOf(Color(0xFFE53935), Color(0xFFC62828))),
                    onPressedChange = { isPressed ->
                        isAttackingOrBreaking = isPressed
                    }
                )

                // Place / Interact Button
                ActionButton(
                    text = "🧱 PLACE",
                    tag = "place_btn",
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF43A047), Color(0xFF2E7D32))),
                    onClick = {
                        val yawRad = Math.toRadians(world.playerYaw.toDouble())
                        val pitchRad = Math.toRadians(world.playerPitch.toDouble())
                        val fwdX = (-sin(yawRad) * cos(pitchRad)).toFloat()
                        val fwdY = (-sin(pitchRad)).toFloat()
                        val fwdZ = (cos(yawRad) * cos(pitchRad)).toFloat()

                        val ray = world.raycast(
                            world.playerX, world.playerY + 1.62f, world.playerZ,
                            fwdX, fwdY, fwdZ,
                            maxDistance = 5.0f
                        )

                        if (ray.hit) {
                            val targetBlock = world.getBlock(ray.blockX, ray.blockY, ray.blockZ)
                            when (targetBlock) {
                                BlockRegistry.CRAFTING_TABLE.id -> onOpenCrafting()
                                BlockRegistry.FURNACE.id -> onOpenFurnace()
                                else -> {
                                    val equipped = world.getEquippedItem()
                                    if (equipped.item.category == ItemCategory.FOOD) {
                                        world.eatFood()
                                    } else if (equipped.item.id == ItemRegistry.BOW.id) {
                                        world.shootBow()
                                    } else {
                                        world.placeBlock(ray)
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // Row 2: Jump & Sneak & Inventory
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Sneak / Sprint Toggle
                ActionButton(
                    text = if (isSneakActive) "🚶 SNEAK" else "🏃 SPRINT",
                    tag = "sneak_sprint_btn",
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF5E35B1), Color(0xFF4527A0))),
                    onClick = {
                        if (isSneakActive) {
                            isSneakActive = false
                            isSprintActive = true
                            world.isSneaking = false
                            world.isSprinting = true
                        } else if (isSprintActive) {
                            isSprintActive = false
                            isSneakActive = false
                            world.isSneaking = false
                            world.isSprinting = false
                        } else {
                            isSneakActive = true
                            isSprintActive = false
                            world.isSneaking = true
                            world.isSprinting = false
                        }
                    }
                )

                // Jump / Swim Up Button
                ActionButton(
                    text = "⬆️ JUMP",
                    tag = "jump_btn",
                    bgBrush = Brush.linearGradient(listOf(Color(0xFF0288D1), Color(0xFF01579B))),
                    onClick = {
                        if (world.isGrounded || world.isInWater) {
                            world.playerVy = if (world.isInWater) 5.0f else 7.8f
                            world.soundEngine.playJump()
                        }
                    }
                )
            }
        }

        // 10. Minecraft Bottom Center HUD (Hearts, Drumsticks, XP, Hotbar)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Survival Status: Health Hearts & Hunger Drumsticks
            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hearts (20 HP = 10 Hearts)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val fullHearts = (health / 2).toInt()
                    val hasHalfHeart = (health % 2) >= 1.0f
                    for (i in 0 until 10) {
                        Text(
                            text = when {
                                i < fullHearts -> "❤️"
                                i == fullHearts && hasHalfHeart -> "💔"
                                else -> "🖤"
                            },
                            fontSize = 12.sp
                        )
                    }
                }

                // Hunger Drumsticks (20 Hunger = 10 Drumsticks)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val fullFood = (hunger / 2).toInt()
                    val hasHalfFood = (hunger % 2) >= 1.0f
                    for (i in 0 until 10) {
                        Text(
                            text = when {
                                i < fullFood -> "🍗"
                                i == fullFood && hasHalfFood -> "🍖"
                                else -> "🦴"
                            },
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Oxygen Bubbles (when in water)
            if (world.isInWater && oxygen < 10f) {
                Row(
                    modifier = Modifier.padding(bottom = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val bubbles = oxygen.toInt()
                    for (i in 0 until 10) {
                        Text(
                            text = if (i < bubbles) "🫧" else "◌",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // XP Bar & Level Badge
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(10.dp),
                contentAlignment = Alignment.Center
            ) {
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF76FF03),
                    trackColor = Color(0xFF263238)
                )
                Text(
                    text = "$level",
                    color = Color(0xFF76FF03),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 9-Slot Hotbar + Inventory Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                for (i in 0..8) {
                    val stack = world.hotbar[i]
                    val isSelected = (selectedSlot == i)

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xEE455A64) else Color(0x99263238))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFFFFD54F) else Color(0x66FFFFFF),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                selectedSlot = i
                                world.selectedHotbarIndex = i
                            }
                            .testTag("hotbar_slot_$i"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!stack.isEmpty) {
                            Text(
                                text = stack.item.iconSymbol,
                                fontSize = 16.sp
                            )
                            if (stack.count > 1) {
                                Text(
                                    text = "${stack.count}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(1.dp)
                                )
                            }
                            // Durability Bar
                            if (stack.item.durability > 0 && stack.currentDurability < stack.item.durability) {
                                val ratio = stack.currentDurability.toFloat() / stack.item.durability
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(2.dp)
                                        .background(Color.Red)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(ratio)
                                            .background(Color.Green)
                                    )
                                }
                            }
                        }
                    }
                }

                // Inventory Open Button (...)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC1A237E))
                        .border(1.5.dp, Color(0xFF90CAF9), RoundedCornerShape(6.dp))
                        .clickable { onOpenInventory() }
                        .testTag("inventory_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Backpack,
                        contentDescription = "Inventory",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 11. "You Died!" Game Over Overlay Screen
        if (isDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE5C0000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "You Died!",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Score: ${world.xp + world.level * 100}",
                        color = Color(0xFFFFD54F),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { world.respawn() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.testTag("respawn_btn")
                    ) {
                        Text("Respawn", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0x9910141E),
        shape = RoundedCornerShape(8.dp),
        border = borderStroke(),
        modifier = Modifier.size(36.dp).testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    tag: String,
    bgBrush: Brush,
    onClick: (() -> Unit)? = null,
    onPressedChange: ((Boolean) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(width = 82.dp, height = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgBrush)
            .border(1.5.dp, Color(0x88FFFFFF), RoundedCornerShape(10.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null) { onClick() }
                } else if (onPressedChange != null) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val isDown = event.changes.any { it.pressed }
                                onPressedChange(isDown)
                            }
                        }
                    }
                } else Modifier
            )
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
    }
}

private fun borderStroke() = androidx.compose.foundation.BorderStroke(1.dp, Color(0x44FFFFFF))
