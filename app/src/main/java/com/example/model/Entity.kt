package com.example.model

import kotlin.math.*

enum class EntityType {
    PLAYER,
    REMOTE_PLAYER,
    ZOMBIE,
    SKELETON,
    CREEPER,
    PIG,
    COW,
    ARROW,
    ITEM_DROP,
    PARTICLE
}

enum class MobState {
    IDLE,
    WANDER,
    CHASE,
    ATTACK,
    HISSING,
    FLEEING,
    DEAD
}

open class Entity(
    val id: String,
    val type: EntityType,
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var yaw: Float = 0f,
    var pitch: Float = 0f,
    var width: Float = 0.6f,
    var height: Float = 1.8f,
    var isGrounded: Boolean = false,
    var health: Float = 20f,
    var maxHealth: Float = 20f,
    var isDead: Boolean = false
) {
    var hurtTime: Float = 0f // flash red when > 0
    var animTime: Float = 0f
    var walkAnim: Float = 0f
}

class MobEntity(
    id: String,
    type: EntityType,
    x: Float,
    y: Float,
    z: Float,
    var mobName: String = type.name,
    initialHealth: Float = 20f,
    var attackDamage: Float = 3f,
    var moveSpeed: Float = 2.2f,
    var attackRange: Float = 1.5f,
    var followRange: Float = 16f
) : Entity(
    id = id,
    type = type,
    x = x,
    y = y,
    z = z,
    health = initialHealth,
    maxHealth = initialHealth
) {
    var state: MobState = MobState.IDLE
    var stateTimer: Float = 0f
    var targetX: Float = x
    var targetY: Float = y
    var targetZ: Float = z
    var attackCooldown: Float = 0f
    var creeperFuse: Float = 0f // 0 to 1.5s
    var isBurning: Boolean = false

    init {
        when (type) {
            EntityType.CREEPER -> {
                width = 0.6f
                height = 1.7f
                maxHealth = 20f
                health = 20f
                moveSpeed = 2.0f
                attackDamage = 20f // explosive
            }
            EntityType.SKELETON -> {
                width = 0.6f
                height = 1.9f
                maxHealth = 20f
                health = 20f
                moveSpeed = 2.0f
                attackDamage = 3f
                attackRange = 12f
            }
            EntityType.ZOMBIE -> {
                width = 0.6f
                height = 1.9f
                maxHealth = 20f
                health = 20f
                moveSpeed = 2.0f
                attackDamage = 3.5f
                attackRange = 1.5f
            }
            EntityType.PIG -> {
                width = 0.8f
                height = 0.9f
                maxHealth = 10f
                health = 10f
                moveSpeed = 1.6f
                attackDamage = 0f
            }
            EntityType.COW -> {
                width = 0.9f
                height = 1.3f
                maxHealth = 10f
                health = 10f
                moveSpeed = 1.5f
                attackDamage = 0f
            }
            else -> {}
        }
    }
}

class ArrowEntity(
    id: String,
    x: Float,
    y: Float,
    z: Float,
    vx: Float,
    vy: Float,
    vz: Float,
    val shooterId: String,
    val damage: Float = 4f
) : Entity(
    id = id,
    type = EntityType.ARROW,
    x = x,
    y = y,
    z = z,
    vx = vx,
    vy = vy,
    vz = vz,
    width = 0.2f,
    height = 0.2f
) {
    var inGround: Boolean = false
    var groundTimer: Float = 0f
}

class ItemDropEntity(
    id: String,
    x: Float,
    y: Float,
    z: Float,
    val itemStack: ItemStack
) : Entity(
    id = id,
    type = EntityType.ITEM_DROP,
    x = x,
    y = y,
    z = z,
    width = 0.35f,
    height = 0.35f
) {
    var age: Float = 0f
    var hoverOffset: Float = 0f
    var spinAngle: Float = 0f
}

class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val color: Int,
    val size: Float = 0.1f,
    val maxLife: Float = 0.6f,
    val gravity: Float = 9.8f
) {
    var life: Float = 0f
    val isDead: Boolean get() = life >= maxLife
}

data class RemotePlayer(
    val playerId: String,
    var playerName: String,
    var x: Float,
    var y: Float,
    var z: Float,
    var yaw: Float = 0f,
    var pitch: Float = 0f,
    var holdingItemId: String = "",
    var skinColor: Int = 0xFF4CAF50.toInt(),
    var isSneaking: Boolean = false
)
