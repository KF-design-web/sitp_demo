import { spawn } from 'node:child_process';
import { readFileSync, writeFileSync, existsSync, rmSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const BASE = 'http://localhost:4200';
const OUT = [];
let chromeProc = null;
let profileDir = null;
const say = (l) => { OUT.push(l); writeFileSync('../logs/i18n-course-proof-result.txt', OUT.join('\n')); try { console.log(l); } catch {} };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function cdp(wsUrl) {
  let mid = 0; const pending = new Map();
  const ws = new WebSocket(wsUrl);
  const ready = new Promise((res, rej) => { ws.onopen = res; ws.onerror = rej; });
  ws.onmessage = (ev) => { const m = JSON.parse(ev.data); if (m.id && pending.has(m.id)) { const p = pending.get(m.id); pending.delete(m.id); m.error ? p.reject(new Error(JSON.stringify(m.error))) : p.resolve(m.result); } };
  const send = async (method, params = {}) => { await ready; const id = ++mid; return new Promise((res, rej) => { pending.set(id, { resolve: res, reject: rej }); ws.send(JSON.stringify({ id, method, params })); setTimeout(() => { if (pending.has(id)) { pending.delete(id); rej(new Error('timeout ' + method)); } }, 25000); }); };
  return { send, close: () => { try { ws.close(); } catch {} } };
}

async function launchChrome() {
  const chrome = ['C:/Program Files/Google/Chrome/Application/chrome.exe'].find(existsSync);
  if (!chrome) throw new Error('Chrome not found');
  profileDir = mkdtempSync(join(tmpdir(), 'i18n-'));
  chromeProc = spawn(chrome, ['--headless=new', '--remote-debugging-port=9228', `--user-data-dir=${profileDir}`, '--no-first-run', '--window-size=1366,900', 'about:blank'], { detached: true, stdio: 'ignore' });
  chromeProc.unref();
  for (let i = 0; i < 30; i++) {
    try { const list = await (await fetch('http://127.0.0.1:9228/json/list')).json(); const page = list.find((t) => t.type === 'page'); if (page?.webSocketDebuggerUrl) return page.webSocketDebuggerUrl; } catch {}
    await sleep(500);
  }
  throw new Error('devtools never came up');
}

async function evalJs(c, expression) {
  const r = await c.send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (r.exceptionDetails) throw new Error('eval failed: ' + (r.exceptionDetails.exception?.description || r.exceptionDetails.text));
  return r.result.value;
}

async function nav(c, url) {
  await c.send('Page.navigate', { url });
  const t0 = Date.now();
  while (Date.now() - t0 < 20000) {
    await sleep(400);
    try { if (await evalJs(c, 'document.readyState === "complete"')) { await sleep(600); return; } } catch {}
  }
  throw new Error('nav timeout ' + url);
}

async function setLangReload(c, lang, minCards) {
  await nav(c, `${BASE}/courses`);
  await evalJs(c, `localStorage.setItem('sitp-lang', '${lang}'); document.documentElement.lang = '${lang}'; 'ok'`);
  await nav(c, `${BASE}/courses`);
  for (let i = 0; i < 20; i++) {
    if (await evalJs(c, `document.querySelectorAll('app-course-card').length >= ${minCards}`)) break;
    await sleep(300);
  }
}

async function boardText(c) {
  return JSON.parse(await evalJs(c, `JSON.stringify([...document.querySelectorAll('app-course-card')].map(card => ({
    t: card.querySelector('h3')?.textContent?.trim() ?? '',
    d: card.querySelector('p')?.textContent?.trim() ?? ''
  })))`));
}

async function main() {
  say(`=== #121 i18n COURSE-TEXT PROOF — ${new Date().toISOString()} ===`);
  const wsUrl = await launchChrome();
  const c = cdp(wsUrl);
  await c.send('Page.enable');
  await c.send('Runtime.enable');

  const cookiePair = readFileSync('../logs/proof/t4-cookie.txt', 'utf8').trim();
  await c.send('Network.enable');
  const [n, ...v] = cookiePair.split('=');
  await c.send('Network.setCookie', { name: n, value: v.join('='), url: BASE });

  await setLangReload(c, 'en', 6);
  const en = await boardText(c);
  const enOk = en.length === 6 && en[0].t === 'Git Foundations'
    && en.some(x => x.t === 'Port Logistics & Freight Operations')
    && en.some(x => x.d.startsWith('Douala port to final mile'));
  say(`EN board: ${en.length} cards, first="${en[0]?.t}", logistics=${en.some(x => x.t.startsWith('Port Logistics'))}`);
  say(enOk ? 'LEG1-EN: PASS (English titles + descriptions)' : 'LEG1-EN: FAIL');

  await setLangReload(c, 'fr', 6);
  const fr = await boardText(c);
  const frOk = fr.length === 6
    && fr.some(x => x.t === 'Fondations de Git')
    && fr.some(x => x.t === 'Logistique portuaire et opérations de fret')
    && fr.some(x => x.d.startsWith('Du port de Douala'))
    && !fr.some(x => x.t === 'Git Foundations')
    && !fr.some(x => x.d.startsWith('Branches without fear'));
  say(`FR board: ${fr.length} cards, first="${fr[0]?.t}"`);
  say(`FR logistics title: "${fr.find(x => x.t.includes('portuaire'))?.t ?? 'MISSING'}"`);
  say(frOk ? 'LEG2-FR-BOARD: PASS (titles + descriptions flipped, no English left)' : 'LEG2-FR-BOARD: FAIL');

  await nav(c, `${BASE}/courses/8`);
  let body = '';
  for (let i = 0; i < 20; i++) {
    body = await evalJs(c, 'document.body.innerText');
    if (body.includes('conteneur') || body.includes('Logistique portuaire')) break;
    await sleep(300);
  }
  const roomOk = body.includes('Logistique portuaire et opérations de fret')
    && body.includes('Du port de Douala')
    && body.includes('Le voyage du conteneur')
    && body.includes('Le connaissement et les documents')
    && !body.includes('The container journey');
  say(`FR room: title=${body.includes('Logistique portuaire')} desc=${body.includes('Du port de Douala')} ch1=${body.includes('Le voyage du conteneur')} ch2=${body.includes('Le connaissement')} enLeak=${body.includes('The container journey')}`);
  say(roomOk ? 'LEG3-FR-ROOM: PASS (title, description, chapter names all French)' : 'LEG3-FR-ROOM: FAIL');

  await setLangReload(c, 'en', 6);
  const back = await boardText(c);
  const backOk = back.length === 6 && back[0].t === 'Git Foundations'
    && back.some(x => x.d.startsWith('Branches without fear'));
  say(`EN restored: first="${back[0]?.t}", ${back.length} cards (data untouched)`);
  say(backOk ? 'LEG4-EN-RESTORE: PASS (English back, 6 rows intact)' : 'LEG4-EN-RESTORE: FAIL');

  c.close();
  say('');
  say(`#121-PROOF: ${enOk && frOk && roomOk && backOk ? 'PASS — 4/4 legs' : 'FAIL'}`);
}

main().then(() => { cleanup(); process.exit(0); }).catch((e) => { say('FATAL: ' + (e?.message || e)); cleanup(); process.exit(1); });
function cleanup() { try { if (chromeProc?.pid) process.kill(chromeProc.pid); } catch {} try { if (profileDir) rmSync(profileDir, { recursive: true, force: true }); } catch {} }
