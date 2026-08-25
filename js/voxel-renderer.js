/**
 * 3D WebGL Voxel Renderer Engine
 * Converted from VoxelRenderer.kt
 */

const CameraMode = {
  FIRST_PERSON: 'FIRST_PERSON',
  THIRD_PERSON_BACK: 'THIRD_PERSON_BACK',
  THIRD_PERSON_FRONT: 'THIRD_PERSON_FRONT'
};

class VoxelRenderer {
  constructor(canvas, world) {
    this.canvas = canvas;
    this.world = world;
    this.cameraMode = CameraMode.FIRST_PERSON;
    this.fov = 70.0;
    this.renderDistance = 3;

    this.gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
    if (!this.gl) {
      console.error("WebGL not supported");
      return;
    }

    this.initShaders();
    this.initUnitCube();
    this.resizeCanvas();
    window.addEventListener('resize', () => this.resizeCanvas());
  }

  resizeCanvas() {
    if (!this.canvas) return;
    this.canvas.width = window.innerWidth;
    this.canvas.height = window.innerHeight;
    this.gl.viewport(0, 0, this.canvas.width, this.canvas.height);
  }

  initShaders() {
    const gl = this.gl;

    const vsSource = `
      attribute vec3 aPosition;
      attribute vec3 aNormal;
      attribute vec4 aColor;
      attribute vec2 aTexCoord;
      attribute float aLight;

      uniform mat4 uMVPMatrix;

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
    `;

    const fsSource = `
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

        vec2 pixelUv = floor(uv * 16.0) / 16.0;
        float n = hash(pixelUv);

        base.rgb *= (0.88 + 0.24 * n);

        if (vPattern == 5.0) {
          vec2 grid = fract(uv * 4.0);
          if (grid.x < 0.08 || grid.y < 0.08) {
            base.rgb *= 0.65;
          }
        }

        float finalLight = max(vLight, uSkyLight * 0.95);
        finalLight = clamp(finalLight, 0.12, 1.0);

        vec3 litColor = base.rgb * finalLight;
        vec3 finalColor = mix(litColor, uFogColor, vFogFactor);

        gl_FragColor = vec4(finalColor, base.a);
      }
    `;

    const vs = this.compileShader(gl.VERTEX_SHADER, vsSource);
    const fs = this.compileShader(gl.FRAGMENT_SHADER, fsSource);
    this.program = gl.createProgram();
    gl.attachShader(this.program, vs);
    gl.attachShader(this.program, fs);
    gl.linkProgram(this.program);

    this.aPosLocation = gl.getAttribLocation(this.program, "aPosition");
    this.aNormalLocation = gl.getAttribLocation(this.program, "aNormal");
    this.aColorLocation = gl.getAttribLocation(this.program, "aColor");
    this.aTexCoordLocation = gl.getAttribLocation(this.program, "aTexCoord");
    this.aLightLocation = gl.getAttribLocation(this.program, "aLight");

    this.uMvpMatrixLocation = gl.getUniformLocation(this.program, "uMVPMatrix");
    this.uSkyLightLocation = gl.getUniformLocation(this.program, "uSkyLight");
    this.uFogColorLocation = gl.getUniformLocation(this.program, "uFogColor");
  }

  compileShader(type, source) {
    const gl = this.gl;
    const shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    return shader;
  }

