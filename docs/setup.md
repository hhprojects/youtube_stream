# Setup Guide — Pi backend + Android APK

End-to-end: get the backend on the Raspberry Pi and a working standalone APK on an Android phone. Do each block on the right machine.

## Part 1 — Raspberry Pi (the backend)

SSH into the Pi, then:

### 1.1 Install dependencies
```bash
sudo apt update
sudo apt install -y nodejs npm ffmpeg git
node -v                       # must be ≥ 18
```

If Node is older than 18 (Bookworm ships 18.x, Bullseye is 12.x), use NodeSource:
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

`yt-dlp` must be recent or YouTube breaks it — **don't** `apt install yt-dlp` on Bullseye, it's ancient. Use pip:
```bash
sudo apt install -y python3-pip
sudo pip3 install --break-system-packages -U yt-dlp
yt-dlp --version              # should print a 2025.xx.xx date
```

### 1.2 Clone and install
```bash
cd ~
git clone https://github.com/hhprojects/youtube_stream.git
cd youtube_stream
npm install                    # runs patch-package via postinstall
```

### 1.3 Configure `.env`
```bash
cp .env.example .env
nano .env
```

Pick how the phone will reach the Pi and set `SERVER_URL` accordingly — this is the host:port that gets baked into download URLs returned to the app, so the phone must be able to hit it.

```ini
# If phone + Pi on the same Wi-Fi:
SERVER_URL=192.168.1.11:3001   # use your Pi's actual LAN IP (check with `hostname -I`)

# If using Tailscale (install it on the Pi first, see 1.4):
SERVER_URL=100.87.0.56:3001    # use the Pi's Tailscale IP

BACKEND_PORT=3001
DOWNLOAD_DIR=/home/pi/youtube_stream/backend/downloads
DOWNLOADS_MAX_BYTES=5368709120 # 5 GB, adjust to your SD card
```

### 1.4 (Optional) Tailscale
Use this if you want to reach the Pi from cellular data or a different Wi-Fi.
```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
tailscale ip -4                # prints the Pi's Tailscale IPv4 — put this in SERVER_URL
```

### 1.5 Test the server
```bash
npm run backend
```
Expected output:
```
Server running on http://0.0.0.0:3001
Server accessible at http://100.87.0.56:3001
Downloads cap: 5.0 GB
```
From another machine, sanity check:
```bash
curl http://<pi-ip>:3001/api/library     # should return {"songs":[…]}
```
Ctrl-C to stop.

### 1.6 Run it as a service (survives reboots)
```bash
sudo nano /etc/systemd/system/ytstream.service
```
Paste:
```ini
[Unit]
Description=YouTube Streamer backend
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/youtube_stream
ExecStart=/usr/bin/npm run backend
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```
Then:
```bash
sudo systemctl daemon-reload
sudo systemctl enable --now ytstream
sudo systemctl status ytstream
journalctl -u ytstream -f      # tail logs
```
Server now auto-starts on boot.

---

## Part 2 — Windows (build the Android APK)

Build the APK once in the cloud with EAS, then sideload it. After that the phone runs standalone.

### 2.1 One-time setup
```bash
# in Git Bash or PowerShell, in the repo
npm install                    # pull deps locally too (needed for EAS)
npm install -g eas-cli         # global EAS CLI
eas login                      # uses your Expo account
```

### 2.2 Tell the build where the backend lives
Edit `eas.json` and add an `env` block to the `preview` profile so the APK hits the right Pi:

```jsonc
{
  "build": {
    "development": { "developmentClient": true, "distribution": "internal", "android": { "buildType": "apk" } },
    "preview": {
      "distribution": "internal",
      "android": { "buildType": "apk" },
      "env": {
        "EXPO_PUBLIC_API_URL": "http://192.168.1.11:3001/api"
      }
    },
    "production": { "android": { "buildType": "apk" } }
  }
}
```

- Same Wi-Fi → `http://192.168.1.11:3001/api`
- Tailscale → `http://100.87.0.56:3001/api` (you can skip the `env` block since that's the baked-in default)

### 2.3 Pre-flight (catches regressions before burning EAS minutes)
```bash
npm run typecheck
npm run lint
npm test
```
All three must be clean.

### 2.4 Build
```bash
eas build --profile preview --platform android
```
- First time: EAS asks to create a keystore → accept (it manages it).
- Build runs in the cloud, ~10–20 min.
- When it finishes you get a URL like `https://expo.dev/accounts/.../builds/<id>`.
- From that page, click **Install** on your phone, or download the `.apk` link and transfer it.

---

## Part 3 — Phone (install and use)

### 3.1 Tailscale (only if you chose that path in 1.4)
- Install **Tailscale** from Play Store.
- Sign in with the same account used on the Pi.
- Verify: in a browser on the phone, open `http://100.87.0.56:3001/api/library` → should return `{"songs":[]}` or similar JSON.

### 3.2 Install the APK
- On Android, enable **Install unknown apps** for your browser / file manager.
- Download the APK from the EAS build URL, or transfer the file via USB.
- Tap to install — you may see a Play Protect warning; accept.

### 3.3 Daily use
1. Open **YouTube Streamer**.
2. **Search** tab → type a song → tap the download icon. Progress `%` shows under the icon.
3. When done, you land in **Library**. The song is now on your phone — you can listen offline.
4. **Player** opens on tap → full controls, scrub bar, repeat / shuffle, up-next queue.
5. Lock your phone — playback continues, notification shows title / artist and **play / next / prev** buttons.
6. **Library → cloud-download icon** opens **Import** — lists any songs on the Pi that aren't on this phone yet (useful for multiple devices or after a reinstall).

### 3.4 Offline behaviour
Music player, library, and queue work fully without any network — once songs are downloaded they live in `documentDirectory/songs/` on the phone. Only **Search** and **Import** need the Pi reachable.

---

## Things to know

- **Backend must be running** when you search or import. `sudo systemctl status ytstream` on the Pi tells you.
- **YouTube breaks old yt-dlp** every few weeks. If search/download suddenly fails, SSH in and run `sudo pip3 install --break-system-packages -U yt-dlp`.
- **The Pi caches every downloaded file** under `backend/downloads/` up to `DOWNLOADS_MAX_BYTES`. Oldest files prune automatically. This lets any phone reinstall the app and re-import without re-downloading from YouTube.
- **Deleting a song in the app only deletes it from the phone** — the Pi copy stays. Re-import it any time via Library → cloud icon.
- **Rebuilding the APK** is only needed when you change code. The installed APK keeps working — the backend is the only moving part.
- **Cleartext HTTP** — the app is configured to accept plain HTTP, which only works on a trusted LAN / Tailscale network. Do not expose port 3001 to the public internet.
