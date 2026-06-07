#!/usr/bin/env node
/*
 * Live drift guard for the discovery sidecar.
 *
 * Runs the REAL ytmusicapi sidecar (discovery.py) for all four commands and validates the
 * response shape with validateDiscoveryShape(). ytmusicapi is unofficial and its output
 * occasionally changes shape; when it does, discovery.py returns empty payloads and the
 * Discover shelves silently go blank in production while the mocked unit tests stay green.
 * This script is the thing that catches that — run it on demand (e.g. when Discover looks
 * empty) or from cron on the Pi.
 *
 * Requires ytmusicapi installed for the python3 on PATH (the same one discovery.py uses):
 *   on the Pi: it's already there (pip install ytmusicapi==1.12.1)
 *   locally:   python3 -m venv .venv && .venv/bin/pip install ytmusicapi==1.12.1
 *              PATH="$(pwd)/.venv/bin:$PATH" node scripts/check-discovery.js
 *
 * Exit 0 = all four commands returned the expected shape.
 * Exit 1 = drift/breakage (prints exactly what's wrong) — fix discovery.py and re-run.
 *
 * Pass --update to also refresh the committed fixtures from this live capture (only writes
 * when the shape is valid). Re-run `node --test` and commit the refreshed fixtures afterward.
 */
const fs = require('node:fs');
const path = require('node:path');
const { makeRunner, validateDiscoveryShape } = require('../discovery');

const FIXTURES_DIR = path.join(__dirname, '..', 'test', 'fixtures', 'discovery');
const REGION = process.env.DISCOVERY_REGION || 'US';
const UPDATE = process.argv.includes('--update');
const run = makeRunner();

async function main() {
  const problems = [];
  const captured = {};

  const record = (name, kind, data) => {
    captured[name] = data;
    problems.push(...validateDiscoveryShape(kind, data).map((p) => `[${name}] ${p}`));
  };

  // moods + trending have fixed args; mood + related derive their args from the live
  // responses (the params token and seed videoId rotate, so never hardcode them).
  record('moods', 'moods', await run('moods'));
  record('trending', 'trending', await run('trending', REGION));

  const moodKey = captured.moods && captured.moods.categories && captured.moods.categories[0] && captured.moods.categories[0].key;
  const seedId = captured.trending && captured.trending.songs && captured.trending.songs[0] && captured.trending.songs[0].id;

  if (moodKey) record('mood', 'mood', await run('mood', moodKey));
  else problems.push('[mood] skipped — no mood key available (moods is broken)');

  if (seedId) record('related', 'related', await run('related', seedId));
  else problems.push('[related] skipped — no seed videoId available (trending is broken)');

  record('genrecharts', 'genrecharts', await run('genrecharts', 'US'));   // genres section is US-only
  const genreId = captured.genrecharts && captured.genrecharts.charts && captured.genrecharts.charts[0] && captured.genrecharts.charts[0].key;
  if (genreId) record('playlist', 'playlist', await run('playlist', genreId));
  else problems.push('[playlist] skipped — no genre playlistId available (genrecharts broken)');

  if (problems.length) {
    console.error('DISCOVERY DRIFT DETECTED — ytmusicapi output no longer matches what discovery.py expects:');
    for (const p of problems) console.error('  - ' + p);
    console.error('\nFix backend/discovery.py against the real output, then re-run this check.');
    process.exit(1);
  }

  console.log(`OK — all ${Object.keys(captured).length} discovery commands returned the expected shape (region=${REGION}).`);
  if (UPDATE) {
    for (const [name, data] of Object.entries(captured)) {
      fs.writeFileSync(path.join(FIXTURES_DIR, `${name}.json`), JSON.stringify(data) + '\n');
      console.log(`  refreshed fixture: ${name}.json`);
    }
    console.log('Fixtures refreshed — run `node --test` and commit them if green.');
  }
}

main().catch((e) => {
  console.error('check-discovery failed to run:', e.message);
  console.error('(Is ytmusicapi installed for the python3 on PATH? See the header of this file.)');
  process.exit(1);
});
