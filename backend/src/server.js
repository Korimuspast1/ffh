import http from "node:http";
import { promises as fs } from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";
import { content, achievements, shopItems } from "./content.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");
const PUBLIC_DIR = path.join(ROOT, "public");
const STORAGE_DIR = path.join(ROOT, "storage");
const DB_FILE = path.join(STORAGE_DIR, "lexora-db.json");
const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.HOST || "0.0.0.0";
const JWT_SECRET = process.env.JWT_SECRET || "lexora-local-development-secret-change-in-production";
const ACCESS_TOKEN_TTL_SECONDS = 60 * 60 * 6;
const REFRESH_TOKEN_TTL_SECONDS = 60 * 60 * 24 * 30;

const jsonHeaders = {
  "content-type": "application/json; charset=utf-8",
  "access-control-allow-origin": "*",
  "access-control-allow-methods": "GET,POST,PATCH,DELETE,OPTIONS",
  "access-control-allow-headers": "content-type,authorization",
  "access-control-max-age": "86400"
};

let db = await loadDb();
const sockets = new Set();

function nowIso() {
  return new Date().toISOString();
}

function requestId() {
  return crypto.randomUUID();
}

async function loadDb() {
  await fs.mkdir(STORAGE_DIR, { recursive: true });
  try {
    const raw = await fs.readFile(DB_FILE, "utf8");
    return JSON.parse(raw);
  } catch {
    const initial = {
      users: [],
      refreshTokens: [],
      progress: [],
      quests: [],
      inventory: [],
      srsCards: [],
      friends: [],
      leaderboard: [],
      completedMistakes: []
    };
    await saveDb(initial);
    return initial;
  }
}

async function saveDb(next = db) {
  await fs.mkdir(STORAGE_DIR, { recursive: true });
  await fs.writeFile(DB_FILE, JSON.stringify(next, null, 2));
}

function send(res, status, payload, headers = {}) {
  res.writeHead(status, { ...jsonHeaders, ...headers });
  res.end(JSON.stringify(payload));
}

function ok(res, data, status = 200) {
  send(res, status, { data, requestId: requestId() });
}

function fail(res, status, code, message, details = undefined) {
  send(res, status, { error: { code, message, details }, requestId: requestId() });
}

async function readJson(req) {
  const chunks = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > 1_000_000) throw new HttpError(413, "PAYLOAD_TOO_LARGE", "Payload is too large");
    chunks.push(chunk);
  }
  if (chunks.length === 0) return {};
  const raw = Buffer.concat(chunks).toString("utf8");
  try {
    return JSON.parse(raw);
  } catch {
    throw new HttpError(400, "INVALID_JSON", "Request body must be valid JSON");
  }
}

class HttpError extends Error {
  constructor(status, code, message, details = undefined) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

function base64url(input) {
  return Buffer.from(input).toString("base64url");
}

function signToken(payload, ttlSeconds) {
  const header = { alg: "HS256", typ: "JWT" };
  const now = Math.floor(Date.now() / 1000);
  const body = { ...payload, iat: now, exp: now + ttlSeconds, iss: "lexora-api" };
  const encodedHeader = base64url(JSON.stringify(header));
  const encodedBody = base64url(JSON.stringify(body));
  const signature = crypto.createHmac("sha256", JWT_SECRET).update(`${encodedHeader}.${encodedBody}`).digest("base64url");
  return `${encodedHeader}.${encodedBody}.${signature}`;
}

function verifyToken(token) {
  const parts = token?.split(".");
  if (!parts || parts.length !== 3) throw new HttpError(401, "UNAUTHORIZED", "Invalid token");
  const [encodedHeader, encodedBody, signature] = parts;
  const expected = crypto.createHmac("sha256", JWT_SECRET).update(`${encodedHeader}.${encodedBody}`).digest("base64url");
  if (!crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expected))) throw new HttpError(401, "UNAUTHORIZED", "Invalid token signature");
  const payload = JSON.parse(Buffer.from(encodedBody, "base64url").toString("utf8"));
  if (payload.exp < Math.floor(Date.now() / 1000)) throw new HttpError(401, "TOKEN_EXPIRED", "Token expired");
  return payload;
}

