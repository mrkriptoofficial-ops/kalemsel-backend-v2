'use strict';
/* Zikirmatik web — Android sürümünün (3.5) tesbih, kubbe, ses ve sayma kurallarının web karşılığı. */

// ───────────── Yardımcılar ─────────────
const $ = (s) => document.querySelector(s);
const nf = new Intl.NumberFormat('tr-TR');
const fmt = (n) => nf.format(n);
const clamp = (x, a, b) => Math.max(a, Math.min(b, x));
const lerp = (a, b, t) => a + (b - a) * t;
const PI = Math.PI;
const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
function dayKey(d = new Date()) {
  const z = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`;
}
function esc(s) { return String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
// renk: [r,g,b,a]
function hex(h, a = 1) { const n = parseInt(h.slice(1), 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255, a]; }
const rgba = (c, a = 1) => `rgba(${c[0] | 0},${c[1] | 0},${c[2] | 0},${+(c[3] * a).toFixed(4)})`;
const mix = (a, b, t) => [lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t), lerp(a[3], b[3], t)];

// ───────────── Kalıcı durum ─────────────
const KEY = 'zikirmatik.v1';
let uid = Date.now();
const defaults = () => ({
  list: DATA.varsayilan.map((z, i) => ({ id: i + 1, ar: z.ar, ok: z.ok, an: z.an, fz: z.fz, c: 0, t: z.t })),
  selId: 1, total: 0, history: {}, laps: {},
  virds: DATA.virdler.map((v) => ({ id: v.id, name: v.name, steps: v.steps })),
  session: null,
  settings: { bead: 0, shape: 0, imame: 0, cord: 0, sound: true, soundType: 0, vol: 1, slide: false, haptic: true, stopAtTarget: false, milestone: 33, wake: true },
});
let S;
try { S = Object.assign(defaults(), JSON.parse(localStorage.getItem(KEY) || 'null') || {}); } catch { S = defaults(); }
S.settings = Object.assign(defaults().settings, S.settings || {});
if (!S.settings.soundV2) { S.settings.sound = true; S.settings.soundV2 = true; }
if (!S.list.length) S.list = defaults().list;
if (!S.list.find((z) => z.id === S.selId)) S.selId = S.list[0].id;
let saveT = 0;
function save() { clearTimeout(saveT); saveT = setTimeout(() => { try { localStorage.setItem(KEY, JSON.stringify(S)); } catch {} }, 150); }
window.addEventListener('pagehide', () => { try { localStorage.setItem(KEY, JSON.stringify(S)); } catch {} });
if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(() => {});

const sel = () => S.list.find((z) => z.id === S.selId) || S.list[0];
const vird = () => S.session && S.virds.find((v) => v.id === S.session.virdId);
function disp() {
  const v = vird();
  if (v) {
    const st = v.steps[S.session.step];
    return { ar: st.ar, ok: st.ok, an: st.an, fz: '', count: S.session.count, target: st.t, vird: v, step: S.session.step, key: 'v' + v.id + '_' + S.session.step };
  }
  const z = sel();
  return { ar: z.ar, ok: z.ok, an: z.an, fz: z.fz, count: z.c, target: z.t, vird: null, key: 'z' + z.id };
}
function streak() {
  const d = new Date(); let n = 0;
  if (!(S.history[dayKey(d)] > 0)) d.setDate(d.getDate() - 1);
  while (S.history[dayKey(d)] > 0) { n++; d.setDate(d.getDate() - 1); }
  return n;
}

// ───────────── Ses (Android'deki sentez formüllerinin aynısı) ─────────────
const SR = 48000;
function rng(seed) { let a = seed >>> 0; return () => { a = (a + 0x6D2B79F5) >>> 0; let t = a; t = Math.imul(t ^ (t >>> 15), t | 1); t ^= t + Math.imul(t ^ (t >>> 7), t | 61); return ((t ^ (t >>> 14)) >>> 0) / 4294967296; }; }
const SPECS = {
  kehribar: [0.075, (t, r, st) => {
    const hit = (tt, amp) => tt < 0 ? 0 : amp * (Math.sin(2 * PI * 1850 * tt) * Math.exp(-tt * 420) * 0.55 + Math.sin(2 * PI * 2950 * tt) * Math.exp(-tt * 600) * 0.30 +
      Math.sin(2 * PI * 4300 * tt) * Math.exp(-tt * 950) * 0.12 + Math.sin(2 * PI * 900 * tt) * Math.exp(-tt * 260) * 0.18);
    const v = hit(t, 1) + hit(t - 0.0095, 0.42) + hit(t - 0.0195, 0.20);
    st[0] += 0.55 * ((r() * 2 - 1) - st[0]); st[1] += 0.12 * (st[0] - st[1]);
    const band = st[0] - st[1], burst = (tt, a) => tt < 0 ? 0 : Math.exp(-tt * 750) * a;
    return v + band * (burst(t, 0.55) + burst(t - 0.0095, 0.22) + burst(t - 0.0195, 0.10));
  }],
  ahsap: [0.075, (t, r, st) => {
    let v = Math.sin(2 * PI * 1150 * t) * Math.exp(-t * 95) * 0.45 + Math.sin(2 * PI * 2380 * t) * Math.exp(-t * 160) * 0.20 + Math.sin(2 * PI * 520 * t) * Math.exp(-t * 60) * 0.22;
    const t2 = t - 0.006; if (t2 > 0) v += Math.sin(2 * PI * 1320 * t2) * Math.exp(-t2 * 120) * 0.12;
    st[0] += 0.25 * ((r() * 2 - 1) - st[0]);
    return v + st[0] * Math.exp(-t * 700) * 0.30;
  }],
  kristal: [0.30, (t) => Math.sin(2 * PI * 3520 * t) * Math.exp(-t * 18) * 0.30 + Math.sin(2 * PI * 5280 * t) * Math.exp(-t * 26) * 0.16 +
    Math.sin(2 * PI * 7040 * t) * Math.exp(-t * 40) * 0.08 + Math.sin(2 * PI * 4980 * t) * Math.exp(-t * 300) * 0.20],
  kayma: [0.160, (t, r, st) => {
    const x = t / 0.160, env = Math.sin(PI * x) ** 2, a = 0.06 + 0.30 * Math.sin(PI * x);
    st[0] += a * ((r() * 2 - 1) - st[0]); st[1] += a * (st[0] - st[1]);
    return (st[0] - st[1]) * env * 2.2;
  }],
  zil: [1.6, (t) => {
    const bell = (f, tt) => tt < 0 ? 0 : (Math.sin(2 * PI * f * tt) * Math.exp(-tt * 3.2) * 0.50 + Math.sin(2 * PI * f * 2 * tt) * Math.exp(-tt * 4.5) * 0.22 +
      Math.sin(2 * PI * f * 2.76 * tt) * Math.exp(-tt * 6) * 0.14 + Math.sin(2 * PI * f * 5.4 * tt) * Math.exp(-tt * 9) * 0.06) * Math.min(tt / 0.004, 1);
    return bell(659.25, t) * 0.75 + bell(987.77, t - 0.13) * 0.55;
  }],
};
const SOUND_TYPES = ['kehribar', 'ahsap', 'kristal'];
const VOLS = [0.55, 0.8, 1];
const PULL_MS = 170;
const Audio = {
  ctx: null, buf: {},
  unlock() {
    if (this.ctx) { if (this.ctx.state === 'suspended') this.ctx.resume(); return; }
    const AC = window.AudioContext || window.webkitAudioContext; if (!AC) return;
    this.ctx = new AC({ latencyHint: 'interactive' });
    // Tesbih sesi her zaman duyulsun: iPhone'da sessiz anahtarı açık olsa da çalar
    try { if (navigator.audioSession) navigator.audioSession.type = 'playback'; } catch {}
    const rate = this.ctx.sampleRate;
    for (const [name, [sec, gen]] of Object.entries(SPECS)) {
      const n = Math.floor(rate * sec), b = this.ctx.createBuffer(1, n, rate), d = b.getChannelData(0);
      const r = rng(11), st = [0, 0]; let pk = 0;
      for (let i = 0; i < n; i++) {
        const env = Math.min(i / 24, 1) * Math.min((n - i) / 240, 1);
        const v = Math.tanh(gen(i / rate, r, st) * 1.8) * env; d[i] = v; pk = Math.max(pk, Math.abs(v));
      }
      if (pk > 0) for (let i = 0; i < n; i++) d[i] *= 0.97 / pk;
      this.buf[name] = b;
    }
    // iOS: ilk dokunuşta sessiz bir ses çalarak bağlamı aç
    const s = this.ctx.createBufferSource(); s.buffer = this.ctx.createBuffer(1, 1, rate); s.connect(this.ctx.destination); s.start();
  },
  play(name, vol, delay = 0, rate = 1) {
    const c = this.ctx; if (!c || !this.buf[name] || vol <= 0) return;
    const s = c.createBufferSource(), g = c.createGain();
    s.buffer = this.buf[name]; s.playbackRate.value = rate; g.gain.value = vol;
    s.connect(g).connect(c.destination); s.start(c.currentTime + delay);
  },
};
function soundOn() { return S.settings.sound; }
function fbPull(milestone, completed) {
  if (!soundOn()) return;
  const v = VOLS[S.settings.vol] ?? 0.8, type = SOUND_TYPES[S.settings.soundType] || 'kehribar';
  if (S.settings.slide) Audio.play('kayma', v * 0.55, 0, 0.96 + Math.random() * 0.08);
  Audio.play(type, v, PULL_MS / 1000, 0.96 + Math.random() * 0.08);
  if (milestone) Audio.play(type, v * 0.85, PULL_MS / 1000 + 0.09, 1.04);
  if (completed) Audio.play('zil', v, PULL_MS / 1000);
}
// Titreşim: Android'de Vibration API; iPhone'da (iOS 18+) sistem anahtarının dokunsal geri bildirimi
const hapticSw = $('#hapticSw');
function haptic(kind, delay = 0) {
  if (!S.settings.haptic) return;
  if (navigator.vibrate) {
    const pat = kind === 'complete' ? [40, 60, 40, 60, 90] : kind === 'milestone' ? [18, 70, 18] : kind === 'error' ? [30, 50, 30] : [14];
    navigator.vibrate(delay > 0 ? [0, delay, ...pat] : [0, ...pat]);
  } else if (hapticSw) {
    const lab = hapticSw.parentElement;
    if (delay > 0) setTimeout(() => lab.click(), delay); else lab.click();
    if (kind === 'milestone' || kind === 'complete') setTimeout(() => lab.click(), 110);
    if (kind === 'complete') setTimeout(() => lab.click(), 230);
  }
}

// ───────────── Ekran açık kalsın ─────────────
let wakeLock = null;
async function keepAwake() {
  if (!S.settings.wake || !('wakeLock' in navigator) || wakeLock || document.visibilityState !== 'visible') return;
  try { wakeLock = await navigator.wakeLock.request('screen'); wakeLock.addEventListener('release', () => { wakeLock = null; }); } catch {}
}
document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') { keepAwake(); render(); } });

// ───────────── Tespih malzemeleri ─────────────
const BEADS = [
  { name: 'Kehribar', base: hex('#D3922A'), light: hex('#E6AA45'), edge: hex('#955A10'), mL: hex('#E8B04C'), mD: hex('#A0702A'), spec: 0.85, silver: true, pat: 'DROPS' },
  { name: 'Oltu', base: hex('#1C1915'), light: hex('#524940'), edge: hex('#040303'), mL: hex('#332C25'), mD: hex('#0A0908'), spec: 1, silver: true },
  { name: 'Sedef', base: hex('#E6DED3'), light: hex('#FFFFFF'), edge: hex('#A0928A'), mL: hex('#F7E3EE'), mD: hex('#D9E5F3'), spec: 0.8, silver: false },
  { name: 'Kristal', base: hex('#DCE6FF', 0.4), light: hex('#FFFFFF', 0.933), edge: hex('#5E7AB4', 0.8), mL: hex('#FFFFFF', 0.533), mD: hex('#A9BDF0', 0.333), spec: 1, silver: false },
  { name: 'Akik', base: hex('#B5401F'), light: hex('#E0703F'), edge: hex('#6E1A0A'), mL: hex('#F09A6A'), mD: hex('#7A2210'), spec: 0.9, silver: true, pat: 'BANDS' },
  { name: 'Kaplan Gözü', base: hex('#8A5A1C'), light: hex('#D49A3E'), edge: hex('#3E2408'), mL: hex('#F0C060'), mD: hex('#4A2C0C'), spec: 0.9, silver: true, pat: 'CHATOYANT' },
  { name: 'Lapis', base: hex('#1F3A93'), light: hex('#3C62C8'), edge: hex('#0A1745'), mL: hex('#5577D8'), mD: hex('#0F1F5C'), spec: 0.8, silver: false, pat: 'FLECKS' },
  { name: 'Turkuaz', base: hex('#3FB3B0'), light: hex('#7FD8D2'), edge: hex('#1E6F70'), mL: hex('#A5E8E0'), mD: hex('#2A8A88'), spec: 0.5, silver: true, pat: 'VEINS' },
  { name: 'Ametist', base: hex('#7B3FA8', 0.88), light: hex('#C08BE8', 0.95), edge: hex('#3A1260'), mL: hex('#D9B2F5'), mD: hex('#4E1F7A'), spec: 1, silver: true },
  { name: 'Pembe Kuvars', base: hex('#E8A9B8'), light: hex('#F8D3DC'), edge: hex('#B06A80'), mL: hex('#FFE6EC'), mD: hex('#C88596'), spec: 0.7, silver: false },
  { name: 'Yeşim', base: hex('#3E8E5E'), light: hex('#74C08E'), edge: hex('#17462B'), mL: hex('#9AD8AE'), mD: hex('#245F3C'), spec: 0.7, silver: false },
  { name: 'Malakit', base: hex('#1E7A4E'), light: hex('#3FAF77'), edge: hex('#08331F'), mL: hex('#5CCB92'), mD: hex('#0B4A2C'), spec: 0.8, silver: false, pat: 'BANDS' },
  { name: 'Hematit', base: hex('#4A4D52'), light: hex('#9DA2AA'), edge: hex('#16171A'), mL: hex('#C0C5CC'), mD: hex('#222428'), spec: 1, silver: true },
  { name: 'Obsidyen', base: hex('#121318'), light: hex('#3A3D48'), edge: hex('#000000'), mL: hex('#5A6070'), mD: hex('#050506'), spec: 1, silver: true },
  { name: 'Oniks', base: hex('#2A2522'), light: hex('#6A625C'), edge: hex('#0A0807'), mL: hex('#D8D0C4'), mD: hex('#0A0807'), spec: 0.9, silver: true, pat: 'BANDS' },
  { name: 'Ay Taşı', base: hex('#DDE3EE', 0.92), light: hex('#FFFFFF'), edge: hex('#8E9BB5'), mL: hex('#B9D2FF'), mD: hex('#C8CFDC'), spec: 0.9, silver: false, pat: 'SHEEN' },
  { name: 'Mercan', base: hex('#D9483B'), light: hex('#F27C66'), edge: hex('#8A1E16'), mL: hex('#FF9E88'), mD: hex('#A0281E'), spec: 0.8, silver: false },
  { name: 'Sitrin', base: hex('#E8B020', 0.88), light: hex('#FFE27A'), edge: hex('#9A6406'), mL: hex('#FFF0A8'), mD: hex('#B07A10'), spec: 1, silver: false },
  { name: 'Yakut', base: hex('#9E1030', 0.92), light: hex('#E0405E'), edge: hex('#4A0414'), mL: hex('#FF7A90'), mD: hex('#5E0A1C'), spec: 1, silver: true },
  { name: 'Ateş Kehribar', base: hex('#F9A02A'), light: hex('#FFC84E'), edge: hex('#B05606'), mL: hex('#FFD27A'), mD: hex('#E07A10'), spec: 0.95, silver: false,
    tone: { core: hex('#FFC84E'), mid: hex('#F9A02A'), edge: hex('#E07A10'), deep: hex('#B05606'), glow: hex('#FFB347'), spec: 0.55, specW: 0.20, tr: 0.35 } },
  { name: 'Vişne Kehribar', base: hex('#5A0912'), light: hex('#8E1A22'), edge: hex('#220104'), mL: hex('#C42A30'), mD: hex('#320306'), spec: 1, silver: false, pat: 'GLOW',
    tone: { core: hex('#320306'), mid: hex('#5A0912'), edge: hex('#741019'), deep: hex('#220104'), glow: hex('#C42A30'), spec: 0.95, specW: 0.09, tr: 0.85 } },
];
const WARM_SHEEN = hex('#FFE7AE'), WARM_HALO = hex('#FFE9BE'), WARM_SPEC = hex('#FFF1D2');
const DROP_CORE = hex('#B06A0C'), DROP_EDGE = hex('#B8760F'), DROP_GLOW = hex('#F5BE52');
const DROP_PARTS = [[0, 0, 1, 0.62], [0.45, 0.18, 0.7, 0.5], [-0.35, 0.22, 0.6, 0.42]];
const BEAD_DROPS = [[[0.25, 0.30, 0.55]], [], [[-0.30, 0.28, 0.50]], [[0.10, 0.45, 0.55]], [], [[0.28, -0.05, 0.45]]];
const PHASE = [hex('#7FA7F0'), hex('#A68CF0'), hex('#EA96BE'), hex('#F2AE62'), hex('#F6DDA0')];
function phase(t) { const x = clamp(t, 0, 1) * 4, i = Math.min(Math.floor(x), 3); return mix(PHASE[i], PHASE[i + 1], x - i); }
const PULL_TRAIL = hex('#F6D58C');
// İp renkleri: [gövde, iplik, parlama] — kahve ipek, kırmızı ipek, siyah
const CORDS = [[hex('#5B3518'), hex('#A9773E'), hex('#E8C48A', 0.75)], [hex('#7E0E16'), hex('#C9303A'), hex('#F08080', 0.70)], [hex('#111114'), hex('#3A3A40'), hex('#9A9AA4', 0.60)]];
const OVAL = 0.72, OVAL_LEN = 1.0, BEAD_SCALE = 1.0;
const GLASS_TINT = hex('#BFD3FF'), GLASS_EDGE = hex('#D6E4FF');
const GOLD = hex('#D4AE62'), GOLD_L = hex('#F1D9A0'), GOLD_D = hex('#9C7838');
const CLUSTER = 3, PULL_GAP = 1.5, GAP_DEG = 18, DOME_H = 1.16;

function radial(ctx, x, y, R, stops) { const g = ctx.createRadialGradient(x, y, 0, x, y, Math.max(R, 0.01)); for (const [o, c] of stops) g.addColorStop(o, c); return g; }
function circle(ctx, x, y, r, fill) { ctx.beginPath(); ctx.arc(x, y, Math.max(r, 0), 0, 2 * PI); ctx.fillStyle = fill; ctx.fill(); }
function radialDot(ctx, x, y, R, col, a) { circle(ctx, x, y, R, radial(ctx, x, y, R, [[0, rgba(col, a)], [1, rgba(col, 0)]])); }

function drawAmberDrop(ctx, x, y, rr, angle) {
  for (const [dx, dy, sx, sy] of DROP_PARTS) {
    ctx.save(); ctx.translate(x + dx * rr, y + dy * rr); ctx.rotate(angle * PI / 180); ctx.scale(rr * sx, rr * sy);
    circle(ctx, 0, 0, 1, radial(ctx, 0, 0, 1, [[0, rgba(DROP_CORE, 0.32)], [0.6, rgba(DROP_EDGE, 0.20)], [1, rgba(DROP_EDGE, 0)]]));
    ctx.restore();
  }
  ctx.save(); ctx.translate(x + rr * 0.25, y + rr * 0.32); ctx.rotate(angle * PI / 180); ctx.scale(rr * 0.7, rr * 0.35);
  circle(ctx, 0, 0, 1, radial(ctx, 0, 0, 1, [[0, rgba(DROP_GLOW, 0.28)], [1, rgba(DROP_GLOW, 0)]]));
  ctx.restore();
}
// Doğal taş desenleri (tane dairesine kırpılmış)
function stonePattern(ctx, m, x, y, r, v) {
  const R = rng(v * 7919 + m.name.length * 131 + m.name.charCodeAt(0));
  switch (m.pat) {
    case 'DROPS': BEAD_DROPS[v % BEAD_DROPS.length].forEach(([dx, dy, dr], k) => drawAmberDrop(ctx, x + dx * r, y + dy * r, dr * r, 20 + k * 55 + v * 30)); break;
    case 'FLECKS':
      for (let i = 0; i < 2; i++) radialDot(ctx, x + (R() - 0.5) * r * 1.4, y + (R() - 0.5) * r * 1.4, r * 0.35, hex('#DCE4F5'), 0.16);
      for (let i = 0; i < 9; i++) circle(ctx, x + (R() - 0.5) * r * 1.7, y + (R() - 0.5) * r * 1.7, r * (0.03 + R() * 0.05), `rgba(233,199,102,${0.55 + R() * 0.4})`);
      break;
    case 'CHATOYANT': {
      ctx.translate(x, y); ctx.rotate((-35 + v * 17) * PI / 180);
      ctx.lineWidth = r * 0.05; ctx.strokeStyle = rgba(m.mD, 0.18);
      for (let i = -7; i <= 7; i++) { ctx.beginPath(); ctx.moveTo(-r, i * r * 0.13); ctx.lineTo(r, i * r * 0.13); ctx.stroke(); }
      const by = (R() - 0.5) * r * 0.5, g = ctx.createLinearGradient(0, by - r * 0.32, 0, by + r * 0.32);
      g.addColorStop(0, rgba(m.mL, 0)); g.addColorStop(0.5, rgba(m.mL, 0.55)); g.addColorStop(1, rgba(m.mL, 0));
      ctx.fillStyle = g; ctx.fillRect(-r, by - r * 0.32, 2 * r, r * 0.64); break;
    }
    case 'BANDS': {
      const cx = x + (R() - 0.5) * r * 1.6, cy = y + (R() - 0.5) * r * 1.6, onyx = m.name === 'Oniks';
      let rad = r * (0.15 + R() * 0.1), i = 0;
      while (rad < r * 2.4) {
        const a = onyx ? (i % 2 === 0 ? 0.55 : 0) : 0.30;
        if (a > 0) { ctx.strokeStyle = rgba(i % 2 === 0 ? m.mL : m.mD, a); ctx.lineWidth = r * (onyx ? 0.035 : 0.07 + R() * 0.05); ctx.beginPath(); ctx.arc(cx, cy, rad, 0, 2 * PI); ctx.stroke(); }
        rad += r * (0.12 + R() * 0.12); i++;
      }
      break;
    }
    case 'VEINS':
      ctx.strokeStyle = 'rgba(90,70,48,.45)'; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
      for (let k = 0; k < 3; k++) {
        let a = x + (R() - 0.5) * r * 2, b = y - r; ctx.beginPath(); ctx.moveTo(a, b);
        while (b < y + r) { a += (R() - 0.5) * r * 0.5; b += r * (0.15 + R() * 0.2); ctx.lineTo(a, b); }
        ctx.lineWidth = r * (0.03 + R() * 0.03); ctx.stroke();
      }
      break;
    case 'GLOW':
      radialDot(ctx, x + r * 0.22, y + r * 0.34, r * 0.75, m.mL, 0.55);
      radialDot(ctx, x - r * 0.15, y - r * 0.2, r * 0.6, m.mD, 0.35); break;
    case 'SHEEN':
      ctx.translate(x + r * 0.15, y - r * 0.05); ctx.scale(1, 0.8 / 1.4);
      radialDot(ctx, 0, 0, r * 0.7, hex('#8FB8FF'), 0.55); break;
  }
}
// Malzemenin oval tonu: tanımlıysa o, değilse kendi renklerinden türetilir
function toneOf(m) {
  return m.tone || (m.tone = { core: m.light, mid: m.base, edge: m.edge, deep: mix(m.edge, [0, 0, 0, 1], 0.35), glow: m.mL, spec: 0.55 * m.spec, specW: 0.16, tr: m.base[3] < 1 ? 0.8 : 0.4 });
}
// Gerçek oval (arpa) tane: uzun ekseni ip boyunca; ışık sol üstten, yansıma tanenin boyunca, uçlar hafif koyu
function ovalBead(ctx, m, cx, cy, a, ang, v, alpha, tint) {
  const t = toneOf(m), b = a * OVAL, lx = -0.55, ly = -0.83, ca = Math.cos(-ang), sa = Math.sin(-ang);
  const llx = lx * ca - ly * sa, lly = lx * sa + ly * ca;
  ctx.save(); ctx.globalAlpha = alpha; ctx.translate(cx, cy); ctx.rotate(ang);
  ctx.beginPath(); ctx.ellipse(0, 0, a, b, 0, 0, 2 * PI); ctx.clip();
  ctx.save(); ctx.scale(1, OVAL);
  let g = ctx.createRadialGradient(llx * a * 0.35, lly * a * 0.35, 0, 0, 0, a * 1.15);
  g.addColorStop(0, rgba(t.core)); g.addColorStop(0.45, rgba(t.mid)); g.addColorStop(0.85, rgba(t.edge)); g.addColorStop(1, rgba(t.deep));
  ctx.fillStyle = g; ctx.fillRect(-a * 1.3, -a * 1.3, a * 2.6, a * 2.6);
  radialDot(ctx, -llx * a * 0.55, -lly * a * 0.55, a * 0.75, t.glow, 0.55 * t.tr);
  circle(ctx, 0, 0, a, radial(ctx, 0, 0, a, [[0.7, 'rgba(0,0,0,0)'], [1, 'rgba(0,0,0,.28)']]));
  ctx.restore();
  for (const sd of [-1, 1]) radialDot(ctx, sd * a, 0, b * 0.9, [0, 0, 0, 1], 0.30);
  if (m.pat && m.pat !== 'GLOW') { ctx.save(); stonePattern(ctx, m, 0, 0, a, v); ctx.restore(); }
  const side = lly < 0 ? -1 : 1, hy = side * b * 0.48, hx = llx * a * 0.12;
  ctx.save(); ctx.translate(hx, hy); ctx.scale(1, t.specW / 0.5);
  circle(ctx, 0, 0, a * 0.62, radial(ctx, 0, 0, a * 0.62, [[0, `rgba(255,248,235,${t.spec})`], [0.55, `rgba(255,248,235,${t.spec * 0.35})`], [1, 'rgba(255,248,235,0)']]));
  ctx.restore();
  ctx.fillStyle = `rgba(255,255,255,${Math.min(1, t.spec + 0.05)})`; ctx.beginPath(); ctx.ellipse(hx + (llx < 0 ? -1 : 1) * a * 0.38, hy * 0.9, a * 0.07, b * 0.05, 0, 0, 2 * PI); ctx.fill();
  ctx.strokeStyle = rgba(t.glow, 0.35 * t.tr + 0.1); ctx.lineWidth = b * 0.06;
  ctx.beginPath(); ctx.ellipse(0, 0, a * 0.9, b * 0.84, 0, side > 0 ? PI * 1.15 : PI * 0.15, side > 0 ? PI * 1.85 : PI * 0.85); ctx.stroke();
  if (tint) { ctx.fillStyle = tint; ctx.fillRect(-a, -b, 2 * a, 2 * b); }
  ctx.restore();
}

function drawBead(ctx, m, x, y, r, v) {
  circle(ctx, x, y, r, radial(ctx, x - r * 0.30, y - r * 0.36, r * 1.45, [[0, rgba(m.light)], [0.55, rgba(m.base)], [1, rgba(m.edge)]]));
  const h1 = ((v * 73 + 17) % 97) / 97, h2 = ((v * 41 + 59) % 89) / 89;
  radialDot(ctx, x + (h1 - 0.5) * r * 0.8, y + (h2 - 0.5) * r * 0.7, r * (0.35 + 0.2 * h2), m.mL, 0.55);
  radialDot(ctx, x + (h2 - 0.5) * r * 0.7, y + (0.5 - h1) * r * 0.8, r * (0.30 + 0.2 * h1), m.mD, 0.28);
  if (m.pat) { ctx.save(); ctx.beginPath(); ctx.arc(x, y, r, 0, 2 * PI); ctx.clip(); stonePattern(ctx, m, x, y, r, v); ctx.restore(); }
  circle(ctx, x, y, r, radial(ctx, x, y, r, [[0.70, rgba(m.edge, 0)], [1, rgba(m.edge, 0.55)]]));
  radialDot(ctx, x + r * 0.12, y + r * 0.52, r * 0.45, m.light, 0.10);
  radialDot(ctx, x - r * 0.28, y - r * 0.34, r * 0.55, WARM_SHEEN, 0.10);
  radialDot(ctx, x - r * 0.18, y - r * 0.22, r * 0.12, WARM_HALO, 0.30 * m.spec);
  circle(ctx, x - r * 0.18, y - r * 0.22, r * 0.065, rgba(WARM_SPEC, 0.85 * m.spec));
}
// Tane görüntüleri önbelleği
const sprites = { key: '', beads: [], shadow: null, size: 0 };
function getSprites(m, r, dpr) {
  const size = Math.ceil(r * 2 + 4), key = m.name + size + '@' + dpr;
  if (sprites.key === key) return sprites;
  const mk = (fn) => { const c = document.createElement('canvas'); c.width = c.height = Math.ceil(size * dpr); const x = c.getContext('2d'); x.scale(dpr, dpr); fn(x); return c; };
  sprites.beads = [0, 1, 2, 3, 4, 5].map((v) => mk((x) => drawBead(x, m, size / 2, size / 2, r, v)));
  sprites.shadow = mk((x) => radialDot(x, size / 2, size / 2, r * 1.3, [0, 0, 0, 1], 0.38));
  sprites.size = size; sprites.key = key;
  return sprites;
}
function metal(ctx, silver, x0, y0, x1, y1) {
  const g = ctx.createLinearGradient(x0, y0, x1, y1);
  (silver ? ['#F5F5F7', '#9C9CA4', '#E4E4E8', '#5E5E66'] : ['#F7E4AA', '#B88A3A', '#EDCD82', '#6E5018']).forEach((c, i) => g.addColorStop(i / 3, c));
  return g;
}

// ───────────── Kubbe çizimi ─────────────
const dome = $('#dome'), dctx = dome.getContext('2d');
let W = 300, DPR = 1;
const anim = { pos: 0, from: 0, to: 0, t0: 0, mode: 'snap', prog: 0, progFrom: 0, progTo: 0, progT0: 0, pulse: 1, pulseT0: 0, burst: 1, burstT0: 0, press: 0, pressTarget: 0 };
const easeIn = (x) => x * x * (2.2 - 1.2 * x) * 0.5 + x * x * 0.5;     // yerçekimi gibi hızlanan
const easeOut = (x) => 1 - (1 - x) * (1 - x);
function posAt(now) {
  const e = now - anim.t0, a = anim.from, t = anim.to;
  if (anim.mode === 'snap') return t;
  if (anim.mode === 'back') { const x = clamp(e / 180, 0, 1); return lerp(a, t, easeOut(x)); }
  // çekiş: düşüş (ivmeli) → hafif sekme → yerleşme
  if (e < PULL_MS) return lerp(a, t, easeIn(e / PULL_MS));
  if (e < PULL_MS + 55) return lerp(t, t + 0.07, easeOut((e - PULL_MS) / 55));
  if (e < PULL_MS + 110) return lerp(t + 0.07, t - 0.015, (e - PULL_MS - 55) / 55);
  if (e < PULL_MS + 150) return lerp(t - 0.015, t, (e - PULL_MS - 110) / 40);
  return t;
}
function animating(now) {
  return (anim.mode !== 'snap' && now - anim.t0 < PULL_MS + 160) || now - anim.progT0 < 520 || anim.pulse < 1 || anim.burst < 1 || Math.abs(anim.press - anim.pressTarget) > 0.01;
}

function drawDome(now) {
  const ctx = dctx, k = W / 380;
  ctx.setTransform(DPR, 0, 0, DPR, 0, 0);
  ctx.clearRect(0, 0, W, W * DOME_H);
  const m = BEADS[S.settings.bead] || BEADS[0];
  const pos = anim.pos, prog = anim.prog, pulse = anim.pulse, burst = anim.burst, press = anim.press;
  const seg = (360 - GAP_DEG) / (33 + PULL_GAP), f = PI * seg / 360;
  const BS = BEAD_SCALE, rB = (W / 2 - 3 * k) / (1 + f * BS), r = rB * f * BS, cx = W / 2, cy = W / 2;
  const rDisc = rB - r - 16 * k, rArc = rDisc - 13 * k, stroke = 3.4 * k;
  const ph = phase(prog);
  const polar = (rad, deg) => { const a = deg * PI / 180; return [cx + rad * Math.cos(a), cy + rad * Math.sin(a)]; };

  // hâle
  radialDot(ctx, cx, cy, rB * 1.22, ph, 0.30);
  // kubbenin yere düşen gölgesi
  const shy = cy + rDisc * 0.10;
  circle(ctx, cx, shy, rDisc * 1.14, radial(ctx, cx, shy, rDisc * 1.14, [[0.80, 'rgba(0,0,0,0)'], [0.92, 'rgba(0,0,0,.35)'], [1, 'rgba(0,0,0,0)']]));
  // sade koyu zemin; tesbihin rengi kenarlardan içeri hafifçe yansır
  circle(ctx, cx, cy, rDisc, radial(ctx, cx, cy, rDisc, [[0, '#0E1A38'], [1, '#081127']]));
  circle(ctx, cx, cy, rDisc, radial(ctx, cx, cy, rDisc, [[0.55, rgba(m.base, 0)], [1, rgba(m.light, 0.22)]]));
  ctx.lineWidth = 1 * k; ctx.strokeStyle = rgba(m.light, 0.45); ctx.beginPath(); ctx.arc(cx, cy, rDisc, 0, 2 * PI); ctx.stroke();
  if (press > 0) circle(ctx, cx, cy, rDisc, `rgba(0,0,0,${0.12 * press})`);
  if (pulse < 1) circle(ctx, cx, cy, rDisc, radial(ctx, cx, cy, rDisc, [[0, `rgba(255,255,255,${(1 - pulse) * 0.20})`], [1, 'rgba(255,255,255,0)']]));

  // ilerleme yayı
  ctx.lineWidth = stroke; ctx.strokeStyle = 'rgba(255,255,255,.07)'; ctx.beginPath(); ctx.arc(cx, cy, rArc, 0, 2 * PI); ctx.stroke();
  const sweep = 360 * prog;
  if (sweep > 0.4) {
    const a0 = -PI / 2, a1 = a0 + sweep * PI / 180;
    ctx.save(); ctx.lineCap = 'round';
    ctx.lineWidth = stroke * 2.4; ctx.strokeStyle = 'rgba(255,244,214,.18)'; ctx.beginPath(); ctx.arc(cx, cy, rArc, a0, a1); ctx.stroke();
    ctx.lineWidth = stroke; ctx.strokeStyle = 'rgba(255,241,204,.92)'; ctx.beginPath(); ctx.arc(cx, cy, rArc, a0, a1); ctx.stroke();
    ctx.restore();
    const [hx, hy] = polar(rArc, -90 + sweep);
    circle(ctx, hx, hy, stroke * 2.4, 'rgba(255,255,255,.35)'); circle(ctx, hx, hy, stroke * 0.85, '#fff');
  }

  // tespih ipi: iki kat bükülmüş ipek
  {
    const cw = 2.6 * k;
    ctx.lineWidth = cw * 1.1; ctx.strokeStyle = 'rgba(0,0,0,.35)'; ctx.beginPath(); ctx.arc(cx, cy + cw * 0.35, rB, 0, 2 * PI); ctx.stroke();
    const cc = CORDS[S.settings.cord] || CORDS[0];
    ctx.lineWidth = cw; ctx.strokeStyle = rgba(cc[0]); ctx.beginPath(); ctx.arc(cx, cy, rB, 0, 2 * PI); ctx.stroke();
    const n = Math.floor(2 * PI * rB / (cw * 1.05)), hl = cw * 0.52;
    ctx.lineCap = 'round';
    const p1 = new Path2D(), p2 = new Path2D();
    for (let i = 0; i < n; i++) {
      const ang = i * 360 / n, [qx, qy] = polar(rB, ang), d = (ang + 132) * PI / 180, dx = Math.cos(d) * hl, dy = Math.sin(d) * hl;
      p1.moveTo(qx - dx, qy - dy); p1.lineTo(qx + dx, qy + dy);
      p2.moveTo(qx - dx * 0.35, qy - dy * 0.35); p2.lineTo(qx + dx * 0.15, qy + dy * 0.15);
    }
    ctx.lineWidth = cw * 0.40; ctx.strokeStyle = rgba(cc[1]); ctx.stroke(p1);
    ctx.lineWidth = cw * 0.16; ctx.strokeStyle = rgba(cc[2]); ctx.stroke(p2);
  }

  // taneler: gerçek tespih mekaniği (1'de bekler, düşer, 2'de birikir, imamenin arkasından geçer)
  const gapSlots = GAP_DEG / seg, ltot = 33 + PULL_GAP + gapSlots;
  const slotOf = (j) => j < CLUSTER - 1 ? j + 0.5 : j < CLUSTER ? (CLUSTER - 0.5) + (j - (CLUSTER - 1)) * (PULL_GAP + 1) : j < 32 ? j + PULL_GAP + 0.5 : 32 + PULL_GAP + 0.5 + (j - 32) * (gapSlots + 1);
  const angleOf = (v) => 90 - GAP_DEG / 2 - v * seg;
  const beads = [];
  for (let id = 0; id < 33; id++) {
    const j = (((id - pos) % 33) + 33) % 33, v = slotOf(j), vm = ((v % ltot) + ltot) % ltot;
    let a = 1;
    if (vm > 33 + PULL_GAP) a = 1 - clamp(Math.min(vm - (33 + PULL_GAP), ltot - vm) / 0.8, 0, 1);
    if (a <= 0.01) continue;
    const [x, y] = polar(rB, angleOf(v));
    beads.push({ id, j, v, x, y, a });
  }
  const sp = getSprites(m, r, DPR), half = sp.size / 2;
  for (const b of beads) {
    if (S.settings.shape === 1) {
      const a = r / BEAD_SCALE * OVAL_LEN;
      ctx.save(); ctx.translate(b.x, b.y + a * 0.45); ctx.rotate(angleOf(b.v) * PI / 180 + PI / 2); ctx.scale(1, OVAL);
      radialDot(ctx, 0, 0, a * 1.25, [0, 0, 0, 1], 0.40 * b.a); ctx.restore();
    } else { ctx.globalAlpha = b.a; ctx.drawImage(sp.shadow, b.x - half, b.y - half + r * 0.38, sp.size, sp.size); }
  }
  ctx.globalAlpha = 1;
  const litUpTo = prog * 33;
  for (const b of beads) {
    const recent = ((((CLUSTER - 1) - b.j) % 33) + 33) % 33, lit = recent + 0.5 <= litUpTo + 0.01;
    if (lit) radialDot(ctx, b.x, b.y, r * 1.7, phase((recent + 0.5) / 33), 0.50 * b.a);
    if (b.j > CLUSTER - 1 && b.j < CLUSTER && !reduceMotion) {
      const s = CLUSTER - b.j;
      for (let q = 1; q <= 3; q++) { const [tx, ty] = polar(rB, angleOf(b.v + q * 0.32 * (0.4 + s))); circle(ctx, tx, ty, r * (1 - 0.1 * q), rgba(PULL_TRAIL, (0.24 - 0.06 * q) * s)); }
    }
    if (b.j <= CLUSTER - 1 && b.j > CLUSTER - 1.25 && !reduceMotion) radialDot(ctx, b.x, b.y, r * 1.8, PULL_TRAIL, 0.45 * (1 - (CLUSTER - 1 - b.j) / 0.25));
  }
  for (const b of [...beads].sort((p, q) => p.y - q.y)) {
    const recent = ((((CLUSTER - 1) - b.j) % 33) + 33) % 33, lit = recent + 0.5 <= litUpTo + 0.01;
    const img = sp.beads[(b.id * 5) % 6], tint = lit ? rgba(phase((recent + 0.5) / 33), 0.10 * b.a) : null;
    if (S.settings.shape === 1) {
      ovalBead(ctx, m, b.x, b.y, r / BEAD_SCALE * OVAL_LEN, angleOf(b.v) * PI / 180 + PI / 2, b.id % 6, b.a, tint);
    } else {
      ctx.globalAlpha = b.a; ctx.drawImage(img, b.x - half, b.y - half, sp.size, sp.size); ctx.globalAlpha = 1;
      if (tint) circle(ctx, b.x, b.y, r, tint);
    }
  }

  const ir = Math.min(r, 13 * k), x0 = cx;
  const sway = reduceMotion || pulse >= 1 ? 0 : Math.sin(pulse * 2 * PI * 1.5) * (1 - pulse) * 9;
  if (S.settings.imame === 1) drawClassicImame(ctx, m, x0, cy + rB - ir * 0.15, Math.min(r / BEAD_SCALE * 1.05, 12 * k), k, sway);
  else {
  // imame: telkari başlık + uzun tane + uç + halka + püskül
  let y = cy + rB - ir * 0.55;
  const capH = ir * 0.95;
  ctx.beginPath(); ctx.moveTo(x0 - ir * 0.62, y); ctx.lineTo(x0 + ir * 0.62, y); ctx.lineTo(x0 + ir * 0.30, y + capH); ctx.lineTo(x0 - ir * 0.30, y + capH); ctx.closePath();
  ctx.fillStyle = metal(ctx, m.silver, x0 - ir, y, x0 + ir, y + capH); ctx.fill();
  ctx.lineWidth = 0.6 * k; ctx.strokeStyle = 'rgba(0,0,0,.35)';
  for (let j = -2; j <= 2; j++) { ctx.beginPath(); ctx.moveTo(x0 + j * ir * 0.22, y + capH * 0.12); ctx.lineTo(x0 + j * ir * 0.11, y + capH * 0.9); ctx.stroke(); }
  y += capH;
  const bodyH = ir * 2.3, bodyW = ir * 1.35;
  ctx.save(); ctx.translate(x0, y + bodyH / 2); ctx.scale(bodyW / bodyH, 1);
  circle(ctx, 0, 0, bodyH / 2, radial(ctx, -bodyH * 0.18, -bodyH * 0.20, bodyH * 0.75, [[0, rgba(m.light)], [0.55, rgba(m.base)], [1, rgba(m.edge)]]));
  ctx.restore();
  circle(ctx, x0 - bodyW * 0.2, y + bodyH * 0.26, ir * 0.09, rgba(WARM_SPEC, 0.9 * m.spec));
  y += bodyH - ir * 0.15;
  ctx.beginPath(); ctx.moveTo(x0 - ir * 0.42, y); ctx.lineTo(x0 + ir * 0.42, y); ctx.lineTo(x0 + ir * 0.14, y + ir * 0.8); ctx.lineTo(x0 - ir * 0.14, y + ir * 0.8); ctx.closePath();
  ctx.fillStyle = metal(ctx, m.silver, x0 - ir, y, x0 + ir, y + ir); ctx.fill();
  y += ir * 0.8;
  ctx.lineWidth = 1.4 * k; ctx.strokeStyle = metal(ctx, m.silver, x0 - ir, y, x0 + ir, y + ir); ctx.beginPath(); ctx.arc(x0, y + ir * 0.22, ir * 0.24, 0, 2 * PI); ctx.stroke();
  y += ir * 0.46;
  const tassel = 24 * k, silk = m.silver ? '#D9A441' : '#D4AE62';
  ctx.save(); ctx.translate(x0, y); ctx.rotate(sway * PI / 180);
  const tg = ctx.createLinearGradient(0, 0, 0, tassel); tg.addColorStop(0, silk); tg.addColorStop(1, silk + '8C');
  ctx.strokeStyle = tg; ctx.lineWidth = 1.1 * k; ctx.lineCap = 'round';
  for (let j = 0; j < 15; j++) { const t = (j - 7) / 7; ctx.beginPath(); ctx.moveTo(t * ir * 0.18, 0); ctx.lineTo(t * ir * 0.9, tassel * (0.92 + 0.08 * (1 - Math.abs(t)))); ctx.stroke(); }
  circle(ctx, 0, ir * 0.05, ir * 0.2, metal(ctx, m.silver, -ir, 0, ir, ir));
  ctx.restore();
  }

  // hedef kutlaması
  if (burst < 1) {
    const a = 1 - burst;
    ctx.lineWidth = stroke * (1 + 2.5 * burst); ctx.strokeStyle = rgba(ph, a * 0.7); ctx.beginPath(); ctx.arc(cx, cy, rArc, 0, 2 * PI); ctx.stroke();
    if (!reduceMotion) {
      const e = 1 - (1 - burst) * (1 - burst);
      for (let j = 0; j < 28; j++) {
        const ang = j * (360 / 28) + (j % 3) * 4, dist = rDisc * 0.85 + (rB * 1.12 - rDisc * 0.85) * e * (0.7 + 0.3 * ((j * 7) % 5) / 4);
        const [qx, qy] = polar(dist, ang), rr = (1.6 + (j % 4) * 0.6) * k * a * 1.6, col = rgba(phase((j % 5) / 4), a);
        if (j % 3 === 0) { const L = rr * 3.2; ctx.lineWidth = rr * 0.55; ctx.strokeStyle = col; ctx.lineCap = 'round'; ctx.beginPath(); ctx.moveTo(qx - L, qy); ctx.lineTo(qx + L, qy); ctx.moveTo(qx, qy - L); ctx.lineTo(qx, qy + L); ctx.stroke(); }
        else circle(ctx, qx, qy, rr, col);
      }
    }
  }
}

// Klasik imame (fotoğraftaki gibi): oval bilezik, aşağıda şişkinleşen uzun gövde, alt bilezik, durak ve taneli kamçı
function drawClassicImame(ctx, m, x0, y, ir, k, sway) {
  const t = toneOf(m);
  ovalBead(ctx, m, x0, y + ir * 0.15, ir * 0.55, 0, 0, 1, null);
  y += ir * 0.42;
  const L = ir * 2.8;
  ctx.beginPath(); ctx.moveTo(x0 - ir * 0.22, y);
  ctx.bezierCurveTo(x0 - ir * 0.20, y + L * 0.30, x0 - ir * 0.30, y + L * 0.55, x0 - ir * 0.48, y + L * 0.78);
  ctx.bezierCurveTo(x0 - ir * 0.56, y + L * 0.90, x0 - ir * 0.36, y + L, x0 - ir * 0.20, y + L);
  ctx.lineTo(x0 + ir * 0.20, y + L);
  ctx.bezierCurveTo(x0 + ir * 0.36, y + L, x0 + ir * 0.56, y + L * 0.90, x0 + ir * 0.48, y + L * 0.78);
  ctx.bezierCurveTo(x0 + ir * 0.30, y + L * 0.55, x0 + ir * 0.20, y + L * 0.30, x0 + ir * 0.22, y); ctx.closePath();
  const g = ctx.createLinearGradient(x0 - ir * 0.55, 0, x0 + ir * 0.55, 0);
  g.addColorStop(0, rgba(t.deep)); g.addColorStop(0.25, rgba(t.core)); g.addColorStop(0.5, rgba(t.mid)); g.addColorStop(0.8, rgba(t.edge)); g.addColorStop(1, rgba(t.deep));
  ctx.fillStyle = g; ctx.fill();
  ctx.strokeStyle = `rgba(255,250,240,${t.spec * 0.8})`; ctx.lineWidth = ir * 0.06; ctx.lineCap = 'round';
  ctx.beginPath(); ctx.moveTo(x0 - ir * 0.12, y + L * 0.10); ctx.lineTo(x0 - ir * 0.14, y + L * 0.50); ctx.stroke();
  ctx.beginPath(); ctx.moveTo(x0 - ir * 0.30, y + L * 0.72); ctx.lineTo(x0 - ir * 0.36, y + L * 0.88); ctx.stroke();
  y += L;
  ovalBead(ctx, m, x0, y + ir * 0.12, ir * 0.42, 0, 0, 1, null);
  ovalBead(ctx, m, x0, y + ir * 0.45, ir * 0.24, PI / 2, 0, 1, null);
  y += ir * 0.70;
  const tail = ir * 2.4, cc = CORDS[S.settings.cord] || CORDS[0];
  ctx.save(); ctx.translate(x0, y); ctx.rotate(sway * PI / 180);
  ctx.strokeStyle = rgba(cc[0]); ctx.lineWidth = 1.5 * k; ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(0, tail); ctx.stroke();
  for (const f of [0.30, 0.68, 0.98]) ovalBead(ctx, m, 0, tail * f, ir * 0.26, PI / 2, 0, 1, null);
  ctx.restore();
}

let raf = 0;
function frame(now) {
  raf = 0;
  anim.pos = posAt(now);
  const pe = clamp((now - anim.progT0) / 480, 0, 1);
  anim.prog = lerp(anim.progFrom, anim.progTo, reduceMotion ? 1 : 1 - Math.pow(1 - pe, 3));
  anim.pulse = reduceMotion ? 1 : clamp((now - anim.pulseT0) / 480, 0, 1);
  anim.burst = clamp((now - anim.burstT0) / 1700, 0, 1);
  anim.press += (anim.pressTarget - anim.press) * 0.45;
  drawDome(now);
  if (animating(now)) kick();
}
function kick() { if (!raf) raf = requestAnimationFrame(frame); }

function layout() {
  const wrap = $('#domeWrap').getBoundingClientRect();
  const side = Math.floor(Math.min(wrap.width + 40, wrap.height / DOME_H, 460));
  W = Math.max(side, 120); DPR = Math.min(window.devicePixelRatio || 1, 3);
  const box = $('#domeBox');
  box.style.width = W + 'px'; box.style.height = W * DOME_H + 'px';
  dome.width = Math.round(W * DPR); dome.height = Math.round(W * DOME_H * DPR);
  const seg = (360 - GAP_DEG) / (33 + PULL_GAP), f = PI * seg / 360, k = W / 380;
  const rB = (W / 2 - 3 * k) / (1 + f * BEAD_SCALE), rDisc = rB - rB * f * BEAD_SCALE - 16 * k;
  const hit = $('#hit'); hit.style.left = W / 2 - rB + 'px'; hit.style.top = W / 2 - rB + 'px'; hit.style.width = hit.style.height = rB * 2 + 'px';
  placeNum();
  $('#nTarget').style.fontSize = Math.max(11, W * 0.034) + 'px';
  $('#nLap').style.fontSize = Math.max(11, W * 0.032) + 'px';
  drawDome(performance.now());
}
let lastShown = 0;
// Rakam tam kubbenin ortasında; hedef ve tur altında
function placeNum() {
  const fs = W * (lastShown >= 1000 ? 0.18 : 0.25);
  $('#nVal').style.fontSize = fs + 'px';
  $('#num').style.top = (W / 2 - fs * 0.52) + 'px';
}

// ───────────── Arayüz ─────────────
function progressOf(d) { return d.count <= 0 ? 0 : d.vird ? clamp(d.count / d.target, 0, 1) : (((d.count - 1) % d.target) + 1) / d.target; }
function setProg(p, now) { anim.progFrom = anim.prog; anim.progTo = p; anim.progT0 = now; }
function renderCard(dir) {
  const d = disp(), c = $('#card');
  let h = '';
  if (d.vird) h += `<div class="step">${esc(d.vird.name.toUpperCase())} · ${d.step + 1}/${d.vird.steps.length}</div>`;
  if (d.ar) h += `<div class="ar">${esc(d.ar)}</div>`;
  h += `<div class="ok">${esc(d.ok)}</div>`;
  if (d.an) h += `<div class="an">${esc(d.an)}</div>`;
  $('#cardBody').innerHTML = h;
  if (dir) { c.classList.remove('anim-l', 'anim-r'); void c.offsetWidth; c.classList.add(dir < 0 ? 'anim-l' : 'anim-r'); }
  // noktalar
  const dots = $('#dots');
  if (d.vird) { dots.innerHTML = d.vird.steps.map((_, i) => `<i class="${i === d.step ? 'on' : ''}"></i>`).join(''); return; }
  const n = S.list.length, idx = S.list.findIndex((z) => z.id === S.selId), MAX = 7;
  const start = clamp(idx - 3, 0, Math.max(0, n - MAX));
  let s = ''; for (let i = start; i < Math.min(n, start + MAX); i++) s += `<i class="${i === idx ? 'on' : ''}"></i>`;
  dots.innerHTML = n > 1 ? s : '';
}
function renderNum(bump) {
  const d = disp(), nv = $('#nVal');
  const shown = d.vird ? d.count : (S.settings.stopAtTarget ? Math.min(d.count, d.target) : d.count);
  const big = (shown >= 1000) !== (lastShown >= 1000);
  lastShown = shown; if (big) placeNum();
  nv.textContent = fmt(shown);
  if (bump && !reduceMotion) { nv.classList.remove('bump'); void nv.offsetWidth; nv.classList.add('bump'); }
  $('#nTarget').textContent = `HEDEF ${fmt(d.target)}`;
  const lap = d.vird ? 0 : d.count <= 0 ? 1 : Math.floor((d.count - 1) / d.target) + 1;
  const lapEl = $('#nLap'); lapEl.style.display = d.vird ? 'none' : ''; lapEl.textContent = `${lap}. TUR`;
  $('#targetLbl').textContent = d.vird ? `Adım ${d.step + 1}/${d.vird.steps.length}` : `Hedef ${fmt(d.target)}`;
}
function renderStats() {
  $('#sToday').textContent = fmt(S.history[dayKey()] || 0);
  const s = streak(); $('#sStreak').textContent = `${s} gün`;
  $('#sTotal').textContent = fmt(S.total);
}
function render(dir) {
  const d = disp();
  renderCard(dir); renderNum(false); renderStats();
  const now = performance.now();
  if (anim.key !== d.key) { anim.key = d.key; anim.mode = 'snap'; anim.to = anim.pos = d.count; anim.prog = anim.progFrom = anim.progTo = progressOf(d); anim.progT0 = 0; }
  kick();
}

// ───────────── Sayma kuralları ─────────────
let lastTap = 0;
function bumpDay(n) { const k = dayKey(); S.history[k] = Math.max(0, (S.history[k] || 0) + n); S.total = Math.max(0, S.total + n); }
function tap() {
  const now = performance.now();
  if (now - lastTap < 70) return; lastTap = now;
  Audio.unlock(); keepAwake();
  const d = disp(), ms = S.settings.milestone;
  let n, completed, milestone;
  if (d.vird) {
    if (d.count >= d.target) return;
    n = d.count + 1; completed = n >= d.target; milestone = ms > 0 && n % ms === 0 && n < d.target;
    S.session.count = n;
  } else {
    const z = sel();
    if (S.settings.stopAtTarget && z.c >= z.t) { haptic('error'); toast('Hedefe ulaşıldı'); return; }
    n = z.c + 1; completed = n % z.t === 0; milestone = ms > 0 && n % ms === 0 && n % z.t !== 0;
    z.c = n; if (completed) S.laps[z.id] = (S.laps[z.id] || 0) + 1;
  }
  bumpDay(1);
  fbPull(milestone, completed);
  haptic(completed ? 'complete' : milestone ? 'milestone' : 'tap', reduceMotion ? 0 : PULL_MS - 20);
  // tane animasyonu
  anim.from = anim.pos; anim.to = n; anim.t0 = now; anim.mode = reduceMotion ? 'snap' : 'pull';
  setProg(progressOf(disp()), now);
  anim.pulseT0 = now;
  if (completed) anim.burstT0 = now + PULL_MS;
  renderNum(true); renderStats(); save(); kick();
  if (completed && d.vird) {
    const s0 = S.session;
    setTimeout(() => {
      if (S.session !== s0 || S.session.count < d.target) return;
      if (s0.step + 1 < d.vird.steps.length) { S.session = { virdId: s0.virdId, step: s0.step + 1, count: 0 }; render(1); }
      else { S.session = null; render(1); toast(`${d.vird.name} tamamlandı`); }
      save();
    }, 900);
  }
}
function undo() {
  const d = disp(), now = performance.now();
  if (d.vird) { if (S.session.count <= 0) return; S.session.count--; }
  else {
    const z = sel(), vis = S.settings.stopAtTarget ? Math.min(z.c, z.t) : z.c;
    if (vis <= 0) return;
    if (z.c > 0 && z.c % z.t === 0 && S.laps[z.id]) S.laps[z.id]--;
    z.c = vis - 1;
  }
  bumpDay(-1);
  const nd = disp();
  anim.from = anim.pos; anim.to = nd.count; anim.t0 = now; anim.mode = reduceMotion ? 'snap' : 'back';
  setProg(progressOf(nd), now);
  renderNum(true); renderStats(); save(); kick();
  haptic('tap');
}
function reset() {
  const d = disp(); if (d.count === 0) return;
  if (d.vird) { const s = { ...S.session }; S.session.count = 0; render(); save(); toast('Adım sıfırlandı', 'Geri al', () => { S.session = s; anim.key = ''; render(); save(); }); return; }
  const z = sel(), before = z.c; z.c = 0; anim.key = ''; render(); save();
  haptic('milestone');
  toast('Sayaç sıfırlandı', 'Geri al', () => { z.c += before; anim.key = ''; render(); save(); });
}
function select(id, dir) { if (S.session) S.session = null; S.selId = id; anim.key = ''; render(dir); save(); }
function step(dir) {
  if (S.session) return;
  const i = S.list.findIndex((z) => z.id === S.selId), n = S.list.length;
  if (n < 2) return;
  select(S.list[(i + dir + n) % n].id, dir);
}

// ───────────── Bildirim ─────────────
let toastT = 0;
function toast(msg, act, fn) {
  const t = $('#toast'), b = $('#toastAct');
  $('#toastMsg').textContent = msg; b.textContent = act || ''; b.style.display = act ? '' : 'none';
  b.onclick = () => { t.classList.remove('on'); fn && fn(); };
  t.classList.add('on'); clearTimeout(toastT); toastT = setTimeout(() => t.classList.remove('on'), act ? 5000 : 2200);
}

// ───────────── Alt sayfalar ─────────────
const ICON = {
  trash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M5 7h14M10 7V5h4v2M7 7l1 12h8l1-12"/></svg>',
  plus: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>',
  check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7"/></svg>',
  play: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5.5v13l10.5-6.5z"/></svg>',
  share: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v12M8 7l4-4 4 4"/><path d="M6 11H5v10h14V11h-1"/></svg>',
  addbox: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round"><rect x="4" y="4" width="16" height="16" rx="4"/><path d="M12 8.5v7M8.5 12h7"/></svg>',
  dots: '<svg viewBox="0 0 24 24" fill="currentColor"><circle cx="5.5" cy="12" r="1.8"/><circle cx="12" cy="12" r="1.8"/><circle cx="18.5" cy="12" r="1.8"/></svg>',
};
let sheetState = null;
function openSheet(title, tabs, active, renderBody) {
  sheetState = { tabs, active, renderBody };
  $('#sheetTitle').textContent = title;
  drawTabs(); renderBody(active);
  $('#scrim').classList.add('on'); $('#sheet').classList.add('on');
  $('#sheetBody').scrollTop = 0;
}
function drawTabs() {
  const el = $('#sheetTabs'), st = sheetState;
  if (!st.tabs) { el.style.display = 'none'; return; }
  el.style.display = '';
  el.innerHTML = st.tabs.map(([k, l]) => `<button class="tab ${k === st.active ? 'on' : ''}" data-k="${k}">${l}</button>`).join('');
  el.querySelectorAll('.tab').forEach((b) => b.onclick = () => { st.active = b.dataset.k; drawTabs(); st.renderBody(st.active); $('#sheetBody').scrollTop = 0; });
}
function closeSheet() { $('#scrim').classList.remove('on'); $('#sheet').classList.remove('on'); sheetState = null; }
$('#scrim').onclick = closeSheet;
// aşağı kaydırarak kapat
(() => {
  const sh = $('#sheet'); let y0 = null, dy = 0;
  sh.addEventListener('touchstart', (e) => { if ($('#sheetBody').scrollTop > 0 && e.target.closest('.body')) { y0 = null; return; } y0 = e.touches[0].clientY; dy = 0; }, { passive: true });
  sh.addEventListener('touchmove', (e) => { if (y0 == null) return; dy = Math.max(0, e.touches[0].clientY - y0); if (dy > 0) { sh.style.transition = 'none'; sh.style.transform = `translateY(${dy}px)`; } }, { passive: true });
  sh.addEventListener('touchend', () => { if (y0 == null) return; sh.style.transition = ''; sh.style.transform = ''; if (dy > 90) closeSheet(); y0 = null; });
})();

const LIBS = [['mine', 'Zikirlerim'], ['vird', 'Tesbihat'], ['sunnet', 'Sünnet'], ['dualar', 'Dualar'], ['hisn', "Hisnü'l-Müslim"], ['gunluk', 'Günlük'], ['salavat', 'Salavât'], ['esma', 'Esmâ']];
function libSheet(tab = 'mine') { openSheet('Zikirler', LIBS, tab, libBody); }
function libBody(tab) {
  const body = $('#sheetBody');
  if (tab === 'mine') {
    body.innerHTML = `<button class="btn ghost" id="addZ" style="margin-bottom:12px">${ICON.plus}<span>Yeni zikir ekle</span></button>` +
      S.list.map((z) => `<div class="row ${z.id === S.selId && !S.session ? 'sel' : ''}" data-id="${z.id}"><div class="tx">${z.ar ? `<div class="t0">${esc(z.ar)}</div>` : ''}<div class="t1">${esc(z.ok)}</div>${z.an ? `<div class="t2">${esc(z.an)}</div>` : ''}</div>
       <div class="meta">${fmt(z.c)} / ${fmt(z.t)}${S.laps[z.id] ? `<br>${S.laps[z.id]} tur` : ''}</div><button class="ib" data-del="${z.id}" aria-label="Sil">${ICON.trash}</button></div>`).join('');
    $('#addZ').onclick = () => editSheet();
    body.querySelectorAll('.row').forEach((r) => r.onclick = (e) => {
      const del = e.target.closest('[data-del]');
      if (del) { e.stopPropagation(); delZikir(+del.dataset.del); return; }
      select(+r.dataset.id); closeSheet();
    });
    return;
  }
  if (tab === 'vird') {
    body.innerHTML = `<p class="note">Tesbihat adım adım ilerler; her adımın hedefi dolunca sonrakine kendiliğinden geçer.</p>` +
      S.virds.map((v) => `<div class="row ${S.session && S.session.virdId === v.id ? 'sel' : ''}" data-v="${v.id}"><div class="tx"><div class="t1">${esc(v.name)}</div>
      <div class="t2">${v.steps.map((s) => `${esc(s.ok)} ×${s.t}`).join(' · ')}</div></div><button class="ib" aria-label="Başlat">${ICON.play}</button></div>`).join('');
    body.querySelectorAll('.row').forEach((r) => r.onclick = () => {
      S.session = { virdId: +r.dataset.v, step: 0, count: 0 }; anim.key = ''; render(1); save(); closeSheet();
    });
    return;
  }
  if (tab === 'hisn') { hisnBody(body); return; }
  const items = DATA[tab];
  const note = tab === 'sunnet' ? "Peygamber Efendimiz'in (s.a.v.) tavsiye ettiği zikirler; kaynakları her maddede belirtilmiştir." : tab === 'dualar' ? DATA.duaNote : tab === 'esma' ? DATA.esmaNote : '';
  body.innerHTML = (note ? `<p class="note">${esc(note)}</p>` : '') +
    items.map((it, i) => {
      const have = S.list.some((z) => z.ar === it.ar && z.ok === it.zk);
      return `<div class="row" data-i="${i}"><div class="tx">${it.bs ? `<div class="t4">${esc(it.bs)}</div>` : ''}<div class="t0">${esc(it.ar)}</div><div class="t1">${esc(it.ok)}</div><div class="t2">${esc(it.an)}</div>${it.fz ? `<div class="t3">${esc(it.fz)}</div>` : ''}</div>
      <button class="ib" aria-label="Ekle">${have ? ICON.check : ICON.plus}</button></div>`;
    }).join('');
  body.querySelectorAll('.row').forEach((r) => r.onclick = () => {
    const it = items[+r.dataset.i];
    let z = S.list.find((q) => q.ar === it.ar && q.ok === it.zk);
    if (!z) { z = { id: ++uid, ar: it.ar, ok: it.zk, an: it.an, fz: it.fz, c: 0, t: it.t }; S.list.push(z); }
    select(z.id, 1); closeSheet(); toast(`${it.zk} eklendi`);
  });
}
// Hisnü'l-Müslim: veriler ilk açılışta yüklenir
function hisnBody(body) {
  if (!window.HISN) {
    body.innerHTML = '<p class="note">Yükleniyor…</p>';
    const sc = document.createElement('script'); sc.src = 'hisn.js';
    sc.onload = () => { if (sheetState && sheetState.active === 'hisn') hisnBody(body); };
    sc.onerror = () => { body.innerHTML = '<p class="note">Yüklenemedi. İnternet bağlantınızı kontrol edip tekrar deneyin.</p>'; };
    document.head.appendChild(sc); return;
  }
  body.innerHTML = `<p class="note">Saîd b. Ali el-Kahtânî\'nin Kur\'an ve Sünnetten derlediği dua ve zikirler (Hisnü\'l-Müslim). Türkçesi: İsmail Yaşa, tashih: Muhammed Şahin. Bir bölüme dokunarak açın.</p>` + window.HISN.map((ch, ci) => `<details class="hch" data-ci="${ci}"><summary><b>${ci + 1}</b><span>${esc(ch.t)}</span><small>${ch.i.length}</small></summary></details>`).join('');
  body.querySelectorAll('details.hch').forEach((d) => d.addEventListener('toggle', () => {
    if (!d.open || d.dataset.done) return; d.dataset.done = '1';
    const ch = window.HISN[+d.dataset.ci];
    d.insertAdjacentHTML('beforeend', ch.i.map((it, k) => {
      const have = S.list.some((z) => z.ar === it.ar);
      return `<div class="row hrow" data-k="${k}"><div class="tx">${it.n || it.c > 1 ? `<div class="t4">${[it.n ? 'No. ' + it.n : '', it.c > 1 ? it.c + ' ×' : ''].filter(Boolean).join(' · ')}</div>` : ''}${it.ar ? `<div class="t0">${esc(it.ar)}</div>` : ''}<div class="t2" style="color:var(--text)">${esc(it.tr)}</div>${it.src ? `<div class="t2">${esc(it.src)}</div>` : ''}</div><button class="ib" aria-label="Ekle">${have ? ICON.check : ICON.plus}</button></div>`;
    }).join(''));
    d.querySelectorAll('.hrow').forEach((r) => r.onclick = () => {
      const it = ch.i[+r.dataset.k];
      let z = S.list.find((q) => q.ar === it.ar);
      if (!z) { z = { id: ++uid, ar: it.ar, ok: ch.t, an: it.tr, fz: it.src, c: 0, t: it.c }; S.list.push(z); }
      select(z.id, 1); closeSheet(); toast('Zikirlerime eklendi');
    });
  }));
}

