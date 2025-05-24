/******************************************************************
 *  Animal Gesture (frontend) – версия без Socket.IO
 ******************************************************************/

import { FilesetResolver, PoseLandmarker, HandLandmarker }
  from 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.10/+esm';
import { connectWS } from './ws.js';

/* ───────── глобальные переменные ───────── */
let ws;                       // объявляем СРАЗУ, до initApp()

/* DOM-узлы */
const v      = document.getElementById('video');
const dots   = document.getElementById('dots');
const dctx   = dots.getContext('2d');
const st     = document.getElementById('status');
const hist   = document.querySelector('#hist tbody');
const yesNo  = document.getElementById('yesNo');
const yesBtn = document.getElementById('yes');
const noBtn  = document.getElementById('no');

/* login-modal */
const loginMask = document.getElementById('loginMask');
const nameInput = document.getElementById('nameInput');
const loginBtn  = document.getElementById('loginBtn');

/* ───── auth ───── */
function logged() {
  return document.cookie.split(';')
      .some(c => c.trim().startsWith('zoo_uid='));
}

if (!logged()) {
  loginMask.style.display = 'flex';
  loginBtn.onclick = async () => {
    const name = nameInput.value.trim();
    if (!name) return alert('Введите имя');

    const r = await fetch('/api/register', {
      method : 'POST',
      headers: {'Content-Type':'application/json'},
      body   : JSON.stringify({name})
    });
    if (!r.ok) return alert('Ошибка регистрации');

    loginMask.style.display = 'none';
    initApp();
  };
} else {
  loginMask.style.display = 'none';
  initApp();
}

/* ───── приложение ───── */
async function initApp() {
  /* 1  WS-подключение */
  ws = connectWS(showResult,
      () => console.log('WS connected'));

  /* 2  камера */
  let stream;
  try {
    stream = await navigator.mediaDevices.getUserMedia({video:true});
  } catch (e) {
    alert('Не удалось открыть камеру: ' + e.name);
    console.error(e);
    return;
  }
  v.srcObject = stream;
  await v.play();
  dots.width  = v.videoWidth;
  dots.height = v.videoHeight;

  /* 3  Mediapipe */
  const base = await FilesetResolver.forVisionTasks(
      'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.10/wasm');
  const poseLm = await PoseLandmarker.createFromOptions(base, {
    runningMode:'VIDEO', numPoses:1,
    baseOptions:{modelAssetPath:'/static/models/pose_landmarker_full.task'}});
  const handLm = await HandLandmarker.createFromOptions(base, {
    runningMode:'VIDEO', numHands:2,
    baseOptions:{modelAssetPath:'/static/models/hand_landmarker_full.task'}});

  /* 4  цикл */
  let last = 0;
  function loop(ts){
    if (ts - last > 33) {                 // ~30 fps
      last = ts;
      const pose = poseLm.detectForVideo(v, ts);
      const hand = handLm.detectForVideo(v, ts);

      drawDots(pose, hand);

      const vec = [];
      hand.landmarks.forEach(a => a.forEach(lm =>
          vec.push(lm.x, lm.y, lm.z)));
      while (vec.length < 21*2*3) vec.push(0);   // до 2 рук

      if (pose.landmarks.length)
        pose.landmarks[0].forEach(lm =>
            vec.push(lm.x, lm.y, lm.z));
      else vec.push(...Array(33*3).fill(0));

      ws.sendLandmarks(new Float32Array(vec).buffer);
    }
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
}

/* ───── рисование точек ───── */
function drawDots(pose, hand) {
  dctx.clearRect(0,0,dots.width,dots.height);
  dctx.fillStyle = '#0f0';
  hand.landmarks.forEach(a => a.forEach(dot));
  if (pose.landmarks.length) pose.landmarks[0].forEach(dot);

  function dot(lm) {
    dctx.beginPath();
    dctx.arc(lm.x*dots.width, lm.y*dots.height, 5, 0, Math.PI*2);
    dctx.fill();
  }
}

/* ───── результат от сервера ───── */
function showResult(d) {
  st.textContent = `🔎 ${d.animal} (${(d.confidence*100).toFixed(1)} %)`;
  yesNo.style.display = 'block';
  refreshStats();
}

/* ───── answer / stats ───── */
yesBtn.onclick = () => sendAnswer(true);
noBtn.onclick  = () => sendAnswer(false);

async function sendAnswer(val) {
  await fetch('/api/answer', {
    method :'POST',
    headers:{'Content-Type':'application/json'},
    body   : JSON.stringify({ val })
  });
  yesNo.style.display = 'none';
  refreshStats();
}

async function refreshStats() {
  const r  = await fetch('/api/stats');
  const data = await r.json();
  hist.innerHTML = '';
  data.forEach((it,i) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${i+1}</td>
      <td>${new Date().toLocaleTimeString()}</td>
      <td>${it.name ?? '-'}</td>
      <td>${it.ok}/${it.shots}</td>`;
    hist.appendChild(tr);
  });
}
