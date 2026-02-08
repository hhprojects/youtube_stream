# YouTube Music Streamer 🎵

A React Native app for searching YouTube videos, downloading music, and managing a personal music library with a built-in music player.

## Features

- **Search**: Find YouTube videos and music using yt-dlp
- **Download**: Download audio-only versions of videos using yt-dlp
- **Library**: View and manage downloaded songs
- **Player**: Full-featured music player with play/pause, next/previous, loop, shuffle, and queue
- **3 Tabs**: Search, Library, and Player tabs with easy navigation

## Tech Stack

- **Frontend**: React Native 0.74.6
- **Navigation**: React Navigation (Bottom Tabs)
- **Backend**: Node.js + Express API
- **YouTube API**: yt-dlp (system-level wrapper)
- **State Management**: React hooks (useState)
- **Audio Playback**: expo-av
- **Icons**: React Native Vector Icons (Ionicons)

## Project Structure

```
youtube_stream/
├── backend/
│   ├── server.js              # Express API server
│   └── downloads/              # Downloaded music files
├── src/
│   ├── screens/
│   │   ├── SearchScreen.tsx    # YouTube search
│   │   ├── LibraryScreen.tsx   # Downloaded songs
│   │   └── PlayerScreen.tsx   # Music player
│   ├── services/
│   │   ├── api.ts             # API client
│   │   └── audioPlayer.ts     # Audio player service
│   ├── hooks/
│   │   └── useMusicPlayer.ts  # Player state hook
│   └── types/
│       └── index.ts           # TypeScript types
├── app.json                   # React Native app config
├── package.json               # Project dependencies
├── tsconfig.json              # TypeScript config
└── README.md                  # This file
```

## Setup Instructions

### Prerequisites

1. **Install Node.js dependencies**
   ```bash
   cd /home/hh-pi/.openclaw/workspace/youtube_stream
   npm install --legacy-peer-deps
   ```

2. **Install yt-dlp** (YouTube downloader)
   ```bash
   sudo apt install -y yt-dlp
   ```

### Running the Backend

The backend API server is required for the app to function. Start it with:

```bash
npm run backend
```

The backend will run on `http://localhost:3001` and serves:
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
- ⏳ React Native mobile app (ready to run)

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

## TODO / Future Enhancements

- [ ] Add download progress indicators
- [ ] Implement persistent local storage for music library metadata
- [ ] Add playlist creation/management
- [ ] Support for multiple audio formats
- [ ] Audio visualization
- [ ] Lyrics integration
- [ ] Share functionality

## GitHub Repository

https://github.com/hhprojects/youtube_stream

---

Built with ❤️ for music lovers