function hashPassword(password, salt = crypto.randomBytes(16).toString("base64url")) {
  const hash = crypto.scryptSync(password, salt, 64).toString("base64url");
  return { salt, hash };
}

function verifyPassword(password, user) {
  const { hash } = hashPassword(password, user.salt);
  return crypto.timingSafeEqual(Buffer.from(hash), Buffer.from(user.passwordHash));
}

function validateEmail(email) {
  const normalized = String(email || "").trim().toLowerCase();
  if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(normalized)) throw new HttpError(422, "INVALID_EMAIL", "Email format is invalid");
  return normalized;
}

function validatePassword(password) {
  const value = String(password || "");
  if (value.length < 8 || !/[a-z]/.test(value) || !/[A-Z]/.test(value) || !/[0-9]/.test(value)) {
    throw new HttpError(422, "WEAK_PASSWORD", "Password must be 8+ chars and contain uppercase, lowercase and digit");
  }
  return value;
}

function publicUser(user) {
  return {
    id: user.id,
    email: user.email,
    username: user.username,
    displayName: user.displayName,
    bio: user.bio || null,
    avatarUrl: user.avatarUrl || null,
    interfaceLanguage: user.interfaceLanguage || "ru",
    gems: user.gems,
    hearts: user.hearts,
    totalXp: user.totalXp,
    currentStreak: user.currentStreak,
    longestStreak: user.longestStreak,
    createdAt: user.createdAt
  };
}

function createAuthResponse(user) {
  const accessToken = signToken({ sub: user.id, type: "access" }, ACCESS_TOKEN_TTL_SECONDS);
  const refreshToken = signToken({ sub: user.id, type: "refresh", jti: crypto.randomUUID() }, REFRESH_TOKEN_TTL_SECONDS);
  db.refreshTokens.push({ token: refreshToken, userId: user.id, expiresAt: Date.now() + REFRESH_TOKEN_TTL_SECONDS * 1000, revoked: false });
  return {
    user: publicUser(user),
    tokens: { accessToken, refreshToken, expiresAtEpochMillis: Date.now() + ACCESS_TOKEN_TTL_SECONDS * 1000 }
  };
}

function auth(req) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : "";
  const payload = verifyToken(token);
  const user = db.users.find((item) => item.id === payload.sub);
  if (!user) throw new HttpError(401, "UNAUTHORIZED", "User does not exist");
  return user;
}

function todayKey() {
  return new Date().toISOString().slice(0, 10);
}

function ensureDailyQuests(userId) {
  const key = todayKey();
  const existing = db.quests.filter((quest) => quest.userId === userId && quest.day === key);
  if (existing.length === 3) return existing;
  db.quests = db.quests.filter((quest) => !(quest.userId === userId && quest.day === key));
  const quests = [
    { id: `quest_${userId}_${key}_xp`, userId, day: key, kind: "EarnXp", title: "Заработай 30 XP", description: "Получи опыт за уроки", target: 30, progress: 0, rewardXp: 10, rewardGems: 10, status: "Active" },
    { id: `quest_${userId}_${key}_lessons`, userId, day: key, kind: "CompleteLessons", title: "Пройди 2 урока", description: "Заверши любые два урока", target: 2, progress: 0, rewardXp: 15, rewardGems: 15, status: "Active" },
    { id: `quest_${userId}_${key}_accuracy`, userId, day: key, kind: "Accuracy", title: "Точность 80%", description: "Закончи урок с точностью от 80%", target: 1, progress: 0, rewardXp: 20, rewardGems: 20, status: "Active" }
  ];
  db.quests.push(...quests);
  return quests;
}

