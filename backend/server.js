require('dotenv').config();

const express = require('express');
const cors = require('cors');
const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');
const { createCache } = require('./cache');
const discovery = require('./discovery');

const app = express();
const PORT = process.env.BACKEND_PORT || 3001;
const HOST = '0.0.0.0';
const SERVER_URL = process.env.SERVER_URL || 'localhost:3001';
const DOWNLOAD_DIR = process.env.DOWNLOAD_DIR || path.join(__dirname, 'downloads');
const DOWNLOADS_MAX_BYTES = Number(process.env.DOWNLOADS_MAX_BYTES) || 2 * 1024 * 1024 * 1024;

const YOUTUBE_ID_REGEX = /^[\w-]{1,20}$/;

// --- Podcasts ---
// Separate download bucket: podcasts are MANUAL-DELETE-ONLY (no auto-prune), so episodes never
// evict music and vice versa.
const PODCAST_DIR = process.env.PODCAST_DIR || path.join(__dirname, 'podcasts');
// Fixed curated categories for the Podcast home (edit to change interests).
const PODCAST_CATEGORIES = [
  { label: 'Programming & Dev', query: 'software engineering podcast' },
  { label: 'AI', query: 'artificial intelligence podcast' },
  { label: 'Business & Startups', query: 'startup business podcast' },
  { label: 'Finance', query: 'personal finance podcast' },
  { label: 'Self-improvement', query: 'self improvement podcast' },
];
// Featured show browseIds — resolve once via `discovery.py podcast_search "<name>"` and pin them here
// (e.g. Android Developers, Apple Developer). Empty is valid: /home just omits the Featured shelf.
const FEATURED_SHOW_IDS = [];
// Real browseIds run ~38-39 chars; the sidecar runs via execFile (no shell → no injection surface),
// so this is a generous sanity cap, not a security bound. Do NOT tighten toward 40 (some shows exceed it).
const SHOW_ID_REGEX = /^[\w-]{1,128}$/;

const DISCOVERY_TTL_MS = Number(process.env.DISCOVERY_TTL_MS) || 12 * 60 * 60 * 1000;
const discoveryCache = createCache({ ttlMs: DISCOVERY_TTL_MS });
const runDiscovery = discovery.makeRunner();

function sendDiscovery(res, promise) {
  promise
    .then((data) => res.json(data))
    .catch((e) => {
      console.warn('[discovery] failed', e.message);
      res.status(502).json({ error: 'Discovery unavailable' });   // client degrades this shelf away
    });
}

if (!fs.existsSync(DOWNLOAD_DIR)) {
  fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });
}
if (!fs.existsSync(PODCAST_DIR)) {
  fs.mkdirSync(PODCAST_DIR, { recursive: true });
}

// Shared yt-dlp error → user message mapping (used by the podcast download route).
function ytDlpErrorMessage(error, stderr) {
  const errMsg = (error.message || stderr || '').toLowerCase();
  if (errMsg.includes('ffmpeg') || errMsg.includes('ffprobe')) {
    return 'FFmpeg is required for audio conversion. Install FFmpeg and add it to your PATH.';
  }
  if (errMsg.includes('getaddrinfo failed') || errMsg.includes('failed to resolve')) {
    return 'Network error: Could not reach YouTube. Check your internet connection.';
  }
  return 'Download failed';
}

function pruneDownloads(dir = DOWNLOAD_DIR, maxBytes = DOWNLOADS_MAX_BYTES) {
  try {
    const files = fs.readdirSync(dir)
      .filter((f) => f.endsWith('.m4a') || f.endsWith('.mp3'))
      .map((f) => {
        const p = path.join(dir, f);
        const s = fs.statSync(p);
        return { name: f, path: p, size: s.size, mtime: s.mtimeMs };
      });
    let total = files.reduce((sum, f) => sum + f.size, 0);
    const removed = [];
    if (total <= maxBytes) return { removed, totalAfter: total };
    files.sort((a, b) => a.mtime - b.mtime);
    for (const f of files) {
      if (total <= maxBytes) break;
      try {
        fs.unlinkSync(f.path);
        try { fs.unlinkSync(sidecarPathFor(dir, f.name)); } catch {}
        total -= f.size;
        removed.push(f.name);
        console.log(`[prune] removed ${f.name} (${(f.size / 1024 / 1024).toFixed(1)} MB)`);
      } catch (err) {
        console.warn(`[prune] failed to remove ${f.name}`, err.message);
      }
    }
    return { removed, totalAfter: total };
  } catch (err) {
    console.warn('[prune] scan failed', err.message);
    return { removed: [], totalAfter: 0 };
  }
}

