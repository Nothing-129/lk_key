import * as THREE from 'three';
import { sampleTailSequence, TAIL_SEGMENTS } from './tail-light-sequence.js';

// Weld positions only for connectivity analysis: hard-normal seams in the
// Draco mesh otherwise split each crystal blade into individual polygons.
function components(geometry) {
  const p = geometry.attributes.position, parents = Array.from({ length: p.count }, (_, i) => i), welded = new Map();
  const root = i => { while (parents[i] !== i) { parents[i] = parents[parents[i]]; i = parents[i]; } return i; };
  const join = (a, b) => { parents[root(a)] = root(b); };
  for (let i = 0; i < p.count; i++) {
    const key = [p.getX(i), p.getY(i), p.getZ(i)].map(v => Math.round(v * 10000)).join(',');
    if (welded.has(key)) join(i, welded.get(key)); else welded.set(key, i);
  }
  const index = geometry.index, count = index ? index.count : p.count;
  for (let i = 0; i < count; i += 3) {
    const a = index ? index.getX(i) : i;
    join(a, index ? index.getX(i + 1) : i + 1); join(a, index ? index.getX(i + 2) : i + 2);
  }
  const groups = new Map();
  for (let i = 0; i < p.count; i++) { const key = root(i); if (!groups.has(key)) groups.set(key, []); groups.get(key).push(i); }
  return [...groups.values()].map(vertices => ({ vertices, x: vertices.reduce((sum, i) => sum + Math.abs(p.getX(i)), 0) / vertices.length }));
}

export function createTailLights(car) {
  const levels = { value: Array(8).fill(0) }, materials = new Set(), bladeMeshes = [], loopMeshes = [];
  car.traverse(mesh => {
    if (!mesh.isMesh) return;
    if (/Taillight LED blades/.test(mesh.material?.name)) bladeMeshes.push({ mesh, groups: components(mesh.geometry) });
    if (/Taillight LED lower loop/.test(mesh.material?.name)) loopMeshes.push(mesh);
  });
  // Four inner pairs on the lid and three outer pairs on each rear quarter.
  // Cluster mirrored blade centers, then light each adjacent pair together.
  const centers = [];
  for (const x of bladeMeshes.flatMap(({ groups }) => groups.map(g => g.x)).sort((a, b) => a - b)) {
    if (!centers.length || x - centers.at(-1) > .006) centers.push(x);
  }
  if (centers.length !== TAIL_SEGMENTS * 2) throw new Error(`尾灯分段识别失败：${centers.length}`);
  for (const { mesh, groups } of bladeMeshes) {
    const ids = new Float32Array(mesh.geometry.attributes.position.count);
    for (const group of groups) {
      const nearest = centers.reduce((best, x, i) => Math.abs(x - group.x) < Math.abs(centers[best] - group.x) ? i : best, 0);
      for (const vertex of group.vertices) ids[vertex] = Math.floor(nearest / 2);
    }
    mesh.geometry.setAttribute('tailSegment', new THREE.BufferAttribute(ids, 1)); materials.add(mesh.material);
  }
  for (const mesh of loopMeshes) {
    mesh.geometry.setAttribute('tailSegment', new THREE.BufferAttribute(new Float32Array(mesh.geometry.attributes.position.count).fill(7), 1));
    materials.add(mesh.material);
  }
  // One shared uniform array controls all segments without adding draw calls.
  // Segment IDs stay attached to lid geometry while the trunk opens.
  for (const material of materials) {
    material.toneMapped = false;
    const lower = /lower loop/.test(material.name);
    material.emissive.setRGB(1, lower ? .06 : .35, lower ? .01 : .09);
    material.emissiveIntensity = lower ? 3.8 : 6;
    material.onBeforeCompile = shader => {
      shader.uniforms.tailLevels = levels;
      shader.vertexShader = 'attribute float tailSegment; varying float vTailSegment;\n' + shader.vertexShader;
      shader.vertexShader = shader.vertexShader.replace('#include <begin_vertex>', '#include <begin_vertex>\nvTailSegment = tailSegment;');
      shader.fragmentShader = 'uniform float tailLevels[8]; varying float vTailSegment;\n' + shader.fragmentShader;
      shader.fragmentShader = shader.fragmentShader.replace('#include <emissivemap_fragment>', `#include <emissivemap_fragment>
        float tailLevel = tailLevels[int(vTailSegment + 0.5)];
        totalEmissiveRadiance *= tailLevel;`);
    };
    material.customProgramCacheKey = () => 'lynk-tail-segments-v2';
    material.needsUpdate = true;
  }
  let action = null, started = 0;
  return {
    play(value, now) { if (!['lock', 'unlock'].includes(value)) return; action = value; started = now; },
    update(now, reducedMotion, finding = false) {
      const result = sampleTailSequence(action, now - started, reducedMotion);
      const values = finding ? Array(8).fill(reducedMotion || Math.sin(now / 130) > 0 ? 1 : 0) : result.levels;
      for (let i = 0; i < values.length; i++) levels.value[i] = values[i];
      if (!result.active) action = null;
      return result.active;
    },
    getState: () => ({ action, segments: levels.value.slice(0, 7), lowerLoop: levels.value[7], pairedSegmentsPerSide: 7 }),
  };
}
