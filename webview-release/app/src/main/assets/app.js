const API = "/api/v1";
const FORCE_LOCAL_API = Boolean(window.LEXORA_FORCE_LOCAL_API);
const state = {
  token: localStorage.getItem("lexora_token") || "",
  refreshToken: localStorage.getItem("lexora_refresh") || "",
  user: null,
  courses: [],
  selectedCourseId: localStorage.getItem("lexora_course") || "",
  units: [],
  lessons: [],
  screen: "learn",
  authMode: "register",
  lesson: null,
  exerciseIndex: 0,
  selectedAnswer: "",
  checked: false,
  correctAnswers: 0,
  combo: 0,
  bestCombo: 0,
  lessonStartedAt: 0,
  lessonResult: null,
};

const icons = {
  heart: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 21C9.7 18.9 4 14.1 4 8.8 4 5.8 6.2 3.6 9.1 3.6c1.7 0 2.7.8 2.9 1.5.2-.7 1.2-1.5 2.9-1.5 2.9 0 5.1 2.2 5.1 5.2 0 5.3-5.7 10.1-8 12.2Z"/></svg>`,
  gem: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M6 3.5h12L22 9 12 21 2 9l4-5.5Zm-.6 5.5h4.1l2.5 7.1L14.5 9h4.1L16.7 6H7.3L5.4 9Z"/></svg>`,
  fire: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12.3 22C7.8 22 5 18.8 5 15.1c0-3.3 2.1-5.7 3.7-7.6C10.2 5.8 10.9 4.2 10.8 2c3.6 1.6 6.1 4.9 5.7 9.1 1-.5 1.7-1.5 2-2.9 1.5 1.9 2.5 4.2 2.5 6.9 0 3.8-3 6.9-8.7 6.9Z"/></svg>`,
  star: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="m12 2.5 2.9 6 6.6.9-4.8 4.6 1.2 6.5-5.9-3.2-5.9 3.2L7.3 14 2.5 9.4l6.6-.9L12 2.5Z"/></svg>`,
  check: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="m9.2 16.6-4.3-4.3L3.2 14l6 6L21 8.2l-1.7-1.7-10.1 10.1Z"/></svg>`,
  lock: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M7 10V8c0-2.8 2.2-5 5-5s5 2.2 5 5v2h2v11H5V10h2Zm2.5 0h5V8c0-1.4-1.1-2.5-2.5-2.5S9.5 6.6 9.5 8v2Z"/></svg>`,
  trophy: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M7 3h10v2h4v3.4c0 2.9-2.1 4.9-4.8 5.4-.6 1.5-1.7 2.6-3.2 3V19h4v2H7v-2h4v-2.2c-1.5-.4-2.6-1.5-3.2-3C5.1 13.3 3 11.3 3 8.4V5h4V3Z"/></svg>`,
  book: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 4c1.5-.8 3.5-1 6 .2V20c-2.1-1.1-4.1-1.1-6 0V4Zm8 .2c2.5-1.2 4.5-1 6-.2v16c-1.9-1.1-3.9-1.1-6 0V4.2Z"/></svg>`,
  shop: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 7h14l-1 14H6L5 7Zm2-4h10l2 3H5l2-3Zm3 8v2h4v-2h-4Z"/></svg>`,
  user: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 12c-2.5 0-4.5-2-4.5-4.5S9.5 3 12 3s4.5 2 4.5 4.5S14.5 12 12 12ZM4 21c.8-4.2 3.9-6.7 8-6.7s7.2 2.5 8 6.7H4Z"/></svg>`,
  quests: `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2 4 5v6.5c0 5.1 3.4 8.6 8 10.5 4.6-1.9 8-5.4 8-10.5V5l-8-3Zm-1 13.2-3.2-3.2 1.5-1.5 1.7 1.7 4-4 1.5 1.5-5.5 5.5Z"/></svg>`
};

