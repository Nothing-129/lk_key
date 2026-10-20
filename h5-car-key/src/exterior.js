import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { DRACOLoader } from 'three/addons/loaders/DRACOLoader.js';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { createPartMotion } from './part-motion.js';
import { createWheels } from './wheels.js';
import { createGroundShadow } from './ground-shadow.js';
import vehicleFinish from './vehicle-finish.json';
import { createTailLights } from './tail-lights.js';
import { createDrlSignal } from './drl-signal.js';

export async function createExteriorView(container, { onFrame = () => {}, onMotion = () => {}, onProgress = () => {}, landscapeComposition = false, reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches } = {}) {
  const renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
  renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
  renderer.setClearColor(0, 0);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.toneMapping = THREE.ACESFilmicToneMapping;
  renderer.toneMappingExposure = .78;
  renderer.localClippingEnabled = true;
  renderer.domElement.setAttribute('aria-hidden', 'true');
  container.appendChild(renderer.domElement);
  const scene = new THREE.Scene();
  const pmrem = new THREE.PMREMGenerator(renderer);
  // Broad, neutral studio reflections: keep the hood gray and let the long
  // side highlights describe the metallic finish in the APP reference.
  const room = new THREE.Scene();
  room.background = new THREE.Color().setRGB(.32, .32, .32);
  const panels = [];
  function softbox(position, width, height, intensity) {
    const panel = new THREE.Mesh(new THREE.PlaneGeometry(width, height), new THREE.MeshBasicMaterial({
      color: new THREE.Color().setRGB(intensity, intensity, intensity), side: THREE.DoubleSide,
    }));
    panel.position.set(...position); panel.lookAt(0, 0, 0); room.add(panel); panels.push(panel);
  }
  softbox([6, 2, 0], 10, 3, 1.6);
  softbox([-6, 2, 0], 9, 3, .95);
  softbox([0, 7, 0], 6, 10, .7);
  softbox([6, -2, -3], 10, 1.8, 1.6);
  softbox([0, 3, -7], 6, 4, .65);
  softbox([0, 2, 7], 7, 4, .6);
  const environment = pmrem.fromScene(room, .06);
  scene.environment = environment.texture;
  scene.environmentIntensity = .85;
  panels.forEach(panel => { panel.geometry.dispose(); panel.material.dispose(); }); pmrem.dispose();
  scene.add(new THREE.HemisphereLight(0xf0f1f5, 0x252629, .4));
  const key = new THREE.DirectionalLight(0xffffff, .75);
  key.position.set(3, 6, 5); scene.add(key);
  const rim = new THREE.DirectionalLight(0xe5e8ee, .6);
  rim.position.set(-4, 2.5, -4); scene.add(rim);
  const camera = new THREE.PerspectiveCamera(35, 1, .05, 80);
  const controls = new OrbitControls(camera, renderer.domElement);
  controls.target.set(0, .50, 0);
  controls.enablePan = false;
  controls.enableDamping = !reducedMotion;
  controls.dampingFactor = .09;
  controls.minPolarAngle = .43;
  controls.maxPolarAngle = Math.PI / 2 - .03;
  controls.rotateSpeed = .55;
  controls.zoomSpeed = .55;
  controls.minDistance = 5;
  controls.maxDistance = 13;
  const defaultTheta = landscapeComposition ? .96 : .73, defaultPhi = landscapeComposition ? 1.40 : 1.19;
  let baseRadius = 8.3, animation = 0, tween = null, pulseUntil = 0, disposed = false, activeView = true;
  let lastFrame = -1, lastDegrees = -1;
  function setCamera(theta, phi = defaultPhi, radius = baseRadius) {
    camera.position.copy(new THREE.Vector3().setFromSphericalCoords(radius, phi, theta).add(controls.target));
    controls.update();
  }
  function render() { if (!disposed && activeView) renderer.render(scene, camera); }
  function resize() {
    const { width, height } = container.getBoundingClientRect();
    if (!width || !height) return;
    renderer.setSize(width, height);
    camera.aspect = width / height; camera.updateProjectionMatrix();
    if (landscapeComposition) camera.setViewOffset(width, height, 0, -height * .02, width, height);
    const oldRadius = baseRadius;
    baseRadius = Math.max(6.65, (landscapeComposition ? 9.4 : 7.8) / Math.min(camera.aspect, 1.45));
    if (oldRadius && camera.position.length()) camera.position.sub(controls.target).multiplyScalar(baseRadius / oldRadius).add(controls.target);
    render();
  }
  setCamera(defaultTheta);
  const observer = new ResizeObserver(resize); observer.observe(container); resize();
  const halo = new THREE.Mesh(new THREE.RingGeometry(1, 1.025, 96), new THREE.MeshBasicMaterial({ color: 0xe5c59a, transparent: true, opacity: 0, depthWrite: false, side: THREE.DoubleSide }));
  halo.rotation.x = -Math.PI / 2; halo.position.y = .015; scene.add(halo);
  const draco = new DRACOLoader().setDecoderPath('/draco/').setDecoderConfig({ type: 'wasm' }).setWorkerLimit(2);
  const loader = new GLTFLoader().setDRACOLoader(draco);
  let gltf;
  try {
    gltf = await loader.loadAsync('/vehicle/lynk03.glb?v=20261012-rear-silver', event => onProgress(event.total ? Math.round(event.loaded / event.total * 100) : 0));
  } catch (error) { observer.disconnect(); draco.dispose(); environment.dispose(); renderer.dispose(); throw error; }
  const car = gltf.scene;
  const wheels = createWheels();
  car.add(wheels);
  const groundShadow = createGroundShadow(wheels.userData.groundContacts);
  scene.add(groundShadow.mesh);
  halo.position.y = groundShadow.mesh.position.y + .003;
  scene.add(car); car.updateMatrixWorld(true);
  const windows = ['Window_front_left', 'Window_front_right', 'Window_rear_left', 'Window_rear_right'].map(name => {
    const mesh = car.getObjectByName(name);
    if (!mesh?.isMesh) throw new Error(`模型缺少可动车窗：${name}`);
    mesh.material = mesh.material.clone();
    mesh.material.clippingPlanes = [new THREE.Plane(new THREE.Vector3(0, 1, 0), -.97)];
    mesh.material.depthWrite = false;
    return { mesh, base: mesh.position.clone(), travel: name.includes('front') ? .45 : .43 };
  });
  const trunk = car.getObjectByName('Trunk_hinge');
  if (!trunk || !trunk.children.length) throw new Error('模型缺少后备箱部件');
  const mirrors = ['left', 'right'].map(side => {
    const hinge = car.getObjectByName(`Mirror_${side}_hinge`);
    if (!hinge?.children.length) throw new Error(`模型缺少后视镜：${side}`);
    return { hinge, direction: side === 'left' ? -1 : 1 };
  });
  const lightMaterials = new Map();
  car.traverse(mesh => {
    if (!mesh.isMesh) return;
    const mats = Array.isArray(mesh.material) ? mesh.material : [mesh.material];
    for (const m of mats) {
      for (const [property, value] of Object.entries(vehicleFinish[m.name] || {})) {
        if (property === 'color') m.color.setRGB(...value);
        else m[property] = value;
      }
      if (/Taillight (smoked cover|clear optics|red perimeter lens)/.test(m.name)) {
        m.depthWrite = false; m.side = THREE.FrontSide; m.envMapIntensity = .22;
      }
      if (/Headlight (clear cover|inner optics)/.test(m.name)) {
        m.depthWrite = false; m.side = THREE.FrontSide; m.envMapIntensity = .3;
      }
      if (/Headlight DRL/.test(m.name)) {
        m.emissive.setRGB(.84, .91, 1); m.emissiveIntensity = 2;
      } else if (/LED_running/.test(m.name)) {
        m.emissive.copy(m.color); m.emissiveIntensity = .2; lightMaterials.set(m, .2);
      }
    }
  });
  const tailLights = createTailLights(car);
  const drlSignal = createDrlSignal(car);
  const motion = createPartMotion({ reducedMotion, initial: { mirrors: 1 }, onState: onMotion, update: ({ windows: progress, trunk: angle, mirrors: folded }) => {
    for (const { mesh, base, travel } of windows) mesh.position.set(base.x, base.y - travel * progress, base.z);
    trunk.rotation.x = THREE.MathUtils.degToRad(68) * angle;
    for (const { hinge, direction } of mirrors) hinge.rotation.y = direction * THREE.MathUtils.degToRad(72) * folded;
    render();
  } });
  function publishAngle() {
    const degrees = (Math.round(THREE.MathUtils.radToDeg(controls.getAzimuthalAngle() - defaultTheta)) % 360 + 360) % 360;
    const frame = Math.round(degrees / 10) % 36;
    if (degrees !== lastDegrees) { lastDegrees = degrees; lastFrame = frame; onFrame({ frame, total: 36, degrees }); }
  }
  function tick(now) {
    animation = 0;
    if (disposed || !activeView) return;
    let active = false;
    if (tween) {
      const t = Math.min(1, (now - tween.start) / 850), ease = t * t * (3 - 2 * t);
      setCamera(THREE.MathUtils.lerp(tween.from, tween.to, ease), THREE.MathUtils.lerp(tween.phi, defaultPhi, ease), THREE.MathUtils.lerp(tween.radius, tween.targetRadius, ease));
      if (t === 1) tween = null; else active = true;
    }
    if (controls.update()) active = true;
    if (tailLights.update(now, reducedMotion, now < pulseUntil)) active = true;
    if (drlSignal.update(now, reducedMotion, now < pulseUntil)) active = true;
    if (now < pulseUntil) {
      const t = (now % 1050) / 1050;
      halo.scale.set(1.8 + t * 1.6, 2.7 + t * 1.6, 1);
      halo.material.opacity = reducedMotion ? .2 : .6 * (1 - t);
      for (const [m, resting] of lightMaterials) m.emissiveIntensity = Math.sin(now / 130) > 0 ? 2.5 : (resting > 1 ? .15 : resting);
      active = true;
    } else if (halo.material.opacity > 0) {
      halo.material.opacity = 0;
      for (const [m, resting] of lightMaterials) m.emissiveIntensity = resting;
    }
    publishAngle(); render();
    if (active) start();
  }
  function start() { if (!animation && !disposed && activeView) animation = requestAnimationFrame(tick); }
  controls.addEventListener('change', start);
  controls.addEventListener('start', () => { tween = null; start(); });
  function goTo(index, immediate = false) {
    // Match the existing preset labels while allowing continuous 3D orbit.
    const requested = index === 31 ? Math.PI / 2 : index === 18 ? Math.PI - .63 : defaultTheta + index / 36 * Math.PI * 2;
    const from = controls.getAzimuthalAngle();
    const delta = ((requested - from + Math.PI * 3) % (Math.PI * 2)) - Math.PI;
    const targetRadius = baseRadius * (index === 18 ? 1.13 : 1);
    if (immediate || reducedMotion) { tween = null; setCamera(from + delta, defaultPhi, targetRadius); publishAngle(); render(); }
    else { tween = { from, to: from + delta, phi: controls.getPolarAngle(), radius: camera.position.distanceTo(controls.target), targetRadius, start: performance.now() }; start(); }
  }
  container.addEventListener('keydown', event => {
    if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', '+', '=', '-', 'Home'].includes(event.key)) return;
    event.preventDefault(); tween = null;
    let theta = controls.getAzimuthalAngle(), phi = controls.getPolarAngle(), radius = camera.position.distanceTo(controls.target);
    if (event.key === 'ArrowLeft') theta -= .14;
    if (event.key === 'ArrowRight') theta += .14;
    if (event.key === 'ArrowUp') phi -= .08;
    if (event.key === 'ArrowDown') phi += .08;
    if (['+', '='].includes(event.key)) radius -= .4;
    if (event.key === '-') radius += .4;
    if (event.key === 'Home') { goTo(0); return; }
    setCamera(theta, THREE.MathUtils.clamp(phi, controls.minPolarAngle, controls.maxPolarAngle), THREE.MathUtils.clamp(radius, controls.minDistance, controls.maxDistance));
    publishAngle(); render();
  });
  publishAngle(); render();
  return {
    goTo, reset() { goTo(0); },
    isDefaultView() {
      const delta = controls.getAzimuthalAngle() - defaultTheta;
      const angle = Math.atan2(Math.sin(delta), Math.cos(delta));
      const distance = THREE.MathUtils.clamp(baseRadius, controls.minDistance, controls.maxDistance);
      return Math.abs(angle) < .005 && Math.abs(controls.getPolarAngle() - defaultPhi) < .005
        && Math.abs(camera.position.distanceTo(controls.target) - distance) < distance * .005;
    },
    setWindows(open) { motion.set('windows', open); },
    setTrunk(open) { motion.set('trunk', open); },
    setMirrors(folded) { motion.set('mirrors', folded); },
    pulse(duration = 1400) { drlSignal.cancel(); pulseUntil = performance.now() + duration; start(); },
    playLockLights(action) {
      const now = performance.now(); pulseUntil = 0;
      tailLights.play(action, now); drlSignal.play(action, now); start();
    },
    setActive(active) {
      activeView = !!active; motion.setActive(activeView);
      if (activeView) start();
      else { cancelAnimationFrame(animation); animation = 0; }
    },
    getState: () => ({ frame: lastFrame, frameCount: 36, mode: 'articulated-model', color: 'gray', lights: tailLights.getState(), frontLights: drlSignal.getState(), motion: motion.getState(), trunkAngle: THREE.MathUtils.radToDeg(trunk.rotation.x), mirrorAngles: mirrors.map(({ hinge }) => THREE.MathUtils.radToDeg(hinge.rotation.y)), windowOffsets: windows.map(({ mesh, base }) => mesh.position.y - base.y), camera: { theta: controls.getAzimuthalAngle(), phi: controls.getPolarAngle(), distance: camera.position.distanceTo(controls.target) }, rendering: { triangles: renderer.info.render.triangles, calls: renderer.info.render.calls } }),
    capture() { resize(); render(); return renderer.domElement.toDataURL('image/png'); },
    dispose() { disposed = true; cancelAnimationFrame(animation); motion.dispose(); observer.disconnect(); controls.dispose(); draco.dispose(); environment.dispose(); groundShadow.dispose(); halo.geometry.dispose(); halo.material.dispose(); car.traverse(o => { o.geometry?.dispose(); if (o.material) (Array.isArray(o.material) ? o.material : [o.material]).forEach(m => { m.map?.dispose(); m.dispose(); }); }); renderer.dispose(); },
  };
}
