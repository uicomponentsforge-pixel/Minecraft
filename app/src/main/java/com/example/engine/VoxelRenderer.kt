package com.example.engine

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

enum class CameraMode {
    FIRST_PERSON,
    THIRD_PERSON_BACK,
    THIRD_PERSON_FRONT
}

class VoxelRenderer(val world: VoxelWorld) : GLSurfaceView.Renderer {

    var cameraMode: CameraMode = CameraMode.FIRST_PERSON
    var fov: Float = 70.0f
    var renderDistance: Int = 3
    var viewWidth: Int = 1
    var viewHeight: Int = 1

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Shaders
    private var programId = 0
    private var aPosLocation = 0
    private var aNormalLocation = 0
    private var aColorLocation = 0
    private var aTexCoordLocation = 0
    private var aLightLocation = 0
    private var uMvpMatrixLocation = 0
    private var uSkyLightLocation = 0
    private var uFogColorLocation = 0
    private var uFogDensityLocation = 0

    // Unit Cube buffer for entities, particles, and breaking overlay
    private lateinit var unitCubeBuffer: FloatBuffer

    private val vertexShaderCode = """
        uniform mat4 uMVPMatrix;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;
        attribute vec2 aTexCoord;
        attribute float aLight;
        
        varying vec4 vColor;
        varying vec2 vTexCoord;
        varying float vPattern;
        varying float vFogFactor;
        varying float vLight;
        
        void main() {
            vec4 pos = uMVPMatrix * vec4(aPosition, 1.0);
            gl_Position = pos;
            
            float dist = length(pos.xyz);
            vFogFactor = clamp((dist - 18.0) / (45.0 - 18.0), 0.0, 1.0);
            
            vColor = aColor;
            vTexCoord = aTexCoord;
            
            float rawLight = mod(aLight, 100.0);
            vPattern = floor(aLight / 100.0);
            vLight = rawLight / 100.0;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        precision mediump float;
        
        varying vec4 vColor;
        varying vec2 vTexCoord;
        varying float vPattern;
        varying float vFogFactor;
        varying float vLight;
        
        uniform float uSkyLight;
        uniform vec3 uFogColor;
        
        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
        }
        
        void main() {
            vec2 uv = vTexCoord;
            vec4 base = vColor;
            
            // Procedural pixel pattern details
            vec2 pixelUv = floor(uv * 16.0) / 16.0;
            float n = hash(pixelUv);
            
            // Subtle noise variation
            base.rgb *= (0.88 + 0.24 * n);
            
            // Cobblestone / Brick seams
            if (vPattern == 5.0) {
                vec2 grid = fract(uv * 4.0);
                if (grid.x < 0.08 || grid.y < 0.08) {
                    base.rgb *= 0.65;
                }
            }
            
            // Apply Dynamic Sky Light & Block Light
            float finalLight = max(vLight, uSkyLight * 0.95);
            finalLight = clamp(finalLight, 0.12, 1.0);
            
            vec3 litColor = base.rgb * finalLight;
            vec3 finalColor = mix(litColor, uFogColor, vFogFactor);
            
            gl_FragColor = vec4(finalColor, base.a);
        }
    """.trimIndent()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        programId = createProgram(vertexShaderCode, fragmentShaderCode)
        aPosLocation = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalLocation = GLES20.glGetAttribLocation(programId, "aNormal")
        aColorLocation = GLES20.glGetAttribLocation(programId, "aColor")
        aTexCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord")
        aLightLocation = GLES20.glGetAttribLocation(programId, "aLight")
        uMvpMatrixLocation = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uSkyLightLocation = GLES20.glGetUniformLocation(programId, "uSkyLight")
        uFogColorLocation = GLES20.glGetUniformLocation(programId, "uFogColor")
        uFogDensityLocation = GLES20.glGetUniformLocation(programId, "uFogDensity")

