# YouTube Streamer

An offline-first music player. A Raspberry Pi backend fetches audio from YouTube
via `yt-dlp`; a **native Android app** (Kotlin / Jetpack Compose / Media3)
downloads those files onto the device for local playback — with background audio
and lock-screen controls.

> **Status.** The backend is complete and runs in production on a Raspberry Pi.
> The native Android client (Kotlin / Compose / Media3) is in progress and lands
> under `android/`.

## Architecture

```
  ┌────────────────────┐   HTTP (cleartext)   ┌────────────────────┐
  │  Android app       │ ───────────────────▶ │  Pi backend        │
  │  Kotlin · Compose  │   /api/search         │  Express + yt-dlp  │
  │  Media3 (ExoPlayer)│   /api/download       │  <PI_IP>:3001      │
  │                    │ ◀─────────────────── │                    │
  │  Room + DataStore  │   m4a file (static)   │  /downloads/*.m4a  │
  └────────────────────┘                       └────────────────────┘
           │
           ▼
   app-private storage   (filesDir/songs/*.m4a — no storage permission)
```

Two-step download flow:
1. App calls `POST /api/download` → Pi runs `yt-dlp -x --audio-format m4a` →
   writes the file to `backend/downloads/`.
2. App streams the file from `/downloads/<file>.m4a` into `filesDir/songs/` and
   records the metadata locally (Room).

After that the app plays from the local file. The Pi keeps its copy so any device
can re-import via the Import screen.

## Repo layout

```
youtube_stream/
├── backend/                 # Node/Express + yt-dlp API (runs on the Pi)
│   ├── server.js            # search / download / library endpoints + static files
│   ├── server.test.js       # node --test suite (pure, no network)
│   ├── package.json         # self-contained: express, cors, dotenv
│   └── downloads/           # Pi-side m4a cache (gitignored)
└── android/                 # native Android app (Kotlin/Compose/Media3) — in progress
```

## Backend API

| Endpoint | Request | Response |
|---|---|---|
| `POST /api/search` | `{query}` | `{results:[{id,title,channel,duration?,url?,thumbnail?}]}` |
| `POST /api/download` | `{videoId,title}` | `{success,filename,downloadUrl,title,artist,size}` |
| `GET /api/library` | — | `{songs:[{id,title,artist,duration,filename,downloadUrl,size,dateAdded}]}` |
| `DELETE /api/library/:filename` | — | deletes the Pi-side copy |
| `GET /downloads/*` | — | static `.m4a` files |

## Quick start (backend)

```bash
cd backend
cp .env.example .env      # set SERVER_URL to your Pi's reachable IP:port
npm install               # express, cors, dotenv  (yt-dlp + ffmpeg are system binaries)
npm start                 # → http://0.0.0.0:3001
npm test                  # node --test
```

## Tech stack

| Layer | What |
|---|---|
| Backend | Node 18+, Express 4.21, `yt-dlp` (system binary), FFmpeg |
| Android | Kotlin, Jetpack Compose + Material 3, Media3 (ExoPlayer + `MediaSessionService`), Retrofit, Room, DataStore, Hilt |
| Transport | HTTP over LAN / Tailscale (cleartext — keep it off the public internet) |
