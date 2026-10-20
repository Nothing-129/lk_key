import './style.css';
import { createExteriorView } from './exterior.js';

const icons={
 lock:'<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4M12 14v3"/>',
 unlock:'<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V6a4 4 0 0 1 7.7-1.5M12 14v3"/>',
 'window-down':'<path d="M3 8h18v13H3V8l5-5h13v5M12 11v7m-3-3 3 3 3-3"/>',
 'window-up':'<path d="M3 8h18v13H3V8l5-5h13v5M12 18v-7m-3 3 3-3 3 3"/>',
 trunk:'<path d="m2 16 3-1 4-5h7l5 5v4h-3m-11 0h7M2 16v3h1m13-9 3-5m-2 0h3"/><circle cx="5" cy="19" r="2"/><circle cx="16" cy="19" r="2"/>',
 find:'<path d="m4 14 2-6h12l2 6M3 14h18v6H3zM6 20v2m12-2v2M6 17h2m8 0h2M5 2v2m7-3v3m7-2v2"/>',
 shield:'<path d="M12 2 21 6v6c0 5-5 8-9 10-4-2-9-5-9-10V6l9-4Z"/><path d="m8 12 3 3 5-6"/>',
 key:'<circle cx="8" cy="8" r="4"/><path d="m11 11 9 9m-4-4 3-3m-1 5 3-3"/>',
 rotate:'<path d="M20 8a8 8 0 1 0 0 8M20 3v5h-5"/>',
 orbit:'<ellipse cx="12" cy="12" rx="10" ry="4"/><path d="M7 5c3-4 9 3 10 7s0 9-3 7S5 10 7 5Z"/>',
 check:'<path d="m5 12 4 4L19 6"/>'
};
document.querySelectorAll('[data-icon]').forEach(el=>el.innerHTML=`<svg viewBox="0 0 24 24" aria-hidden="true">${icons[el.dataset.icon]}</svg>`);
const $=id=>document.getElementById(id);
const state={locked:true,windowsOpen:false,trunkOpen:false,finding:false};
let toastTimer;
function toast(message){$('toast-text').textContent=message;$('toast').classList.add('visible');clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').classList.remove('visible'),2600);}
function vibrate(){navigator.vibrate?.(12);}
function refreshMotion(parts) {
  for (const [part, id] of [['windows', 'window-status'], ['trunk', 'trunk-status']]) {
    const channel = parts[part];
    $(id).textContent = channel.moving ? (channel.target ? '正在打开' : '正在关闭') : (channel.target ? '已打开' : '已关闭');
  }
  $('lock-status').textContent = parts.mirrors.moving
    ? (parts.mirrors.target ? '车辆已锁定 · 后视镜收起中' : '车辆已解锁 · 后视镜展开中')
    : (state.locked ? '车辆已锁定' : '车辆已解锁');
  $('vehicle-status').title = parts.mirrors.moving
    ? (parts.mirrors.target ? '后视镜正在收起' : '后视镜正在展开')
    : (parts.mirrors.target ? '后视镜已收起' : '后视镜已展开');
}

async function init() {
  const actionButtons = [...document.querySelectorAll('.controls button,.view-reset,.view-preset')];
  actionButtons.forEach(button => button.disabled = true);
  const view = await createExteriorView($('scene'), {
    onFrame: ({ degrees }) => {
      $('angle-readout').textContent = `${String(degrees).padStart(3, '0')}°`;
      $('scene').setAttribute('aria-valuenow', String(degrees));
      $('scene').setAttribute('aria-valuetext', `灰色领克 03，环视角度 ${degrees} 度`);
      const presetAngles = { 0: 0, 31: 48, 18: 102 };
      document.querySelectorAll('.view-preset').forEach(button => button.setAttribute('aria-pressed', String(Math.abs(presetAngles[button.dataset.frame] - degrees) < 4)));
    },
    onProgress: percent => { $('loading-label').textContent = percent ? `正在加载 3D 座驾 · ${percent}%` : '正在准备 3D 座驾'; },
    onMotion: refreshMotion,
  });
  actionButtons.forEach(button => button.disabled = false);
  $('loading').classList.add('loaded');
  $('loading').setAttribute('aria-hidden', 'true');
  function refreshUI() {
    $('lock-status').textContent = state.locked ? '车辆已锁定' : '车辆已解锁';
    $('vehicle-status').classList.toggle('unlocked', !state.locked);
    $('summary-status').textContent = state.locked ? '安心停驻' : '随时出发';
    $('lock').setAttribute('aria-pressed', String(state.locked));
    $('unlock').setAttribute('aria-pressed', String(!state.locked));
    $('open-windows').classList.toggle('active', state.windowsOpen);
    $('close-windows').classList.toggle('active', !state.windowsOpen);
    $('open-windows').setAttribute('aria-pressed', String(state.windowsOpen));
    $('close-windows').setAttribute('aria-pressed', String(!state.windowsOpen));
    $('trunk').classList.toggle('active', state.trunkOpen);
    $('trunk').setAttribute('aria-pressed', String(state.trunkOpen));
    $('trunk-description').textContent = state.trunkOpen ? '轻触关闭后备箱' : '装下生活的可能';
    $('find-car').classList.toggle('active', state.finding);
    refreshMotion(view.getState().motion);
  }
  $('lock').addEventListener('click', () => { state.locked = true; refreshUI(); view.setMirrors(true); view.playLockLights('lock'); vibrate(); toast('车辆已锁定 · 后视镜收起'); });
  $('unlock').addEventListener('click', () => { state.locked = false; refreshUI(); view.setMirrors(false); view.playLockLights('unlock'); vibrate(); toast('车辆已解锁 · 后视镜展开'); });
  $('open-windows').addEventListener('click', () => { state.windowsOpen = true; refreshUI(); view.setWindows(true); vibrate(); toast('四扇车窗正在降下'); });
  $('close-windows').addEventListener('click', () => { state.windowsOpen = false; refreshUI(); view.setWindows(false); vibrate(); toast('四扇车窗正在升起'); });
  $('trunk').addEventListener('click', () => {
    state.trunkOpen = !state.trunkOpen; refreshUI(); vibrate();
    if (state.trunkOpen) view.goTo(18);
    view.setTrunk(state.trunkOpen);
    toast(state.trunkOpen ? '后备箱正在开启' : '后备箱正在关闭');
  });
  let findTimer;
  $('find-car').addEventListener('click', () => {
    state.finding = true; refreshUI(); view.pulse(3600); vibrate(); toast('寻车演示 · 光圈已标记车辆');
    clearTimeout(findTimer); findTimer = setTimeout(() => { state.finding = false; refreshUI(); }, 3600);
  });
  $('reset-view').addEventListener('click', () => { view.reset(); toast('已恢复默认视角'); });
  document.querySelectorAll('.view-preset').forEach(button => button.addEventListener('click', () => view.goTo(Number(button.dataset.frame))));
  refreshUI();
  if (new URLSearchParams(location.search).get('view') === 'rear') view.goTo(18, true);
  window.__keyPreview = { capture: view.capture, getState: () => ({ ...state, ...view.getState() }) };
}
init().catch(error => {
  console.error(error);
  $('loading').classList.remove('loaded');
  $('loading').removeAttribute('aria-hidden');
  $('loading').innerHTML = '<p>车辆外观暂时加载失败</p><button id="retry-scene">重新加载</button>';
  $('retry-scene').addEventListener('click', () => location.reload());
});
