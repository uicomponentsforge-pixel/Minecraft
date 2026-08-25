/**
 * Procedural Web Audio API Synthesizer Engine
 * Converted from SoundEngine.kt
 */
class SoundEngine {
  constructor() {
    this.audioCtx = null;
    this.isMuted = false;
    this.sampleRate = 22050;
  }

  initContext() {
    if (!this.audioCtx) {
      const AudioCtxClass = window.AudioContext || window.webkitAudioContext;
      if (AudioCtxClass) {
        this.audioCtx = new AudioCtxClass();
      }
    }
    if (this.audioCtx && this.audioCtx.state === 'suspended') {
      this.audioCtx.resume();
    }
  }

  playPcm(generator) {
    if (this.isMuted) return;
    try {
      this.initContext();
      if (!this.audioCtx) return;

      const floatSamples = generator(this.sampleRate);
      if (!floatSamples || floatSamples.length === 0) return;

      const buffer = this.audioCtx.createBuffer(1, floatSamples.length, this.sampleRate);
      const channelData = buffer.getChannelData(0);
      for (let i = 0; i < floatSamples.length; i++) {
        channelData[i] = floatSamples[i];
      }

      const source = this.audioCtx.createBufferSource();
      source.buffer = buffer;
      source.connect(this.audioCtx.destination);
      source.start();
    } catch (e) {
      // Ignore sound errors gracefully
    }
  }

  playPlace() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.08);
      const data = new Float32Array(duration);
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const freq = 240.0 - (t / 0.08) * 120.0;
        const env = Math.pow(1.0 - t / 0.08, 1.5);
        data[i] = Math.sin(2.0 * Math.PI * freq * t) * env * 0.5;
      }
      return data;
    });
  }

  playDig() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.06);
      const data = new Float32Array(duration);
      let last = 0.0;
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const env = 1.0 - t / 0.06;
        const noise = Math.random() * 2.0 - 1.0;
        last = last * 0.7 + noise * 0.3;
        data[i] = last * env * 0.45;
      }
      return data;
    });
  }

  playBreak() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.15);
      const data = new Float32Array(duration);
      let last = 0.0;
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const env = Math.pow(1.0 - t / 0.15, 0.8);
        const noise = Math.random() * 2.0 - 1.0;
        last = last * 0.6 + noise * 0.4;
        const sub = Math.sin(2.0 * Math.PI * (120.0 - t * 400.0) * t) * 0.5;
        data[i] = (last * 0.8 + sub * 0.2) * env * 0.6;
      }
      return data;
    });
  }

  playStep() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.05);
      const data = new Float32Array(duration);
      let last = 0.0;
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const env = 1.0 - t / 0.05;
        const noise = Math.random() * 2.0 - 1.0;
        last = last * 0.8 + noise * 0.2;
        data[i] = last * env * 0.25;
      }
      return data;
    });
  }

  playJump() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.12);
      const data = new Float32Array(duration);
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const freq = 160.0 + (t / 0.12) * 200.0;
        const env = Math.sin((t / 0.12) * Math.PI);
        data[i] = Math.sin(2.0 * Math.PI * freq * t) * env * 0.4;
      }
      return data;
    });
  }

  playHurt() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.2);
      const data = new Float32Array(duration);
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const freq = 280.0 - (t / 0.2) * 160.0;
        const env = Math.pow(1.0 - t / 0.2, 1.2);
        const square = Math.sin(2.0 * Math.PI * freq * t) > 0 ? 1.0 : -1.0;
        data[i] = square * env * 0.5;
      }
      return data;
    });
  }

  playSwordSwing() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.14);
      const data = new Float32Array(duration);
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const env = Math.sin((t / 0.14) * Math.PI);
        const noise = Math.random() * 2.0 - 1.0;
        const sweep = Math.sin(2.0 * Math.PI * (400.0 + (1.0 - t / 0.14) * 800.0) * t);
        data[i] = (noise * 0.6 + sweep * 0.4) * env * 0.45;
      }
      return data;
    });
  }

  playCreeperHiss() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 1.5);
      const data = new Float32Array(duration);
      let last = 0.0;
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const env = Math.pow(t / 1.5, 1.5);
        const noise = Math.random() * 2.0 - 1.0;
        last = last * 0.4 + noise * 0.6;
        data[i] = last * env * 0.65;
      }
      return data;
    });
  }

  playExplosion() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.7);
      const data = new Float32Array(duration);
      let rumble = 0.0;
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const env = Math.pow(1.0 - t / 0.7, 0.6);
        const noise = Math.random() * 2.0 - 1.0;
        rumble = rumble * 0.85 + noise * 0.15;
        const sub = Math.sin(2.0 * Math.PI * 55.0 * t);
        data[i] = Math.max(-1.0, Math.min(1.0, (noise * 0.5 + rumble * 0.3 + sub * 0.2) * env)) * 0.8;
      }
      return data;
    });
  }

  playEat() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.3);
      const data = new Float32Array(duration);
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const crunchPhase = (t * 8.0) % 1.0;
        const env = Math.pow(1.0 - crunchPhase, 2.0);
        const noise = Math.random() * 2.0 - 1.0;
        data[i] = noise * env * 0.45;
      }
      return data;
    });
  }

  playBowShoot() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.18);
      const data = new Float32Array(duration);
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const freq = 480.0 - (t / 0.18) * 320.0;
        const env = Math.pow(1.0 - t / 0.18, 1.2);
        data[i] = Math.sin(2.0 * Math.PI * freq * t) * env * 0.55;
      }
      return data;
    });
  }

  playCraftSuccess() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.35);
      const data = new Float32Array(duration);
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const note1 = Math.sin(2.0 * Math.PI * 523.25 * t);
        const note2 = t > 0.12 ? Math.sin(2.0 * Math.PI * 659.25 * t) : 0.0;
        const note3 = t > 0.22 ? Math.sin(2.0 * Math.PI * 783.99 * t) : 0.0;
        const env = Math.pow(1.0 - (t / 0.35), 0.8);
        data[i] = (note1 * 0.4 + note2 * 0.4 + note3 * 0.5) * env * 0.5;
      }
      return data;
    });
  }

  playLevelUp() {
    this.playPcm((rate) => {
      const duration = Math.floor(rate * 0.6);
      const data = new Float32Array(duration);
      const notes = [440.0, 554.37, 659.25, 880.0];
      for (let i = 0; i < duration; i++) {
        const t = i / rate;
        const noteIdx = Math.min(3, Math.floor(t / 0.15));
        const freq = notes[noteIdx];
        const noteT = t % 0.15;
        const env = Math.pow(1.0 - noteT / 0.15, 0.5);
        data[i] = Math.sin(2.0 * Math.PI * freq * t) * env * 0.55;
      }
      return data;
    });
  }
}
