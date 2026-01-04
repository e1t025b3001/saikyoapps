(function () {
  // HTMLAudio ベースの簡易サウンドマネージャ
  const manifestUrl = '/sounds/manifest.json';
  const SoundManager = {
    manifest: {},
    muted: false,
    volume: 1.0,
    preloaded: {},
    _initialized: false,

    init: async function () {
      if (this._initialized) return;
      try {
        const res = await fetch(manifestUrl, { cache: 'no-store' });
        if (res.ok) this.manifest = await res.json();
      } catch (e) {
        console.warn('sound manifest load failed', e);
      }

      try {
        const m = localStorage.getItem('soundMuted');
        if (m !== null) this.muted = (m === 'true');
        const v = localStorage.getItem('soundVolume');
        if (v !== null) this.volume = parseFloat(v);
      } catch (e) { }

      // プリロードフラグがある音を先に読み込む
      for (const key in this.manifest) {
        const entry = this.manifest[key];
        if (entry && entry.files && entry.preload) {
          this._preload(key, entry.files[0]);
        }
      }

      this._initialized = true;
      try { console.log('soundManager initialized', this.manifest); } catch (e) { }
    },

    _preload: function (key, src) {
      try {
        const a = new Audio(src);
        a.preload = 'auto';
        a.volume = this.volume;
        a.muted = this.muted;
        // keep only the src reference for simple use
        this.preloaded[key] = src;
      } catch (e) {
        console.warn('preload failed', e);
      }
    },

    play: function (key) {
      try { console.log('soundManager.play request', key); } catch (e) { }
      const entry = this.manifest[key];
      let src = null;
      if (entry && entry.files && entry.files.length > 0) src = entry.files[0];
      else if (this.preloaded[key]) src = this.preloaded[key];
      try { console.log('resolved src for', key, src); } catch (e) { }
      if (!src) {
        try { console.warn('soundManager: no src for key', key); } catch (e) { }
        return null;
      }
      try {
        const a = new Audio(src);
        const baseVol = (entry && typeof entry.volume === 'number') ? entry.volume : 1.0;
        a.volume = Math.max(0, Math.min(1, baseVol * this.volume));
        a.muted = this.muted;
        // keep reference to avoid GC and allow inspection
        this._audios = this._audios || [];
        this._audios.push(a);
        try { window._lastSound = a; } catch (e) { }
        const playPromise = a.play();
        if (playPromise && typeof playPromise.then === 'function') {
          playPromise.then(() => {
            try { console.log('soundManager: play succeeded for', key); } catch (e) { }
          }).catch(err => {
            try { console.error('soundManager: play failed', err); } catch (e) { }
          });
        }
        return playPromise;
      } catch (e) {
        console.warn('play error', e);
        return Promise.reject(e);
      }
    },

    setMuted: function (m) {
      this.muted = !!m;
      try { localStorage.setItem('soundMuted', this.muted ? 'true' : 'false'); } catch (e) { }
    },

    isMuted: function () { return this.muted; },

    setVolume: function (v) {
      this.volume = Math.max(0, Math.min(1, v));
      try { localStorage.setItem('soundVolume', String(this.volume)); } catch (e) { }
    }
  };

  window.soundManager = SoundManager;

  // 自動で manifest を読み込んでおく（オートプレイは行わない）
  try {
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', function () {
        if (!SoundManager._initialized) SoundManager.init().catch(e => console.warn('soundManager auto init failed', e));
      });
    } else {
      if (!SoundManager._initialized) SoundManager.init().catch(e => console.warn('soundManager auto init failed', e));
    }
  } catch (e) { }
})(
);
