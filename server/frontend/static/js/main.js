import { FilesetResolver, PoseLandmarker, HandLandmarker }
  from 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.10/+esm';
import { connectWS } from './ws.js';

/* ---------- константы ---------- */
const NUM_POSE  = 33;
const NUM_HAND  = 21;
const SEQ_LEN   = 30;
const FRAME_DIM = (NUM_POSE + NUM_HAND * 2) * 3;     // 225

const CLASSES = [
  'Лось','Бык','Слон','Кролик','Жираф',
  'Крокодил','Суслик','Курица','Горилла','Носорог'
];

/* ---------- DOM ---------- */
const v       = document.getElementById('video');
const dots    = document.getElementById('dots');
const dctx    = dots.getContext('2d');
const st      = document.getElementById('status');
const probsT  = document.querySelector('#probs tbody');
const histT   = document.querySelector('#hist tbody');
const histBox = document.getElementById('histBox');
const pauseB  = document.getElementById('pauseBtn');
const loginM  = document.getElementById('loginMask');
const nameI   = document.getElementById('nameInput');
const loginB  = document.getElementById('loginBtn');

/* ---------- глобал ---------- */
let wsPromise, ws, awaiting = false, paused = false;
const seqBuffer = [];

/* ---------- таблица вероятностей ---------- */
function buildProbs(){
  probsT.innerHTML = '';
  CLASSES.forEach(c =>
      probsT.insertAdjacentHTML('beforeend',
          `<tr><td>${c}</td><td id="p-${c}">0.0%</td></tr>`));
}
buildProbs();

/* ---------- загрузка истории (≤100) ---------- */
async function loadHistory(){
  const res = await fetch('/api/history', { credentials:'same-origin', cache:'no-cache' });
  if (!res.ok) return;

  // сохраняем скролл — чтобы страница не дёргалась
  const oldScroll = histBox.scrollTop;

  histT.innerHTML = '';
  (await res.json()).forEach(r =>
      histT.insertAdjacentHTML('beforeend',
          `<tr><td>${r.id}</td><td>${r.utc.slice(11,19)}</td>
           <td>${r.user}</td><td>${r.animal}</td>
           <td>${(r.conf*100).toFixed(1)}%</td></tr>`));

  histBox.scrollTop = oldScroll;               // возвращаем положение
}

/* ---------- отправка последовательности ---------- */
function sendSequence(){
  if (paused || awaiting || !ws) return;
  const flat = new Float32Array(SEQ_LEN * FRAME_DIM);
  for (let i = 0; i < SEQ_LEN; i++) flat.set(seqBuffer[i], i * FRAME_DIM);
  awaiting = true;
  ws.sendLandmarks(flat.buffer);
}

/* ---------- обработка результата от сервера ---------- */
async function onResult(msg){
  awaiting = false;
  st.textContent = `🔎 ${msg.animal} (${(msg.confidence * 100).toFixed(1)}%)`;

  if (msg.probs?.length === CLASSES.length){
    msg.probs.forEach((v,i)=>
        document.getElementById(`p-${CLASSES[i]}`).textContent =
            (v*100).toFixed(1) + '%');
  }

  if (msg.finalShot)
    await loadHistory();
}

/* ---------- кнопка Пауза / Продолжить ---------- */
pauseB.onclick = () => {
  paused = !paused;
  pauseB.textContent = paused ? '▶ Продолжить' : '⏸ Пауза';
  if (paused)
    st.textContent = '⏸ Пауза';
};

/* ---------- запуск ---------- */
async function initApp(){
  if (!wsPromise) wsPromise = connectWS(onResult);
  ws = await wsPromise;

  await loadHistory();

  v.srcObject = await navigator.mediaDevices.getUserMedia({ video:true });
  await v.play();
  dots.width  = v.videoWidth;
  dots.height = v.videoHeight;

  const baseFs = await FilesetResolver.forVisionTasks(
      'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.10/wasm');

  const pose = await PoseLandmarker.createFromOptions(baseFs,{
    runningMode:'VIDEO', numPoses:1,
    baseOptions:{ modelAssetPath:'/models/pose_landmarker_full.task' }});

  const hand = await HandLandmarker.createFromOptions(baseFs,{
    runningMode:'VIDEO', numHands:2,
    baseOptions:{ modelAssetPath:'/models/hand_landmarker_full.task' }});

  /* ---------- основной цикл ---------- */
  let lastTS = 0;
  const frame = new Float32Array(FRAME_DIM);

  function loop(ts){
    if (!paused && ts - lastTS > 33){
      lastTS = ts;

      const pr = pose.detectForVideo(v, ts);
      const hr = hand.detectForVideo(v, ts);
      drawDots(pr, hr);

      let off = 0;

      /* 1. Руки — Left -> Right */
      const sorted = hr.landmarks
          .map((lms,i)=>({
            label: hr.handedness?.[i]?.categories?.[0]?.categoryName,
            pts  : lms }))
          .sort((a,b)=>(a.label==='Left'?0:1)-(b.label==='Left'?0:1));

      sorted.forEach(h=>h.pts.forEach(l=>{
        frame[off++] = l.x;
        frame[off++] = l.y;
        frame[off++] = l.z;
      }));

      /* 2. Поза */
      if (pr.landmarks.length)
        pr.landmarks[0].forEach(l=>{
          frame[off++] = l.x;
          frame[off++] = l.y;
          frame[off++] = l.z;
        });

      /* 3. Заполнение -1.0 */
      if (off < FRAME_DIM) frame.fill(-1.0, off);

      seqBuffer.push(frame.slice());
      if (seqBuffer.length > SEQ_LEN) seqBuffer.shift();
      if (seqBuffer.length === SEQ_LEN) sendSequence();
    }
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
}

/* ---------- рисование точек ---------- */
function drawDots(pose, hand){
  dctx.clearRect(0,0,dots.width,dots.height);
  dctx.fillStyle = '#0f0';
  hand.landmarks.forEach(a => a.forEach(pt));
  if (pose.landmarks.length) pose.landmarks[0].forEach(pt);
  function pt(l){
    dctx.beginPath();
    dctx.arc(l.x*dots.width, l.y*dots.height, 4, 0, 2*Math.PI);
    dctx.fill();
  }
}

/* ---------- логин ---------- */
function loggedIn(){
  return document.cookie.split(';').some(c => c.trim().startsWith('zoo_uid='));
}

if (loggedIn()){
  loginM.style.display = 'none';
  initApp();
}else{
  loginM.style.display = 'flex';
  loginB.onclick = async () => {
    const name = nameI.value.trim();
    if (!name) return alert('Введите имя');

    const r = await fetch('/api/register', {
      method      : 'POST',
      headers     : { 'Content-Type':'application/json' },
      body        : JSON.stringify({ name }),
      credentials : 'same-origin'
    });

    if (r.ok){
      loginM.style.display = 'none';
      initApp();
    }else{
      alert('Ошибка регистрации');
    }
  };
}