function delZikir(id) {
  if (S.list.length <= 1) { toast('En az bir zikir kalmalı'); return; }
  const i = S.list.findIndex((z) => z.id === id), z = S.list[i];
  S.list.splice(i, 1);
  if (S.selId === id) { S.selId = S.list[Math.min(i, S.list.length - 1)].id; anim.key = ''; }
  render(); save(); libBody('mine');
  toast(`${z.ok} silindi`, 'Geri al', () => { S.list.splice(i, 0, z); render(); save(); if (sheetState) libBody('mine'); });
}
function editSheet() {
  openSheet('Yeni zikir', null, null, () => {
    $('#sheetBody').innerHTML = `
      <label class="field"><span>OKUNUŞ</span><input id="fOk" placeholder="Örn. Sübhânallâh" maxlength="120" autocomplete="off"></label>
      <label class="field"><span>ARAPÇA (İSTEĞE BAĞLI)</span><input id="fAr" class="arin" dir="rtl" maxlength="400" autocomplete="off"></label>
      <label class="field"><span>ANLAMI (İSTEĞE BAĞLI)</span><input id="fAn" maxlength="300" autocomplete="off"></label>
      <label class="field"><span>HEDEF</span><input id="fT" type="number" inputmode="numeric" value="33" min="1" max="1000000"></label>
      <div class="chips">${[33, 99, 100, 500, 1000].map((n) => `<button class="chip" data-n="${n}">${fmt(n)}</button>`).join('')}</div>
      <button class="btn" id="fSave">Kaydet</button>`;
    $('#sheetBody').querySelectorAll('.chip').forEach((c) => c.onclick = () => { $('#fT').value = c.dataset.n; });
    $('#fSave').onclick = () => {
      const ok = $('#fOk').value.trim(); if (!ok) { $('#fOk').focus(); toast('Okunuşu yazın'); return; }
      const t = clamp(parseInt($('#fT').value, 10) || 33, 1, 1000000);
      const z = { id: ++uid, ar: $('#fAr').value.trim(), ok, an: $('#fAn').value.trim(), fz: '', c: 0, t };
      S.list.push(z); select(z.id, 1); closeSheet(); toast('Zikir eklendi');
    };
    setTimeout(() => $('#fOk') && $('#fOk').focus(), 350);
  });
}
function targetSheet() {
  const d = disp();
  if (d.vird) { libSheet('vird'); return; }
  openSheet('Hedef', null, null, () => {
    const z = sel();
    $('#sheetBody').innerHTML = `
      <div class="chips">${[11, 33, 99, 100, 300, 500, 1000, 5000].map((n) => `<button class="chip ${n === z.t ? 'on' : ''}" data-n="${n}">${fmt(n)}</button>`).join('')}</div>
      <label class="field"><span>ÖZEL HEDEF</span><input id="tIn" type="number" inputmode="numeric" value="${z.t}" min="1" max="1000000"></label>
      <button class="btn" id="tSave">Kaydet</button>`;
    const setT = (t) => {
      z.t = clamp(t | 0, 1, 1000000);
      if (S.settings.stopAtTarget) z.c = Math.min(z.c, z.t);
      anim.key = ''; render(); save(); closeSheet(); toast(`Hedef ${fmt(z.t)}`);
    };
    $('#sheetBody').querySelectorAll('.chip').forEach((c) => c.onclick = () => setT(+c.dataset.n));
    $('#tSave').onclick = () => setT(parseInt($('#tIn').value, 10) || z.t);
  });
}
function settingsSheet() {
  openSheet('Ayarlar', null, null, function body() {
    const st = S.settings;
    const sw = (k, l, d) => `<div class="set"><div><div class="l">${l}</div>${d ? `<div class="d">${d}</div>` : ''}</div><button class="sw ${st[k] ? 'on' : ''}" data-sw="${k}" aria-label="${l}"></button></div>`;
    const seg = (k, l, opts) => `<div class="set"><div class="l">${l}</div><div class="seg">${opts.map((o, i) => `<button class="${st[k] === i ? 'on' : ''}" data-seg="${k}" data-v="${i}">${o}</button>`).join('')}</div></div>`;
    const ios = /iP(hone|ad|od)/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    $('#sheetBody').innerHTML = `
      <div class="sec">TESBİH</div>
      <div class="beads">${BEADS.map((b, i) => `<button class="${st.bead === i ? 'on' : ''}" data-bead="${i}"><canvas width="138" height="138"></canvas>${b.name}</button>`).join('')}</div>
      ${seg('shape', 'Tane şekli', ['Yuvarlak', 'Oval'])}
      ${seg('imame', 'İmame', ['Telkari', 'Klasik'])}
      ${seg('cord', 'İp', ['Kahve', 'Kırmızı', 'Siyah'])}
      <div class="sec">SES VE TİTREŞİM</div>
      ${sw('sound', 'Ses', 'Tane çekme ve iniş sesi')}
      ${seg('soundType', 'Ses türü', ['Kehribar', 'Ahşap', 'Kristal'])}
      ${seg('vol', 'Ses seviyesi', ['Düşük', 'Orta', 'Yüksek'])}
      ${sw('slide', 'Sürtünme sesi', 'Tane ipte kayıp inmeden önce çıkan hafif hışırtı')}
      ${sw('haptic', 'Titreşim', ios ? 'iPhone’da iOS 18 ve sonrası; hafif dokunsal tık' : 'Her çekişte hafif titreşim')}
      <div class="sec">SAYAÇ</div>
      ${sw('stopAtTarget', 'Hedefte dur', 'Hedef dolunca saymayı durdurur')}
      ${seg('milestone', 'Ara uyarı', ['Kapalı', '33'].map((x) => x))}
      ${sw('wake', 'Ekran açık kalsın', 'Zikir çekerken ekran kararmaz')}
      <div class="sec">UYGULAMA</div>
      <button class="btn ghost" id="sInstall" style="margin:10px 0">${ICON.addbox}<span>Ana ekrana ekle</span></button>
      <button class="btn ghost" id="sExport" style="margin:10px 0">Yedeği indir</button>
      <button class="btn ghost" id="sImport" style="margin:10px 0">Yedeği geri yükle</button>
      <input type="file" id="sFile" accept="application/json,.json" hidden>
      <p class="note">Veriler yalnızca bu cihazda saklanır. Zikirmatik web · 4.7</p>`;
    // milestone segmenti 0/33 değerleriyle çalışır
    $('#sheetBody').querySelectorAll('[data-seg="milestone"]').forEach((b, i) => { b.classList.toggle('on', (st.milestone > 0) === (i === 1)); b.dataset.v = i ? 33 : 0; });
    $('#sheetBody').querySelectorAll('.beads canvas').forEach((c, i) => {
      const x = c.getContext('2d'); x.scale(3, 3); x.fillStyle = '#0B1733'; x.fillRect(0, 0, 46, 46); drawBead(x, BEADS[i], 23, 23, 17, 0);
    });
    $('#sheetBody').querySelectorAll('[data-bead]').forEach((b) => b.onclick = () => { st.bead = +b.dataset.bead; if (st.bead >= BEADS.length) st.bead = 0; sprites.key = ''; save(); body(); kick(); drawDome(performance.now()); });
    $('#sheetBody').querySelectorAll('[data-sw]').forEach((b) => b.onclick = () => {
      const k = b.dataset.sw; st[k] = !st[k]; b.classList.toggle('on', st[k]); save();
      if ((k === 'sound' && st.sound) || k === 'slide') { Audio.unlock(); fbPull(false, false); }
      if (k === 'haptic' && st.haptic) haptic('tap');
      if (k === 'wake') { if (st.wake) keepAwake(); else if (wakeLock) wakeLock.release(); }
      if (k === 'stopAtTarget') render();
    });
    $('#sheetBody').querySelectorAll('[data-seg]').forEach((b) => b.onclick = () => {
      st[b.dataset.seg] = +b.dataset.v; save(); body();
      if (b.dataset.seg === 'soundType' || b.dataset.seg === 'vol') { Audio.unlock(); fbPull(false, false); }
      drawDome(performance.now());
    });
    $('#sInstall').onclick = () => { closeSheet(); showInstall(true); };
    $('#sExport').onclick = () => {
      const blob = new Blob([JSON.stringify({ app: 'zikirmatik', v: 1, data: S })], { type: 'application/json' });
      const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `zikirmatik-yedek-${dayKey()}.json`; a.click();
      setTimeout(() => URL.revokeObjectURL(a.href), 4000);
    };
    $('#sImport').onclick = () => $('#sFile').click();
    $('#sFile').onchange = async (e) => {
      try {
        const o = JSON.parse(await e.target.files[0].text()), d = o.data || o;
        if (!Array.isArray(d.list) || !d.list.length) throw 0;
        S = Object.assign(defaults(), d); S.settings = Object.assign(defaults().settings, d.settings || {});
        uid = Math.max(uid, ...S.list.map((z) => z.id));
        anim.key = ''; sprites.key = ''; render(); save(); closeSheet(); toast('Yedek geri yüklendi');
      } catch { toast('Yedek dosyası okunamadı'); }
    };
  });
}

