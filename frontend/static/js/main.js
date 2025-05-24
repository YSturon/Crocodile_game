import {FilesetResolver, PoseLandmarker, HandLandmarker}
  from 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.10/+esm';

const v      = document.getElementById('video');
const dots   = document.getElementById('dots');
const dctx   = dots.getContext('2d');
const st     = document.getElementById('status');
const hist   = document.querySelector('#hist tbody');
const yesNo  = document.getElementById('yesNo');
const yesBtn = document.getElementById('yes');
const noBtn  = document.getElementById('no');

const loginMask = document.getElementById('loginMask');
const nameInput = document.getElementById('nameInput');
const loginBtn  = document.getElementById('loginBtn');

const ioSock = io({autoConnect:false});

/* ───────── login ───────── */
function haveCookie() {
  return document.cookie.split(';').some(c => c.trim().startsWith('zoo_uid='));
}
if (!haveCookie()) {
  loginMask.style.display = 'flex';
  loginBtn.onclick = async () => {
    const name = nameInput.value.trim();
    if (!name) return alert('Введите имя');
    await fetch('/api/register', {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body:JSON.stringify({name})
    });
  loginMask.style.display = 'none';
  ioSock.connect();                          // ② открываем WS
  initApp();                                 // ③ камера + landmarks              // продолжить загрузку
  };
} else {
  loginMask.style.display = 'none';
  initApp();
}

/* ───────── основное приложение ───────── */
async function initApp() {
  /* камера */
  const stream = await navigator.mediaDevices.getUserMedia({video:true});
  v.srcObject = stream;
  await new Promise(r => v.onloadedmetadata = r);
  dots.width  = v.videoWidth;
  dots.height = v.videoHeight;

  /* landmarkers */
  const base = await FilesetResolver.forVisionTasks(
      'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.10/wasm');
  const POSE_MODEL = '/static/models/pose_landmarker_full.task';
  const HAND_MODEL = '/static/models/hand_landmarker_full.task';

  const poseLm = await PoseLandmarker.createFromOptions(base,{
        runningMode:'VIDEO',numPoses:1,
        baseOptions:{modelAssetPath:POSE_MODEL}});
  const handLm = await HandLandmarker.createFromOptions(base,{
        runningMode:'VIDEO',numHands:2,
        baseOptions:{modelAssetPath:HAND_MODEL}});

  /* цикл */
  let last=0;
  function loop(ts){
    if (ts-last>33){
      last=ts;
      const pose = poseLm.detectForVideo(v,ts);
      const hand = handLm.detectForVideo(v,ts);

      drawDots(pose,hand);

      const vec=[];
      hand.landmarks.forEach(arr=>arr.forEach(lm=>vec.push(lm.x,lm.y,lm.z)));
      while(vec.length<21*2*3) vec.push(0);        // до 2 рук
      if (pose.landmarks.length)
        pose.landmarks[0].forEach(lm=>vec.push(lm.x,lm.y,lm.z));
      else vec.push(...Array(33*3).fill(0));

      ioSock.emit('landmarks', new Float32Array(vec).buffer);
    }
    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
}

/* ───────── dot render ───────── */
function drawDots(pose,hand){
  dctx.clearRect(0,0,dots.width,dots.height);
  dctx.fillStyle='#0f0';
  hand.landmarks.forEach(arr=>arr.forEach(l=>dot(l)));
  if (pose.landmarks.length) pose.landmarks[0].forEach(l=>dot(l));
}
function dot(lm){
  dctx.beginPath();
  dctx.arc(lm.x*dots.width, lm.y*dots.height, 5, 0, Math.PI*2);
  dctx.fill();
}

/* ───────── server events ───────── */
ioSock.on('result', d=>{
  st.textContent=`🔎 ${d.class} (${(d.confidence*100).toFixed(1)}%)`;
  yesNo.style.display='block';
  refreshHist();
});
yesBtn.onclick = () => sendAnswer(true);
noBtn.onclick  = () => sendAnswer(false);

async function sendAnswer(val){
  await fetch('/api/answer',{
    method:'POST',
    headers:{'Content-Type':'application/json'},
    body:JSON.stringify({val})
  });
  yesNo.style.display='none';
  refreshHist();
}

async function refreshHist(){
  const r=await fetch('/api/stats'); const data=await r.json();
  hist.innerHTML='';
  data.forEach((it,i)=>{
    const tr=document.createElement('tr');
    tr.innerHTML=`<td>${i+1}</td><td>${new Date().toLocaleTimeString()}</td>
      <td>${it.animal||'-'}</td><td>${it.ok}/${it.shots}</td>`;
    hist.appendChild(tr);
  });
}
