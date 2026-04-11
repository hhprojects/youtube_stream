# YouTube Streamer

An offline-first React Native music player. A Raspberry Pi backend fetches audio from YouTube via `yt-dlp`, and the Expo app downloads those files onto the device for local playback — including lock-screen controls and background audio.

## Architecture

```
  ┌────────────────┐   HTTP (cleartext)   ┌────────────────────┐
  │  Expo app      │ ───────────────────▶ │  Pi backend        │
  │  (Android)     │   /api/search         │  Express + yt-dlp  │
  │                │   /api/download       │  192.168.1.11:3001 │
  │  expo-audio    │ ◀─────────────────── │                    │
  │  expo-file-sys │   m4a file (static)   │  /downloads/*.m4a  │
  └────────────────┘                       └────────────────────┘
         │
         ▼
  device file system   (documentDirectory/songs/*.m4a)
  AsyncStorage         (library metadata: @local_library)
```

Two-step download flow:
1. App calls `POST /api/download` → Pi runs `yt-dlp -x --audio-format m4a` → writes file to `backend/downloads/`.
2. App uses `expo-file-system` `createDownloadResumable` to pull the file from `/downloads/<file>.m4a` into `documentDirectory/songs/` and records metadata in AsyncStorage.

After that the app plays from the local file URI. The Pi copy is kept so the song can be re-imported on any device via the Import screen.

## Tech stack

| Layer | What |
|---|---|
| Runtime | Expo SDK 54, React Native 0.81.5, React 19.1, TypeScript 5.9 |
| Audio | `expo-audio` 55.0.5 (patched — see `patches/`) |
| Storage | `expo-file-system` 19 (legacy import), `@react-native-async-storage/async-storage` 2.2 |
| Navigation | React Navigation 7 bottom tabs |
| Backend | Node 18+, Express 4.21, `yt-dlp` (system binary), FFmpeg |
| Build | EAS (dev/preview/production all produce APKs) |

`newArchEnabled` is off. `expo-audio` is patched via `patch-package` to add lock-screen **Next / Previous** buttons on Android (`patches/expo-audio+55.0.5.patch`).

## Project layout

```
youtube_stream/
├── App.tsx                        # Tab navigator + AudioProvider
├── AppEntry.js                    # Expo entry
├── backend/
│   ├── server.js                  # Express API (search / download / library)
│   └── downloads/                 # Pi-side m4a cache
├── frontend/src/
│   ├── screens/
│   │   ├── SearchScreen.tsx       # YouTube search + single download
│   │   ├── LibraryScreen.tsx      # On-device songs, play + delete
│   │   ├── PlayerScreen.tsx       # Full player + queue dropdown
│   │   └── ImportScreen.tsx       # Bulk pull from Pi → device
│   ├── services/
│   │   ├── audioPlayer.tsx        # AudioProvider (expo-audio + queue + shuffle + repeat)
│   │   ├── api.ts                 # axios client → Pi
│   │   └── localLibrary.ts        # File download + AsyncStorage metadata
│   ├── hooks/useMusicPlayer.ts    # Thin facade over AudioProvider
│   ├── components/MiniPlayer.tsx  # Persistent bottom bar
│   ├── config/apiConfig.ts        # API_BASE_URL resolution
│   └── types/index.ts             # Song, SearchResult, PiSong, AppRepeatMode
├── patches/expo-audio+55.0.5.patch
├── app.json / eas.json
└── android/app/src/main/AndroidManifest.xml
```

## Features

- **Search** — `ytsearch10:` via `yt-dlp` on the Pi, returns 10 results with thumbnails.
- **Download** — one-tap download with live progress (`%` of bytes written).
- **Bulk import** — Library → cloud icon opens ImportScreen, lists Pi songs that aren't on the device, multi-select + download.
- **Local library** — persisted in AsyncStorage, file stored under `documentDirectory/songs/`. Delete removes the local copy only (the Pi copy stays).
- **Player** — full controls, scrubbing slider, 3-state repeat (off / one / queue), shuffle with position preservation, expandable up-next list.
- **Mini player** — persistent bottom bar on Search / Library with play-pause, next, time.
- **Background + lock screen** — `shouldPlayInBackground: true`, Android media notification with play/pause + next/previous (via the patch).

## Setup

> For the full end-to-end walkthrough (Pi install, systemd service, EAS build, installing the APK on your phone), see [`docs/setup.md`](docs/setup.md). The sections below are the short version.

### Pi / backend

```bash
# On the Pi (or anywhere running the backend)
sudo apt install -y yt-dlp ffmpeg   # both required
npm install
npm run backend                     # listens on 0.0.0.0:3001
```

Backend env vars (optional, all have defaults):

| Var | Default | Purpose |
|---|---|---|
| `BACKEND_PORT` | `3001` | Listen port |
| `SERVER_URL` | `100.87.0.56:3001` | Host:port embedded in `downloadUrl` responses — must be reachable from the phone |
| `DOWNLOAD_DIR` | `./backend/downloads` | Where yt-dlp writes files |
| `DOWNLOADS_MAX_BYTES` | `2147483648` (2 GiB) | Max total size of `DOWNLOAD_DIR`. On startup and after each download, oldest files are pruned until total is under the cap. |

