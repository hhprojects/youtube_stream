# YouTube Music Streamer 🎵

A React Native app for searching YouTube videos, downloading music, and managing a personal music library with a built-in music player.

## Project Structure

```
youtube_stream/
├── backend/                 # Node.js + Express API server
│   ├── server.js            # API endpoints
│   └── downloads/           # Downloaded audio files
├── frontend/                # React Native frontend
│   ├── src/
│   │   ├── screens/         # App screens
│   │   ├── services/        # API client & audio player
│   │   ├── hooks/          # Custom React hooks
│   │   ├── components/      # Reusable components
│   │   ├── types/          # TypeScript definitions
│   │   ├── config/         # Configuration
│   │   ├── navigation/      # Navigation setup
│   │   └── assets/         # App assets
│   ├── hooks/              # Git hooks
│   └── routers/            # Backend routers (if any)
├── App.tsx                 # Main app component
├── index.js                # App entry point
├── app.json                # React Native config
├── metro.config.js          # Metro bundler config
├── tsconfig.json           # TypeScript config
├── package.json            # Dependencies and scripts
└── .env.example           # Environment variables template
```

## Features

- **Search**: Find YouTube videos and music using yt-dlp
- **Download**: Download audio-only versions of videos using yt-dlp
- **Library**: View and manage downloaded songs
- **Player**: Full-featured music player with play/pause, next/previous, loop, shuffle, and queue
- **3 Tabs**: Search, Library, and Player tabs with easy navigation

## Tech Stack

- **Frontend**: React Native 0.74.7 (compatible with Node.js >= 18)
- **Navigation**: React Navigation 6.x (Bottom Tabs)
- **Backend**: Node.js + Express 4.21.0
- **YouTube API**: yt-dlp 2025.04.30
- **State Management**: React hooks (useState)
- **Audio Playback**: expo-av 14.0.0
- **Icons**: React Native Vector Icons 10.0.0
- **HTTP Client**: Axios 1.8.0

**Note**: React Native 0.74.7 works with Node.js 20.12.2 without engine warnings. See `NODE_VERSION_FIX.md` for details.

## Setup Instructions

### Prerequisites

1. **Install Node.js dependencies**
   ```bash
   cd youtube_stream
   npm install
   ```

   **Note**: Requires Node.js >= 18 (tested with Node.js 20.12.2)

2. **Install yt-dlp** (YouTube downloader)
   ```bash
   sudo apt install -y yt-dlp
   ```

3. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

### Running the Backend

The backend API server is required for the app to function. Start it with:

```bash
npm run backend
```

The backend will run on `http://localhost:3001` (or configured port) and serves:
- `/api/search` - YouTube search endpoint
- `/api/download` - Download audio endpoint
- `/api/library` - List downloaded songs
- `/downloads/` - Serve downloaded audio files

### Running the React Native App

#### Start Metro Bundler
```bash
npm start
```

#### Run on Android
```bash
npm run android
```

#### Run on iOS (requires Mac)
```bash
npm run ios
```

## End-to-End Flow

1. **Search**: Use the Search tab to find music on YouTube
2. **Download**: Tap the download button on any search result
3. **Library**: View all downloaded songs in the Library tab
4. **Play**: Tap any song to play it in the Player tab
5. **Controls**: Use play/pause, next/previous, loop, and shuffle controls

## Current Status

- ✅ Backend API server (Express + yt-dlp integration)
- ✅ YouTube search functionality
- ✅ Audio download (m4a format)
- ✅ Local library management
- ✅ Audio playback with controls
- ✅ Playlist/queue support
- ✅ Loop and shuffle modes
- ✅ Environment variables support
- ✅ Frontend/Backend separation

## API Endpoints

### POST /api/search
Search YouTube videos.

Request:
```json
{
  "query": "song name"
}
```

Response:
```json
{
  "results": [
    {
      "id": "videoId",
      "title": "Video Title",
      "channel": "Channel Name",
      "duration": 180,
      "url": "https://youtube.com/watch?v=...",
      "thumbnail": "https://i.ytimg.com/vi/..."
    }
  ]
}
```

### POST /api/download
Download audio from a YouTube video.

Request:
```json
{
  "videoId": "videoId",
  "title": "Video Title"
}
```

Response:
```json
{
  "success": true,
  "filename": "song_name.m4a",
  "path": "http://localhost:3001/downloads/song_name.m4a",
  "title": "Song Name",
  "size": 3166078
}
```

### GET /api/library
List all downloaded songs.

Response:
```json
{
  "songs": [
    {
      "id": "song_name.m4a",
      "title": "Song Name",
      "artist": "Unknown",
      "duration": "Unknown",
      "path": "http://localhost:3001/downloads/song_name.m4a",
      "size": 3166078,
      "dateAdded": "2026-01-31T11:18:13.000Z"
    }
  ]
}
```

### DELETE /api/library/:filename
Delete a song from the library.

Response:
```json
{
  "success": true
}
```

## Environment Variables

Create a `.env` file from `.env.example`:

```bash
cp .env.example .env
```

Available variables:
- `REACT_APP_API_URL` - Backend API URL for frontend
- `BACKEND_PORT` - Backend server port (default: 3001)
- `DOWNLOAD_DIR` - Download directory path (default: ./backend/downloads)

## TODO / Future Enhancements

- [ ] Add download progress indicators
- [ ] Implement persistent local storage for music library metadata
- [ ] Add playlist creation/management
- [ ] Support for multiple audio formats
- [ ] Audio visualization
- [ ] Lyrics integration
- [ ] Share functionality
- [ ] Background audio support

## Documentation

- **README.md** - This file
- **QUICKSTART.md** - Quick start guide
- **LOCAL_DEV.md** - Local machine development with emulator
- **STATUS.md** - Detailed project status and to-do list
- **WARNINGS_FIX.md** - Installation warnings and fixes
- **DEPENDENCY_UPDATE.md** - Dependency update history

## GitHub Repository

https://github.com/hhprojects/youtube_stream

---

Built with ❤️ for music lovers
