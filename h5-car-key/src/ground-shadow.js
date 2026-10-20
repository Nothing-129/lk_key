import * as THREE from 'three';

// A static ambient/contact shadow: one draw call, with no per-frame shadow map.
// All marks live on the car's ground plane, so they stay under the tires while orbiting.
export function createGroundShadow(contacts) {
  const width = 2.8, length = 5.8;
  const canvas = document.createElement('canvas');
  canvas.width = 256; canvas.height = 512;
  const ctx = canvas.getContext('2d');
  ctx.scale(canvas.width / width, canvas.height / length);
  ctx.translate(width / 2, length / 2);
  function ellipse(x, z, radiusX, radiusZ, stops) {
    ctx.save();
    ctx.translate(x, z); ctx.scale(radiusX, radiusZ);
    const gradient = ctx.createRadialGradient(0, 0, 0, 0, 0, 1);
    for (const [offset, alpha] of stops) gradient.addColorStop(offset, `rgba(0,0,0,${alpha})`);
    ctx.fillStyle = gradient; ctx.fillRect(-1, -1, 2, 2);
    ctx.restore();
  }
  // Soft outer falloff plus a denser, narrower occluded area beneath the chassis.
  ellipse(0, -.05, 1.35, 2.8, [[0, .38], [.55, .28], [.8, .1], [1, 0]]);
  ellipse(0, -.05, .95, 2.35, [[0, .72], [.55, .65], [.82, .32], [1, 0]]);
  for (const { x, z } of contacts) {
    ellipse(x, z, .30, .48, [[0, .8], [.25, .65], [.65, .22], [1, 0]]);
    ellipse(x, z, .16, .28, [[0, .96], [.4, .85], [.75, .35], [1, 0]]);
  }
  const texture = new THREE.CanvasTexture(canvas);
  texture.colorSpace = THREE.SRGBColorSpace;
  const mesh = new THREE.Mesh(new THREE.PlaneGeometry(width, length), new THREE.MeshBasicMaterial({
    map: texture, transparent: true, depthWrite: false, toneMapped: false,
  }));
  mesh.name = 'Chassis_and_tire_contact_shadow';
  mesh.rotation.x = -Math.PI / 2;
  // Slightly below the measured rubber, never intersecting the tire silhouette.
  mesh.position.y = Math.min(...contacts.map(contact => contact.y)) - .001;
  return {
    mesh,
    dispose() { texture.dispose(); mesh.geometry.dispose(); mesh.material.dispose(); },
  };
}