function noriSvg(){return `<div class="nori"><svg viewBox="0 0 220 220"><defs><linearGradient id="noriG" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#ff8fb3"/><stop offset="1" stop-color="#7cf4c9"/></linearGradient></defs><path d="M47 88c-31-30-12-62 22-39M173 88c31-30 12-62-22-39" fill="none" stroke="#ff5e9d" stroke-width="14" stroke-linecap="round"/><rect x="48" y="54" width="124" height="104" rx="44" fill="url(#noriG)"/><ellipse cx="110" cy="139" rx="35" ry="28" fill="#ffc8d7" opacity=".86"/><circle cx="84" cy="100" r="8" fill="#10203a"/><circle cx="136" cy="100" r="8" fill="#10203a"/><path d="M86 126c14 18 34 18 48 0" fill="none" stroke="#10203a" stroke-width="6" stroke-linecap="round"/><path d="M67 151l-31 23M153 151l31 23" stroke="#e95c8b" stroke-width="13" stroke-linecap="round"/><circle cx="75" cy="175" r="13" fill="#ff8fb3"/><circle cx="145" cy="175" r="13" fill="#ff8fb3"/></svg></div>`}

async function api(path, options = {}) {
  if (FORCE_LOCAL_API) return localApi(path, options);
  const headers = {"content-type":"application/json", ...(options.headers || {})};
  if (state.token) headers.authorization = `Bearer ${state.token}`;
  try {
    const res = await fetch(`${API}${path}`, {...options, headers});
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw data.error || data;
    return data.data;
  } catch (error) {
    if (window.LEXORA_STATIC_DATA) return localApi(path, options);
    throw error;
  }
}