function updateQuestProgress(user, summary) {
  const quests = ensureDailyQuests(user.id);
  for (const quest of quests) {
    if (quest.status === "Claimed") continue;
    if (quest.kind === "EarnXp") quest.progress = Math.min(quest.target, quest.progress + summary.earnedXp);
    if (quest.kind === "CompleteLessons") quest.progress = Math.min(quest.target, quest.progress + 1);
    if (quest.kind === "Accuracy" && summary.accuracy >= 0.8) quest.progress = 1;
    if (quest.progress >= quest.target && quest.status === "Active") quest.status = "Completed";
  }
}

function weekStartIso() {
  const date = new Date();
  const day = date.getUTCDay() || 7;
  date.setUTCHours(0, 0, 0, 0);
  date.setUTCDate(date.getUTCDate() - day + 1);
  return date.toISOString();
}

function addLeaderboardXp(user, xp) {
  const weekStart = weekStartIso();
  let entry = db.leaderboard.find((item) => item.userId === user.id && item.leagueId === "Bronze" && item.weekStart === weekStart);
  if (!entry) {
    entry = { userId: user.id, username: user.username, displayName: user.displayName, avatarUrl: user.avatarUrl || null, leagueId: "Bronze", weeklyXp: 0, rank: 1, weekStart, updatedAt: nowIso() };
    db.leaderboard.push(entry);
  }
  entry.weeklyXp += xp;
  entry.updatedAt = nowIso();
  const ranked = db.leaderboard.filter((item) => item.leagueId === "Bronze" && item.weekStart === weekStart).sort((a, b) => b.weeklyXp - a.weeklyXp);
  ranked.forEach((item, index) => { item.rank = index + 1; });
  broadcastLeaderboard(entry);
}

function achievementState(user) {
  const completedLessons = db.progress.filter((p) => p.userId === user.id && (p.status === "Completed" || p.status === "Legendary")).length;
  return achievements.map((achievement) => {
    let progress = 0;
    if (achievement.code.startsWith("wildfire")) progress = user.currentStreak;
    else if (achievement.code.startsWith("scholar")) progress = completedLessons;
    else if (achievement.code.startsWith("sage")) progress = user.totalXp;
    else if (achievement.code.startsWith("explorer")) progress = Math.floor(completedLessons / 5);
    else if (achievement.code === "sharpshooter") progress = db.progress.some((p) => p.userId === user.id && p.accuracy === 1) ? 1 : 0;
    else if (achievement.code === "champion") progress = db.leaderboard.some((entry) => entry.userId === user.id && entry.rank === 1) ? 1 : 0;
    return { ...achievement, progress: Math.min(progress, achievement.target), unlockedAtEpochMillis: progress >= achievement.target ? Date.now() : null };
  });
}

function parseRoute(req) {
  const url = new URL(req.url, `http://${req.headers.host || "localhost"}`);
  return { url, pathname: decodeURIComponent(url.pathname), segments: url.pathname.split("/").filter(Boolean) };
}

