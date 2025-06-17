import { FilesetResolver, PoseLandmarker, HandLandmarker }
  from 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.10/+esm';
import { connectWS } from './ws.js';

/* ---------- классы ---------- */
const CLASSES = ["bear","cat","cow","deer","dog","frog",
                 "goat","goose","horse","moose"];

/* ---------- DOM ---------- */
const v      = document.getElementById('video');
const dots   = document.getElementById('dots');
const dctx   = dots.getContext('2d');
const st     = document.getElementById('status');
const probsT = document.querySelector('#probs tbody');
const hist   = document.querySelector('#hist tbody');
const loginM = document.getElementById('loginMask');
const nameI  = document.getElementById('nameInput');
const loginB = document.getElementById('loginBtn');

let ws;

/* ---------- таблица вероятностей ---------- */
function buildProbs(){
  probsT.innerHTML = '';
  CLASSES.forEach(c=>{
    probsT.insertAdjacentHTML('beforeend',
      `<tr><td>${c}</td><td id="p-${c}">0%</td></tr>`);
  });
}
buildProbs();

/* ---------- Float32Array → Base-64 и отправка ---------- */
function sendVec(arr){
  const u8 = new Uint8Array(arr.buffer);
  let bin  = '';
  for (const b of u8) bin += String.fromCharCode(b);
  ws.sendLandmarks(btoa(bin));
}

/* ---------- приём результата ---------- */
async function onResult(msg){
  st.textContent = `🔎 ${msg.animal} (${(msg.confidence*100).toFixed(1)} %)`;

  if (msg.probs?.length === 10){
    msg.probs.forEach((v,i)=>
      document.getElementById(`p-${CLASSES[i]}`).textContent =
        (v*100).toFixed(1)+'%');
  }

  if (msg.finalShot){
    const res  = await fetch('/api/history', { credentials:'same-origin' });
    const rows = await res.json();
    hist.innerHTML = '';
    rows.forEach(r=>{
      hist.insertAdjacentHTML('beforeend',`
        <tr><td>${r.id}</td><td>${r.utc.slice(11,19)}</td>
            <td>${r.user}</td><td>${r.animal}</td>
            <td>${(r.conf*100).toFixed(1)}%</td></tr>`);
    });
  }
}

/* ---------- инициализация ---------- */
async function initApp(){
  ws = connectWS(onResult, () => console.log('WS open'));

  const stream = await navigator.mediaDevices.getUserMedia({ video:true });
  v.srcObject = stream; await v.play();
  dots.width  = v.videoWidth; dots.height = v.videoHeight;

  const base = await FilesetResolver.forVisionTasks(
        'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.10/wasm');

  const pose = await PoseLandmarker.createFromOptions(base,{
        runningMode:'VIDEO', numPoses:1,
        baseOptions:{modelAssetPath:'/models/pose_landmarker_full.task'}});
  const hand = await HandLandmarker.createFromOptions(base,{
        runningMode:'VIDEO', numHands:2,
        baseOptions:{modelAssetPath:'/models/hand_landmarker_full.task'}});

  let last = 0;
  (function loop(ts){
    if (ts - last > 33){
      last = ts;
      const pr = pose.detectForVideo(v, ts);
      const hr = hand.detectForVideo(v, ts);
      drawDots(pr, hr);

      /* формируем вектор  (21×2 + 33) точек × 3 координаты */
      const vec = [];
      hr.landmarks.forEach(a => a.forEach(l => vec.push(l.x,l.y,l.z)));
      while (vec.length < 21*2*3) vec.push(0);

      if (pr.landmarks.length)
           pr.landmarks[0].forEach(l => vec.push(l.x,l.y,l.z));
      else
           vec.push(...Array(33*3).fill(0));   // ← исправлено “.Array”

      sendVec(new Float32Array(vec));
    }
    requestAnimationFrame(loop);
  })(0);
}

/* ---------- рисуем точки ---------- */
function drawDots(pose, hand){
  dctx.clearRect(0,0,dots.width,dots.height);
  dctx.fillStyle = '#0f0';
  hand.landmarks.forEach(a => a.forEach(pt));
  if (pose.landmarks.length) pose.landmarks[0].forEach(pt);
  function pt(l){
    dctx.beginPath();
    dctx.arc(l.x*dots.width, l.y*dots.height, 4, 0, 6.28);
    dctx.fill();
  }
}

/* ---------- логин ---------- */
function loggedIn(){
  return document.cookie.split(';')
           .some(c => c.trim().startsWith('zoo_uid='));
}
if (loggedIn()){ loginM.style.display = 'none'; initApp(); }
else{
  loginM.style.display = 'flex';
  loginB.onclick = async ()=>{
    const name = nameI.value.trim();
    if (!name) return alert('Введите имя');
    const ok = await fetch('/api/register',{
      method :'POST',
      headers:{'Content-Type':'application/json'},
      body   : JSON.stringify({ name })
    });
    if (ok.ok){ loginM.style.display ='none'; initApp(); }
    else alert('Ошибка регистрации');
  };
}