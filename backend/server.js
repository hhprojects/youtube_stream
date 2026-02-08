const express = require('express');
const cors = require('cors');
const { exec } = require('child_process');
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = 3001;
const HOST = '0.0.0.0'; // Listen on all network interfaces
const SERVER_URL = '192.168.1.11:3001'; // Pi's IP address
const DOWNLOAD_DIR = path.join(__dirname, 'downloads');

// Ensure downloads directory exists
if (!fs.existsSync(DOWNLOAD_DIR)) {
  fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });
}

app.use(cors());
app.use(express.json());
app.use('/downloads', express.static(DOWNLOAD_DIR));

// Search YouTube
app.post('/api/search', async (req, res) => {
  const { query } = req.body;
  
  if (!query) {
    return res.status(400).json({ error: 'Query is required' });
  }

  const command = `yt-dlp "ytsearch10:${query}" --dump-json --flat-playlist`;
  
  exec(command, (error, stdout, stderr) => {
    if (error) {
      console.error('Search error:', error);
      return res.status(500).json({ error: 'Search failed' });
    }

    try {
      const lines = stdout.trim().split('\n');
      const results = lines
        .map(line => {
          const data = JSON.parse(line);
          return {
            id: data.id,
            title: data.title,
            channel: data.channel || data.uploader || 'Unknown',
            duration: data.duration,
            url: `https://www.youtube.com/watch?v=${data.id}`,
            thumbnail: data.thumbnail || `https://i.ytimg.com/vi/${data.id}/hqdefault.jpg`
          };
        })
        .filter(item => item.id && item.title);
      
      res.json({ results });
    } catch (parseError) {
      console.error('Parse error:', parseError);
      res.status(500).json({ error: 'Failed to parse results' });
    }
  });
});

// Download audio
app.post('/api/download', async (req, res) => {
  const { videoId, title } = req.body;
  
  if (!videoId) {
    return res.status(400).json({ error: 'Video ID is required' });
  }

  const safeTitle = title.replace(/[^a-zA-Z0-9]/g, '_').substring(0, 50);
  const outputPath = path.join(DOWNLOAD_DIR, `${safeTitle}.m4a`);
  
  const command = `yt-dlp -x --audio-format m4a -o "${outputPath}" "https://www.youtube.com/watch?v=${videoId}"`;
  
  exec(command, (error, stdout, stderr) => {
    if (error) {
      console.error('Download error:', error);
      return res.status(500).json({ error: 'Download failed' });
    }

    // Get file info
    try {
      const stats = fs.statSync(outputPath);
      res.json({
        success: true,
        filename: `${safeTitle}.m4a`,
        path: `http://${SERVER_URL}/downloads/${safeTitle}.m4a`,
        title: title,
        size: stats.size
      });
    } catch (err) {
      console.error('File error:', err);
      res.status(500).json({ error: 'Failed to access downloaded file' });
    }
  });
});

// List downloaded songs
app.get('/api/library', (req, res) => {
  try {
    const files = fs.readdirSync(DOWNLOAD_DIR)
      .filter(file => file.endsWith('.m4a') || file.endsWith('.mp3'))
      .map(file => {
        const filePath = path.join(DOWNLOAD_DIR, file);
        const stats = fs.statSync(filePath);
        return {
          id: file,
          title: file.replace(/\.(m4a|mp3)$/, '').replace(/_/g, ' '),
          artist: 'Unknown',
          duration: 'Unknown',
          path: `http://${SERVER_URL}/downloads/${file}`,
          size: stats.size,
          dateAdded: stats.mtime
        };
      })
      .sort((a, b) => b.dateAdded - a.dateAdded);
    
    res.json({ songs: files });
  } catch (error) {
    console.error('Library error:', error);
    res.status(500).json({ error: 'Failed to list library' });
  }
});

// Delete song
app.delete('/api/library/:filename', (req, res) => {
  const { filename } = req.params;
  const filePath = path.join(DOWNLOAD_DIR, filename);
  
  try {
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
      res.json({ success: true });
    } else {
      res.status(404).json({ error: 'File not found' });
    }
  } catch (error) {
    console.error('Delete error:', error);
    res.status(500).json({ error: 'Failed to delete file' });
  }
});

app.listen(PORT, HOST, () => {
  console.log(`Server running on http://${HOST}:${PORT}`);
  console.log(`Server accessible at http://${SERVER_URL}`);
  console.log(`Downloads directory: ${DOWNLOAD_DIR}`);
});
