import { spawn } from 'node:child_process';
import { existsSync, rmSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const chrome = ['C:/Program Files/Google/Chrome/Application/chrome.exe'].find(existsSync);
const profileDir = mkdtempSync(join(tmpdir(), 'probe-'));
const proc = spawn(chrome, ['--headless=new', '--remote-debugging-port=9227', `--user-data-dir=${profileDir}`, '--no-first-run', 'about:blank'], { detached: true, stdio: 'ignore' });
proc.unref();
let ws = null;
for (let i = 0; i < 30 && !ws; i++) { try { const l = await (await fetch('http://127.0.0.1:9227/json/list')).json(); ws = l.find(t => t.type === 'page')?.webSocketDebuggerUrl; } catch {} if (!ws) await sleep(500); }
let mid = 0; const pending = new Map();
const w = new WebSocket(ws);
await new Promise((res, rej) => { w.onopen = res; w.onerror = rej; });
w.onmessage = (ev) => { const m = JSON.parse(ev.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m.result); pending.delete(m.id); } };
const send = (method, params = {}) => { const id = ++mid; return new Promise((res) => { pending.set(id, res); w.send(JSON.stringify({ id, method, params })); }); };
await send('Runtime.enable');
const urls = [
  'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=640&h=360&fit=crop&fm=jpg&q=70',
  'https://images.unsplash.com/photo-1518770660439-4636190af475?w=640&h=360&fit=crop&fm=jpg&q=70',
  'https://picsum.photos/seed/x/240/135',
];
for (const u of urls) {
  const r = await send('Runtime.evaluate', { expression: `fetch(${JSON.stringify(u)}).then(r => JSON.stringify({s: r.status, t: r.headers.get('content-type')})).catch(e => 'FETCH-ERR: ' + e.message)`, awaitPromise: true, returnByValue: true });
  console.log(u.slice(8, 50), '=>', r.result?.value);
}
try { process.kill(proc.pid); } catch {}
rmSync(profileDir, { recursive: true, force: true });