async function routeApi(req, res) {
  const { url, pathname, segments } = parseRoute(req);
  if (req.method === "OPTIONS") return send(res, 204, {});

  if (req.method === "GET" && pathname === "/api/v1/health") {
    return ok(res, { status: "ok", service: "lexora-api", version: "0.1.0", serverTime: nowIso() });
  }

  if (req.method === "GET" && pathname === "/api/v1/config") {
    return ok(res, { apiVersion: "v1", languages: content.languages.length, courses: content.courses.length, auth: "jwt" });
  }

  if (req.method === "POST" && pathname === "/api/v1/auth/register") {
    const body = await readJson(req);
    const email = validateEmail(body.email);
    const password = validatePassword(body.password);
    const username = String(body.username || "").trim().toLowerCase();
    const displayName = String(body.displayName || username).trim();
    if (!/^[a-z0-9_]{3,24}$/.test(username)) throw new HttpError(422, "INVALID_USERNAME", "Username must be 3-24 chars: a-z, 0-9, underscore");
    if (db.users.some((user) => user.email === email)) throw new HttpError(409, "EMAIL_EXISTS", "Email is already registered");
    if (db.users.some((user) => user.username === username)) throw new HttpError(409, "USERNAME_EXISTS", "Username is already taken");
    const { salt, hash } = hashPassword(password);
    const user = { id: crypto.randomUUID(), email, username, displayName, passwordHash: hash, salt, avatarUrl: null, bio: null, interfaceLanguage: "ru", gems: 100, hearts: 5, totalXp: 0, currentStreak: 0, longestStreak: 0, createdAt: nowIso(), updatedAt: nowIso() };
    db.users.push(user);
    ensureDailyQuests(user.id);
    const response = createAuthResponse(user);
    await saveDb();
    return ok(res, response, 201);
  }

  if (req.method === "POST" && pathname === "/api/v1/auth/login") {
    const body = await readJson(req);
    const email = validateEmail(body.email);
    const user = db.users.find((item) => item.email === email);
    if (!user || !verifyPassword(String(body.password || ""), user)) throw new HttpError(401, "INVALID_CREDENTIALS", "Email or password is incorrect");
    const response = createAuthResponse(user);
    await saveDb();
    return ok(res, response);
  }

  if (req.method === "POST" && pathname === "/api/v1/auth/refresh") {
    const body = await readJson(req);
    const refreshToken = String(body.refreshToken || "");
    const payload = verifyToken(refreshToken);
    if (payload.type !== "refresh") throw new HttpError(401, "INVALID_REFRESH", "Invalid refresh token");
    const stored = db.refreshTokens.find((item) => item.token === refreshToken && !item.revoked && item.expiresAt > Date.now());
    if (!stored) throw new HttpError(401, "INVALID_REFRESH", "Refresh token is not active");
    stored.revoked = true;
    const user = db.users.find((item) => item.id === payload.sub);
    if (!user) throw new HttpError(401, "UNAUTHORIZED", "User does not exist");
    const response = createAuthResponse(user);
    await saveDb();
    return ok(res, response);
  }

  if (req.method === "POST" && pathname === "/api/v1/auth/logout") {
    const user = auth(req);
    db.refreshTokens.filter((item) => item.userId === user.id).forEach((item) => { item.revoked = true; });
    await saveDb();
    return ok(res, { loggedOut: true });
  }

  if (req.method === "GET" && pathname === "/api/v1/users/me") {
    const user = auth(req);
    return ok(res, publicUser(user));
  }

  if (req.method === "PATCH" && pathname === "/api/v1/users/me") {
    const user = auth(req);
    const body = await readJson(req);
    if (body.displayName !== undefined) user.displayName = String(body.displayName).trim().slice(0, 60);
    if (body.bio !== undefined) user.bio = String(body.bio).trim().slice(0, 240);
    if (body.avatarUrl !== undefined) user.avatarUrl = body.avatarUrl ? String(body.avatarUrl) : null;
    user.updatedAt = nowIso();
    await saveDb();
    return ok(res, publicUser(user));
  }

  if (req.method === "GET" && pathname === "/api/v1/courses") {
    return ok(res, content.courses);
  }

  if (req.method === "GET" && segments[0] === "api" && segments[2] === "courses" && segments[4] === "units") {
    const courseId = segments[3];
    const units = content.units.filter((unit) => unit.courseId === courseId).map((unit) => ({ ...unit, lessons: content.lessons.filter((lesson) => lesson.unitId === unit.id) }));
    return ok(res, units);
  }

  if (req.method === "GET" && segments[0] === "api" && segments[2] === "lessons" && segments.length === 4) {
    const lessonId = segments[3];
    const lesson = content.lessons.find((item) => item.id === lessonId);
    if (!lesson) throw new HttpError(404, "LESSON_NOT_FOUND", "Lesson not found");
    return ok(res, { lesson, exercises: content.exercises.filter((exercise) => exercise.lessonId === lessonId) });
  }

  if (req.method === "POST" && segments[0] === "api" && segments[2] === "lessons" && segments[4] === "complete") {
    const user = auth(req);
    const lessonId = segments[3];
    const lesson = content.lessons.find((item) => item.id === lessonId);
    if (!lesson) throw new HttpError(404, "LESSON_NOT_FOUND", "Lesson not found");
    const body = await readJson(req);
    const accuracy = Math.max(0, Math.min(1, Number(body.accuracy ?? 0)));
    const earnedXp = Math.max(1, Math.round(Number(body.score || lesson.xpReward) * accuracy) + Math.min(5, Math.floor(Number(body.bestCombo || 0) / 3)) + (accuracy === 1 ? 5 : 0));
    let progress = db.progress.find((item) => item.userId === user.id && item.lessonId === lessonId);
    if (!progress) {
      progress = { userId: user.id, lessonId, status: "Completed", score: earnedXp, accuracy, bestCombo: Number(body.bestCombo || 0), attempts: 0, completedAt: nowIso(), updatedAt: nowIso() };
      db.progress.push(progress);
    }
    progress.status = accuracy === 1 ? "Legendary" : "Completed";
    progress.score = Math.max(progress.score, earnedXp);
    progress.accuracy = Math.max(progress.accuracy, accuracy);
    progress.bestCombo = Math.max(progress.bestCombo, Number(body.bestCombo || 0));
    progress.attempts += 1;
    progress.completedAt = nowIso();
    progress.updatedAt = nowIso();
    user.totalXp += earnedXp;
    user.gems += Math.max(1, Math.floor(earnedXp / 3));
    user.currentStreak = Math.max(1, user.currentStreak);
    user.longestStreak = Math.max(user.longestStreak, user.currentStreak);
    user.updatedAt = nowIso();
    updateQuestProgress(user, { earnedXp, accuracy });
    addLeaderboardXp(user, earnedXp);
    const unlockedAchievementIds = achievementState(user).filter((item) => item.unlockedAtEpochMillis).map((item) => item.id);
    await saveDb();
    return ok(res, { earnedXp, earnedGems: Math.max(1, Math.floor(earnedXp / 3)), streakUpdated: true, unlockedAchievementIds, nextLessonId: nextLessonId(lessonId) });
  }

  if (req.method === "GET" && segments[0] === "api" && segments[2] === "leaderboard") {
    const leagueId = segments[3] || "Bronze";
    const weekStart = weekStartIso();
    const entries = db.leaderboard.filter((item) => item.leagueId.toLowerCase() === leagueId.toLowerCase() && item.weekStart === weekStart).sort((a, b) => b.weeklyXp - a.weeklyXp).slice(0, 30);
    return ok(res, entries);
  }

  if (req.method === "GET" && pathname === "/api/v1/quests/daily") {
    const user = auth(req);
    const quests = ensureDailyQuests(user.id);
    await saveDb();
    return ok(res, quests);
  }

  if (req.method === "POST" && segments[0] === "api" && segments[2] === "quests" && segments[4] === "claim") {
    const user = auth(req);
    const quest = db.quests.find((item) => item.id === segments[3] && item.userId === user.id);
    if (!quest) throw new HttpError(404, "QUEST_NOT_FOUND", "Quest not found");
    if (quest.status !== "Completed") throw new HttpError(409, "QUEST_NOT_COMPLETED", "Quest is not completed yet");
    quest.status = "Claimed";
    user.gems += quest.rewardGems;
    user.totalXp += quest.rewardXp;
    addLeaderboardXp(user, quest.rewardXp);
    await saveDb();
    return ok(res, quest);
  }

  if (req.method === "GET" && pathname === "/api/v1/shop/items") {
    return ok(res, shopItems);
  }

  if (req.method === "POST" && segments[0] === "api" && segments[2] === "shop" && segments[3] === "buy") {
    const user = auth(req);
    const item = shopItems.find((entry) => entry.id === segments[4]);
    if (!item) throw new HttpError(404, "SHOP_ITEM_NOT_FOUND", "Shop item not found");
    if (user.gems < item.priceGems) throw new HttpError(402, "NOT_ENOUGH_GEMS", "Not enough gems");
    user.gems -= item.priceGems;
    if (item.type === "Hearts") user.hearts = Math.min(5, user.hearts + Number(item.payload.hearts || 0));
    let inventory = db.inventory.find((entry) => entry.userId === user.id && entry.itemId === item.id);
    if (!inventory) {
      inventory = { userId: user.id, itemId: item.id, quantity: 0, acquiredAt: nowIso() };
      db.inventory.push(inventory);
    }
    inventory.quantity += 1;
    inventory.acquiredAt = nowIso();
    await saveDb();
    return ok(res, { itemId: item.id, quantity: inventory.quantity, remainingGems: user.gems });
  }

  if (req.method === "GET" && pathname === "/api/v1/dictionary") {
    const languageId = url.searchParams.get("languageId") || "lang_en";
    const query = (url.searchParams.get("query") || "").toLowerCase();
    const words = content.words.filter((word) => word.languageId === languageId && (!query || word.word.toLowerCase().includes(query) || word.translation.toLowerCase().includes(query))).slice(0, 100);
    return ok(res, words);
  }

  if (req.method === "POST" && pathname === "/api/v1/srs/review") {
    const user = auth(req);
    const body = await readJson(req);
    const card = reviewSrs(user.id, String(body.wordId), Number(body.quality ?? body.quality0To5 ?? 0));
    await saveDb();
    return ok(res, { wordId: card.wordId, nextDueEpochMillis: Date.parse(card.dueDate), easeFactor: card.easeFactor, intervalDays: card.intervalDays });
  }

  if (req.method === "GET" && pathname === "/api/v1/achievements") {
    const user = auth(req);
    return ok(res, achievementState(user));
  }

  if (req.method === "GET" && pathname === "/api/v1/friends") {
    const user = auth(req);
    const friends = db.friends.filter((item) => item.userId === user.id).map((item) => db.users.find((candidate) => candidate.id === item.friendId)).filter(Boolean).map(publicUser);
    return ok(res, friends);
  }

  if (req.method === "POST" && segments[0] === "api" && segments[2] === "friends" && segments[3] === "add") {
    const user = auth(req);
    const friend = db.users.find((item) => item.id === segments[4]);
    if (!friend) throw new HttpError(404, "USER_NOT_FOUND", "User not found");
    if (friend.id === user.id) throw new HttpError(409, "SELF_FRIEND", "Cannot add yourself");
    if (!db.friends.some((item) => item.userId === user.id && item.friendId === friend.id)) db.friends.push({ userId: user.id, friendId: friend.id, createdAt: nowIso() });
    await saveDb();
    return ok(res, publicUser(friend));
  }

  throw new HttpError(404, "NOT_FOUND", "Route not found");
}

