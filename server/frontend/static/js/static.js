(async ()=>{
  const res=await fetch('/api/stats');
  const data=await res.json();
  const tb=document.querySelector('#stats tbody');
  data.forEach(r=>{
    tb.insertAdjacentHTML('beforeend',`<tr><td>${r.name}</td><td>${r.shots}</td><td>${r.ok}</td></tr>`);
  });
})();