const ALLOWED_ORIGIN_PATTERNS = [
  /^https?:\/\/localhost(:\d+)?$/,
  /^https?:\/\/127\.0\.0\.1(:\d+)?$/,
  /^https?:\/\/192\.168\.\d+\.\d+(:\d+)?$/,
  /^https?:\/\/100\.\d+\.\d+\.\d+(:\d+)?$/,
];

app.use(cors({
  origin(origin, cb) {
    if (!origin) return cb(null, true);
    if (ALLOWED_ORIGIN_PATTERNS.some((re) => re.test(origin))) return cb(null, true);
    return cb(new Error('Origin not allowed'));
  },
}));
app.use(express.json());
app.use('/downloads', express.static(DOWNLOAD_DIR));
app.use('/podcasts', express.static(PODCAST_DIR));

function parseArtistTitle(rawTitle) {
  const separators = [' - ', ' — ', ' – ', ' | '];
  for (const sep of separators) {
    const idx = rawTitle.indexOf(sep);
    if (idx > 0) {
      return {
        artist: rawTitle.substring(0, idx).trim(),
        title: rawTitle.substring(idx + sep.length).trim(),
      };
    }
  }
  return { artist: 'Unknown', title: rawTitle };
}

function thumbnailUrl(videoId) {
  return videoId ? `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg` : null;
}

function sidecarPathFor(dir, audioFile) {
  return path.join(dir, audioFile.replace(/\.(m4a|mp3)$/i, '.json'));
}

function readSidecar(dir, audioFile) {
  try {
    const p = sidecarPathFor(dir, audioFile);
    if (!fs.existsSync(p)) return null;
    return JSON.parse(fs.readFileSync(p, 'utf8'));
  } catch {
    return null;
  }
}

function writeSidecar(dir, audioFile, data) {
  fs.writeFileSync(sidecarPathFor(dir, audioFile), JSON.stringify(data));
}

// Back-compat thin wrappers (existing callers/tests).
function readVideoId(dir, audioFile) {
  const sc = readSidecar(dir, audioFile);
  return sc && typeof sc.videoId === 'string' ? sc.videoId : null;
}

function writeVideoId(dir, audioFile, videoId) {
  // Merge, don't clobber: the artwork endpoint sets only videoId and must keep any title/artist.
  writeSidecar(dir, audioFile, { ...(readSidecar(dir, audioFile) || {}), videoId });
}

// One /api/library row. Prefer the sidecar's title/artist/videoId (written at download time, when the
// real " - " separator still exists) over re-deriving from the underscored filename (which can't be
// split → "Unknown"). Forward-only: files downloaded before sidecars carried title/artist still fall back.
function libraryRowFor(dir, file) {
  const stats = fs.statSync(path.join(dir, file));
  const sc = readSidecar(dir, file);
  const rawTitle = file.replace(/\.(m4a|mp3)$/, '').replace(/_/g, ' ');
  const parsed = parseArtistTitle(rawTitle);
  return {
    id: file,
    title: sc && typeof sc.title === 'string' ? sc.title : parsed.title,
    artist: sc && typeof sc.artist === 'string' ? sc.artist : parsed.artist,
    duration: 'Unknown',
    filename: file,
    downloadUrl: `http://${SERVER_URL}/downloads/${encodeURIComponent(file)}`,
    size: stats.size,
    dateAdded: stats.mtime,
    thumbnail: thumbnailUrl(sc && typeof sc.videoId === 'string' ? sc.videoId : null),
    videoId: sc && typeof sc.videoId === 'string' ? sc.videoId : null,
  };
}