function nextLessonId(lessonId) {
  const index = content.lessons.findIndex((lesson) => lesson.id === lessonId);
  return index >= 0 && index + 1 < content.lessons.length ? content.lessons[index + 1].id : null;
}

function reviewSrs(userId, wordId, qualityInput) {
  const quality = Math.max(0, Math.min(5, qualityInput));
  let card = db.srsCards.find((item) => item.userId === userId && item.wordId === wordId);
  if (!card) {
    card = { userId, wordId, status: "Learning", easeFactor: 2.5, intervalDays: 0, repetitions: 0, lapses: 0, dueDate: nowIso(), updatedAt: nowIso() };
    db.srsCards.push(card);
  }
  if (quality < 3) {
    card.status = "Forgotten";
    card.easeFactor = Math.max(1.3, card.easeFactor - 0.2);
    card.intervalDays = 1;
    card.repetitions = 0;
    card.lapses += 1;
  } else {
    card.easeFactor = Math.max(1.3, card.easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)));
    card.repetitions += 1;
    card.intervalDays = card.repetitions === 1 ? 1 : card.repetitions === 2 ? 6 : Math.max(1, Math.round(card.intervalDays * card.easeFactor));
    card.status = quality >= 4 ? "Known" : "Learning";
  }
  card.dueDate = new Date(Date.now() + card.intervalDays * 86_400_000).toISOString();
  card.updatedAt = nowIso();
  return card;
}

