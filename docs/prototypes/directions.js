'use strict';
const places=[
 {name:'해운대 해수욕장',type:'바다 · 산책',time:'10:30',stay:'60분',description:'첫날은 가볍게. 해변을 따라 걷고 바다를 즐겨요.',image:'https://upload.wikimedia.org/wikipedia/commons/3/33/Hauendae.jpg',status:'지역 일치',hours:'방문시간 미확인',warning:false,walk:'도보 12분',distance:'750m'},
 {name:'해운대 전통시장',type:'맛집 · 시장',time:'12:00',stay:'70분',description:'취향에 맞는 점심을 고르고 시장 골목을 둘러봐요.',image:'https://upload.wikimedia.org/wikipedia/commons/5/5e/Haeundae_market_%28%ED%95%B4%EC%9A%B4%EB%8C%80%EC%8B%9C%EC%9E%A5%29.JPG',status:'지역 일치',hours:'영업시간 확인 필요',warning:true,walk:'도보 24분',distance:'1.5km'},
 {name:'동백섬',type:'자연 · 산책',time:'14:00',stay:'60분',description:'산책 뒤에는 충분히 쉬어갈 수 있도록 여유를 뒀어요.',image:'https://upload.wikimedia.org/wikipedia/commons/5/5c/Dongbaekseom_island%2C_Busan.jpg',status:'지역 일치',hours:'방문시간 미확인',warning:false}
];
const icon=name=>`<svg aria-hidden="true"><use href="#i-${name}"/></svg>`;
const escapeHtml=s=>s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
document.querySelector('#places').innerHTML=places.map((p,i)=>`<article class="stop"><div class="place-card"><div class="photo-wrap"><div class="photo-fallback" hidden>사진을 불러오지 못했어요</div><img src="${p.image}" alt="${escapeHtml(p.name)} 사진" loading="eager"><span class="index">${i+1}</span></div><div class="place-content"><div class="place-time"><span class="time">${p.time}<span class="approx">예상</span></span><span class="more" aria-hidden="true">${icon('more')}</span></div><h3>${p.name}</h3><div class="place-meta"><span>${p.type}</span><span>·</span><span class="duration">${icon('clock')}체류 ${p.stay}</span></div><p class="description">${p.description}</p><div class="badges"><span class="badge">${icon('shield')}Places · ${p.status}<span class="sample-inline">예시</span></span><span class="badge ${p.warning?'warn':'neutral'}">${p.hours}</span></div></div></div>${p.walk?`<div class="connection">${icon('route')}<span>다음 장소까지 <strong>${p.walk}</strong> · ${p.distance}</span><span class="route-source">Routes UI 예시</span></div>`:''}</article>`).join('');
document.querySelectorAll('.photo-wrap img').forEach(img=>img.addEventListener('error',()=>{img.hidden=true;img.previousElementSibling.hidden=false}));
const concepts={a:'균형형 AI 여행 도구 · 사진과 일정, 검증 정보를 함께 보는 기본안',b:'여행 탐색 중심 · 큰 사진과 열린 여백, 가벼운 consumer app',c:'일정 운영 중심 · 촘촘한 시간표와 상태, 빠른 판단을 위한 workspace'};
let view='list';const scrollPositions={list:0,map:null};
function setDesign(key){
 if(!concepts[key])key='a';
 document.body.dataset.design=key;
 document.querySelectorAll('button[data-design]').forEach(b=>b.setAttribute('aria-pressed',b.dataset.design===key));
 document.querySelector('#concept-description').textContent=concepts[key];
 document.querySelector('#announcement').textContent=key.toUpperCase()+' 디자인 선택됨';
 history.replaceState(null,'','#'+key);
}
document.querySelectorAll('button[data-design]').forEach(b=>b.addEventListener('click',()=>setDesign(b.dataset.design)));
document.querySelectorAll('button[data-view]').forEach(b=>b.addEventListener('click',()=>{
 scrollPositions[view]=window.scrollY;view=b.dataset.view;
 document.querySelector('.workspace').dataset.view=view;
 document.querySelectorAll('button[data-view]').forEach(x=>x.setAttribute('aria-pressed',x.dataset.view===view));
 requestAnimationFrame(()=>window.scrollTo({top:scrollPositions[view]??(scrollY+document.querySelector('.itinerary-nav').getBoundingClientRect().top),behavior:'instant'}));
}));
setDesign(location.hash.slice(1)||'a');