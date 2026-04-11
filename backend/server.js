require('dotenv').config();

const express = require('express');
const cors = require('cors');
const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.BACKEND_PORT || 3001;
const HOST = '0.0.0.0';
const SERVER_URL = process.env.SERVER_URL || '100.87.0.56:3001';
const DOWNLOAD_DIR = process.env.DOWNLOAD_DIR || path.join(__dirname, 'downloads');
const DOWNLOADS_MAX_BYTES = Number(process.env.DOWNLOADS_MAX_BYTES) || 2 * 1024 * 1024 * 1024;

const YOUTUBE_ID_REGEX = /^[\w-]{1,20}$/;

if (!fs.existsSync(DOWNLOAD_DIR)) {
  fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });
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
            thumbnail: data.thumbnail || `https://i.ytimg.com/vi/${data.id}/hqdefault.jpg`,
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
      const parsed = parseArtistTitle(title || safeTitle);
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
      .map((file) => {
        const filePath = path.join(DOWNLOAD_DIR, file);
        const stats = fs.statSync(filePath);
        const rawTitle = file.replace(/\.(m4a|mp3)$/, '').replace(/_/g, ' ');
        const parsed = parseArtistTitle(rawTitle);
        return {
          id: file,
          title: parsed.title,
          artist: parsed.artist,
          duration: 'Unknown',
          filename: file,
          downloadUrl: `http://${SERVER_URL}/downloads/${encodeURIComponent(file)}`,
          size: stats.size,
          dateAdded: stats.mtime,
        };
      })
      .sort((a, b) => b.dateAdded - a.dateAdded);
    res.json({ songs: files });
  } catch {
    res.status(500).json({ error: 'Failed to list library' });
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

module.exports = { app, parseArtistTitle, YOUTUBE_ID_REGEX, pruneDownloads };
