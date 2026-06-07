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

/**
 * Shape guard for discovery payloads. Returns a list of problem strings (empty = OK).
 * Used by the unit tests (against the committed fixtures) and the live `check-discovery`
 * script to detect when ytmusicapi's output drifts from what discovery.py expects — the
 * failure mode that would otherwise silently empty the Discover shelves in production.
 */
function validateDiscoveryShape(kind, data) {
  const problems = [];
  const isStr = (v) => typeof v === 'string' && v.length > 0;
  const checkSongs = (songs, label) => {
    if (!Array.isArray(songs)) return void problems.push(`${label}: songs is not an array`);
    if (songs.length === 0) return void problems.push(`${label}: songs is empty`);
    if (!isStr(songs[0].id)) problems.push(`${label}: songs[0].id missing/empty`);
    if (!isStr(songs[0].title)) problems.push(`${label}: songs[0].title missing/empty`);
  };
  switch (kind) {
    case 'trending':
    case 'related':
    case 'mood':
      checkSongs(data && data.songs, kind);
      break;
    case 'moods': {
      const cats = data && data.categories;
      if (!Array.isArray(cats)) { problems.push('moods: categories is not an array'); break; }
      if (cats.length === 0) { problems.push('moods: categories is empty'); break; }
      if (!isStr(cats[0].key)) problems.push('moods: categories[0].key missing/empty');
      if (!isStr(cats[0].title)) problems.push('moods: categories[0].title missing/empty');
      break;
    }
    default:
      problems.push(`unknown kind: ${kind}`);
  }
  return problems;
}

module.exports = { parseDiscoveryOutput, makeRunner, getTrending, getRelated, getMoods, getMood, validateDiscoveryShape };