async function serveStatic(req, res) {
  const { pathname } = parseRoute(req);
  const requested = pathname === "/" ? "index.html" : pathname.slice(1);
  const target = path.normalize(path.join(PUBLIC_DIR, requested));
  if (!target.startsWith(PUBLIC_DIR)) return fail(res, 403, "FORBIDDEN", "Forbidden");
  try {
    const data = await fs.readFile(target);
    const ext = path.extname(target).toLowerCase();
    const type = ext === ".html" ? "text/html; charset=utf-8" : ext === ".css" ? "text/css; charset=utf-8" : ext === ".js" ? "application/javascript; charset=utf-8" : ext === ".apk" ? "application/vnd.android.package-archive" : "application/octet-stream";
    res.writeHead(200, { "content-type": type, "cache-control": "no-store", "access-control-allow-origin": "*" });
    res.end(data);
  } catch {
    res.writeHead(404, { "content-type": "text/html; charset=utf-8" });
    res.end("<h1>404</h1><p>Page not found</p>");
  }
}

function wsFrame(payload) {
  const body = Buffer.from(payload);
  if (body.length < 126) return Buffer.concat([Buffer.from([0x81, body.length]), body]);
  if (body.length < 65536) {
    const header = Buffer.alloc(4);
    header[0] = 0x81;
    header[1] = 126;
    header.writeUInt16BE(body.length, 2);
    return Buffer.concat([header, body]);
  }
  const header = Buffer.alloc(10);
  header[0] = 0x81;
  header[1] = 127;
  header.writeBigUInt64BE(BigInt(body.length), 2);
  return Buffer.concat([header, body]);
}

