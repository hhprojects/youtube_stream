#!/usr/bin/env node
/*
 * Live drift guard for the podcast sidecar (mirrors check-discovery.js).
 *
 * Runs the REAL ytmusicapi sidecar (discovery.py) for the two podcast commands and validates the
 * response shape with validatePodcastShape(). ytmusicapi is unofficial and its output occasionally
 * changes shape; when it does, discovery.py returns empty payloads and the Podcast shelves silently
 * go blank in production while the mocked unit tests stay green. This script catches that — run it on
 * demand (e.g. when podcasts look empty) or from cron on the Pi.
 *
 * Requires ytmusicapi installed for the python3 on PATH (see check-discovery.js header).
 */
const { makeRunner, validatePodcastShape } = require('../discovery');

const run = makeRunner();
const SEED_QUERY = process.env.PODCAST_SEED_QUERY || 'software engineering podcast';

async function main() {
  const problems = [];

  const search = await run('podcast_search', SEED_QUERY);
  problems.push(...validatePodcastShape('podcast_search', search).map((p) => `[podcast_search] ${p}`));

  const showId = search && search.shows && search.shows[0] && search.shows[0].showId;
  if (showId) {
    const pod = await run('podcast', showId);
    problems.push(...validatePodcastShape('podcast', pod).map((p) => `[podcast] ${p}`));
  } else {
    problems.push('[podcast] skipped — no showId from podcast_search (search is broken)');
  }

  if (problems.length) {
    console.error('PODCAST DRIFT DETECTED — ytmusicapi output no longer matches discovery.py:');
    for (const p of problems) console.error('  - ' + p);
    console.error('\nFix backend/discovery.py against the real output, then re-run this check.');
    process.exit(1);
  }
  console.log('OK — both podcast commands returned the expected shape.');
}

main().catch((e) => {
  console.error('check-podcasts failed to run:', e.message);
  console.error('(Is ytmusicapi installed for the python3 on PATH? See check-discovery.js header.)');
  process.exit(1);
});
