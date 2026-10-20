// The upper daytime-running-light blades also serve as amber indicators.
// Only lock/unlock acknowledgements trigger this brief, simultaneous signal.
export function createDrlSignal(car) {
  const lamps = new Map();
  car.traverse(mesh => {
    if (!mesh.isMesh) return;
    for (const material of Array.isArray(mesh.material) ? mesh.material : [mesh.material]) {
      if (!/Headlight DRL/.test(material.name) || lamps.has(material)) continue;
      lamps.set(material, { color: material.color.clone(), emissive: material.emissive.clone(), intensity: material.emissiveIntensity });
    }
  });
  let until = 0, amber = false;
  return {
    play(action, now) { if (action === 'lock' || action === 'unlock') until = now + 700; },
    cancel() { until = 0; },
    update(now, reducedMotion, finding = false) {
      amber = now < until && !finding;
      for (const [material, original] of lamps) {
        if (amber) {
          material.color.setRGB(1, .22, .004);
          material.emissive.setRGB(1, .22, .004);
          material.emissiveIntensity = 2.3;
        } else {
          material.color.copy(original.color); material.emissive.copy(original.emissive);
          material.emissiveIntensity = finding && !reducedMotion ? (Math.sin(now / 130) > 0 ? 2.5 : .15) : original.intensity;
        }
      }
      return amber;
    },
    getState: () => ({ amber, materialCount: lamps.size }),
  };
}
