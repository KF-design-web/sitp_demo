import { spawn } from 'node:child_process';
import { readFileSync, writeFileSync, existsSync, rmSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const BASE = 'http://localhost:4200';
const OUT = [];
let chromeProc = null;
let profileDir = null;
const say = (l) => { OUT.push(l); writeFileSync('../logs/verify120-result.txt', OUT.join('\n')); try { console.log(l); } catch {} };
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
  const chrome = ["C:/Program Files/Google/Chrome/Application/chrome.exe"].find(existsSync);
  if (!chrome) throw new Error('Chrome not found');
  profileDir = mkdtempSync(join(tmpdir(), 'v120-'));
  chromeProc = spawn(chrome, ['--headless=new', '--remote-debugging-port=9226', `--user-data-dir=${profileDir}`, '--no-first-run', '--window-size=1366,900', 'about:blank'], { detached: true, stdio: 'ignore' });
  chromeProc.unref();
  for (let i = 0; i < 30; i++) {
    try { const list = await (await fetch('http://127.0.0.1:9226/json/list')).json(); const page = list.find((t) => t.type === 'page'); if (page?.webSocketDebuggerUrl) return page.webSocketDebuggerUrl; } catch {}
    await sleep(500);
  }
  throw new Error('devtools never came up');
}

async function evalJs(c, expression) {
  const r = await c.send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true });
  if (r.exceptionDetails) throw new Error('eval failed: ' + JSON.stringify(r.exceptionDetails.exception?.description || r.exceptionDetails.text));
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