// ───────────── Ana ekrana ekle ─────────────
const isIOS = /iP(hone|ad|od)/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
const standalone = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;
let deferredPrompt = null;
window.addEventListener('beforeinstallprompt', (e) => { e.preventDefault(); deferredPrompt = e; if (!standalone && !localStorage.getItem('zk.inst')) showInstall(false); });
window.addEventListener('appinstalled', () => { $('#install').classList.remove('on'); toast('Ana ekrana eklendi'); });
function showInstall(force) {
  if (standalone && !force) return;
  const el = $('#install'), body = $('#instBody');
  if (standalone) { $('#instSub').textContent = 'Zikirmatik zaten ana ekranınızda.'; body.innerHTML = ''; }
  else if (deferredPrompt) {
    $('#instSub').textContent = 'Tek dokunuşla ana ekrana eklenir, uygulama gibi açılır.';
    body.innerHTML = `<button class="btn" id="instGo" style="margin-top:14px">${ICON.addbox}<span>Ana ekrana ekle</span></button>`;
    $('#instGo').onclick = async () => { deferredPrompt.prompt(); const r = await deferredPrompt.userChoice.catch(() => null); deferredPrompt = null; el.classList.remove('on'); if (r && r.outcome === 'accepted') localStorage.setItem('zk.inst', '1'); };
  } else if (isIOS) {
    const safari = !/CriOS|FxiOS|EdgiOS|OPiOS|GSA\//.test(navigator.userAgent);
    $('#instSub').textContent = 'Uygulama gibi tam ekran açılır, internetsiz çalışır.';
    body.innerHTML = safari
      ? `<ol><li><span>Alttaki <b>Paylaş</b> ${ICON.share} düğmesine dokunun <span style="color:var(--sub)">(görünmüyorsa önce ${ICON.dots})</span></span></li><li><span><b>Ana Ekrana Ekle</b> ${ICON.addbox} seçin</span></li><li><span>Sağ üstte <b>Ekle</b>’ye dokunun</span></li></ol><svg class="arrow" width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 4v15M6 13l6 6 6-6"/></svg>`
      : `<ol><li><span>Paylaş ${ICON.share} menüsünü açın</span></li><li><span><b>Ana Ekrana Ekle</b> seçin</span></li><li><span>Görünmüyorsa bu sayfayı <b>Safari</b>’de açın</span></li></ol>`;
  } else {
    $('#instSub').textContent = 'Tarayıcı menüsünden “Ana ekrana ekle” veya “Uygulamayı yükle” seçin.'; body.innerHTML = '';
  }
  el.classList.add('on');
}
$('#instX').onclick = () => { $('#install').classList.remove('on'); localStorage.setItem('zk.inst', '1'); };

