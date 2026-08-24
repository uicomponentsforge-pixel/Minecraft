package com.example.engine

import com.example.model.BlockProperties
import com.example.model.BlockRegistry
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VoxelChunk(
    val chunkX: Int,
    val chunkZ: Int
) {
    companion object {
        const val SIZE_X = 16
        const val SIZE_Y = 64
        const val SIZE_Z = 16
        const val TOTAL_BLOCKS = SIZE_X * SIZE_Y * SIZE_Z

        // Face Directions: 0=+Y(Top), 1=-Y(Bottom), 2=+Z(North), 3=-Z(South), 4=+X(East), 5=-X(West)
        val DIR_VECTORS = arrayOf(
            intArrayOf(0, 1, 0),   // Top
            intArrayOf(0, -1, 0),  // Bottom
            intArrayOf(0, 0, 1),   // North
            intArrayOf(0, 0, -1),  // South
            intArrayOf(1, 0, 0),   // East
            intArrayOf(-1, 0, 0)   // West
        )

        val FACE_SHADING = floatArrayOf(
            1.0f,   // Top
            0.5f,   // Bottom
            0.8f,   // North
            0.8f,   // South
            0.7f,   // East
            0.7f    // West
        )
    }

    val blocks = ByteArray(TOTAL_BLOCKS)
    val lightLevels = ByteArray(TOTAL_BLOCKS) // block light level (0-15)

    var isDirty = true
    var isGenerated = false

    var vertexBuffer: FloatBuffer? = null
    var vertexCount = 0

    var waterVertexBuffer: FloatBuffer? = null
    var waterVertexCount = 0

    @Synchronized
    fun getBlock(x: Int, y: Int, z: Int): Int {
        if (x !in 0 until SIZE_X || y !in 0 until SIZE_Y || z !in 0 until SIZE_Z) return 0
        return blocks[y * SIZE_X * SIZE_Z + z * SIZE_X + x].toInt() and 0xFF
    }

    @Synchronized
    fun setBlock(x: Int, y: Int, z: Int, blockId: Int) {
        if (x !in 0 until SIZE_X || y !in 0 until SIZE_Y || z !in 0 until SIZE_Z) return
        blocks[y * SIZE_X * SIZE_Z + z * SIZE_X + x] = blockId.toByte()
        isDirty = true
    }

    @Synchronized
    fun getLight(x: Int, y: Int, z: Int): Int {
        if (x !in 0 until SIZE_X || y !in 0 until SIZE_Y || z !in 0 until SIZE_Z) return 15
        return lightLevels[y * SIZE_X * SIZE_Z + z * SIZE_X + x].toInt() and 0x0F
    }

    @Synchronized
    fun setLight(x: Int, y: Int, z: Int, level: Int) {
        if (x !in 0 until SIZE_X || y !in 0 until SIZE_Y || z !in 0 until SIZE_Z) return
        lightLevels[y * SIZE_X * SIZE_Z + z * SIZE_X + x] = (level and 0x0F).toByte()
    }

    /**
     * Builds OpenGL mesh with face culling:
     * Format per vertex:
     * posX, posY, posZ, normalX, normalY, normalZ, r, g, b, a, texU, texV, lightLevel
     * 13 floats per vertex * 6 vertices per quad (2 triangles) = 78 floats per face
     */
    fun buildMesh(world: VoxelWorld) {
        val solidVertices = ArrayList<Float>(10000)
        val waterVertices = ArrayList<Float>(2000)

        val worldOffsetX = chunkX * SIZE_X
        val worldOffsetZ = chunkZ * SIZE_Z

        for (y in 0 until SIZE_Y) {
            for (z in 0 until SIZE_Z) {
                for (x in 0 until SIZE_X) {
                    val blockId = getBlock(x, y, z)
                    if (blockId == 0) continue

                    val block = BlockRegistry.get(blockId)
                    val wx = worldOffsetX + x
                    val wy = y
                    val wz = worldOffsetZ + z

                    val isWater = block.isLiquid

                    // Check all 6 faces
                    for (face in 0..5) {
                        val dx = DIR_VECTORS[face][0]
                        val dy = DIR_VECTORS[face][1]
                        val dz = DIR_VECTORS[face][2]

                        val neighborId = world.getBlock(wx + dx, wy + dy, wz + dz)
                        val neighbor = BlockRegistry.get(neighborId)

                        var shouldRenderFace = false
                        if (isWater) {
                            // Only render water face if neighbor is not water and is transparent
                            shouldRenderFace = neighborId != blockId && neighbor.isTransparent
                        } else {
                            // Solid or transparent block: render if neighbor is transparent/air or water
                            shouldRenderFace = neighbor.isTransparent || neighbor.isLiquid || neighborId == 0
                            // don't render internal leaf faces or glass against glass
                            if (block.isTransparent && neighborId == blockId && blockId != BlockRegistry.TORCH.id) {
                                shouldRenderFace = false
                            }
                        }

                        if (shouldRenderFace) {
                            val color = when (face) {
                                0 -> block.topColor
                                1 -> block.bottomColor
                                else -> block.sideColor
                            }
                            val shading = FACE_SHADING[face]
                            val light = (world.getLight(wx + dx, wy + dy, wz + dz) / 15.0f).coerceIn(0.15f, 1.0f)

                            val r = ((color shr 16) and 0xFF) / 255.0f * shading
                            val g = ((color shr 8) and 0xFF) / 255.0f * shading
                            val b = (color and 0xFF) / 255.0f * shading
                            val a = if (block.isLiquid) 0.65f else if (block.isTransparent && blockId == BlockRegistry.GLASS.id) 0.35f else 1.0f

                            val targetList = if (isWater) waterVertices else solidVertices
                            addQuad(targetList, wx.toFloat(), wy.toFloat(), wz.toFloat(), face, r, g, b, a, block, light)
                        }
                    }
                }
            }
        }

        // Convert lists to Direct FloatBuffers
        solidVertices.let { list ->
            vertexCount = list.size / 13
            if (vertexCount > 0) {
                val fb = ByteBuffer.allocateDirect(list.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                val floatArray = FloatArray(list.size)
                for (i in list.indices) {
                    floatArray[i] = list[i]
                }
                fb.put(floatArray)
                fb.position(0)
                vertexBuffer = fb
            } else {
                vertexBuffer = null
            }
        }

        waterVertices.let { list ->
            waterVertexCount = list.size / 13
            if (waterVertexCount > 0) {
                val fb = ByteBuffer.allocateDirect(list.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                val floatArray = FloatArray(list.size)
                for (i in list.indices) {
                    floatArray[i] = list[i]
                }
                fb.put(floatArray)
                fb.position(0)
                waterVertexBuffer = fb
            } else {
                waterVertexBuffer = null
            }
        }

        isDirty = false
    }

    private fun addQuad(
        list: ArrayList<Float>,
        x: Float, y: Float, z: Float,
        face: Int,
        r: Float, g: Float, b: Float, a: Float,
        block: BlockProperties,
        light: Float
    ) {
        val pattern = block.texturePattern.toFloat()

        // 6 faces geometry (2 triangles each)
        when (face) {
            0 -> { // Top (+Y)
                // v0: 0,1,1; v1: 1,1,1; v2: 1,1,0; v3: 0,1,0
                addVertex(list, x + 0f, y + 1f, z + 1f, 0f, 1f, 0f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 1f, 0f, 1f, 0f, r, g, b, a, 1f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 0f, 0f, 1f, 0f, r, g, b, a, 1f, 0f, light, pattern)

                addVertex(list, x + 0f, y + 1f, z + 1f, 0f, 1f, 0f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 0f, 0f, 1f, 0f, r, g, b, a, 1f, 0f, light, pattern)
                addVertex(list, x + 0f, y + 1f, z + 0f, 0f, 1f, 0f, r, g, b, a, 0f, 0f, light, pattern)
            }
            1 -> { // Bottom (-Y)
                addVertex(list, x + 0f, y + 0f, z + 0f, 0f, -1f, 0f, r, g, b, a, 0f, 0f, light, pattern)
                addVertex(list, x + 1f, y + 0f, z + 0f, 0f, -1f, 0f, r, g, b, a, 1f, 0f, light, pattern)
                addVertex(list, x + 1f, y + 0f, z + 1f, 0f, -1f, 0f, r, g, b, a, 1f, 1f, light, pattern)

                addVertex(list, x + 0f, y + 0f, z + 0f, 0f, -1f, 0f, r, g, b, a, 0f, 0f, light, pattern)
                addVertex(list, x + 1f, y + 0f, z + 1f, 0f, -1f, 0f, r, g, b, a, 1f, 1f, light, pattern)
                addVertex(list, x + 0f, y + 0f, z + 1f, 0f, -1f, 0f, r, g, b, a, 0f, 1f, light, pattern)
            }
            2 -> { // North (+Z)
                addVertex(list, x + 0f, y + 0f, z + 1f, 0f, 0f, 1f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 0f, z + 1f, 0f, 0f, 1f, r, g, b, a, 1f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 1f, 0f, 0f, 1f, r, g, b, a, 1f, 0f, light, pattern)

                addVertex(list, x + 0f, y + 0f, z + 1f, 0f, 0f, 1f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 1f, 0f, 0f, 1f, r, g, b, a, 1f, 0f, light, pattern)
                addVertex(list, x + 0f, y + 1f, z + 1f, 0f, 0f, 1f, r, g, b, a, 0f, 0f, light, pattern)
            }
            3 -> { // South (-Z)
                addVertex(list, x + 1f, y + 0f, z + 0f, 0f, 0f, -1f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 0f, y + 0f, z + 0f, 0f, 0f, -1f, r, g, b, a, 1f, 1f, light, pattern)
                addVertex(list, x + 0f, y + 1f, z + 0f, 0f, 0f, -1f, r, g, b, a, 1f, 0f, light, pattern)

                addVertex(list, x + 1f, y + 0f, z + 0f, 0f, 0f, -1f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 0f, y + 1f, z + 0f, 0f, 0f, -1f, r, g, b, a, 1f, 0f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 0f, 0f, 0f, -1f, r, g, b, a, 0f, 0f, light, pattern)
            }
            4 -> { // East (+X)
                addVertex(list, x + 1f, y + 0f, z + 1f, 1f, 0f, 0f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 0f, z + 0f, 1f, 0f, 0f, r, g, b, a, 1f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 0f, 1f, 0f, 0f, r, g, b, a, 1f, 0f, light, pattern)

                addVertex(list, x + 1f, y + 0f, z + 1f, 1f, 0f, 0f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 0f, 1f, 0f, 0f, r, g, b, a, 1f, 0f, light, pattern)
                addVertex(list, x + 1f, y + 1f, z + 1f, 1f, 0f, 0f, r, g, b, a, 0f, 0f, light, pattern)
            }
            5 -> { // West (-X)
                addVertex(list, x + 0f, y + 0f, z + 0f, -1f, 0f, 0f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 0f, y + 0f, z + 1f, -1f, 0f, 0f, r, g, b, a, 1f, 1f, light, pattern)
                addVertex(list, x + 0f, y + 1f, z + 1f, -1f, 0f, 0f, r, g, b, a, 1f, 0f, light, pattern)

                addVertex(list, x + 0f, y + 0f, z + 0f, -1f, 0f, 0f, r, g, b, a, 0f, 1f, light, pattern)
                addVertex(list, x + 0f, y + 1f, z + 1f, -1f, 0f, 0f, r, g, b, a, 1f, 0f, light, pattern)
                addVertex(list, x + 0f, y + 1f, z + 0f, -1f, 0f, 0f, r, g, b, a, 0f, 0f, light, pattern)
            }
        }
    }

    private fun addVertex(
        list: ArrayList<Float>,
        vx: Float, vy: Float, vz: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float,
        u: Float, v: Float,
        light: Float,
        pattern: Float
    ) {
        list.add(vx); list.add(vy); list.add(vz)
        list.add(nx); list.add(ny); list.add(nz)
        list.add(r); list.add(g); list.add(b); list.add(a)
        list.add(u); list.add(v)
        list.add(light * 100.0f + pattern) // pack light and pattern
    }
}