app.post('/api/search', (req, res) => {
  const query = req.body?.query;
  if (!query || typeof query !== 'string') {
    return res.status(400).json({ error: 'Query is required' });
  }
  const searchArg = `ytsearch10:${query.trim()}`;
  execFile('yt-dlp', [searchArg, '--dump-json', '--flat-playlist'], { timeout: 30000 }, (error, stdout, stderr) => {
    if (error) {
      const errMsg = (error.message || stderr || '').toLowerCase();
      const isNetworkError = errMsg.includes('getaddrinfo failed') || errMsg.includes('failed to resolve') || errMsg.includes('econnrefused');
      const message = isNetworkError
        ? 'Network error: Could not reach YouTube. Check your internet connection, DNS, and firewall.'
        : 'Search failed';
      return res.status(500).json({ error: message });
    }
    try {
      const lines = stdout.trim().split('\n').filter(Boolean);
      const results = lines
        .map((line) => {
          const data = JSON.parse(line);
          return {
            id: data.id,
            title: data.title,
            channel: data.channel || data.uploader || 'Unknown',
            duration: data.duration,
            url: `https://www.youtube.com/watch?v=${data.id}`,
            thumbnail: data.thumbnail || thumbnailUrl(data.id),
          };
        })
        .filter((item) => item.id && item.title);
      res.json({ results });
    } catch {
      res.status(500).json({ error: 'Failed to parse results' });
    }
  });
});

app.post('/api/download', (req, res) => {
  const { videoId, title } = req.body || {};
  if (!videoId || !YOUTUBE_ID_REGEX.test(String(videoId))) {
    return res.status(400).json({ error: 'Valid video ID is required' });
  }
  const safeTitle = String(title || videoId).replace(/[^a-zA-Z0-9]/g, '_').substring(0, 50);
  const outputPath = path.join(DOWNLOAD_DIR, `${safeTitle}.m4a`);
  const ytUrl = `https://www.youtube.com/watch?v=${videoId}`;
  execFile('yt-dlp', ['-x', '--audio-format', 'm4a', '-o', outputPath, ytUrl], { timeout: 300000 }, (error, stdout, stderr) => {
    if (error) {
      const errMsg = (error.message || stderr || '').toLowerCase();
      let message = 'Download failed';
      if (errMsg.includes('ffmpeg') || errMsg.includes('ffprobe')) {
        message = 'FFmpeg is required for audio conversion. Install FFmpeg and add it to your PATH.';
      } else if (errMsg.includes('getaddrinfo failed') || errMsg.includes('failed to resolve')) {
        message = 'Network error: Could not reach YouTube. Check your internet connection.';
      }
      return res.status(500).json({ error: message });
    }
    try {
      const stats = fs.statSync(outputPath);
      const parsed = parseArtistTitle(title || safeTitle);   // from the ORIGINAL title (still has " - ")
      // best-effort (a sidecar miss must not fail a good download): store the real title/artist so
      // /api/library doesn't re-derive them from the separator-less filename and land on "Unknown".
      try { writeSidecar(DOWNLOAD_DIR, `${safeTitle}.m4a`, { videoId, title: parsed.title, artist: parsed.artist }); } catch {}
      res.json({
        success: true,
        filename: `${safeTitle}.m4a`,
        downloadUrl: `http://${SERVER_URL}/downloads/${encodeURIComponent(safeTitle + '.m4a')}`,
        title: parsed.title,
        artist: parsed.artist,
        size: stats.size,
      });
      pruneDownloads();
    } catch {
      res.status(500).json({ error: 'Failed to access downloaded file' });
    }
  });
});

app.get('/api/library', (req, res) => {
  try {
    const files = fs.readdirSync(DOWNLOAD_DIR)
      .filter((file) => file.endsWith('.m4a') || file.endsWith('.mp3'))
      .map((file) => libraryRowFor(DOWNLOAD_DIR, file))
      .sort((a, b) => b.dateAdded - a.dateAdded);
    res.json({ songs: files });
  } catch {
    res.status(500).json({ error: 'Failed to list library' });
  }
});

app.post('/api/library/:filename/artwork', (req, res) => {
  const filename = path.basename(req.params.filename).replace(/[^a-zA-Z0-9_.-]/g, '');
  const { videoId } = req.body || {};
  if (!filename || !(filename.endsWith('.m4a') || filename.endsWith('.mp3'))) {
    return res.status(400).json({ error: 'Invalid filename' });
  }
  if (!videoId || !YOUTUBE_ID_REGEX.test(String(videoId))) {
    return res.status(400).json({ error: 'Valid video ID is required' });
  }
  if (!fs.existsSync(path.join(DOWNLOAD_DIR, filename))) {
    return res.status(404).json({ error: 'File not found' });
  }
  try {
    writeVideoId(DOWNLOAD_DIR, filename, videoId);
    res.json({ success: true, thumbnail: thumbnailUrl(videoId) });
  } catch {
    res.status(500).json({ error: 'Failed to save artwork' });
  }
});