  initUnitCube() {
    const gl = this.gl;
    const vertices = [];

    const addCubeVertex = (x, y, z, nx, ny, nz, r, g, b, a, u, v) => {
      vertices.push(x, y, z, nx, ny, nz, r, g, b, a, u, v, 100.0);
    };

    const faces = [
      // Top
      [[-0.5,0.5,0.5, 0,1,0, 0,1], [0.5,0.5,0.5, 0,1,0, 1,1], [0.5,0.5,-0.5, 0,1,0, 1,0], [-0.5,0.5,0.5, 0,1,0, 0,1], [0.5,0.5,-0.5, 0,1,0, 1,0], [-0.5,0.5,-0.5, 0,1,0, 0,0]],
      // Bottom
      [[-0.5,-0.5,-0.5, 0,-1,0, 0,0], [0.5,-0.5,-0.5, 0,-1,0, 1,0], [0.5,-0.5,0.5, 0,-1,0, 1,1], [-0.5,-0.5,-0.5, 0,-1,0, 0,0], [0.5,-0.5,0.5, 0,-1,0, 1,1], [-0.5,-0.5,0.5, 0,-1,0, 0,1]],
      // North (+Z)
      [[-0.5,-0.5,0.5, 0,0,1, 0,1], [0.5,-0.5,0.5, 0,0,1, 1,1], [0.5,0.5,0.5, 0,0,1, 1,0], [-0.5,-0.5,0.5, 0,0,1, 0,1], [0.5,0.5,0.5, 0,0,1, 1,0], [-0.5,0.5,0.5, 0,0,1, 0,0]],
      // South (-Z)
      [[0.5,-0.5,-0.5, 0,0,-1, 0,1], [-0.5,-0.5,-0.5, 0,0,-1, 1,1], [-0.5,0.5,-0.5, 0,0,-1, 1,0], [0.5,-0.5,-0.5, 0,0,-1, 0,1], [-0.5,0.5,-0.5, 0,0,-1, 1,0], [0.5,0.5,-0.5, 0,0,-1, 0,0]],
      // East (+X)
      [[0.5,-0.5,0.5, 1,0,0, 0,1], [0.5,-0.5,-0.5, 1,0,0, 1,1], [0.5,0.5,-0.5, 1,0,0, 1,0], [0.5,-0.5,0.5, 1,0,0, 0,1], [0.5,0.5,-0.5, 1,0,0, 1,0], [0.5,0.5,0.5, 1,0,0, 0,0]],
      // West (-X)
      [[-0.5,-0.5,-0.5, -1,0,0, 0,1], [-0.5,-0.5,0.5, -1,0,0, 1,1], [-0.5,0.5,0.5, -1,0,0, 1,0], [-0.5,-0.5,-0.5, -1,0,0, 0,1], [-0.5,0.5,0.5, -1,0,0, 1,0], [-0.5,0.5,-0.5, -1,0,0, 0,0]]
    ];

    for (let f = 0; f < 6; f++) {
      const shading = VoxelChunk.FACE_SHADING[f];
      for (const v of faces[f]) {
        addCubeVertex(v[0], v[1], v[2], v[3], v[4], v[5], shading, shading, shading, 1.0, v[6], v[7]);
      }
    }

    this.unitCubeBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, this.unitCubeBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(vertices), gl.STATIC_DRAW);
  }

  // Matrix math helpers
  createPerspectiveMatrix(fovDeg, aspect, near, far) {
    const f = 1.0 / Math.tan((fovDeg * Math.PI) / 360.0);
    const nf = 1.0 / (near - far);
    return [
      f / aspect, 0, 0, 0,
      0, f, 0, 0,
      0, 0, (far + near) * nf, -1,
      0, 0, 2 * far * near * nf, 0
    ];
  }

  createLookAtMatrix(eyeX, eyeY, eyeZ, targetX, targetY, targetZ, upX = 0, upY = 1, upZ = 0) {
    let z0 = eyeX - targetX, z1 = eyeY - targetY, z2 = eyeZ - targetZ;
    let len = Math.hypot(z0, z1, z2) || 1;
    z0 /= len; z1 /= len; z2 /= len;

    let x0 = upY * z2 - upZ * z1, x1 = upZ * z0 - upX * z2, x2 = upX * z1 - upY * z0;
    len = Math.hypot(x0, x1, x2) || 1;
    x0 /= len; x1 /= len; x2 /= len;

    let y0 = z1 * x2 - z2 * x1, y1 = z2 * x0 - z0 * x2, y2 = z0 * x1 - z1 * x0;

    return [
      x0, y0, z0, 0,
      x1, y1, z1, 0,
      x2, y2, z2, 0,
      -(x0 * eyeX + x1 * eyeY + x2 * eyeZ),
      -(y0 * eyeX + y1 * eyeY + y2 * eyeZ),
      -(z0 * eyeX + z1 * eyeY + z2 * eyeZ),
      1
    ];
  }

  multiplyMatrices(a, b) {
    const out = new Array(16);
    for (let i = 0; i < 4; i++) {
      for (let j = 0; j < 4; j++) {
        out[j * 4 + i] =
          a[i] * b[j * 4] +
          a[i + 4] * b[j * 4 + 1] +
          a[i + 8] * b[j * 4 + 2] +
          a[i + 12] * b[j * 4 + 3];
      }
    }
    return out;
  }

  createTranslationMatrix(tx, ty, tz) {
    return [
      1, 0, 0, 0,
      0, 1, 0, 0,
      0, 0, 1, 0,
      tx, ty, tz, 1
    ];
  }

  createScaleMatrix(sx, sy, sz) {
    return [
      sx, 0, 0, 0,
      0, sy, 0, 0,
      0, 0, sz, 0,
      0, 0, 0, 1
    ];
  }

  createRotationYMatrix(angleDeg) {
    const rad = (angleDeg * Math.PI) / 180.0;
    const c = Math.cos(rad);
    const s = Math.sin(rad);
    return [
      c, 0, -s, 0,
      0, 1, 0, 0,
      s, 0, c, 0,
      0, 0, 0, 1
    ];
  }

  buildChunkMesh(chunk) {
    const gl = this.gl;
    const solidVertices = [];
    const waterVertices = [];

    const worldOffsetX = chunk.chunkX * VoxelChunk.SIZE_X;
    const worldOffsetZ = chunk.chunkZ * VoxelChunk.SIZE_Z;

    for (let y = 0; y < VoxelChunk.SIZE_Y; y++) {
      for (let z = 0; z < VoxelChunk.SIZE_Z; z++) {
        for (let x = 0; x < VoxelChunk.SIZE_X; x++) {
          const blockId = chunk.getBlock(x, y, z);
          if (blockId === 0) continue;

          const block = BlockRegistry.get(blockId);
          const wx = worldOffsetX + x;
          const wy = y;
          const wz = worldOffsetZ + z;
          const isWater = block.isLiquid;

          for (let face = 0; face < 6; face++) {
            const dx = VoxelChunk.DIR_VECTORS[face][0];
            const dy = VoxelChunk.DIR_VECTORS[face][1];
            const dz = VoxelChunk.DIR_VECTORS[face][2];

            const neighborId = this.world.getBlock(wx + dx, wy + dy, wz + dz);
            const neighbor = BlockRegistry.get(neighborId);

            let shouldRenderFace = false;
            if (isWater) {
              shouldRenderFace = neighborId !== blockId && neighbor.isTransparent;
            } else {
              shouldRenderFace = neighbor.isTransparent || neighbor.isLiquid || neighborId === 0;
              if (block.isTransparent && neighborId === blockId && blockId !== BlockRegistry.TORCH.id) {
                shouldRenderFace = false;
              }
            }

            if (shouldRenderFace) {
              let colorHex = block.sideColor;
              if (face === 0) colorHex = block.topColor;
              if (face === 1) colorHex = block.bottomColor;

              const shading = VoxelChunk.FACE_SHADING[face];
              const r = (((colorHex >> 16) & 0xFF) / 255.0) * shading;
              const g = (((colorHex >> 8) & 0xFF) / 255.0) * shading;
              const b = ((colorHex & 0xFF) / 255.0) * shading;
              const a = block.isLiquid ? 0.65 : (block.isTransparent && blockId === BlockRegistry.GLASS.id ? 0.35 : 1.0);

              const targetList = isWater ? waterVertices : solidVertices;
              this.addQuad(targetList, wx, wy, wz, face, r, g, b, a, block);
            }
          }
        }
      }
    }

    if (!chunk.solidBuffer) chunk.solidBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, chunk.solidBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(solidVertices), gl.STATIC_DRAW);
    chunk.solidCount = solidVertices.length / 13;

    if (!chunk.waterBuffer) chunk.waterBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, chunk.waterBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(waterVertices), gl.STATIC_DRAW);
    chunk.waterCount = waterVertices.length / 13;

    chunk.isDirty = false;
  }

  addQuad(list, x, y, z, face, r, g, b, a, block) {
    const pattern = block.texturePattern;
    const light = 100.0 + pattern;

    const quadCoords = [
      // Top (+Y)
      [[x, y + 1, z + 1, 0, 1, 0], [x + 1, y + 1, z + 1, 1, 1], [x + 1, y + 1, z, 1, 0], [x, y + 1, z + 1, 0, 1], [x + 1, y + 1, z, 1, 0], [x, y + 1, z, 0, 0]],
      // Bottom (-Y)
      [[x, y, z, 0, 0], [x + 1, y, z, 1, 0], [x + 1, y, z + 1, 1, 1], [x, y, z, 0, 0], [x + 1, y, z + 1, 1, 1], [x, y, z + 1, 0, 1]],
      // North (+Z)
      [[x, y, z + 1, 0, 1], [x + 1, y, z + 1, 1, 1], [x + 1, y + 1, z + 1, 1, 0], [x, y, z + 1, 0, 1], [x + 1, y + 1, z + 1, 1, 0], [x, y + 1, z + 1, 0, 0]],
      // South (-Z)
      [[x + 1, y, z, 0, 1], [x, y, z, 1, 1], [x, y + 1, z, 1, 0], [x + 1, y, z, 0, 1], [x, y + 1, z, 1, 0], [x + 1, y + 1, z, 0, 0]],
      // East (+X)
      [[x + 1, y, z + 1, 0, 1], [x + 1, y, z, 1, 1], [x + 1, y + 1, z, 1, 0], [x + 1, y, z + 1, 0, 1], [x + 1, y + 1, z, 1, 0], [x + 1, y + 1, z + 1, 0, 0]],
      // West (-X)
      [[x, y, z, 0, 1], [x, y, z + 1, 1, 1], [x, y + 1, z + 1, 1, 0], [x, y, z, 0, 1], [x, y + 1, z + 1, 1, 0], [x, y + 1, z, 0, 0]]
    ];

    const normals = [[0,1,0], [0,-1,0], [0,0,1], [0,0,-1], [1,0,0], [-1,0,0]];
    const n = normals[face];

    for (const v of quadCoords[face]) {
      const u = v.length === 5 ? v[3] : v[3];
      const vTex = v.length === 5 ? v[4] : v[4];
      list.push(v[0], v[1], v[2], n[0], n[1], n[2], r, g, b, a, u, vTex, light);
    }
  }

  render() {
    const gl = this.gl;
    if (!gl) return;

    gl.enable(gl.DEPTH_TEST);
    gl.depthFunc(gl.LEQUAL);
    gl.enable(gl.BLEND);
    gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

    // Sky colors
    const tod = this.world.timeOfDay;
    let skyR = 0.48, skyG = 0.72, skyB = 0.98;
    if (tod >= 8000 && tod <= 11000) {
      const t = (tod - 8000) / 3000.0;
      skyR = 0.48 + t * 0.44; skyG = 0.72 - t * 0.27; skyB = 0.98 - t * 0.73;
    } else if (tod > 11000 && tod <= 14000) {
      const t = (tod - 11000) / 3000.0;
      skyR = 0.92 - t * 0.87; skyG = 0.45 - t * 0.37; skyB = 0.25 - t * 0.11;
    } else if (tod > 14000 && tod <= 22000) {
      skyR = 0.04; skyG = 0.07; skyB = 0.14;
    }

    gl.clearColor(skyR, skyG, skyB, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    const aspect = this.canvas.width / this.canvas.height;
    const projMatrix = this.createPerspectiveMatrix(this.fov, aspect, 0.1, 120.0);

    const yawRad = (this.world.playerYaw * Math.PI) / 180.0;
    const pitchRad = (this.world.playerPitch * Math.PI) / 180.0;

    const forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
    const forwardY = -Math.sin(pitchRad);
    const forwardZ = Math.cos(yawRad) * Math.cos(pitchRad);

    let eyeX = this.world.playerX;
    let eyeY = this.world.playerY + 1.62;
    let eyeZ = this.world.playerZ;
    let targetX = eyeX + forwardX;
    let targetY = eyeY + forwardY;
    let targetZ = eyeZ + forwardZ;

    if (this.cameraMode === CameraMode.THIRD_PERSON_BACK) {
      const camDist = 3.5;
      eyeX = this.world.playerX - forwardX * camDist;
      eyeY = this.world.playerY + 1.62 - forwardY * camDist;
      eyeZ = this.world.playerZ - forwardZ * camDist;
      targetX = this.world.playerX; targetY = this.world.playerY + 1.3; targetZ = this.world.playerZ;
    } else if (this.cameraMode === CameraMode.THIRD_PERSON_FRONT) {
      const camDist = 3.5;
      eyeX = this.world.playerX + forwardX * camDist;
      eyeY = this.world.playerY + 1.62 + forwardY * camDist;
      eyeZ = this.world.playerZ + forwardZ * camDist;
      targetX = this.world.playerX; targetY = this.world.playerY + 1.3; targetZ = this.world.playerZ;
    }

    const viewMatrix = this.createLookAtMatrix(eyeX, eyeY, eyeZ, targetX, targetY, targetZ);
    const vpMatrix = this.multiplyMatrices(projMatrix, viewMatrix);

    gl.useProgram(this.program);
    gl.uniform1f(this.uSkyLightLocation, this.world.skyLight);
    gl.uniform3f(this.uFogColorLocation, skyR, skyG, skyB);

    // 1. Sun & Moon
    this.renderSunAndMoon(eyeX, eyeY, eyeZ, vpMatrix);

    // 2. Chunks
    this.renderChunks(vpMatrix);

    // 3. Entities
    this.renderEntities(vpMatrix);

    // 4. Breaking Box
    this.renderBreakingBox(vpMatrix);

    // 5. First person hand
    if (this.cameraMode === CameraMode.FIRST_PERSON) {
      this.renderFirstPersonHand(eyeX, eyeY, eyeZ, forwardX, forwardY, forwardZ, vpMatrix);
    }
  }

  renderSunAndMoon(cx, cy, cz, vpMatrix) {
    const gl = this.gl;
    const angle = (this.world.timeOfDay / 24000.0) * Math.PI * 2.0 - Math.PI / 2.0;
    const sunDist = 45.0;

    // Sun
    const sunX = cx + Math.cos(angle) * sunDist;
    const sunY = cy + Math.sin(angle) * sunDist;
    const sunZ = cz;

    let model = this.multiplyMatrices(this.createTranslationMatrix(sunX, sunY, sunZ), this.createScaleMatrix(7, 7, 7));
    let mvp = this.multiplyMatrices(vpMatrix, model);
    gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);
    this.drawUnitCube(1.0, 0.95, 0.2, 1.0);

    // Moon
    const moonX = cx - Math.cos(angle) * sunDist;
    const moonY = cy - Math.sin(angle) * sunDist;
    const moonZ = cz;

    model = this.multiplyMatrices(this.createTranslationMatrix(moonX, moonY, moonZ), this.createScaleMatrix(5.5, 5.5, 5.5));
    mvp = this.multiplyMatrices(vpMatrix, model);
    gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);
    this.drawUnitCube(0.9, 0.92, 1.0, 1.0);
  }

  renderChunks(vpMatrix) {
    const gl = this.gl;
    const playerChunkX = Math.floor(this.world.playerX / VoxelChunk.SIZE_X);
    const playerChunkZ = Math.floor(this.world.playerZ / VoxelChunk.SIZE_Z);

    gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, vpMatrix);

    // Solid chunks
    for (let dx = -this.renderDistance; dx <= this.renderDistance; dx++) {
      for (let dz = -this.renderDistance; dz <= this.renderDistance; dz++) {
        const cx = playerChunkX + dx;
        const cz = playerChunkZ + dz;
        const chunk = this.world.getChunk(cx, cz);
        if (!chunk) continue;

        if (chunk.isDirty) {
          this.buildChunkMesh(chunk);
        }

        if (chunk.solidCount > 0) {
          gl.bindBuffer(gl.ARRAY_BUFFER, chunk.solidBuffer);
          this.bindAttribPointers();
          gl.drawArrays(gl.TRIANGLES, 0, chunk.solidCount);
        }
      }
    }

    // Water chunks
    for (let dx = -this.renderDistance; dx <= this.renderDistance; dx++) {
      for (let dz = -this.renderDistance; dz <= this.renderDistance; dz++) {
        const cx = playerChunkX + dx;
        const cz = playerChunkZ + dz;
        const chunk = this.world.getChunk(cx, cz);
        if (!chunk || chunk.waterCount === 0) continue;

        gl.bindBuffer(gl.ARRAY_BUFFER, chunk.waterBuffer);
        this.bindAttribPointers();
        gl.drawArrays(gl.TRIANGLES, 0, chunk.waterCount);
      }
    }
  }

  renderEntities(vpMatrix) {
    // Mobs
    for (const mob of this.world.mobs) {
      if (mob.isDead) continue;
      this.renderMob(mob, vpMatrix);
    }

    // Arrows
    for (const arrow of this.world.arrows) {
      const model = this.multiplyMatrices(this.createTranslationMatrix(arrow.x, arrow.y, arrow.z), this.createScaleMatrix(0.15, 0.15, 0.5));
      const mvp = this.multiplyMatrices(vpMatrix, model);
      this.gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);
      this.drawUnitCube(0.8, 0.7, 0.5, 1.0);
    }

    // Item Drops
    for (const drop of this.world.itemDrops) {
      let model = this.createTranslationMatrix(drop.x, drop.y + drop.hoverOffset, drop.z);
      model = this.multiplyMatrices(model, this.createRotationYMatrix(drop.spinAngle));
      model = this.multiplyMatrices(model, this.createScaleMatrix(0.35, 0.35, 0.35));
      const mvp = this.multiplyMatrices(vpMatrix, model);
      this.gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);

      const color = drop.itemStack.item.iconColor || 0xFFFFFF;
      const r = ((color >> 16) & 0xFF) / 255.0;
      const g = ((color >> 8) & 0xFF) / 255.0;
      const b = (color & 0xFF) / 255.0;
      this.drawUnitCube(r, g, b, 1.0);
    }

    // Particles
    for (const p of this.world.particles) {
      if (p.isDead) continue;
      const model = this.multiplyMatrices(this.createTranslationMatrix(p.x, p.y, p.z), this.createScaleMatrix(p.size, p.size, p.size));
      const mvp = this.multiplyMatrices(vpMatrix, model);
      this.gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);

      const r = ((p.color >> 16) & 0xFF) / 255.0;
      const g = ((p.color >> 8) & 0xFF) / 255.0;
      const b = (p.color & 0xFF) / 255.0;
      this.drawUnitCube(r, g, b, 1.0 - p.life / p.maxLife);
    }
  }

  renderMob(mob, vpMatrix) {
    const isHurt = mob.hurtTime > 0;
    const isCreeperFlashing = mob.type === EntityType.CREEPER && mob.state === 'HISSING' && Math.floor(mob.creeperFuse * 8) % 2 === 0;
    const legSwing = Math.sin(mob.animTime * 6.0) * 25.0;

    switch (mob.type) {
      case EntityType.CREEPER:
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 0.6, mob.z, mob.yaw, 0.45, 0.7, 0.25, isCreeperFlashing ? 1 : (isHurt ? 1 : 0.2), isCreeperFlashing ? 1 : (isHurt ? 0.2 : 0.75), isCreeperFlashing ? 1 : (isHurt ? 0.2 : 0.2));
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 1.25, mob.z, mob.yaw, 0.5, 0.5, 0.5, isCreeperFlashing ? 1 : 0.25, isCreeperFlashing ? 1 : 0.8, isCreeperFlashing ? 1 : 0.25);
        this.renderVoxelBox(vpMatrix, mob.x - 0.15, mob.y + 0.2, mob.z - 0.15, mob.yaw + legSwing, 0.2, 0.4, 0.2, 0.18, 0.65, 0.18);
        this.renderVoxelBox(vpMatrix, mob.x + 0.15, mob.y + 0.2, mob.z - 0.15, mob.yaw - legSwing, 0.2, 0.4, 0.2, 0.18, 0.65, 0.18);
        break;

      case EntityType.ZOMBIE:
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 1.6, mob.z, mob.yaw, 0.45, 0.45, 0.45, isHurt ? 1 : 0.32, isHurt ? 0.2 : 0.55, isHurt ? 0.2 : 0.25);
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 1.05, mob.z, mob.yaw, 0.5, 0.65, 0.28, 0.15, 0.45, 0.75);
        this.renderVoxelBox(vpMatrix, mob.x - 0.32, mob.y + 1.15, mob.z + 0.3, mob.yaw, 0.18, 0.18, 0.65, isHurt ? 1 : 0.32, isHurt ? 0.2 : 0.55, isHurt ? 0.2 : 0.25);
        this.renderVoxelBox(vpMatrix, mob.x + 0.32, mob.y + 1.15, mob.z + 0.3, mob.yaw, 0.18, 0.18, 0.65, isHurt ? 1 : 0.32, isHurt ? 0.2 : 0.55, isHurt ? 0.2 : 0.25);
        this.renderVoxelBox(vpMatrix, mob.x - 0.14, mob.y + 0.38, mob.z, mob.yaw + legSwing, 0.22, 0.75, 0.24, 0.12, 0.18, 0.4);
        this.renderVoxelBox(vpMatrix, mob.x + 0.14, mob.y + 0.38, mob.z, mob.yaw - legSwing, 0.22, 0.75, 0.24, 0.12, 0.18, 0.4);
        break;

      case EntityType.SKELETON:
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 1.6, mob.z, mob.yaw, 0.45, 0.45, 0.45, isHurt ? 1 : 0.88, isHurt ? 0.3 : 0.88, isHurt ? 0.3 : 0.85);
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 1.05, mob.z, mob.yaw, 0.45, 0.65, 0.24, 0.7, 0.7, 0.7);
        this.renderVoxelBox(vpMatrix, mob.x - 0.28, mob.y + 1.1, mob.z + 0.25, mob.yaw, 0.12, 0.12, 0.5, 0.5, 0.35, 0.2);
        this.renderVoxelBox(vpMatrix, mob.x + 0.28, mob.y + 1.1, mob.z + 0.25, mob.yaw, 0.12, 0.12, 0.5, 0.5, 0.35, 0.2);
        this.renderVoxelBox(vpMatrix, mob.x - 0.12, mob.y + 0.38, mob.z, mob.yaw + legSwing, 0.15, 0.75, 0.15, 0.88, 0.88, 0.85);
        this.renderVoxelBox(vpMatrix, mob.x + 0.12, mob.y + 0.38, mob.z, mob.yaw - legSwing, 0.15, 0.75, 0.15, 0.88, 0.88, 0.85);
        break;

      case EntityType.PIG:
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 0.55, mob.z, mob.yaw, 0.65, 0.55, 0.9, isHurt ? 1 : 0.95, isHurt ? 0.4 : 0.68, isHurt ? 0.4 : 0.68);
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 0.65, mob.z + 0.5, mob.yaw, 0.45, 0.45, 0.45, isHurt ? 1 : 0.95, isHurt ? 0.4 : 0.68, isHurt ? 0.4 : 0.68);
        this.renderVoxelBox(vpMatrix, mob.x - 0.2, mob.y + 0.2, mob.z - 0.25, mob.yaw + legSwing, 0.18, 0.4, 0.18, 0.85, 0.6, 0.6);
        this.renderVoxelBox(vpMatrix, mob.x + 0.2, mob.y + 0.2, mob.z - 0.25, mob.yaw - legSwing, 0.18, 0.4, 0.18, 0.85, 0.6, 0.6);
        break;

      case EntityType.COW:
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 0.8, mob.z, mob.yaw, 0.8, 0.7, 1.1, isHurt ? 1 : 0.45, isHurt ? 0.3 : 0.35, isHurt ? 0.3 : 0.25);
        this.renderVoxelBox(vpMatrix, mob.x, mob.y + 1.05, mob.z + 0.65, mob.yaw, 0.5, 0.5, 0.5, isHurt ? 1 : 0.45, isHurt ? 0.3 : 0.35, isHurt ? 0.3 : 0.25);
        this.renderVoxelBox(vpMatrix, mob.x - 0.25, mob.y + 0.3, mob.z - 0.35, mob.yaw + legSwing, 0.22, 0.6, 0.22, 0.3, 0.25, 0.2);
        this.renderVoxelBox(vpMatrix, mob.x + 0.25, mob.y + 0.3, mob.z - 0.35, mob.yaw - legSwing, 0.22, 0.6, 0.22, 0.3, 0.25, 0.2);
        break;
    }
  }

  renderVoxelBox(vpMatrix, x, y, z, yaw, sx, sy, sz, r, g, b) {
    let model = this.createTranslationMatrix(x, y, z);
    model = this.multiplyMatrices(model, this.createRotationYMatrix(yaw));
    model = this.multiplyMatrices(model, this.createScaleMatrix(sx, sy, sz));
    const mvp = this.multiplyMatrices(vpMatrix, model);
    this.gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);
    this.drawUnitCube(r, g, b, 1.0);
  }

  renderBreakingBox(vpMatrix) {
    if (this.world.breakingBlockX === -1 || this.world.breakProgress <= 0) return;

    let model = this.createTranslationMatrix(
      this.world.breakingBlockX + 0.5,
      this.world.breakingBlockY + 0.5,
      this.world.breakingBlockZ + 0.5
    );
    model = this.multiplyMatrices(model, this.createScaleMatrix(1.02, 1.02, 1.02));
    const mvp = this.multiplyMatrices(vpMatrix, model);
    this.gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);
    this.drawUnitCube(0.1, 0.1, 0.1, 0.45 * Math.min(0.8, Math.max(0.1, this.world.breakProgress)));
  }

  renderFirstPersonHand(eyeX, eyeY, eyeZ, fwdX, fwdY, fwdZ, vpMatrix) {
    const equipped = this.world.getEquippedItem();
    const yawRad = (this.world.playerYaw * Math.PI) / 180.0;
    const rightX = Math.cos(yawRad);
    const rightZ = Math.sin(yawRad);

    const handX = eyeX + fwdX * 0.6 + rightX * 0.35;
    const handY = eyeY + fwdY * 0.6 - 0.28;
    const handZ = eyeZ + fwdZ * 0.6 + rightZ * 0.35;

    let model = this.createTranslationMatrix(handX, handY, handZ);
    model = this.multiplyMatrices(model, this.createRotationYMatrix(this.world.playerYaw + 45));

    if (equipped.isEmpty) {
      model = this.multiplyMatrices(model, this.createScaleMatrix(0.14, 0.32, 0.14));
      const mvp = this.multiplyMatrices(vpMatrix, model);
      this.gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);
      this.drawUnitCube(0.95, 0.75, 0.65, 1.0);
    } else {
      const color = equipped.item.iconColor || 0xFFFFFF;
      const r = ((color >> 16) & 0xFF) / 255.0;
      const g = ((color >> 8) & 0xFF) / 255.0;
      const b = (color & 0xFF) / 255.0;

      if (equipped.item.blockId) {
        model = this.multiplyMatrices(model, this.createScaleMatrix(0.22, 0.22, 0.22));
      } else {
        model = this.multiplyMatrices(model, this.createScaleMatrix(0.08, 0.45, 0.08));
      }
      const mvp = this.multiplyMatrices(vpMatrix, model);
      this.gl.uniformMatrix4fv(this.uMvpMatrixLocation, false, mvp);
      this.drawUnitCube(r, g, b, 1.0);
    }
  }

  drawUnitCube(r, g, b, a) {
    const gl = this.gl;
    gl.bindBuffer(gl.ARRAY_BUFFER, this.unitCubeBuffer);
    this.bindAttribPointers();
    gl.drawArrays(gl.TRIANGLES, 0, 36);
  }

  bindAttribPointers() {
    const gl = this.gl;
    const stride = 13 * 4;

    if (this.aPosLocation >= 0) {
      gl.enableVertexAttribArray(this.aPosLocation);
      gl.vertexAttribPointer(this.aPosLocation, 3, gl.FLOAT, false, stride, 0);
    }
    if (this.aNormalLocation >= 0) {
      gl.enableVertexAttribArray(this.aNormalLocation);
      gl.vertexAttribPointer(this.aNormalLocation, 3, gl.FLOAT, false, stride, 3 * 4);
    }
    if (this.aColorLocation >= 0) {
      gl.enableVertexAttribArray(this.aColorLocation);
      gl.vertexAttribPointer(this.aColorLocation, 4, gl.FLOAT, false, stride, 6 * 4);
    }
    if (this.aTexCoordLocation >= 0) {
      gl.enableVertexAttribArray(this.aTexCoordLocation);
      gl.vertexAttribPointer(this.aTexCoordLocation, 2, gl.FLOAT, false, stride, 10 * 4);
    }
    if (this.aLightLocation >= 0) {
      gl.enableVertexAttribArray(this.aLightLocation);
      gl.vertexAttribPointer(this.aLightLocation, 1, gl.FLOAT, false, stride, 12 * 4);
    }
  }
}