async function main() {
  say(`=== #120 VERIFY — ${new Date().toISOString()} ===`);
  const wsUrl = await launchChrome();
  const c = cdp(wsUrl);
  await c.send('Page.enable');
  await c.send('Runtime.enable');

  const cookiePair = readFileSync('../logs/proof/t4-cookie.txt', 'utf8').trim();
  await c.send('Network.enable');
  const [n, ...v] = cookiePair.split('=');
  await c.send('Network.setCookie', { name: n, value: v.join('='), url: BASE });

  await nav(c, `${BASE}/courses`);
  const boardRaw = await evalJs(c, `(async () => {
    for (let i = 0; i < 20; i++) {
      if (document.querySelectorAll('app-course-card').length >= 6) break;
      await new Promise(r => setTimeout(r, 300));
    }
    const cards = [...document.querySelectorAll('app-course-card')];
    for (let i = 0; i < 30; i++) {
      const imgsNow = cards.map(c => c.querySelector('img')).filter(Boolean);
      if (imgsNow.length >= cards.length && imgsNow.every(im => im.naturalWidth > 0)) break;
      await new Promise(r => setTimeout(r, 500));
    }
    const EXPECTED = {
      'Git Foundations': '1518770660439',
      'Graphic Design Fundamentals': '1626785774573',
      'Port Logistics & Freight Operations': '1586528116311',
      'HR Essentials for the Port Workforce': '1454165804606',
      'Freight Documentation & Customs English': '1499750310107',
      'Workplace Communication (EN/FR)': '1611224923853',
    };
    const imgs = cards.map(card => {
      const img = card.querySelector('img');
      const title = img?.alt ?? '';
      return img ? { w: img.naturalWidth, title, topical: title in EXPECTED && img.src.includes(EXPECTED[title]) } : null;
    });
    const rects = cards.map(card => {
      const r = card.getBoundingClientRect();
      return { top: Math.round(r.top), left: Math.round(r.left) };
    });
    return JSON.stringify({ count: cards.length, imgs, rects });
  })()`);
  const b = JSON.parse(boardRaw);
  const loaded = b.imgs.filter(Boolean).filter((x) => x.w > 0).length;
  const topical = b.imgs.filter(Boolean).filter((x) => x.topical).length;
  const tops = [...new Set(b.rects.map((r) => r.top))];
  const lefts = [...new Set(b.rects.map((r) => r.left))];
  say(`board: count=${b.count} thumbsLoaded=${loaded}/${b.count} topical=${topical}/${b.count}`);
  say(`grid: ${tops.length} row(s) x ${lefts.length} column(s) — tops=${JSON.stringify(tops)} lefts=${JSON.stringify(lefts)}`);
  const leg1 = b.count === 6 && tops.length === 2 && lefts.length === 3 && loaded === 6 && topical === 6;
  say(leg1 ? 'LEG1-BOARD: PASS (6 cards, 2x3, 6/6 topical photos loaded)' : 'LEG1-BOARD: FAIL');

  await nav(c, `${BASE}/login`);
  const capRaw = await evalJs(c, `(() => {
    const chip = [...document.querySelectorAll('span')].find(s => /\\d+\\s*[+\\-x*]\\s*\\d+/.test(s.textContent) && s.textContent.length < 20);
    return JSON.stringify({
      question: chip ? chip.textContent.trim() : null,
      hasEnglish: document.body.innerText.includes('What is'),
      placeholder: document.querySelector('input[inputmode="numeric"]')?.placeholder ?? null,
      inputW: Math.round(document.querySelector('input[inputmode="numeric"]')?.getBoundingClientRect().width ?? 0),
      chipH: Math.round(chip?.getBoundingClientRect().height ?? 0),
    });
  })()`);
  const cap = JSON.parse(capRaw);
  say(`captcha: ${JSON.stringify(cap)}`);
  const leg2 = Boolean(cap.question) && /^\d+ \+ \d+ = \?$/.test(cap.question) && cap.hasEnglish === false;
  say(leg2 ? 'LEG2-CAPTCHA: PASS (bare math expression, no English words)' : 'LEG2-CAPTCHA: FAIL');

  await nav(c, `${BASE}/courses/8`);
  const roomRaw = await evalJs(c, `(async () => {
    for (let i = 0; i < 20; i++) {
      if (document.body.innerText.includes('Port Logistics')) break;
      await new Promise(r => setTimeout(r, 300));
    }
    for (let i = 0; i < 30; i++) {
      const thumbs = [...document.querySelectorAll('ol li img')];
      const priceShown = /\d[\d\s]*\s*FCFA/.test(document.body.innerText);
      if (priceShown && thumbs.length >= 3 && thumbs.every(im => im.naturalWidth > 0)) break;
      await new Promise(r => setTimeout(r, 500));
    }
    const body = document.body.innerText;
    return JSON.stringify({
      title: body.includes('Port Logistics & Freight Operations'),
      price: body.match(/\\d[\\d\\s]*\\s*FCFA/)?.[0] ?? null,
      rows: document.querySelectorAll('ol li').length,
      thumbLoaded: [...document.querySelectorAll('ol li img')].filter(i => i.naturalWidth > 0).length,
      notFound: body.includes('Course not found'),
    });
  })()`);
  const room = JSON.parse(roomRaw);
  say(`room /courses/8: ${JSON.stringify(room)}`);
  const leg3 = room.title && /^500\s000\sFCFA$/.test(room.price ?? '') && room.rows === 3 && room.thumbLoaded === 3 && room.notFound === false;
  say(leg3 ? 'LEG3-ROOM: PASS (flagship price 500,000 XAF, 3 chapters, thumbs loaded)' : 'LEG3-ROOM: FAIL');

  c.close();
  say('');
  say(`#120-VERIFY: ${leg1 && leg2 && leg3 ? 'PASS — 3/3 legs' : 'FAIL'}`);
}
main().then(() => { cleanup(); process.exit(0); }).catch((e) => { say('FATAL: ' + (e?.message || e)); cleanup(); process.exit(1); });
function cleanup() { try { if (chromeProc?.pid) process.kill(chromeProc.pid); } catch {} try { if (profileDir) rmSync(profileDir, { recursive: true, force: true }); } catch {} }