function broadcastLeaderboard(entry) {
  const message = JSON.stringify({ type: "leaderboard.updated", data: entry });
  for (const socket of sockets) socket.write(wsFrame(message));
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.url?.startsWith("/api/v1/")) return await routeApi(req, res);
    return await serveStatic(req, res);
  } catch (error) {
    if (error instanceof HttpError) return fail(res, error.status, error.code, error.message, error.details);
    console.error(error);
    return fail(res, 500, "INTERNAL_ERROR", "Internal server error");
  }
});

server.on("upgrade", (req, socket) => {
  if (!req.url?.startsWith("/ws/leaderboard")) {
    socket.destroy();
    return;
  }
  const key = req.headers["sec-websocket-key"];
  if (!key) {
    socket.destroy();
    return;
  }
  const accept = crypto.createHash("sha1").update(`${key}258EAFA5-E914-47DA-95CA-C5AB0DC85B11`).digest("base64");
  socket.write([
    "HTTP/1.1 101 Switching Protocols",
    "Upgrade: websocket",
    "Connection: Upgrade",
    `Sec-WebSocket-Accept: ${accept}`,
    "\r\n"
  ].join("\r\n"));
  sockets.add(socket);
  socket.write(wsFrame(JSON.stringify({ type: "connected", data: { service: "lexora-leaderboard" } })));
  socket.on("close", () => sockets.delete(socket));
  socket.on("error", () => sockets.delete(socket));
});

server.listen(PORT, HOST, () => {
  console.log(`Lexora API and web site listening on http://${HOST}:${PORT}`);
  console.log(`Courses: ${content.courses.length}, lessons: ${content.lessons.length}, exercises: ${content.exercises.length}, words: ${content.words.length}`);
});
