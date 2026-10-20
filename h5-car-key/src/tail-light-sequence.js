// Approximate timing measured from the supplied 24 fps clip: two outward
// sweeps for welcome, then a separate all-on / inward extinction farewell.
export const TAIL_SEGMENTS = 7;
export const TAIL_DURATION = { unlock: 3200, lock: 2450 };
const smooth = value => { const t = Math.max(0, Math.min(1, value)); return t * t * (3 - 2 * t); };

export function sampleTailSequence(action, elapsed, reducedMotion = false) {
  const levels = Array(TAIL_SEGMENTS + 1).fill(0);
  const duration = TAIL_DURATION[action];
  if (!duration || elapsed < 0 || elapsed >= duration) return { levels, active: false };
  if (reducedMotion) return { levels: levels.map(() => elapsed < 800 ? 1 : 0), active: elapsed < 800 };
  if (action === 'unlock') {
    const fade = 1 - smooth((elapsed - 3060) / 140);
    levels[7] = smooth(elapsed / 65) * fade;
    const sweep = elapsed < 620 ? elapsed : elapsed < 740 ? -1000 : elapsed - 740;
    for (let i = 0; i < TAIL_SEGMENTS; i++) levels[i] = smooth((sweep - i * 72) / 55) * fade;
  } else {
    const rise = smooth(elapsed / 65);
    for (let i = 0; i < TAIL_SEGMENTS; i++) levels[i] = rise * (1 - smooth((elapsed - 1660 - (6 - i) * 77) / 55));
    levels[7] = rise * (1 - smooth((elapsed - 2270) / 180));
  }
  return { levels, active: true };
}
