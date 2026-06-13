# YouTube Streamer

An offline-first **music + podcast** player. A Raspberry Pi backend fetches audio
from YouTube via `yt-dlp`; a **native Android app** (Kotlin / Jetpack Compose /
Media3) downloads those files onto the device for local playback — with background
audio, lock-screen controls, and a home-screen widget.

> **Status.** The backend runs in production on a Raspberry Pi. The native Android
> client (`android/`) is feature-rich: search & download, an offline library with
> playlists (manual + smart) and bulk management, a Spotify-style discovery home,
> a podcast tab (follow / download / resume), an expandable now-playing player with
> an editable queue and sleep timer, synced lyrics, and a background download queue.

## Architecture

```
  ┌─────────────────────────┐   HTTP (cleartext, LAN/Tailscale)   ┌──────────────────────────┐
  │  Android app            │ ─────────────────────────────────▶ │  Pi backend              │
  │  Kotlin · Compose · M3  │   search / download / library       │  Express + yt-dlp        │
  │  Media3 (ExoPlayer)     │   discovery · podcasts · lyrics      │  + ytmusicapi (Python)   │
  │  Room · DataStore       │ ◀───────────────────────────────── │  <PI_IP>:3001            │
  │  Retrofit · Coil        │   m4a file (static) + JSON metadata  │  /downloads/*.m4a        │
  └─────────────────────────┘                                      └──────────────────────────┘
           │
           ▼
   app-private storage   (filesDir/songs, filesDir/episodes — no storage permission)
```

The core is a **two-step download flow** (audio is always played from a local file —
the app is offline-first):

1. App calls `POST /api/download` → Pi runs `yt-dlp -x --audio-format m4a` → writes the
   file to `backend/downloads/`.
2. App streams that file into app-private storage and records the metadata in Room.

The Pi keeps its copy so any device can re-import via the Import screen. On top of the
download flow, the Pi also serves **read-only metadata** the app renders directly:
discovery shelves (charts / moods / related, via `ytmusicapi`), podcast show & episode
listings, and synced lyrics (lrclib, cached).

## Repo layout

```
youtube_stream/
├── backend/                  # Node/Express + yt-dlp API (runs on the Pi)
│   ├── server.js             #   all HTTP endpoints + static /downloads
│   ├── discovery.py          #   ytmusicapi sidecar: charts, moods, related, podcast search/home
│   ├── discovery.js          #   Node wrapper + cache around the Python sidecar
│   ├── cache.js              #   TTL cache (discovery / lyrics; negative caching)
│   ├── podcasts/ · scripts/  #   podcast helpers + drift-guard / maintenance scripts
│   ├── *.test.js             #   `node --test` suites (pure, no network)
│   └── downloads/            #   Pi-side m4a cache (gitignored)
└── android/                  # native Android app (Kotlin / Compose / Media3)
    └── app/src/main/java/com/youtubestream/app/
        ├── MainActivity.kt · App.kt   # single Activity; App builds the AppContainer
        ├── di/AppContainer.kt         # manual DI (Hilt deferred) — app-scoped singletons
        ├── playback/                  # the ONLY package allowed to import Media3
        │   ├── PlaybackService.kt     #   MediaSessionService (background audio + lock screen)
        │   ├── PlaybackConnection.kt  #   MediaController → StateFlow<PlayerUiState>
        │   ├── PlaybackController.kt  #   UI-facing interface (JVM-testable)
        │   └── …                      #   queue persistence, repeat/shuffle/speed/sleep, podcast resume
        ├── data/
        │   ├── local/                 #   Room (DB v9): songs, playlists, play-events, podcasts, lyrics, recents
        │   ├── remote/                #   Retrofit API + DTOs to the Pi
        │   ├── repository/            #   library, playlist, discovery, podcast, search, lyrics, download…
        │   ├── settings/              #   DataStore (server URL, shuffle/repeat persisted)
        │   └── network/ · model/ · util/
        ├── ui/                        # Compose; observes StateFlow, never touches Media3
        │   ├── home/ search/ library/ playlist/ podcast/ discover/ player/ download/ imports/ settings/
        │   └── components/ navigation/ selection/ theme/
        ├── lyrics/                    # pure LRC parser + current-line selector (JVM-tested)
        └── notifications/ · widget/   # media notification + home-screen media widget
```

## Backend API

**Core (download / library):**

| Endpoint | Request | Response |
|---|---|---|
| `POST /api/search` | `{query}` | `{results:[{id,title,channel,duration?,url?,thumbnail?}]}` |
| `POST /api/download` | `{videoId,title}` | `{success,filename,downloadUrl,title,artist,size}` |
| `GET /api/library` | — | `{songs:[{id,title,artist,duration,filename,downloadUrl,size,dateAdded}]}` |
| `DELETE /api/library/:filename` | — | deletes the Pi-side copy |
| `POST /api/library/:filename/artwork` | `{videoId}` | re-points artwork to a video's thumbnail |
| `GET /downloads/*` | — | static `.m4a` files |

**Discovery (ytmusicapi, cached):** `GET /api/discovery/trending · /moods · /mood · /genre-charts · /playlist · /related`

**Podcasts:** `GET /api/podcasts/home · /fresh · /search · /show/:showId` · `POST /api/podcasts/download · /shows/latest` · `DELETE /api/podcasts/episode/:filename`

**Lyrics:** `GET /api/lyrics` (lrclib-backed, cached with TTL'd negatives)

## Quick start (backend)

```bash
cd backend
cp .env.example .env      # set SERVER_URL to your Pi's reachable IP:port
npm install               # express, cors, dotenv  (yt-dlp + ffmpeg are system binaries)
pip install ytmusicapi    # Python sidecar for discovery + podcasts
npm start                 # → http://0.0.0.0:3001
npm test                  # node --test
```

## Android architecture (in brief)

Single-Activity Compose app. **One `ExoPlayer`**, owned by `playback/PlaybackService`
(a `MediaSessionService`); the media notification + lock-screen controls come from Media3.
The UI never touches the player — it goes through `PlaybackConnection` (wraps a Media3
`MediaController`, mirrors events into a `StateFlow<PlayerUiState>`) and observes that flow.
**DI is manual:** `AppContainer` constructs the app-scoped singletons (repositories, the
playback connection, download queues); **Hilt is deferred**. Pure logic (mappers, LRC
parsing, shelf ranking, selection state) has zero Android imports and is JVM-unit-tested.

## Tech stack

| Layer | What |
|---|---|
| Backend | Node 18+, Express 4.21, `yt-dlp` + FFmpeg (system binaries), Python `ytmusicapi` sidecar |
| Android — UI | Kotlin, Jetpack Compose + Material 3 (M3 Expressive), Navigation-Compose, Coil 3 |
| Android — media | Media3 1.10 (ExoPlayer + `MediaSessionService`), home-screen widget |
| Android — data | Retrofit + kotlinx-serialization + OkHttp, Room (via KSP, DB v9), DataStore, WorkManager |
| Android — DI | **manual `AppContainer`** (Hilt deferred — see `CLAUDE.md`) |
| Transport | HTTP over LAN / Tailscale (cleartext — keep it off the public internet) |
```
