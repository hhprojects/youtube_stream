const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const { parseArtistTitle, YOUTUBE_ID_REGEX, pruneDownloads, thumbnailUrl, sidecarPathFor, readVideoId, writeVideoId, readSidecar, writeSidecar, setSidecarTitle, libraryRowFor, cleanTrackTitle, lyricsPathFor, readLyricsCache, writeLyricsCache, getLyrics, downloadBaseName, displayBaseName } = require('./server');

test('YOUTUBE_ID_REGEX accepts standard ids', () => {
  assert.ok(YOUTUBE_ID_REGEX.test('dQw4w9WgXcQ'));
  assert.ok(YOUTUBE_ID_REGEX.test('abc-123_XYZ'));
});

test('YOUTUBE_ID_REGEX rejects injection attempts', () => {
  assert.equal(YOUTUBE_ID_REGEX.test('abc; rm -rf /'), false);
  assert.equal(YOUTUBE_ID_REGEX.test('../../etc/passwd'), false);
  assert.equal(YOUTUBE_ID_REGEX.test(''), false);
  assert.equal(YOUTUBE_ID_REGEX.test('a'.repeat(21)), false);
});

test('parseArtistTitle splits on hyphen', () => {
  assert.deepEqual(parseArtistTitle('Post Malone - Circles'), {
    artist: 'Post Malone',
    title: 'Circles',
  });
});

test('parseArtistTitle handles em dash and en dash', () => {
  assert.deepEqual(parseArtistTitle('Artist — Title'), { artist: 'Artist', title: 'Title' });
  assert.deepEqual(parseArtistTitle('Artist – Title'), { artist: 'Artist', title: 'Title' });
});

test('parseArtistTitle handles pipe separator', () => {
  assert.deepEqual(parseArtistTitle('Band | Song'), { artist: 'Band', title: 'Song' });
});

test('parseArtistTitle falls back to Unknown when no separator', () => {
  assert.deepEqual(parseArtistTitle('Just a title'), {
    artist: 'Unknown',
    title: 'Just a title',
  });
});

