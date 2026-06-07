const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { createCache } = require('./cache');
const { parseDiscoveryOutput, getTrending, getRelated, getMoods, getMood } = require('./discovery');

const FIX = path.join(__dirname, 'test', 'fixtures', 'discovery');
const raw = (name) => fs.readFileSync(path.join(FIX, name), 'utf8');

test('parseDiscoveryOutput parses a song payload from the fixture', () => {
  const out = parseDiscoveryOutput(raw('trending.json'));
  assert.ok(Array.isArray(out.songs) && out.songs.length > 0);
  for (const s of out.songs) {
    assert.equal(typeof s.id, 'string');
    assert.equal(typeof s.title, 'string');
  }
});

test('parseDiscoveryOutput throws when the sidecar reports an error', () => {
  assert.throws(() => parseDiscoveryOutput(JSON.stringify({ error: 'boom' })), /boom/);
});

test('getTrending caches: runner called once across two identical calls', async () => {
  const cache = createCache({ ttlMs: 10_000, now: () => 0 });
  let calls = 0;
  const run = async (cmd, region) => {
    calls++;
    assert.equal(cmd, 'trending');
    assert.equal(region, 'US');
    return parseDiscoveryOutput(raw('trending.json'));
  };
  const a = await getTrending(run, cache, 'US');
  const b = await getTrending(run, cache, 'US');
  assert.equal(calls, 1);
  assert.deepEqual(a, b);
});

test('cache expiry makes getTrending call the runner again', async () => {
  let t = 0;
  const cache = createCache({ ttlMs: 100, now: () => t });
  let calls = 0;
  const run = async () => { calls++; return { songs: [] }; };
  await getTrending(run, cache, 'US');
  t = 50;  await getTrending(run, cache, 'US');   // cached
  t = 150; await getTrending(run, cache, 'US');   // expired
  assert.equal(calls, 2);
});

test('getMoods / getMood / getRelated key independently', async () => {
  const cache = createCache({ ttlMs: 10_000, now: () => 0 });
  const seen = [];
  const run = async (cmd, arg) => { seen.push([cmd, arg]); return { ok: true }; };
  await getMoods(run, cache);
  await getMood(run, cache, 'TOKEN');
  await getRelated(run, cache, 'vid123');
  await getMoods(run, cache);            // cached, no new call
  assert.deepEqual(seen, [['moods', undefined], ['mood', 'TOKEN'], ['related', 'vid123']]);
});
