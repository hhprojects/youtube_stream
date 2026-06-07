const { execFile } = require('node:child_process');
const path = require('node:path');

const PY_SCRIPT = path.join(__dirname, 'discovery.py');

/** Parse the sidecar's stdout; an `{error}` payload becomes a thrown Error. */
function parseDiscoveryOutput(stdout) {
  const data = JSON.parse(String(stdout).trim());
  if (data && data.error) throw new Error(data.error);
  return data;
}

/** Real sidecar invoker: spawn `python3 discovery.py <command> [args]`, parse stdout JSON. */
function makeRunner({ python = 'python3', script = PY_SCRIPT, timeoutMs = 30000 } = {}) {
  return (command, ...args) =>
    new Promise((resolve, reject) => {
      execFile(python, [script, command, ...args], { timeout: timeoutMs }, (error, stdout, stderr) => {
        if (error) return reject(new Error((stderr && stderr.trim()) || error.message));
        try { resolve(parseDiscoveryOutput(stdout)); } catch (e) { reject(e); }
      });
    });
}

async function cached(cache, key, produce) {
  const hit = cache.get(key);
  if (hit) return hit;
  const data = await produce();
  cache.set(key, data);
  return data;
}

const getTrending = (run, cache, region) => cached(cache, `trending:${region}`, () => run('trending', region));
const getRelated = (run, cache, videoId) => cached(cache, `related:${videoId}`, () => run('related', videoId));
const getMoods = (run, cache) => cached(cache, 'moods', () => run('moods'));
const getMood = (run, cache, params) => cached(cache, `mood:${params}`, () => run('mood', params));

module.exports = { parseDiscoveryOutput, makeRunner, getTrending, getRelated, getMoods, getMood };