### App / frontend

Because this is an EAS-built app (not Expo Go), background audio and the lock-screen patch only work in a dev client or standalone build.

```bash
npm install                         # runs patch-package via postinstall
npx expo prebuild                   # if android/ needs regenerating
eas build --profile development --platform android
```

API URL resolution (`frontend/src/config/apiConfig.ts`):

```ts
API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://100.87.0.56:3001/api';
```

Set `EXPO_PUBLIC_API_URL` at build time (e.g., in `eas.json` env or `.env.local`) to point at your Pi. The baked-in default is a Tailscale IP and will not work on a normal LAN without Tailscale installed on the device.

### Android cleartext

`app.json` sets `android.usesCleartextTraffic: true` and `AndroidManifest.xml` mirrors it — the Pi is served over plain HTTP on the LAN. Do not ship this app to a public network without adding TLS.

## API

### `POST /api/search`
```json
// request
{ "query": "string" }
// response
{ "results": [{ "id", "title", "channel", "duration", "url", "thumbnail" }] }
```

### `POST /api/download`
```json
// request
{ "videoId": "string(^[\\w-]{1,20}$)", "title": "string" }
// response
{
  "success": true,
  "filename": "Safe_Title.m4a",
  "downloadUrl": "http://<SERVER_URL>/downloads/Safe_Title.m4a",
  "title": "Parsed title",
  "artist": "Parsed artist",
  "size": 3166078
}
```

### `GET /api/library`
Lists every `.m4a`/`.mp3` under `DOWNLOAD_DIR`, newest first. Used by ImportScreen.
```json
{ "songs": [{ "id", "title", "artist", "duration": "Unknown",
              "filename", "downloadUrl", "size", "dateAdded" }] }
```

### `DELETE /api/library/:filename`
Filename is sanitised with `path.basename` + strict regex before deletion. The app does **not** currently call this endpoint — delete is device-only, and the Pi copy is kept so the song can be re-imported later.

### `GET /downloads/*`
Static file server (`express.static`) for the m4a cache. Unauthenticated — assume the Pi is on a trusted network. CORS is restricted to `localhost`, `127.0.0.1`, `192.168.*`, and `100.*` (Tailscale). React Native clients don't send an `Origin` header so the app itself is unaffected; the whitelist only matters for browsers.

## Scripts

| Command | What |
|---|---|
| `npm start` | Expo dev server |
| `npm run android` | Expo dev server, open Android |
| `npm run ios` | Expo dev server, open iOS |
| `npm run backend` | Start the Pi backend (`node backend/server.js`) |
| `npm run typecheck` | `tsc --noEmit` — verify the whole codebase compiles |
| `npm run lint` | `expo lint` with the Node env layered for `backend/**/*.js` |
| `npm test` | `node --test backend/**/*.test.js` — backend unit tests |

Run `npm run typecheck && npm run lint && npm test` before any EAS build.

## Known issues / leftover items

### Intentional choices (not bugs)
- ImportScreen is registered as a hidden tab (`tabBarButton: () => null`) and reached via the cloud-download button on Library. Unusual, but deliberate.
- Delete only removes the local copy; the Pi keeps the original so the song can be re-imported. The confirm dialog says so.
- Android `usesCleartextTraffic: true` is intentional — the Pi is on HTTP on a trusted network. Do not ship this app to the public internet without adding TLS.
- `frontend/src/config/apiConfig.ts` defaults to the Tailscale IP `100.87.0.56:3001`. Override with `EXPO_PUBLIC_API_URL` at build time for non-Tailscale builds. The backend's `SERVER_URL` default matches.

### Minor / left as-is
- `patches/expo-audio+55.0.5.patch` is locked to the exact version `55.0.5`. Bumping `expo-audio` will require regenerating the patch (edit in `node_modules/expo-audio`, then `npx patch-package expo-audio`).
- `shuffleArray` uses reference equality to pin the current track when toggling shuffle — fine today, would be fragile if the playlist were ever cloned deeply upstream.
- `LibraryScreen` refreshes via `useFocusEffect`, so the Search→download→navigate flow has a cosmetic race if the user taps fast. Catches up on the next focus.
- `yt-dlp` version is not pinned; the Pi uses whatever `apt` or `pip` gave it. YouTube breaks older versions regularly — run `yt-dlp -U` (or a `pip install -U yt-dlp` cron) monthly.
- No CI. Every build is manual. The `typecheck`/`lint`/`test` scripts are there for you to run locally before each build.

## Development notes

- The app was previously streamed-from-Pi and was migrated to download-first for offline playback. The memory of that transition is in the commit log (`feat: switch library to local storage with import button`, `feat: add offline downloads`).
- `react-native-track-player` was tried and abandoned — it is not compatible with Expo SDK 54 (Kotlin / Media3 mismatch). Do not re-try it.
- `useAudioPlayerStatus` from `expo-audio` used to be buggy; the current code uses `player.addListener('playbackStatusUpdate', ...)` manually.
- Background playback only works in an EAS dev client or standalone build — Expo Go is not enough.
