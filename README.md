# YouTube Music Streamer 🎵

A React Native app for searching YouTube videos, downloading music, and managing a personal music library with a built-in music player.

## Features

- **Search**: Find YouTube videos and music
- **Download**: Download audio-only versions of videos using yt-dlp
- **Library**: View and manage downloaded songs
- **Player**: Full-featured music player with play/pause, next/previous, loop, shuffle, and queue
- **3 Tabs**: Search, Library, and Player tabs with easy navigation

## Tech Stack

- **Frontend**: React Native
- **Navigation**: React Navigation (Bottom Tabs)
- **Backend**: None (local storage for now, can add FastAPI later)
- **YouTube API**: yt-dlp (system-level wrapper via shell)
- **State Management**: React hooks (useState)
- **Icons**: React Native Vector Icons (Ionicons)

## Setup Instructions

### Prerequisites

1. **Install Node.js dependencies** (already in package.json)
   ```bash
   npm install
   ```

2. **Install React Native dependencies**
   ```bash
   npm install @react-navigation/native @react-navigation/bottom-tabs react-native-safe-area-context
   npm install react-native-vector-icons
   ```

3. **Install yt-dlp** (YouTube downloader)
   ```bash
   sudo apt install -y yt-dlp
   ```

### Project Structure

```
youtube_stream/
├── app.json                 # React Native app config
├── package.json               # Project dependencies
├── tsconfig.json              # TypeScript config
├── metro.config.js              # Metro bundler config
├── index.js                   # App entry point
├── App.tsx                   # Main app with navigation
├── src/
│   ├── screens/
│   │   ├── SearchScreen.tsx     # YouTube search
│   │   ├── LibraryScreen.tsx     # Downloaded songs
│   │   └── PlayerScreen.tsx     # Music player with controls
│   ├── components/
│   │   ├── MusicPlayer.tsx     # Player component (TODO: audio integration)
│   ├── services/
│   │   ├── youtubeService.ts      # yt-dlp wrapper (TODO)
│   │   └── musicLibrary.ts         # Local storage
│   ├── hooks/
│   │   └── useMusicPlayer.ts     # Background audio (TODO)
│   └── types/
│       └── index.ts                 # TypeScript types
├── assets/                   # Icons, images
└── README.md                   # This file
```

## Getting Started

### 1. Install Dependencies
```bash
cd /home/hh-pi/.openclaw/workspace/youtube_stream
npm install
```

### 2. Install yt-dlp on Pi
```bash
sudo apt install -y yt-dlp
```

### 3. Start Development Server
```bash
npx react-native start
```

### 4. Run on Mobile Device
```bash
# For Android
npm run android

# For iOS (requires Mac)
npm run ios
```

## Current Status

- ✅ React Native CLI installed (deprecated but working)
- ⏳ Project structure created
- ⏳ Basic screens implemented (Search, Library, Player)
- ⏳ Navigation configured (bottom tabs)
- ⚠️ Audio playback not yet implemented (needs @react-native-voice or expo-av)
- ⚠️ YouTube API integration not yet implemented (yt-dlp wrapper)
- ⚠️ Git repository not yet created

## TODO

- [ ] Implement audio playback using @react-native-voice or expo-av
- [ ] Implement yt-dlp service wrapper for YouTube downloads
- [ ] Add download progress indicators
- [ ] Implement persistent local storage for music library
- [ ] Add queue management system
- [ ] Initialize git repository and push to GitHub (hhprojects/youtube_stream)

## GitHub Repository

Will be created at: https://github.com/hhprojects/youtube_stream

---

Built with ❤️ for music lovers