        initUnitCube()
    }

    private fun initUnitCube() {
        val vertices = ArrayList<Float>()
        for (face in 0..5) {
            val shading = VoxelChunk.FACE_SHADING[face]
            val r = shading; val g = shading; val b = shading; val a = 1.0f
            when (face) {
                0 -> { // Top (+Y)
                    addCubeVertex(vertices, -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, 0.5f, 0.5f, 0.5f, 0f, 1f, 0f, r, g, b, a, 1f, 1f)
                    addCubeVertex(vertices, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, -0.5f, 0.5f, 0.5f, 0f, 1f, 0f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, 0.5f, 0.5f, -0.5f, 0f, 1f, 0f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, -0.5f, 0.5f, -0.5f, 0f, 1f, 0f, r, g, b, a, 0f, 0f)
                }
                1 -> { // Bottom (-Y)
                    addCubeVertex(vertices, -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, r, g, b, a, 0f, 0f)
                    addCubeVertex(vertices, 0.5f, -0.5f, -0.5f, 0f, -1f, 0f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, 0.5f, -0.5f, 0.5f, 0f, -1f, 0f, r, g, b, a, 1f, 1f)
                    addCubeVertex(vertices, -0.5f, -0.5f, -0.5f, 0f, -1f, 0f, r, g, b, a, 0f, 0f)
                    addCubeVertex(vertices, 0.5f, -0.5f, 0.5f, 0f, -1f, 0f, r, g, b, a, 1f, 1f)
                    addCubeVertex(vertices, -0.5f, -0.5f, 0.5f, 0f, -1f, 0f, r, g, b, a, 0f, 1f)
                }
                2 -> { // North (+Z)
                    addCubeVertex(vertices, -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, 0.5f, -0.5f, 0.5f, 0f, 0f, 1f, r, g, b, a, 1f, 1f)
                    addCubeVertex(vertices, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, -0.5f, -0.5f, 0.5f, 0f, 0f, 1f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, 0.5f, 0.5f, 0.5f, 0f, 0f, 1f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, -0.5f, 0.5f, 0.5f, 0f, 0f, 1f, r, g, b, a, 0f, 0f)
                }
                3 -> { // South (-Z)
                    addCubeVertex(vertices, 0.5f, -0.5f, -0.5f, 0f, 0f, -1f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, -0.5f, -0.5f, -0.5f, 0f, 0f, -1f, r, g, b, a, 1f, 1f)
                    addCubeVertex(vertices, -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, 0.5f, -0.5f, -0.5f, 0f, 0f, -1f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, -0.5f, 0.5f, -0.5f, 0f, 0f, -1f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, 0.5f, 0.5f, -0.5f, 0f, 0f, -1f, r, g, b, a, 0f, 0f)
                }
                4 -> { // East (+X)
                    addCubeVertex(vertices, 0.5f, -0.5f, 0.5f, 1f, 0f, 0f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, 0.5f, -0.5f, -0.5f, 1f, 0f, 0f, r, g, b, a, 1f, 1f)
                    addCubeVertex(vertices, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, 0.5f, -0.5f, 0.5f, 1f, 0f, 0f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, 0.5f, 0.5f, -0.5f, 1f, 0f, 0f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, 0.5f, 0.5f, 0.5f, 1f, 0f, 0f, r, g, b, a, 0f, 0f)
                }
                5 -> { // West (-X)
                    addCubeVertex(vertices, -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, -0.5f, -0.5f, 0.5f, -1f, 0f, 0f, r, g, b, a, 1f, 1f)
                    addCubeVertex(vertices, -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, -0.5f, -0.5f, -0.5f, -1f, 0f, 0f, r, g, b, a, 0f, 1f)
                    addCubeVertex(vertices, -0.5f, 0.5f, 0.5f, -1f, 0f, 0f, r, g, b, a, 1f, 0f)
                    addCubeVertex(vertices, -0.5f, 0.5f, -0.5f, -1f, 0f, 0f, r, g, b, a, 0f, 0f)
                }
            }
        }
        val fb = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        val arr = FloatArray(vertices.size)
        for (i in vertices.indices) arr[i] = vertices[i]
        fb.put(arr)
        fb.position(0)
        unitCubeBuffer = fb
    }

    private fun addCubeVertex(
        list: ArrayList<Float>,
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float,
        u: Float, v: Float
    ) {
        list.add(x); list.add(y); list.add(z)
        list.add(nx); list.add(ny); list.add(nz)
        list.add(r); list.add(g); list.add(b); list.add(a)
        list.add(u); list.add(v)
        list.add(100f) // full light
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, fov, ratio, 0.1f, 120.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Sky color calculation based on time of day
        val tod = world.timeOfDay
        val (skyR, skyG, skyB) = when {
            tod in 4000.0f..8000.0f -> Triple(0.48f, 0.72f, 0.98f) // Day sky
            tod in 8000.0f..11000.0f -> {
                val t = (tod - 8000.0f) / 3000.0f
                Triple(
                    0.48f + t * (0.92f - 0.48f),
                    0.72f + t * (0.45f - 0.72f),
                    0.98f + t * (0.25f - 0.98f)
                ) // Sunset orange/red
            }
            tod in 11000.0f..14000.0f -> {
                val t = (tod - 11000.0f) / 3000.0f
                Triple(
                    0.92f + t * (0.05f - 0.92f),
                    0.45f + t * (0.08f - 0.45f),
                    0.25f + t * (0.16f - 0.25f)
                ) // Dusk transition to night
            }
            tod in 14000.0f..22000.0f -> Triple(0.04f, 0.07f, 0.14f) // Night dark starry sky
            else -> {
                val t = (tod - 22000.0f) / 6000.0f
                Triple(
                    0.04f + t * (0.48f - 0.04f),
                    0.07f + t * (0.72f - 0.07f),
                    0.14f + t * (0.98f - 0.14f)
                ) // Sunrise
            }
        }

        GLES20.glClearColor(skyR, skyG, skyB, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Setup Camera View Matrix
        val eyeX: Float
        val eyeY: Float
        val eyeZ: Float

        val yawRad = Math.toRadians(world.playerYaw.toDouble())
        val pitchRad = Math.toRadians(world.playerPitch.toDouble())

        val forwardX = (-sin(yawRad) * cos(pitchRad)).toFloat()
        val forwardY = (-sin(pitchRad)).toFloat()
        val forwardZ = (cos(yawRad) * cos(pitchRad)).toFloat()

        when (cameraMode) {
            CameraMode.FIRST_PERSON -> {
                eyeX = world.playerX
                eyeY = world.playerY + 1.62f
                eyeZ = world.playerZ
                Matrix.setLookAtM(
                    viewMatrix, 0,
                    eyeX, eyeY, eyeZ,
                    eyeX + forwardX, eyeY + forwardY, eyeZ + forwardZ,
                    0f, 1f, 0f
                )
            }
            CameraMode.THIRD_PERSON_BACK -> {
                val camDist = 3.5f
                eyeX = world.playerX - forwardX * camDist
                eyeY = world.playerY + 1.62f - forwardY * camDist
                eyeZ = world.playerZ - forwardZ * camDist
                Matrix.setLookAtM(
                    viewMatrix, 0,
                    eyeX, eyeY, eyeZ,
                    world.playerX, world.playerY + 1.3f, world.playerZ,
                    0f, 1f, 0f
                )
            }
            CameraMode.THIRD_PERSON_FRONT -> {
                val camDist = 3.5f
                eyeX = world.playerX + forwardX * camDist
                eyeY = world.playerY + 1.62f + forwardY * camDist
                eyeZ = world.playerZ + forwardZ * camDist
                Matrix.setLookAtM(
                    viewMatrix, 0,
                    eyeX, eyeY, eyeZ,
                    world.playerX, world.playerY + 1.3f, world.playerZ,
                    0f, 1f, 0f
                )
            }
        }

        GLES20.glUseProgram(programId)
        GLES20.glUniform1f(uSkyLightLocation, world.skyLight)
        GLES20.glUniform3f(uFogColorLocation, skyR, skyG, skyB)

        // 1. Render Sky Celestial Body (Sun & Moon)
        renderSunAndMoon(eyeX, eyeY, eyeZ)

        // 2. Render Chunks
        renderChunks()

        // 3. Render Entities (Mobs, Players, Arrows, Item Drops, Particles)
        renderEntities()

        // 4. Render Breaking Crack Box
        renderBreakingBox()

        // 5. Render First Person Held Tool/Arm (if in 1st person mode)
        if (cameraMode == CameraMode.FIRST_PERSON) {
            renderFirstPersonHand(eyeX, eyeY, eyeZ, forwardX, forwardY, forwardZ)
        }
    }

    private fun renderSunAndMoon(cx: Float, cy: Float, cz: Float) {
        val angle = (world.timeOfDay / 24000.0f) * Math.PI.toFloat() * 2f - Math.PI.toFloat() / 2f
        val sunDist = 45.0f

        // Sun
        val sunX = cx + cos(angle) * sunDist
        val sunY = cy + sin(angle) * sunDist
        val sunZ = cz

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, sunX, sunY, sunZ)
        Matrix.scaleM(modelMatrix, 0, 7.0f, 7.0f, 7.0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        val sunMvp = FloatArray(16)
        Matrix.multiplyMM(sunMvp, 0, mvpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, sunMvp, 0)

        // Draw Sun Cube
        drawCube(1.0f, 0.95f, 0.2f, 1.0f)

        // Moon (Opposite side)
        val moonX = cx - cos(angle) * sunDist
        val moonY = cy - sin(angle) * sunDist
        val moonZ = cz

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, moonX, moonY, moonZ)
        Matrix.scaleM(modelMatrix, 0, 5.5f, 5.5f, 5.5f)
        val moonMvp = FloatArray(16)
        Matrix.multiplyMM(moonMvp, 0, mvpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, moonMvp, 0)

        // Draw Moon Cube
        drawCube(0.9f, 0.92f, 1.0f, 1.0f)
    }

    private fun renderChunks() {
        val playerChunkX = floor(world.playerX / VoxelChunk.SIZE_X).toInt()
        val playerChunkZ = floor(world.playerZ / VoxelChunk.SIZE_Z).toInt()

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, mvpMatrix, 0)

        // Solid chunks
        for (dx in -renderDistance..renderDistance) {
            for (dz in -renderDistance..renderDistance) {
                val cx = playerChunkX + dx
                val cz = playerChunkZ + dz
                val chunk = world.getChunk(cx, cz) ?: continue

                if (chunk.isDirty) {
                    chunk.buildMesh(world)
                }

                chunk.vertexBuffer?.let { buf ->
                    if (chunk.vertexCount > 0) {
                        buf.position(0)
                        GLES20.glVertexAttribPointer(aPosLocation, 3, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aPosLocation)

                        buf.position(3)
                        GLES20.glVertexAttribPointer(aNormalLocation, 3, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aNormalLocation)

                        buf.position(6)
                        GLES20.glVertexAttribPointer(aColorLocation, 4, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aColorLocation)

                        buf.position(10)
                        GLES20.glVertexAttribPointer(aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

                        buf.position(12)
                        GLES20.glVertexAttribPointer(aLightLocation, 1, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aLightLocation)

                        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, chunk.vertexCount)
                    }
                }
            }
        }

        // Water/Transparent pass
        for (dx in -renderDistance..renderDistance) {
            for (dz in -renderDistance..renderDistance) {
                val cx = playerChunkX + dx
                val cz = playerChunkZ + dz
                val chunk = world.getChunk(cx, cz) ?: continue

                chunk.waterVertexBuffer?.let { buf ->
                    if (chunk.waterVertexCount > 0) {
                        buf.position(0)
                        GLES20.glVertexAttribPointer(aPosLocation, 3, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aPosLocation)

                        buf.position(3)
                        GLES20.glVertexAttribPointer(aNormalLocation, 3, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aNormalLocation)

                        buf.position(6)
                        GLES20.glVertexAttribPointer(aColorLocation, 4, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aColorLocation)

                        buf.position(10)
                        GLES20.glVertexAttribPointer(aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

                        buf.position(12)
                        GLES20.glVertexAttribPointer(aLightLocation, 1, GLES20.GL_FLOAT, false, 13 * 4, buf)
                        GLES20.glEnableVertexAttribArray(aLightLocation)

                        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, chunk.waterVertexCount)
                    }
                }
            }
        }
    }

    private fun renderEntities() {
        val vpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // 1. Mobs
        for (mob in world.mobs) {
            if (mob.isDead) continue
            renderMob(mob, vpMatrix)
        }

        // 2. Remote Multiplayer Players
        for (rp in world.remotePlayers.values) {
            renderRemotePlayer(rp, vpMatrix)
        }

        // 3. Arrows
        for (arrow in world.arrows) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, arrow.x, arrow.y, arrow.z)
            Matrix.scaleM(modelMatrix, 0, 0.15f, 0.15f, 0.5f)
            val mat = FloatArray(16)
            Matrix.multiplyMM(mat, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, mat, 0)
            drawCube(0.8f, 0.7f, 0.5f, 1.0f)
        }

        // 4. Item Drops
        for (drop in world.itemDrops) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, drop.x, drop.y + drop.hoverOffset, drop.z)
            Matrix.rotateM(modelMatrix, 0, drop.spinAngle, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.35f, 0.35f, 0.35f)
            val mat = FloatArray(16)
            Matrix.multiplyMM(mat, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, mat, 0)

            val color = drop.itemStack.item.iconColor
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            drawCube(r, g, b, 1.0f)
        }

        // 5. Particles
        for (p in world.particles) {
            if (p.isDead) continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.scaleM(modelMatrix, 0, p.size, p.size, p.size)
            val mat = FloatArray(16)
            Matrix.multiplyMM(mat, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, mat, 0)

            val r = ((p.color shr 16) and 0xFF) / 255f
            val g = ((p.color shr 8) and 0xFF) / 255f
            val b = (p.color and 0xFF) / 255f
            val alpha = 1.0f - (p.life / p.maxLife)
            drawCube(r, g, b, alpha)
        }
    }

    private fun renderMob(mob: MobEntity, vpMatrix: FloatArray) {
        val isHurt = mob.hurtTime > 0f
        val isCreeperFlashing = mob.type == EntityType.CREEPER && mob.state == MobState.HISSING && ((mob.creeperFuse * 8).toInt() % 2 == 0)

        // Limb animation
        val legSwing = sin(mob.animTime * 6.0f) * 25.0f

        when (mob.type) {
            EntityType.CREEPER -> {
                // Body
                renderVoxelBox(vpMatrix, mob.x, mob.y + 0.6f, mob.z, mob.yaw, 0.45f, 0.7f, 0.25f, if (isCreeperFlashing) 1f else if (isHurt) 1f else 0.2f, if (isCreeperFlashing) 1f else if (isHurt) 0.2f else 0.75f, if (isCreeperFlashing) 1f else if (isHurt) 0.2f else 0.2f)
                // Head
                renderVoxelBox(vpMatrix, mob.x, mob.y + 1.25f, mob.z, mob.yaw, 0.5f, 0.5f, 0.5f, if (isCreeperFlashing) 1f else 0.25f, if (isCreeperFlashing) 1f else 0.8f, if (isCreeperFlashing) 1f else 0.25f)
                // 4 Legs
                renderVoxelBox(vpMatrix, mob.x - 0.15f, mob.y + 0.2f, mob.z - 0.15f, mob.yaw + legSwing, 0.2f, 0.4f, 0.2f, 0.18f, 0.65f, 0.18f)
                renderVoxelBox(vpMatrix, mob.x + 0.15f, mob.y + 0.2f, mob.z - 0.15f, mob.yaw - legSwing, 0.2f, 0.4f, 0.2f, 0.18f, 0.65f, 0.18f)
                renderVoxelBox(vpMatrix, mob.x - 0.15f, mob.y + 0.2f, mob.z + 0.15f, mob.yaw - legSwing, 0.2f, 0.4f, 0.2f, 0.18f, 0.65f, 0.18f)
                renderVoxelBox(vpMatrix, mob.x + 0.15f, mob.y + 0.2f, mob.z + 0.15f, mob.yaw + legSwing, 0.2f, 0.4f, 0.2f, 0.18f, 0.65f, 0.18f)
            }
            EntityType.ZOMBIE -> {
                val skinR = if (isHurt) 1f else 0.32f
                val skinG = if (isHurt) 0.2f else 0.55f
                val skinB = if (isHurt) 0.2f else 0.25f
                // Head
                renderVoxelBox(vpMatrix, mob.x, mob.y + 1.6f, mob.z, mob.yaw, 0.45f, 0.45f, 0.45f, skinR, skinG, skinB)
                // Torso (Blue shirt)
                renderVoxelBox(vpMatrix, mob.x, mob.y + 1.05f, mob.z, mob.yaw, 0.5f, 0.65f, 0.28f, 0.15f, 0.45f, 0.75f)
                // Outstretched Arms
                renderVoxelBox(vpMatrix, mob.x - 0.32f, mob.y + 1.15f, mob.z + 0.3f, mob.yaw, 0.18f, 0.18f, 0.65f, skinR, skinG, skinB)
                renderVoxelBox(vpMatrix, mob.x + 0.32f, mob.y + 1.15f, mob.z + 0.3f, mob.yaw, 0.18f, 0.18f, 0.65f, skinR, skinG, skinB)
                // Legs (Dark pants)
                renderVoxelBox(vpMatrix, mob.x - 0.14f, mob.y + 0.38f, mob.z, mob.yaw + legSwing, 0.22f, 0.75f, 0.24f, 0.12f, 0.18f, 0.4f)
                renderVoxelBox(vpMatrix, mob.x + 0.14f, mob.y + 0.38f, mob.z, mob.yaw - legSwing, 0.22f, 0.75f, 0.24f, 0.12f, 0.18f, 0.4f)
            }
            EntityType.SKELETON -> {
                val boneR = if (isHurt) 1f else 0.88f
                val boneG = if (isHurt) 0.3f else 0.88f
                val boneB = if (isHurt) 0.3f else 0.85f
                // Skull
                renderVoxelBox(vpMatrix, mob.x, mob.y + 1.6f, mob.z, mob.yaw, 0.45f, 0.45f, 0.45f, boneR, boneG, boneB)
                // Torso & Ribs
                renderVoxelBox(vpMatrix, mob.x, mob.y + 1.05f, mob.z, mob.yaw, 0.45f, 0.65f, 0.24f, boneR * 0.8f, boneG * 0.8f, boneB * 0.8f)
                // Bow & Arms
                renderVoxelBox(vpMatrix, mob.x - 0.28f, mob.y + 1.1f, mob.z + 0.25f, mob.yaw, 0.12f, 0.12f, 0.5f, 0.5f, 0.35f, 0.2f)
                renderVoxelBox(vpMatrix, mob.x + 0.28f, mob.y + 1.1f, mob.z + 0.25f, mob.yaw, 0.12f, 0.12f, 0.5f, 0.5f, 0.35f, 0.2f)
                // Legs
                renderVoxelBox(vpMatrix, mob.x - 0.12f, mob.y + 0.38f, mob.z, mob.yaw + legSwing, 0.15f, 0.75f, 0.15f, boneR, boneG, boneB)
                renderVoxelBox(vpMatrix, mob.x + 0.12f, mob.y + 0.38f, mob.z, mob.yaw - legSwing, 0.15f, 0.75f, 0.15f, boneR, boneG, boneB)
            }
            EntityType.PIG -> {
                val pigR = if (isHurt) 1f else 0.95f
                val pigG = if (isHurt) 0.4f else 0.68f
                val pigB = if (isHurt) 0.4f else 0.68f
                // Body
                renderVoxelBox(vpMatrix, mob.x, mob.y + 0.55f, mob.z, mob.yaw, 0.65f, 0.55f, 0.9f, pigR, pigG, pigB)
                // Head
                renderVoxelBox(vpMatrix, mob.x, mob.y + 0.65f, mob.z + 0.5f, mob.yaw, 0.45f, 0.45f, 0.45f, pigR, pigG, pigB)
                // 4 Legs
                renderVoxelBox(vpMatrix, mob.x - 0.2f, mob.y + 0.2f, mob.z - 0.25f, mob.yaw + legSwing, 0.18f, 0.4f, 0.18f, pigR * 0.9f, pigG * 0.9f, pigB * 0.9f)
                renderVoxelBox(vpMatrix, mob.x + 0.2f, mob.y + 0.2f, mob.z - 0.25f, mob.yaw - legSwing, 0.18f, 0.4f, 0.18f, pigR * 0.9f, pigG * 0.9f, pigB * 0.9f)
                renderVoxelBox(vpMatrix, mob.x - 0.2f, mob.y + 0.2f, mob.z + 0.25f, mob.yaw - legSwing, 0.18f, 0.4f, 0.18f, pigR * 0.9f, pigG * 0.9f, pigB * 0.9f)
                renderVoxelBox(vpMatrix, mob.x + 0.2f, mob.y + 0.2f, mob.z + 0.25f, mob.yaw + legSwing, 0.18f, 0.4f, 0.18f, pigR * 0.9f, pigG * 0.9f, pigB * 0.9f)
            }
            EntityType.COW -> {
                val cowR = if (isHurt) 1f else 0.45f
                val cowG = if (isHurt) 0.3f else 0.35f
                val cowB = if (isHurt) 0.3f else 0.25f
                // Body
                renderVoxelBox(vpMatrix, mob.x, mob.y + 0.8f, mob.z, mob.yaw, 0.8f, 0.7f, 1.1f, cowR, cowG, cowB)
                // Head
                renderVoxelBox(vpMatrix, mob.x, mob.y + 1.05f, mob.z + 0.65f, mob.yaw, 0.5f, 0.5f, 0.5f, cowR, cowG, cowB)
                // Horns
                renderVoxelBox(vpMatrix, mob.x - 0.25f, mob.y + 1.35f, mob.z + 0.65f, mob.yaw, 0.1f, 0.2f, 0.1f, 0.9f, 0.9f, 0.85f)
                renderVoxelBox(vpMatrix, mob.x + 0.25f, mob.y + 1.35f, mob.z + 0.65f, mob.yaw, 0.1f, 0.2f, 0.1f, 0.9f, 0.9f, 0.85f)
                // 4 Legs
                renderVoxelBox(vpMatrix, mob.x - 0.25f, mob.y + 0.3f, mob.z - 0.35f, mob.yaw + legSwing, 0.22f, 0.6f, 0.22f, 0.3f, 0.25f, 0.2f)
                renderVoxelBox(vpMatrix, mob.x + 0.25f, mob.y + 0.3f, mob.z - 0.35f, mob.yaw - legSwing, 0.22f, 0.6f, 0.22f, 0.3f, 0.25f, 0.2f)
                renderVoxelBox(vpMatrix, mob.x - 0.25f, mob.y + 0.3f, mob.z + 0.35f, mob.yaw - legSwing, 0.22f, 0.6f, 0.22f, 0.3f, 0.25f, 0.2f)
                renderVoxelBox(vpMatrix, mob.x + 0.25f, mob.y + 0.3f, mob.z + 0.35f, mob.yaw + legSwing, 0.22f, 0.6f, 0.22f, 0.3f, 0.25f, 0.2f)
            }
            else -> {}
        }
    }

    private fun renderRemotePlayer(rp: RemotePlayer, vpMatrix: FloatArray) {
        val r = ((rp.skinColor shr 16) and 0xFF) / 255f
        val g = ((rp.skinColor shr 8) and 0xFF) / 255f
        val b = (rp.skinColor and 0xFF) / 255f

        // Head
        renderVoxelBox(vpMatrix, rp.x, rp.y + 1.6f, rp.z, rp.yaw, 0.45f, 0.45f, 0.45f, 0.95f, 0.75f, 0.65f)
        // Torso
        renderVoxelBox(vpMatrix, rp.x, rp.y + 1.05f, rp.z, rp.yaw, 0.5f, 0.65f, 0.28f, r, g, b)
        // Arms
        renderVoxelBox(vpMatrix, rp.x - 0.32f, rp.y + 1.05f, rp.z, rp.yaw, 0.18f, 0.65f, 0.18f, r, g, b)
        renderVoxelBox(vpMatrix, rp.x + 0.32f, rp.y + 1.05f, rp.z, rp.yaw, 0.18f, 0.65f, 0.18f, r, g, b)
        // Legs
        renderVoxelBox(vpMatrix, rp.x - 0.14f, rp.y + 0.38f, rp.z, rp.yaw, 0.22f, 0.75f, 0.24f, 0.15f, 0.25f, 0.55f)
        renderVoxelBox(vpMatrix, rp.x + 0.14f, rp.y + 0.38f, rp.z, rp.yaw, 0.22f, 0.75f, 0.24f, 0.15f, 0.25f, 0.55f)
    }

    private fun renderVoxelBox(
        vpMatrix: FloatArray,
        x: Float, y: Float, z: Float,
        yaw: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

        val mat = FloatArray(16)
        Matrix.multiplyMM(mat, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, mat, 0)
        drawCube(r, g, b, 1.0f)
    }

    private fun renderBreakingBox() {
        if (world.breakingBlockX == -1 || world.breakProgress <= 0f) return

        val vpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(
            modelMatrix, 0,
            world.breakingBlockX + 0.5f,
            world.breakingBlockY + 0.5f,
            world.breakingBlockZ + 0.5f
        )
        Matrix.scaleM(modelMatrix, 0, 1.02f, 1.02f, 1.02f)

        val mat = FloatArray(16)
        Matrix.multiplyMM(mat, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, mat, 0)

        // Dark cracking overlay wireframe
        drawCube(0.1f, 0.1f, 0.1f, 0.45f * world.breakProgress.coerceIn(0.1f, 0.8f))
    }

    private fun renderFirstPersonHand(
        eyeX: Float, eyeY: Float, eyeZ: Float,
        fwdX: Float, fwdY: Float, fwdZ: Float
    ) {
        val equipped = world.getEquippedItem()
        val rightX = cos(Math.toRadians(world.playerYaw.toDouble())).toFloat()
        val rightZ = sin(Math.toRadians(world.playerYaw.toDouble())).toFloat()

        val handX = eyeX + fwdX * 0.6f + rightX * 0.35f
        val handY = eyeY + fwdY * 0.6f - 0.28f
        val handZ = eyeZ + fwdZ * 0.6f + rightZ * 0.35f

        val vpMatrix = FloatArray(16)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, handX, handY, handZ)
        Matrix.rotateM(modelMatrix, 0, world.playerYaw + 45f, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, -world.playerPitch + 20f, 1f, 0f, 0f)

        if (equipped.isEmpty) {
            // Player fist / hand
            Matrix.scaleM(modelMatrix, 0, 0.14f, 0.32f, 0.14f)
            val mat = FloatArray(16)
            Matrix.multiplyMM(mat, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, mat, 0)
            drawCube(0.95f, 0.75f, 0.65f, 1.0f)
        } else {
            // Held tool / item block
            val color = equipped.item.iconColor
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f

            if (equipped.item.blockId != null) {
                // Holding block
                Matrix.scaleM(modelMatrix, 0, 0.22f, 0.22f, 0.22f)
            } else {
                // Holding tool / weapon
                Matrix.scaleM(modelMatrix, 0, 0.08f, 0.45f, 0.08f)
            }
            val mat = FloatArray(16)
            Matrix.multiplyMM(mat, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(uMvpMatrixLocation, 1, false, mat, 0)
            drawCube(r, g, b, 1.0f)
        }
    }

    private fun drawCube(r: Float, g: Float, b: Float, a: Float) {
        unitCubeBuffer.position(0)
        GLES20.glVertexAttribPointer(aPosLocation, 3, GLES20.GL_FLOAT, false, 13 * 4, unitCubeBuffer)
        GLES20.glEnableVertexAttribArray(aPosLocation)

        unitCubeBuffer.position(3)
        GLES20.glVertexAttribPointer(aNormalLocation, 3, GLES20.GL_FLOAT, false, 13 * 4, unitCubeBuffer)
        GLES20.glEnableVertexAttribArray(aNormalLocation)

        // Custom uniform tint passed via attribute override
        GLES20.glVertexAttrib4f(aColorLocation, r, g, b, a)
        GLES20.glDisableVertexAttribArray(aColorLocation)

        unitCubeBuffer.position(10)
        GLES20.glVertexAttribPointer(aTexCoordLocation, 2, GLES20.GL_FLOAT, false, 13 * 4, unitCubeBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoordLocation)

        unitCubeBuffer.position(12)
        GLES20.glVertexAttribPointer(aLightLocation, 1, GLES20.GL_FLOAT, false, 13 * 4, unitCubeBuffer)
        GLES20.glEnableVertexAttribArray(aLightLocation)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
    }

    private fun createProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        return program
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}
