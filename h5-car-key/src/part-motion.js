/**
 * Reversible part motion shared by articulated model meshes.
 * Each channel is independent: closing a window must not cancel a boot animation.
 * A new command starts from the currently visible position, without jumping.
 */
export function createPartMotion({ update, onState = () => {}, reducedMotion = false, initial = {} }) {
  const durations = { windows: 1150, trunk: 1400, mirrors: 950 };
  const channels = Object.fromEntries(Object.keys(durations).map(name => {
    const value = initial[name] ?? 0;
    return [name, { value, target: value, from: value, started: 0, duration: 0 }];
  }));
  let frame = 0, disposed = false, active = true;
  const snapshot = () => Object.fromEntries(Object.entries(channels).map(([name, c]) => [name, { progress: c.value, target: c.target, moving: c.value !== c.target }]));
  const smooth = t => t * t * (3 - 2 * t);
  function sample(now) {
    for (const channel of Object.values(channels)) {
      if (channel.value === channel.target) continue;
      const t = channel.duration ? Math.min(1, Math.max(0, (now - channel.started) / channel.duration)) : 1;
      channel.value = t === 1 ? channel.target : channel.from + (channel.target - channel.from) * smooth(t);
    }
  }
  function publish() {
    const state = snapshot();
    update(Object.fromEntries(Object.entries(channels).map(([name, channel]) => [name, channel.value])));
    onState(state);
    return state;
  }
  function tick(now) {
    frame = 0;
    if (disposed || !active) return;
    sample(now); publish();
    if (Object.values(channels).some(c => c.value !== c.target)) frame = requestAnimationFrame(tick);
  }
  publish();
  return {
    set(name, open) {
      if (disposed) return;
      const channel = channels[name];
      if (!channel) throw new Error(`Unknown movable part: ${name}`);
      const target = open ? 1 : 0;
      if (target === channel.target) return;
      const now = performance.now();
      sample(now);
      channel.from = channel.value; channel.target = target; channel.started = now;
      channel.duration = reducedMotion ? 0 : durations[name] * Math.abs(channel.target - channel.from);
      if (!channel.duration) channel.value = target;
      publish();
      if (!frame && active) frame = requestAnimationFrame(tick);
    },
    setActive(value) {
      active = !!value;
      if (!active) { cancelAnimationFrame(frame); frame = 0; }
      else if (!frame && !disposed) frame = requestAnimationFrame(tick);
    },
    getState: snapshot,
    dispose() { disposed = true; cancelAnimationFrame(frame); },
  };
}
