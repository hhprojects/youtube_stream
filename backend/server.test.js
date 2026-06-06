const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const { parseArtistTitle, YOUTUBE_ID_REGEX, pruneDownloads, thumbnailUrl, sidecarPathFor, readVideoId, writeVideoId } = require('./server');

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
