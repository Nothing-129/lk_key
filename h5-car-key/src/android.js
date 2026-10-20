import { createExteriorView } from './exterior.js';

// Presentation-only API. There is deliberately no web-to-Bluetooth bridge.
const api = window.vehicle3D = { ready: false, error: null };
const hint = document.getElementById('hint');
try {
  const view = await createExteriorView(document.getElementById('scene'), {
    landscapeComposition: true,
    reducedMotion: new URLSearchParams(location.search).get('reduce') === '1',
  });
  let last = null;
  api.apply = state => {
    view.setMirrors(state.mirrorsFolded);
    view.setWindows(state.windowsOpen);
    view.setTrunk(state.trunkOpen);
    if (last && state.revision !== last.revision) {
      if (state.lightAction === 'lock' || state.lightAction === 'unlock') view.playLockLights(state.lightAction);
      if (state.trunkOpen && !last.trunkOpen) view.goTo(18);
    }
    last = state;
  };
  api.setActive = active => view.setActive(active);
  api.getState = () => view.getState();
  api.capture = () => view.capture();
  api.reset = () => view.reset();
  api.needsReset = () => !view.isDefaultView();
  document.addEventListener('visibilitychange', () => view.setActive(!document.hidden));
  document.getElementById('scene').addEventListener('webglcontextlost', () => {
    api.ready = false; api.error = 'WebGL context lost';
  }, true);
  hint.hidden = true;
  api.ready = true;
} catch (error) {
  api.error = String(error); hint.textContent = '3D 暂不可用';
}
