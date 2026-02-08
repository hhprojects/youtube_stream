# Quick Start Guide - YouTube Music Streamer

## Backend Server Status

✅ **Backend is running** on `http://192.168.1.11:3001`
✅ **API endpoints are working**
✅ **Static files are accessible**
✅ **Frontend configured to connect**

## Connection Test Results

```
✅ Library endpoint - Working (1 song found)
✅ Search endpoint - Working (10 results returned)
✅ Static file access - Working (audio files accessible)
✅ Download endpoint - Works with valid video IDs
```

## How to Run

### 1. Backend (Already Running)
```bash
cd /home/hh-pi/.openclaw/workspace/youtube_stream
npm run backend
```

Backend is currently running on port 3001, listening on all network interfaces.

### 2. React Native App (Frontend)

#### Start Metro Bundler
```bash
cd /home/hh-pi/.openclaw/workspace/youtube_stream
npm start
```

#### Run on Android Device
```bash
npm run android
```

#### Run on iOS Device (Mac only)
```bash
npm run ios
```

### 3. Test Connection
```bash
node test-connection.js
```

## End-to-End Flow

1. **Start Backend** (if not already running)
   ```bash
   npm run backend
   ```

2. **Start React Native App**
   ```bash
   npm start
   npm run android  # or npm run ios
   ```

3. **Use the App**
   - Open the app on your mobile device
   - Go to **Search** tab
   - Type a song name and search
   - Tap the download button on any result
   - Go to **Library** tab to see downloaded songs
   - Tap any song to play it
   - Go to **Player** tab for full controls

## Network Requirements

- ✅ Backend runs on `http://192.168.1.11:3001`
- ✅ Your mobile device must be on the **same WiFi network** as the Pi
- ✅ Firewall allows connections on port 3001

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/search | Search YouTube videos |
| POST | /api/download | Download audio from YouTube |
| GET | /api/library | List downloaded songs |
| DELETE | /api/library/:filename | Delete a song |
| GET | /downloads/* | Serve downloaded audio files |

## Current State

- ✅ Backend API fully operational
- ✅ Frontend connected to backend
- ✅ Network access enabled
- ✅ Static file serving working
- ✅ 1 test song downloaded: "Dancing With A Stranger.m4a"
- ✅ All code committed and pushed to GitHub

## Troubleshooting

### App can't connect to backend
- Make sure your mobile device is on the same WiFi as the Pi
- Check if backend is running: `ps aux | grep "node backend/server.js"`
- Test from mobile browser: `http://192.168.1.11:3001/api/library`

### Download fails
- Check backend logs for yt-dlp errors
- Verify yt-dlp is installed: `yt-dlp --version`
- Try with a different video

### Audio won't play
- Check if file exists in `backend/downloads/`
- Verify file is accessible: `curl -I http://192.168.1.11:3001/downloads/filename.m4a`
- Check app permissions for audio playback

## Next Steps

1. **Test on Real Device**
   - Connect your Android/iOS device
   - Make sure it's on the same WiFi
   - Run the app and test all features

2. **Optional Enhancements**
   - Add download progress indicators
   - Implement playlists
   - Add background audio support
   - Create user settings

## Repository

GitHub: https://github.com/hhprojects/youtube_stream
Branch: main
Latest Commit: f5ff75f

---

**Ready to use!** 🎉