// ───────────── Yıldızlı arka plan ─────────────
function drawStars() {
  const c = $('#stars'), d = Math.min(window.devicePixelRatio || 1, 2), w = innerWidth, h = innerHeight;
  c.width = w * d; c.height = h * d; c.style.width = w + 'px'; c.style.height = h + 'px';
  const x = c.getContext('2d'); x.scale(d, d); const r = rng(5);
  for (let i = 0; i < Math.round(w * h / 2600); i++) { x.fillStyle = `rgba(255,255,255,${0.12 + r() * 0.45})`; x.beginPath(); x.arc(r() * w, r() * h, 0.4 + r() * 0.8, 0, 2 * PI); x.fill(); }
}

// ───────────── Olaylar ─────────────
const hit = $('#hit');
hit.addEventListener('pointerdown', (e) => { if (e.button > 0) return; e.preventDefault(); anim.pressTarget = 1; tap(); kick(); });
const up = () => { anim.pressTarget = 0; kick(); };
hit.addEventListener('pointerup', up); hit.addEventListener('pointercancel', up); hit.addEventListener('pointerleave', up);
document.addEventListener('keydown', (e) => {
  if (sheetState) { if (e.key === 'Escape') closeSheet(); return; }
  if (e.code === 'Space' || e.code === 'Enter' || e.code === 'ArrowUp') { e.preventDefault(); tap(); }
  else if (e.code === 'Backspace' || e.code === 'ArrowDown') undo();
  else if (e.code === 'ArrowLeft') step(-1); else if (e.code === 'ArrowRight') step(1);
});
$('#bUndo').onclick = undo; $('#bReset').onclick = reset; $('#bTarget').onclick = targetSheet;
$('#bLib').onclick = () => libSheet(S.session ? 'vird' : 'mine'); $('#bSettings').onclick = settingsSheet;
$('#bPrev').onclick = () => { if (S.session) { S.session = null; anim.key = ''; render(-1); save(); } else step(-1); };
$('#bNext').onclick = () => { if (S.session) { libSheet('vird'); } else step(1); };
// kartı kaydırarak zikir değiştir
(() => {
  const c = $('#card'); let x0 = null, y0 = 0;
  c.addEventListener('touchstart', (e) => { x0 = e.touches[0].clientX; y0 = e.touches[0].clientY; }, { passive: true });
  c.addEventListener('touchend', (e) => {
    if (x0 == null) return; const dx = e.changedTouches[0].clientX - x0, dy = e.changedTouches[0].clientY - y0; x0 = null;
    if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.5) step(dx < 0 ? 1 : -1);
  }, { passive: true });
})();
// iOS'ta çift dokunma yakınlaştırmasını ve uzun basma menüsünü engelle
document.addEventListener('dblclick', (e) => e.preventDefault(), { passive: false });
document.addEventListener('contextmenu', (e) => { if (!e.target.closest('input,textarea')) e.preventDefault(); });
window.addEventListener('resize', () => { drawStars(); layout(); });
if (window.ResizeObserver) new ResizeObserver(() => layout()).observe($('#domeWrap'));
document.fonts && document.fonts.ready.then(() => layout());

// iPhone sesi yalnızca parmak kalkınca (touchend/click) açar; ilk dokunuşta ses kaçmasın diye her bırakışta da açılır
['touchend', 'click', 'pointerup', 'keydown'].forEach((ev) => document.addEventListener(ev, () => Audio.unlock(), { passive: true, capture: true }));

// ───────────── Başlat ─────────────
drawStars(); render(); layout();
if (!standalone && isIOS && !localStorage.getItem('zk.inst')) setTimeout(() => showInstall(false), 1200);
if ('serviceWorker' in navigator && location.protocol === 'https:') navigator.serviceWorker.register('sw.js').catch(() => {});
