require('dotenv').config();

const express = require('express');
const cors = require('cors');
const { exec, execFile } = require('child_process');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.BACKEND_PORT || 3001;
const HOST = '0.0.0.0';
const SERVER_URL = process.env.SERVER_URL || '192.168.1.11:3001';
const DOWNLOAD_DIR = process.env.DOWNLOAD_DIR || path.join(__dirname, 'downloads');

const YOUTUBE_ID_REGEX = /^[\w-]{1,20}$/;

if (!fs.existsSync(DOWNLOAD_DIR)) {
  fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });
}

app.use(cors());
app.use(express.json());
app.use('/downloads', express.static(DOWNLOAD_DIR));

app.post('/api/search', (req, res) => {
  const query = req.body?.query;
  if (!query || typeof query !== 'string') {
    return res.status(400).json({ error: 'Query is required' });
  }
  const searchArg = `ytsearch10:${query.trim()}`;
  execFile('yt-dlp', [searchArg, '--dump-json', '--flat-playlist'], (error, stdout, stderr) => {
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
    } catch (parseError) {
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
  const command = `yt-dlp -x --audio-format m4a -o "${outputPath}" "https://www.youtube.com/watch?v=${videoId}"`;
  exec(command, (error, stdout, stderr) => {
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
      res.json({
        success: true,
        filename: `${safeTitle}.m4a`,
        path: `http://${SERVER_URL}/downloads/${safeTitle}.m4a`,
        title: title || safeTitle,
        size: stats.size,
      });
    } catch (err) {
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
        return {
          id: file,
          title: file.replace(/\.(m4a|mp3)$/, '').replace(/_/g, ' '),
          artist: 'Unknown',
          duration: 'Unknown',
          path: `http://${SERVER_URL}/downloads/${file}`,
          size: stats.size,
          dateAdded: stats.mtime,
        };
      })
      .sort((a, b) => b.dateAdded - a.dateAdded);
    res.json({ songs: files });
  } catch (error) {
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
  } catch (error) {
    res.status(500).json({ error: 'Failed to delete file' });
  }
});

app.listen(PORT, HOST, () => {
  console.log(`Server running on http://${HOST}:${PORT}`);
  console.log(`Server accessible at http://${SERVER_URL}`);
});