app.delete('/api/library/:filename', (req, res) => {
  const raw = req.params.filename;
  const filename = path.basename(raw).replace(/[^a-zA-Z0-9_.-]/g, '');
  if (!filename || !(filename.endsWith('.m4a') || filename.endsWith('.mp3'))) {
    return res.status(400).json({ error: 'Invalid filename' });
  }
  const filePath = path.join(DOWNLOAD_DIR, filename);
  try {
    if (!fs.existsSync(filePath)) {
      return res.status(404).json({ error: 'File not found' });
    }
    fs.unlinkSync(filePath);
    try { fs.unlinkSync(sidecarPathFor(DOWNLOAD_DIR, filename)); } catch {}
    res.json({ success: true });
  } catch {
    res.status(500).json({ error: 'Failed to delete file' });
  }
});

app.get('/api/discovery/trending', (req, res) => {
  let region = String(req.query.region || '').toUpperCase();
  if (!/^[A-Z]{2}$/.test(region)) region = 'US';
  sendDiscovery(res, discovery.getTrending(runDiscovery, discoveryCache, region));
});

app.get('/api/discovery/related', (req, res) => {
  const videoId = String(req.query.videoId || '');
  if (!YOUTUBE_ID_REGEX.test(videoId)) return res.status(400).json({ error: 'Valid videoId required' });
  sendDiscovery(res, discovery.getRelated(runDiscovery, discoveryCache, videoId));
});

app.get('/api/discovery/moods', (req, res) => {
  sendDiscovery(res, discovery.getMoods(runDiscovery, discoveryCache));
});

app.get('/api/discovery/mood', (req, res) => {
  const params = String(req.query.params || '');
  if (!params) return res.status(400).json({ error: 'params required' });
  sendDiscovery(res, discovery.getMood(runDiscovery, discoveryCache, params));
});

app.get('/api/discovery/genre-charts', (req, res) => {
  let region = String(req.query.region || '').toUpperCase();
  if (!/^[A-Z]{2}$/.test(region)) region = 'US';
  sendDiscovery(res, discovery.getGenreCharts(runDiscovery, discoveryCache, region));
});

app.get('/api/discovery/playlist', (req, res) => {
  const id = String(req.query.id || '');
  if (!id) return res.status(400).json({ error: 'id required' });
  sendDiscovery(res, discovery.getPlaylist(runDiscovery, discoveryCache, id));
});

// --- Podcasts ---

app.get('/api/podcasts/home', async (req, res) => {
  // Per-shelf degrade: one failing category must not blank the whole home.
  const categoryShelves = await Promise.all(PODCAST_CATEGORIES.map(async (cat) => {
    try {
      const data = await discovery.getPodcastSearch(runDiscovery, discoveryCache, cat.query);
      return { label: cat.label, shows: data.shows || [] };
    } catch {
      return { label: cat.label, shows: [] };
    }
  }));
  let featuredShelf = [];
  if (FEATURED_SHOW_IDS.length) {
    const shows = await Promise.all(FEATURED_SHOW_IDS.map(async (id) => {
      try {
        const pod = await discovery.getPodcast(runDiscovery, discoveryCache, id);
        return { showId: id, title: pod.title, thumbnail: pod.thumbnail };
      } catch {
        return null;
      }
    }));
    const ok = shows.filter(Boolean);
    if (ok.length) featuredShelf = [{ label: 'Featured shows', shows: ok }];
  }
  res.json({ shelves: [...featuredShelf, ...categoryShelves] });
});

app.get('/api/podcasts/show/:showId', (req, res) => {
  const showId = String(req.params.showId || '');
  if (!SHOW_ID_REGEX.test(showId)) return res.status(400).json({ error: 'Valid showId required' });
  sendDiscovery(res, discovery.getPodcast(runDiscovery, discoveryCache, showId));
});