test('pruneDownloads keeps newest files under cap', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'prune-'));
  try {
    // 3 files, 100 bytes each, with ascending mtimes so we know the order
    const names = ['old.m4a', 'mid.m4a', 'new.m4a'];
    const now = Date.now();
    names.forEach((name, i) => {
      const p = path.join(tmp, name);
      fs.writeFileSync(p, Buffer.alloc(100));
      fs.utimesSync(p, new Date(now + i * 1000), new Date(now + i * 1000));
    });
    const { removed, totalAfter } = pruneDownloads(tmp, 150);
    assert.deepEqual(removed, ['old.m4a', 'mid.m4a']);
    assert.equal(totalAfter, 100);
    assert.deepEqual(fs.readdirSync(tmp).sort(), ['new.m4a']);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('pruneDownloads is a no-op when under cap', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'prune-'));
  try {
    fs.writeFileSync(path.join(tmp, 'a.m4a'), Buffer.alloc(50));
    const { removed } = pruneDownloads(tmp, 1000);
    assert.deepEqual(removed, []);
    assert.deepEqual(fs.readdirSync(tmp), ['a.m4a']);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('pruneDownloads ignores non-audio files', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'prune-'));
  try {
    fs.writeFileSync(path.join(tmp, 'song.m4a'), Buffer.alloc(200));
    fs.writeFileSync(path.join(tmp, 'notes.txt'), Buffer.alloc(500));
    pruneDownloads(tmp, 100);
    // song.m4a counted and pruned; notes.txt untouched
    assert.ok(!fs.existsSync(path.join(tmp, 'song.m4a')));
    assert.ok(fs.existsSync(path.join(tmp, 'notes.txt')));
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('thumbnailUrl builds a CDN url, or null without an id', () => {
  assert.equal(thumbnailUrl('dQw4w9WgXcQ'), 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg');
  assert.equal(thumbnailUrl(null), null);
  assert.equal(thumbnailUrl(undefined), null);
});

test('sidecarPathFor swaps the audio extension for .json', () => {
  assert.equal(sidecarPathFor('/d', 'a b.m4a'), path.join('/d', 'a b.json'));
  assert.equal(sidecarPathFor('/d', 'x.mp3'), path.join('/d', 'x.json'));
});

test('writeVideoId / readVideoId round-trip a sidecar', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'art-'));
  try {
    fs.writeFileSync(path.join(tmp, 'song.m4a'), Buffer.alloc(10));
    writeVideoId(tmp, 'song.m4a', 'dQw4w9WgXcQ');
    assert.ok(fs.existsSync(path.join(tmp, 'song.json')));
    assert.equal(readVideoId(tmp, 'song.m4a'), 'dQw4w9WgXcQ');
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('readVideoId returns null for a missing or malformed sidecar', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'art-'));
  try {
    assert.equal(readVideoId(tmp, 'missing.m4a'), null);
    fs.writeFileSync(path.join(tmp, 'bad.json'), 'not json');
    assert.equal(readVideoId(tmp, 'bad.m4a'), null);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('pruneDownloads removes a pruned file\'s sidecar too', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'art-'));
  try {
    fs.writeFileSync(path.join(tmp, 'a.m4a'), Buffer.alloc(200));
    fs.writeFileSync(path.join(tmp, 'a.json'), JSON.stringify({ videoId: 'x' }));
    pruneDownloads(tmp, 100);
    assert.ok(!fs.existsSync(path.join(tmp, 'a.m4a')));
    assert.ok(!fs.existsSync(path.join(tmp, 'a.json')));
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('writeSidecar / readSidecar round-trip videoId + title + artist', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'art-'));
  try {
    writeSidecar(tmp, 'song.m4a', { videoId: 'dQw4w9WgXcQ', title: 'Circles', artist: 'Post Malone' });
    assert.deepEqual(readSidecar(tmp, 'song.m4a'), { videoId: 'dQw4w9WgXcQ', title: 'Circles', artist: 'Post Malone' });
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('writeVideoId merges into an existing sidecar (keeps title/artist)', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'art-'));
  try {
    writeSidecar(tmp, 'song.m4a', { videoId: 'old', title: 'Circles', artist: 'Post Malone' });
    writeVideoId(tmp, 'song.m4a', 'new');   // artwork endpoint sets only the videoId
    assert.deepEqual(readSidecar(tmp, 'song.m4a'), { videoId: 'new', title: 'Circles', artist: 'Post Malone' });
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('libraryRowFor prefers sidecar title/artist/videoId, falls back to the filename', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'art-'));
  try {
    // With a sidecar: real metadata + videoId surface.
    fs.writeFileSync(path.join(tmp, 'Post_Malone_Circles.m4a'), Buffer.alloc(10));
    writeSidecar(tmp, 'Post_Malone_Circles.m4a', { videoId: 'dQw4w9WgXcQ', title: 'Circles', artist: 'Post Malone' });
    const withSc = libraryRowFor(tmp, 'Post_Malone_Circles.m4a');
    assert.equal(withSc.title, 'Circles');
    assert.equal(withSc.artist, 'Post Malone');
    assert.equal(withSc.videoId, 'dQw4w9WgXcQ');

    // Without a sidecar: falls back to deriving from the (separator-less) filename → artist "Unknown", videoId null.
    fs.writeFileSync(path.join(tmp, 'Some_Old_File.m4a'), Buffer.alloc(10));
    const noSc = libraryRowFor(tmp, 'Some_Old_File.m4a');
    assert.equal(noSc.artist, 'Unknown');
    assert.equal(noSc.videoId, null);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('cleanTrackTitle strips official/video/lyrics noise and feat tails', () => {
  assert.equal(cleanTrackTitle('Circles (Official Video)'), 'Circles');
  assert.equal(cleanTrackTitle('Circles [Official Audio]'), 'Circles');
  assert.equal(cleanTrackTitle('Circles (Lyrics)'), 'Circles');
  assert.equal(cleanTrackTitle('Sunflower feat. Someone'), 'Sunflower');
  assert.equal(cleanTrackTitle('Plain Title'), 'Plain Title');
});

test('lyricsPathFor swaps the audio extension for .lyrics.json', () => {
  assert.equal(lyricsPathFor('/d', 'a b.m4a'), path.join('/d', 'a b.lyrics.json'));
  assert.equal(lyricsPathFor('/d', 'x.mp3'), path.join('/d', 'x.lyrics.json'));
});

test('writeLyricsCache / readLyricsCache round-trip', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'lyr-'));
  try {
    writeLyricsCache(tmp, 'song.m4a', { synced: '[00:01.00]Hi', plain: null, source: 'lrclib', fetchedAt: 123 });
    assert.deepEqual(readLyricsCache(tmp, 'song.m4a'), { synced: '[00:01.00]Hi', plain: null, source: 'lrclib', fetchedAt: 123 });
    assert.equal(readLyricsCache(tmp, 'missing.m4a'), null);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('getLyrics returns a cached positive without calling fetch', async () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'lyr-'));
  const orig = global.fetch;
  global.fetch = async () => { throw new Error('fetch should not be called'); };
  try {
    writeLyricsCache(tmp, 'song.m4a', { synced: '[00:01.00]Hi', plain: null, source: 'lrclib', fetchedAt: Date.now() });
    const out = await getLyrics(tmp, 'song.m4a', 'Hi', 'Artist', 0);
    assert.equal(out.synced, '[00:01.00]Hi');
    assert.equal(out.source, 'lrclib');
  } finally {
    global.fetch = orig;
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('getLyrics re-queries lrclib on a STALE negative and rewrites the cache', async () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'lyr-'));
  const orig = global.fetch;
  // /api/get returns a positive synced result.
  global.fetch = async (url) => ({
    ok: true,
    status: 200,
    json: async () => ({ syncedLyrics: '[00:02.00]Now', plainLyrics: 'Now' }),
  });
  try {
    // A negative cached 30 days ago (older than the 14-day TTL).
    const stale = Date.now() - 30 * 24 * 60 * 60 * 1000;
    writeLyricsCache(tmp, 'song.m4a', { synced: null, plain: null, source: null, fetchedAt: stale });
    const out = await getLyrics(tmp, 'song.m4a', 'Now', 'Artist', 120);
    assert.equal(out.synced, '[00:02.00]Now');
    assert.equal(readLyricsCache(tmp, 'song.m4a').synced, '[00:02.00]Now');
  } finally {
    global.fetch = orig;
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('getLyrics writes a negative on a genuine miss (get 404 + search empty)', async () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'lyr-'));
  const orig = global.fetch;
  global.fetch = async (url) => {
    const u = String(url);
    if (u.includes('/api/get')) return { ok: false, status: 404, json: async () => ({}) };
    return { ok: true, status: 200, json: async () => [] };   // /api/search → no candidates
  };
  try {
    const out = await getLyrics(tmp, 'song.m4a', 'Nope', 'Artist', 0);
    assert.equal(out.synced, null);
    assert.equal(out.plain, null);
    const cached = readLyricsCache(tmp, 'song.m4a');
    assert.equal(cached.synced, null);
    assert.equal(typeof cached.fetchedAt, 'number');   // negative was persisted
  } finally {
    global.fetch = orig;
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('downloadBaseName: different videos with the same title get different names', () => {
  const a = downloadBaseName('Song (Official Video)', 'aaaaaaaaaaa');
  const b = downloadBaseName('Song [Official Video]', 'bbbbbbbbbbb');
  assert.notEqual(a, b);
  assert.ok(a.endsWith('_aaaaaaaaaaa'));
  assert.ok(b.endsWith('_bbbbbbbbbbb'));
});

test('downloadBaseName: same video is idempotent (dedupe-friendly)', () => {
  assert.equal(downloadBaseName('My Song', 'dQw4w9WgXcQ'), downloadBaseName('My Song', 'dQw4w9WgXcQ'));
});

test('downloadBaseName: sanitizes and truncates the title to 50 chars but keeps the full id', () => {
  const base = downloadBaseName('x'.repeat(80), 'dQw4w9WgXcQ');
  assert.equal(base, `${'x'.repeat(50)}_dQw4w9WgXcQ`);
});

test('downloadBaseName: empty title falls back to the id', () => {
  assert.equal(downloadBaseName('', 'dQw4w9WgXcQ'), 'dQw4w9WgXcQ_dQw4w9WgXcQ');
});

test('displayBaseName strips the extension and a trailing 11-char id', () => {
  assert.equal(displayBaseName('Song_Name_dQw4w9WgXcQ.m4a'), 'Song_Name');
  assert.equal(displayBaseName('Old_Style_File.m4a'), 'Old_Style_File'); // legacy: nothing to strip
});

test('libraryRowFor without a sidecar does not leak the id into the title', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'rowfor-'));
  try {
    fs.writeFileSync(path.join(tmp, 'Artist___Song_dQw4w9WgXcQ.m4a'), Buffer.alloc(10));
    const row = libraryRowFor(tmp, 'Artist___Song_dQw4w9WgXcQ.m4a');
    assert.ok(!row.title.includes('dQw4w9WgXcQ'), `title leaked: ${row.title}`);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('setSidecarTitle merges the new title and keeps videoId/artist', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'title-'));
  try {
    fs.writeFileSync(path.join(tmp, 'song.m4a'), Buffer.alloc(1));
    writeSidecar(tmp, 'song.m4a', { videoId: 'dQw4w9WgXcQ', title: 'Old', artist: 'Artist' });
    setSidecarTitle(tmp, 'song.m4a', 'New Title');
    assert.deepEqual(readSidecar(tmp, 'song.m4a'), { videoId: 'dQw4w9WgXcQ', title: 'New Title', artist: 'Artist' });
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('setSidecarTitle drops the cached lyrics so the next fetch uses the new title', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'title-'));
  try {
    fs.writeFileSync(path.join(tmp, 'song.m4a'), Buffer.alloc(1));
    writeLyricsCache(tmp, 'song.m4a', { synced: '[00:01.00] la', plain: 'la', fetchedAt: Date.now() });
    setSidecarTitle(tmp, 'song.m4a', 'New Title');
    assert.equal(readLyricsCache(tmp, 'song.m4a'), null);
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});

test('setSidecarTitle creates a sidecar when none exists', () => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'title-'));
  try {
    fs.writeFileSync(path.join(tmp, 'song.m4a'), Buffer.alloc(1));
    setSidecarTitle(tmp, 'song.m4a', 'Fresh');
    assert.deepEqual(readSidecar(tmp, 'song.m4a'), { title: 'Fresh' });
  } finally {
    fs.rmSync(tmp, { recursive: true, force: true });
  }
});
