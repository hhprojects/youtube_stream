# Project Structure

```
youtube_stream/
│
├── backend/                     # Backend API Server
│   ├── server.js              # Express server with API endpoints
│   └── downloads/             # Downloaded audio files
│       └── Dancing_With_A_Stranger.m4a
│
├── frontend/                    # React Native Frontend
│   ├── src/                  # Source code
│   │   ├── screens/           # App screens
│   │   │   ├── SearchScreen.tsx
│   │   │   ├── LibraryScreen.tsx
│   │   │   └── PlayerScreen.tsx
│   │   ├── services/          # API & Audio services
│   │   │   ├── api.ts
│   │   │   └── audioPlayer.ts
│   │   ├── hooks/             # Custom React hooks
│   │   │   └── useMusicPlayer.ts
│   │   ├── components/        # Reusable components
│   │   ├── types/             # TypeScript definitions
│   │   │   └── index.ts
│   │   ├── config/            # Configuration
│   │   │   └── apiConfig.ts
│   │   ├── navigation/         # Navigation setup
│   │   └── assets/            # App assets (images, fonts)
│   ├── hooks/                # Git hooks (if any)
│   └── routers/              # Backend routers (if any)
│
├── App.tsx                     # Main app component
├── index.js                    # App entry point
├── app.json                    # React Native configuration
├── metro.config.js              # Metro bundler configuration
├── tsconfig.json               # TypeScript configuration
├── package.json                # Dependencies and scripts
├── .env.example                # Environment variables template
├── .gitignore                  # Git ignore rules
│
└── Documentation
    ├── README.md                # Main documentation
    ├── QUICKSTART.md            # Quick start guide
    ├── LOCAL_DEV.md            # Local development guide
    ├── STATUS.md               # Project status
    ├── WARNINGS_FIX.md         # Warning fixes
    ├── DEPENDENCY_UPDATE.md    # Dependency history
    └── PROJECT_STRUCTURE.md     # This file
```

## Frontend vs Backend

### Frontend (`/frontend`)
- React Native mobile app
- Runs on Android/iOS devices
- Communicates with backend via API
- Handles UI, audio playback, state management

### Backend (`/backend`)
- Node.js + Express API server
- Runs on server (Raspberry Pi)
- Handles YouTube search, downloads, library
- Serves static audio files

## Communication Flow

```
[Frontend App]        [Backend API]
    |                     |
    |----GET /api/library -->|
    |<---- songs ----------|
    |                     |
    |----POST /api/search-->|
    |<---- results ---------|
    |                     |
    |----POST /api/dowload>|
    |<---- audio file -----|
    |                     |
    |----GET /downloads/---|
    |<---- audio stream ---|
```

## File Organization

### Configuration Files (Root)
- `App.tsx` - Main React component
- `index.js` - Entry point
- `app.json` - React Native config
- `metro.config.js` - Metro bundler config
- `tsconfig.json` - TypeScript config
- `package.json` - Dependencies & scripts
- `.env.example` - Environment template

### Documentation Files (Root)
- `README.md` - Project overview
- `QUICKSTART.md` - Setup guide
- `LOCAL_DEV.md` - Development guide
- `STATUS.md` - Status report
- `WARNINGS_FIX.md` - Troubleshooting
- `DEPENDENCY_UPDATE.md` - Version history
- `PROJECT_STRUCTURE.md` - This file

### Backend Files (`/backend`)
- `server.js` - API server
- `downloads/` - Audio files (gitignored)

### Frontend Files (`/frontend/src`)
- `screens/` - UI screens
- `services/` - API & audio
- `hooks/` - React hooks
- `components/` - UI components
- `types/` - TypeScript types
- `config/` - Configuration
- `navigation/` - Navigation setup
- `assets/` - Static assets

---

**Last Updated**: 2026-02-08