app.get('/api/podcasts/search', (req, res) => {
  const q = String(req.query.q || '').trim();
  if (!q) return res.status(400).json({ error: 'q required' });
  sendDiscovery(res, discovery.getPodcastSearch(runDiscovery, discoveryCache, q));
});

app.post('/api/podcasts/shows/latest', async (req, res) => {
  const showIds = Array.isArray(req.body && req.body.showIds) ? req.body.showIds : [];
  const shows = await Promise.all(showIds.map(async (rawId) => {
    const id = String(rawId);
    if (!SHOW_ID_REGEX.test(id)) return { showId: id, title: null, episodes: [] };
    try {
      const pod = await discovery.getPodcast(runDiscovery, discoveryCache, id);  // reuses per-show cache
      return { showId: id, title: pod.title, episodes: pod.episodes || [] };     // newest-first
    } catch {
      return { showId: id, title: null, episodes: [] };
    }
  }));
  res.json({ shows });
});

app.post('/api/podcasts/download', (req, res) => {
  const { videoId, title, showName, showId, date, description, artworkUrl } = req.body || {};
  if (!videoId || !YOUTUBE_ID_REGEX.test(String(videoId))) {
    return res.status(400).json({ error: 'Valid video ID is required' });
  }
  const safeTitle = String(title || videoId).replace(/[^a-zA-Z0-9]/g, '_').substring(0, 50);
  const filename = `${safeTitle}.m4a`;
  const outputPath = path.join(PODCAST_DIR, filename);
  const ytUrl = `https://www.youtube.com/watch?v=${videoId}`;
  // 30-min ceiling: a 1-2h episode extraction can exceed the song path's 5-min timeout.
  execFile('yt-dlp', ['-x', '--audio-format', 'm4a', '-o', outputPath, ytUrl], { timeout: 1800000 }, (error, stdout, stderr) => {
    if (error) return res.status(500).json({ error: ytDlpErrorMessage(error, stderr) });
    try {
      const stats = fs.statSync(outputPath);
      // Structured sidecar built straight from the request fields — NEVER parseArtistTitle
      // (episode titles routinely contain " - ", which that helper would mangle).
      try {
        writeSidecar(PODCAST_DIR, filename, {
          videoId,
          title: title || safeTitle,
          showName: showName || 'Unknown',
          showId: showId || null,
          date: date || null,
          description: description || null,
          artworkUrl: artworkUrl || null,
        });
      } catch {}
      res.json({
        success: true,
        filename,
        downloadUrl: `http://${SERVER_URL}/podcasts/${encodeURIComponent(filename)}`,
        size: stats.size,
      });
      // No pruneDownloads() — podcasts are manual-delete-only.
    } catch {
      res.status(500).json({ error: 'Failed to access downloaded file' });
    }
  });
});

// Delete a downloaded episode from the Pi (the user's "Delete everywhere"). Mirrors the song
// DELETE /api/library/:filename — manual-delete-only, podcasts are never auto-pruned.
app.delete('/api/podcasts/episode/:filename', (req, res) => {
  const filename = path.basename(req.params.filename).replace(/[^a-zA-Z0-9_.-]/g, '');
  if (!filename || !(filename.endsWith('.m4a') || filename.endsWith('.mp3'))) {
    return res.status(400).json({ error: 'Invalid filename' });
  }
  const filePath = path.join(PODCAST_DIR, filename);
  try {
    if (!fs.existsSync(filePath)) return res.status(404).json({ error: 'File not found' });
    fs.unlinkSync(filePath);
    try { fs.unlinkSync(sidecarPathFor(PODCAST_DIR, filename)); } catch {}
    res.json({ success: true });
  } catch {
    res.status(500).json({ error: 'Failed to delete file' });
  }
});

if (require.main === module) {
  pruneDownloads();
  app.listen(PORT, HOST, () => {
    console.log(`Server running on http://${HOST}:${PORT}`);
    console.log(`Server accessible at http://${SERVER_URL}`);
    console.log(`Downloads cap: ${(DOWNLOADS_MAX_BYTES / 1024 / 1024 / 1024).toFixed(1)} GB`);
  });
}

module.exports = { app, parseArtistTitle, YOUTUBE_ID_REGEX, pruneDownloads, thumbnailUrl, sidecarPathFor, readVideoId, writeVideoId, readSidecar, writeSidecar, libraryRowFor };