async function localApi(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const body = options.body ? JSON.parse(options.body) : {};
  const data = window.LEXORA_STATIC_DATA;
  const db = readLocalDb();
  const currentUser = () => db.users.find(user => `local_${user.id}` === state.token);

  if (method === "GET" && path === "/health") return {status:"ok", service:"lexora-local-api", version:"0.3.0"};
  if (method === "GET" && path === "/courses") return data.courses;
  if (method === "GET" && /^\/courses\/[^/]+\/units$/.test(path)) {
    const courseId = path.split("/")[2];
    return data.units.filter(unit => unit.courseId === courseId).map(unit => ({...unit, lessons: data.lessons.filter(lesson => lesson.unitId === unit.id)}));
  }
  if (method === "GET" && /^\/lessons\/[^/]+$/.test(path)) {
    const lessonId = path.split("/")[2];
    const lesson = data.lessons.find(item => item.id === lessonId);
    if (!lesson) throw {message:"Урок не найден"};
    return {lesson, exercises: data.exercises.filter(exercise => exercise.lessonId === lessonId)};
  }
  if (method === "POST" && path === "/auth/register") {
    const email = String(body.email || "").trim().toLowerCase();
    const password = String(body.password || "");
    const username = String(body.username || "").trim().toLowerCase();
    if (!/^\S+@\S+\.\S+$/.test(email)) throw {message:"Некорректный email"};
    if (password.length < 8) throw {message:"Пароль должен быть от 8 символов"};
    if (db.users.some(user => user.email === email)) throw {message:"Email уже зарегистрирован"};
    const user = {id: cryptoRandom(), email, username: username || `u_${Date.now()}`, displayName: body.displayName || username || "Lexora User", gems:100, hearts:5, totalXp:0, currentStreak:0, longestStreak:0, createdAt:new Date().toISOString(), password};
    db.users.push(user); ensureLocalQuests(db, user.id); writeLocalDb(db);
    return localAuthResponse(user);
  }
  if (method === "POST" && path === "/auth/login") {
    const email = String(body.email || "").trim().toLowerCase();
    const user = db.users.find(item => item.email === email && item.password === String(body.password || ""));
    if (!user) throw {message:"Неверный email или пароль"};
    return localAuthResponse(user);
  }
  if (method === "POST" && path === "/auth/logout") return {loggedOut:true};
  if (method === "GET" && path === "/users/me") {
    const user = currentUser(); if (!user) throw {message:"Нужно войти"}; return publicLocalUser(user);
  }
  if (method === "POST" && /^\/lessons\/[^/]+\/complete$/.test(path)) {
    const user = currentUser(); if (!user) throw {message:"Нужно войти"};
    const lessonId = path.split("/")[2];
    const earnedXp = Math.max(1, Math.round(Number(body.score || 10) * Number(body.accuracy || 0)) + (Number(body.accuracy || 0) === 1 ? 5 : 0));
    user.totalXp += earnedXp; user.gems += Math.max(1, Math.floor(earnedXp / 3)); user.currentStreak = Math.max(1, user.currentStreak); user.longestStreak = Math.max(user.longestStreak, user.currentStreak);
    const progress = db.progress.find(item => item.userId === user.id && item.lessonId === lessonId) || {userId:user.id, lessonId};
    Object.assign(progress, {status:Number(body.accuracy||0)===1?"Legendary":"Completed", score:earnedXp, accuracy:Number(body.accuracy||0), bestCombo:Number(body.bestCombo||0), updatedAt:new Date().toISOString()});
    if (!db.progress.includes(progress)) db.progress.push(progress);
    updateLocalQuests(db, user.id, earnedXp, Number(body.accuracy || 0)); updateLocalLeaderboard(db, user, earnedXp); writeLocalDb(db);
    return {earnedXp, earnedGems:Math.max(1, Math.floor(earnedXp/3)), streakUpdated:true, unlockedAchievementIds:[], nextLessonId:null};
  }
  if (method === "GET" && path === "/quests/daily") { const user=currentUser(); if(!user) throw {message:"Нужно войти"}; const quests=ensureLocalQuests(db,user.id); writeLocalDb(db); return quests; }
  if (method === "GET" && path === "/shop/items") return data.shopItems;
  if (method === "POST" && /^\/shop\/buy\//.test(path)) { const user=currentUser(); if(!user) throw {message:"Нужно войти"}; const item=data.shopItems.find(x=>x.id===path.split('/')[3]); if(!item) throw {message:"Товар не найден"}; if(user.gems<item.priceGems) throw {message:"Не хватает кристаллов"}; user.gems-=item.priceGems; if(item.type==='Hearts') user.hearts=5; writeLocalDb(db); return {itemId:item.id, quantity:1, remainingGems:user.gems}; }
  if (method === "GET" && /^\/leaderboard\//.test(path)) return db.leaderboard.sort((a,b)=>b.weeklyXp-a.weeklyXp).map((entry,index)=>({...entry, rank:index+1})).slice(0,30);
  if (method === "GET" && path === "/achievements") { const user=currentUser(); if(!user) throw {message:"Нужно войти"}; return data.achievements.map(a => ({...a, progress: achievementProgress(a, user, db), unlockedAtEpochMillis: achievementProgress(a,user,db) >= a.target ? Date.now() : null})); }
  if (method === "GET" && path.startsWith("/dictionary")) return data.words.slice(0,100);
  if (method === "POST" && path === "/srs/review") return {wordId:body.wordId, nextDueEpochMillis:Date.now()+86400000, easeFactor:2.5, intervalDays:1};
  if (method === "GET" && path === "/friends") return [];
  throw {message:`Локальный API не знает маршрут ${method} ${path}`};
}

function readLocalDb(){try{return JSON.parse(localStorage.getItem('lexora_local_db')||'')}catch{return {users:[],progress:[],quests:[],leaderboard:[]}}}
function writeLocalDb(db){localStorage.setItem('lexora_local_db', JSON.stringify(db))}
function cryptoRandom(){return (window.crypto && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}_${Math.random().toString(16).slice(2)}`)}
function publicLocalUser(user){return {id:user.id,email:user.email,username:user.username,displayName:user.displayName,bio:null,avatarUrl:null,interfaceLanguage:'ru',gems:user.gems,hearts:user.hearts,totalXp:user.totalXp,currentStreak:user.currentStreak,longestStreak:user.longestStreak,createdAt:user.createdAt}}
function localAuthResponse(user){return {user:publicLocalUser(user),tokens:{accessToken:`local_${user.id}`,refreshToken:`refresh_${user.id}`,expiresAtEpochMillis:Date.now()+999999999}}}
function ensureLocalQuests(db,userId){const day=new Date().toISOString().slice(0,10);let q=db.quests.filter(x=>x.userId===userId&&x.day===day);if(q.length===3)return q;q=[{id:`q_${userId}_${day}_xp`,userId,day,title:'Заработай 30 XP',description:'Получи опыт за уроки',target:30,progress:0,rewardXp:10,rewardGems:10,status:'Active'},{id:`q_${userId}_${day}_lessons`,userId,day,title:'Пройди 2 урока',description:'Заверши любые два урока',target:2,progress:0,rewardXp:15,rewardGems:15,status:'Active'},{id:`q_${userId}_${day}_acc`,userId,day,title:'Точность 80%',description:'Закончи урок с точностью от 80%',target:1,progress:0,rewardXp:20,rewardGems:20,status:'Active'}];db.quests=db.quests.filter(x=>!(x.userId===userId&&x.day===day)).concat(q);return q}
function updateLocalQuests(db,userId,xp,accuracy){ensureLocalQuests(db,userId).forEach(q=>{if(q.id.includes('_xp'))q.progress=Math.min(q.target,q.progress+xp);else if(q.id.includes('_lessons'))q.progress=Math.min(q.target,q.progress+1);else if(accuracy>=.8)q.progress=1;if(q.progress>=q.target)q.status='Completed'})}
function updateLocalLeaderboard(db,user,xp){let e=db.leaderboard.find(x=>x.userId===user.id);if(!e){e={userId:user.id,username:user.username,displayName:user.displayName,avatarUrl:null,leagueId:'Bronze',weeklyXp:0,rank:1,weekStart:new Date().toISOString()};db.leaderboard.push(e)}e.weeklyXp+=xp}
function achievementProgress(a,user,db){if(a.code?.startsWith('wildfire'))return user.currentStreak;if(a.code?.startsWith('sage'))return user.totalXp;if(a.code?.startsWith('scholar'))return db.progress.filter(p=>p.userId===user.id).length;if(a.code==='sharpshooter')return db.progress.some(p=>p.userId===user.id&&p.accuracy===1)?1:0;return 0}

function saveTokens(tokens){state.token=tokens.accessToken;state.refreshToken=tokens.refreshToken;localStorage.setItem("lexora_token",state.token);localStorage.setItem("lexora_refresh",state.refreshToken)}
function clearTokens(){state.token="";state.refreshToken="";state.user=null;localStorage.removeItem("lexora_token");localStorage.removeItem("lexora_refresh")}
function el(id){return document.getElementById(id)}
function escapeHtml(v){return String(v ?? "").replace(/[&<>"]/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;"}[c]))}
function setScreen(screen){state.screen=screen;renderApp(); if(screen!=="learn") loadScreenData(screen)}

async function boot(){
  try{
    await api("/health");
    if(state.token){try{state.user=await api("/users/me")}catch{clearTokens()}}
    if(state.user){await loadCourses()}
  }catch(e){console.error(e)}
  render();
}

function render(){ state.user ? renderApp() : renderAuth(); }

function renderAuth(message=""){
  el("app").innerHTML = `<main class="main" style="min-height:100dvh;padding-top:calc(var(--safe-top) + 30px);display:grid;align-content:center">
    <section class="card mascot-card">
      <div class="row">${noriSvg()}<div><h1 class="title">Lexora</h1><p class="subtitle">Учись как в любимом language app: путь уроков, жизни, стрик, XP, квесты и лиги — но с нашим оригинальным дизайном.</p></div></div>
    </section>
    <section class="card">
      <div class="tabs"><button class="tab ${state.authMode==='register'?'active':''}" id="tabRegister">Регистрация</button><button class="tab ${state.authMode==='login'?'active':''}" id="tabLogin">Вход</button></div>
      <div class="col">
        <input class="input" id="email" inputmode="email" placeholder="Email" value="${localStorage.getItem('last_email')||''}">
        ${state.authMode==='register'?`<input class="input" id="username" placeholder="Username"><input class="input" id="displayName" placeholder="Имя">`:""}
        <input class="input" id="password" type="password" placeholder="Пароль, например Lexora2026">
        ${message?`<div class="${message.startsWith('OK')?'success':'error'}">${escapeHtml(message)}</div>`:""}
        <button class="big-button" id="authSubmit">${state.authMode==='register'?'Создать аккаунт':'Войти'}</button>
      </div>
    </section>
  </main>`;
  el("tabRegister").onclick=()=>{state.authMode='register';renderAuth()};
  el("tabLogin").onclick=()=>{state.authMode='login';renderAuth()};
  el("authSubmit").onclick=submitAuth;
}

async function submitAuth(){
  const email=el("email").value.trim(); const password=el("password").value;
  localStorage.setItem('last_email', email);
  try{
    const payload = state.authMode==='register'
      ? {email,password,username:el("username").value.trim() || `u_${Date.now().toString().slice(-6)}`,displayName:el("displayName").value.trim() || "Lexora User"}
      : {email,password};
    const data = await api(state.authMode==='register'?"/auth/register":"/auth/login", {method:"POST", body:JSON.stringify(payload)});
    saveTokens(data.tokens); state.user=data.user; await loadCourses(); renderApp();
  }catch(e){renderAuth(e.message || "Ошибка авторизации")}
}

async function loadCourses(){
  state.courses = await api("/courses");
  if(!state.selectedCourseId && state.courses[0]) state.selectedCourseId=state.courses[0].id;
  localStorage.setItem("lexora_course", state.selectedCourseId);
  await loadUnits();
}
async function loadUnits(){
  if(!state.selectedCourseId) return;
  state.units = await api(`/courses/${state.selectedCourseId}/units`);
  state.lessons = state.units.flatMap(u => u.lessons.map(l => ({...l, unitTitle:u.title})));
}

function renderApp(){
  el("app").innerHTML = `<div class="app-layout"><header class="top-bar">${topStats()}</header><main class="main" id="main">${renderScreen()}</main>${bottomNav()}</div>`;
  bindCommon();
}
function topStats(){
  const course = state.courses.find(c=>c.id===state.selectedCourseId);
  return `<div class="stats"><div class="flag-pill">▰ ${course?escapeHtml(course.language.nativeName):'Курс'}</div><div class="stat" style="color:var(--orange)">${icons.fire}${state.user?.currentStreak||0}</div><div class="stat" style="color:var(--gem)">${icons.gem}${state.user?.gems??0}</div><div class="stat" style="color:var(--red)">${icons.heart}${state.user?.hearts??5}</div></div>`;
}
function bottomNav(){const items=[['learn','Учить',icons.book],['leaderboard','Лиги',icons.trophy],['quests','Квесты',icons.quests],['shop','Магазин',icons.shop],['profile','Профиль',icons.user]];return `<nav class="bottom-nav">${items.map(([key,label,icon])=>`<button class="nav-item ${state.screen===key?'active':''}" data-nav="${key}">${icon}<span>${label}</span></button>`).join('')}</nav>`}
function bindCommon(){document.querySelectorAll('[data-nav]').forEach(b=>b.onclick=()=>setScreen(b.dataset.nav)); const select=el('courseSelect'); if(select) select.onchange=async()=>{state.selectedCourseId=select.value;localStorage.setItem('lexora_course',state.selectedCourseId);await loadUnits();renderApp()}; document.querySelectorAll('[data-lesson]').forEach(b=>b.onclick=()=>startLesson(b.dataset.lesson));}
function renderScreen(){
  if(state.screen==='learn') return renderLearn();
  if(state.screen==='leaderboard') return renderLeaderboardShell();
  if(state.screen==='quests') return renderQuestsShell();
  if(state.screen==='shop') return renderShopShell();
  if(state.screen==='profile') return renderProfileShell();
  return renderLearn();
}
function renderLearn(){
  return `<section class="card mascot-card"><div class="row">${noriSvg()}<div class="grow"><h1 class="title">Путь обучения</h1><p class="subtitle">Жми на текущий узел и проходи уроки. Прогресс, XP и квесты сохраняются через наш API.</p></div></div></section>
  <select class="course-select" id="courseSelect">${state.courses.map(c=>`<option value="${c.id}" ${c.id===state.selectedCourseId?'selected':''}>${escapeHtml(c.language.nativeName)} — ${escapeHtml(c.title)}</option>`).join('')}</select>
  <h2 class="section-title">Unit path</h2><section class="path">${state.lessons.map((l,i)=>nodeHtml(l,i)).join('')}</section>`;
}
function nodeHtml(lesson,i){const status=i===0?'current':i<3?'':'locked'; const cls=i%7===6?'legend':status; const icon=cls==='locked'?icons.lock:cls==='legend'?icons.trophy:i<3?icons.check:icons.star; return `<div class="node-wrap"><button class="lesson-node ${cls}" ${cls==='locked'?'disabled':''} data-lesson="${lesson.id}">${icon}</button><div class="node-label">${escapeHtml(lesson.title)}</div></div>`}

async function startLesson(id){
  try{state.lesson=await api(`/lessons/${id}`);state.exerciseIndex=0;state.selectedAnswer='';state.checked=false;state.correctAnswers=0;state.combo=0;state.bestCombo=0;state.lessonStartedAt=Date.now();state.lessonResult=null;renderLesson()}catch(e){alert(e.message||'Не удалось открыть урок')}
}
function currentExercise(){return state.lesson.exercises[state.exerciseIndex]}
function exerciseAnswer(ex){return ex.payload?.answer || ex.payload?.options?.[0] || ''}
function renderLesson(){
  const ex=currentExercise(); const progress=(state.exerciseIndex)/(state.lesson.exercises.length);
  el('app').innerHTML=`<div class="lesson-screen"><header class="lesson-top"><button class="close" id="closeLesson">×</button><div class="progress-line grow"><span style="width:${Math.round(progress*100)}%"></span></div><div class="stat" style="color:var(--red)">${icons.heart}${state.user?.hearts??5}</div></header><main class="exercise">${exerciseHtml(ex)}</main><footer class="lesson-bottom">${feedbackHtml(ex)}<button class="big-button" id="checkBtn">${state.checked?'Продолжить':'Проверить'}</button></footer></div>`;
  el('closeLesson').onclick=()=>renderApp();
  document.querySelectorAll('[data-answer]').forEach(b=>b.onclick=()=>{if(!state.checked){state.selectedAnswer=b.dataset.answer;renderLesson()}});
  el('checkBtn').onclick=()=>state.checked?continueLesson():checkAnswer();
}
function exerciseHtml(ex){
  const opts=[...(ex.payload?.options||[])];
  return `<div><div class="pill">${escapeHtml(ex.type)}</div><h2>${escapeHtml(ex.prompt)}</h2></div><div class="options">${opts.map(o=>optionHtml(o,ex)).join('')}</div><p class="subtitle">${ex.type==='Pronunciation'?'Произнеси фразу вслух, затем выбери её в списке.':'Выбери правильный вариант.'}</p>`
}
function optionHtml(option,ex){let cls=''; if(state.selectedAnswer===option) cls='selected'; if(state.checked&&option===exerciseAnswer(ex)) cls='correct'; if(state.checked&&state.selectedAnswer===option&&option!==exerciseAnswer(ex)) cls='wrong'; return `<button class="option ${cls}" data-answer="${escapeHtml(option)}">${escapeHtml(option)}</button>`}
function feedbackHtml(ex){if(!state.checked)return ''; const good=state.selectedAnswer===exerciseAnswer(ex); return `<div class="feedback ${good?'good':'bad'}">${good?'Отлично!':'Правильный ответ: '+escapeHtml(exerciseAnswer(ex))}<br><small>${escapeHtml(ex.explanation||'')}</small></div>`}
function checkAnswer(){if(!state.selectedAnswer){navigator.vibrate?.(40);return} const good=state.selectedAnswer===exerciseAnswer(currentExercise()); if(good){state.correctAnswers++;state.combo++;state.bestCombo=Math.max(state.bestCombo,state.combo);navigator.vibrate?.(18)}else{state.combo=0;navigator.vibrate?.([40,40,40])} state.checked=true;renderLesson()}
async function continueLesson(){if(state.exerciseIndex+1<state.lesson.exercises.length){state.exerciseIndex++;state.selectedAnswer='';state.checked=false;renderLesson()}else await finishLesson()}
async function finishLesson(){
  const total=state.lesson.exercises.length; const accuracy=state.correctAnswers/total; const durationSeconds=Math.round((Date.now()-state.lessonStartedAt)/1000);
  try{state.lessonResult=await api(`/lessons/${state.lesson.lesson.id}/complete`,{method:'POST',body:JSON.stringify({score:state.lesson.lesson.xpReward,accuracy,bestCombo:state.bestCombo,durationSeconds,mistakes:[]})}); state.user=await api('/users/me')}catch(e){state.lessonResult={earnedXp:Math.round(state.lesson.lesson.xpReward*accuracy), offline:true,error:e.message}}
  renderResult(accuracy,durationSeconds,total);
}
function renderResult(accuracy,durationSeconds,total){el('app').innerHTML=`<main class="main" style="min-height:100dvh;display:grid;align-content:center"><section class="card" style="text-align:center"><p class="result-stars">${accuracy===1?'★★★':'★★☆'}</p><h1 class="title">Урок завершён!</h1><p class="subtitle">XP: <b>${state.lessonResult.earnedXp}</b> • Точность: <b>${Math.round(accuracy*100)}%</b> • Время: <b>${durationSeconds}s</b></p><div class="progress-line"><span style="width:${Math.round(accuracy*100)}%"></span></div><button class="big-button" id="backLearn" style="margin-top:18px">Продолжить</button></section></main>`;el('backLearn').onclick=()=>{state.screen='learn';loadCourses().then(renderApp)}}

function renderLeaderboardShell(){loadScreenData('leaderboard');return `<h1 class="title">Бронзовая лига</h1><p class="subtitle">Топ недели обновляется через API.</p><section class="card" id="leaderboardBox"><div class="empty">Загрузка...</div></section>`}
function renderQuestsShell(){loadScreenData('quests');return `<h1 class="title">Ежедневные квесты</h1><p class="subtitle">3 задания каждый день.</p><section class="card" id="questsBox"><div class="empty">Загрузка...</div></section>`}
function renderShopShell(){loadScreenData('shop');return `<h1 class="title">Магазин</h1><p class="subtitle">Покупки только за заработанные кристаллы.</p><section class="card" id="shopBox"><div class="empty">Загрузка...</div></section>`}
function renderProfileShell(){loadScreenData('profile');return `<section class="card mascot-card"><div class="row"><div class="avatar">${escapeHtml((state.user?.displayName||'U')[0])}</div><div class="grow"><h1 class="title">${escapeHtml(state.user?.displayName||'User')}</h1><p class="subtitle">@${escapeHtml(state.user?.username||'user')} • ${state.user?.totalXp||0} XP</p></div><button class="tiny-button" id="logoutBtn">Выйти</button></div></section><section class="card" id="achievementsBox"><div class="empty">Загрузка...</div></section>`}
async function loadScreenData(screen){
  try{
    if(screen==='leaderboard'){const data=await api('/leaderboard/Bronze'); const box=el('leaderboardBox'); if(box) box.innerHTML=data.length?data.map((e,i)=>`<div class="league-row"><div class="league-rank ${i<3?'top':''}">${e.rank}</div><div class="avatar">${escapeHtml((e.displayName||e.username||'U')[0])}</div><div class="grow"><b>${escapeHtml(e.displayName||e.username)}</b><p class="subtitle">@${escapeHtml(e.username)}</p></div><div class="pill">${e.weeklyXp} XP</div></div>`).join(''):`<div class="empty">Пройди урок, чтобы попасть в лигу.</div>`}
    if(screen==='quests'){const data=await api('/quests/daily'); const box=el('questsBox'); if(box) box.innerHTML=data.map(q=>`<div class="quest-row"><div class="shop-icon">${icons.quests}</div><div class="grow"><b>${escapeHtml(q.title)}</b><p class="subtitle">${escapeHtml(q.description)}</p><div class="progress-line"><span style="width:${Math.round(q.progress/q.target*100)}%"></span></div></div><div class="pill">${q.progress}/${q.target}</div></div>`).join('')}
    if(screen==='shop'){const data=await api('/shop/items'); const box=el('shopBox'); if(box) box.innerHTML=data.map(item=>`<div class="shop-row"><div class="shop-icon">${icons.gem}</div><div class="grow"><b>${escapeHtml(item.title)}</b><p class="subtitle">${escapeHtml(item.description)}</p></div><button class="tiny-button" data-buy="${item.id}">${item.priceGems} gems</button></div>`).join(''); document.querySelectorAll('[data-buy]').forEach(b=>b.onclick=async()=>{try{await api(`/shop/buy/${b.dataset.buy}`,{method:'POST'});state.user=await api('/users/me');renderApp()}catch(e){alert(e.message||'Не удалось купить')}})}
    if(screen==='profile'){const data=await api('/achievements'); const box=el('achievementsBox'); if(box) box.innerHTML=`<h2 class="section-title">Достижения</h2>`+data.map(a=>`<div class="achievement-row"><div class="shop-icon">${icons.star}</div><div class="grow"><b>${escapeHtml(a.title)}</b><p class="subtitle">${escapeHtml(a.description)}</p><div class="progress-line"><span style="width:${Math.round(a.progress/a.target*100)}%"></span></div></div><div class="pill">${a.progress}/${a.target}</div></div>`).join(''); const logout=el('logoutBtn'); if(logout) logout.onclick=()=>{clearTokens();renderAuth('OK: сессия очищена')}}
  }catch(e){const box=el(screen+'Box'); if(box) box.innerHTML=`<div class="error">${escapeHtml(e.message||'Ошибка API')}</div>`}
}

boot();