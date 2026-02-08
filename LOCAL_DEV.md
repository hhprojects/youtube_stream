# Running on Local Machine with Android Emulator

## Prerequisites

Your local machine and Pi must be on the **same network** (both connected to same WiFi/router).

## Setup Steps

### 1. On Your Pi (Backend)

Backend should already be running. If not:
```bash
cd /home/hh-pi/.openclaw/workspace/youtube_stream
npm run backend
```

Verify it's accessible:
```bash
# Test from Pi
curl http://192.168.1.11:3001/api/library
```

### 2. On Your Local Machine (Frontend)

#### Clone the Repository
```bash
git clone https://github.com/hhprojects/youtube_stream.git
cd youtube_stream
```

#### Update API Config

Edit `src/config/apiConfig.ts` to point to your Pi's IP:

```typescript
export const API_BASE_URL = 'http://192.168.1.11:3001/api';
```

**Note**: Your Pi's IP might be different. Check it:
```bash
# On your Pi, run:
hostname -I | awk '{print $1}'
```

#### Install Dependencies
```bash
npm install --legacy-peer-deps
```

#### Start Metro Bundler
```bash
npm start
```

Press `a` in the terminal to run on Android emulator.

### 3. Android Emulator Setup

The Android emulator runs in a VM with its own network. To access external servers:

- **Same network**: If your local machine and Pi are on same network, the emulator can access the Pi directly using `http://192.168.1.11:3001`

- **Different networks**: If they're on different networks, you'll need to use ngrok or similar to tunnel the backend.

### 4. Test Connection

From your local machine, test if you can reach the Pi's backend:

```bash
# From your local machine
curl http://192.168.1.11:3001/api/library
```

If this works, the emulator should also be able to reach it.

### 5. Run the App

```bash
npm start
# Press 'a' in the terminal to run on Android emulator
```

## Scenario 2: Different Networks (Use ngrok)

If your Pi and local machine are on different networks (e.g., Pi at home, laptop at office):

### On Your Pi

Install ngrok:
```bash
# Download ngrok
wget https://bin.equinox.io/c/bNyj1mQVY4c/ngrok-v3-stable-linux-arm64.tgz
tar -xzf ngrok-v3-stable-linux-arm64.tgz
sudo mv ngrok /usr/local/bin/

# Start ngrok tunnel for backend
ngrok http 3001
```

You'll get a URL like `https://xxxx-xx-xx-xx-xx.ngrok-free.app`

Update `src/config/apiConfig.ts` on your local machine:
```typescript
export const API_BASE_URL = 'https://xxxx-xx-xx-xx-xx.ngrok-free.app/api';
```

### Update Backend for ngrok

Edit `backend/server.js` to update SERVER_URL to your ngrok URL, or keep it using localhost since ngrok handles the tunneling.

## Troubleshooting

### App can't connect to backend

1. **Check if backend is accessible from local machine:**
   ```bash
   curl http://192.168.1.11:3001/api/library
   ```

2. **Check emulator network:**
   - In emulator browser, try: `http://192.168.1.11:3001/api/library`
   - If this works but app doesn't, check API config

3. **Verify API config:**
   ```bash
   cat src/config/apiConfig.ts
   ```

### Firewall Issues

If you can't reach the Pi from local machine, check firewall:
```bash
# On your Pi, allow port 3001
sudo ufw allow 3001
```

### Metro Bundler Issues

If Metro bundler has issues:
```bash
# Clear cache
npm start -- --reset-cache
```

## Quick Test Checklist

- [ ] Backend running on Pi (port 3001)
- [ ] Pi accessible from local machine (curl test passes)
- [ ] Repository cloned on local machine
- [ ] Dependencies installed on local machine
- [ ] API config updated with Pi's IP
- [ ] Android emulator running
- [ ] Metro bundler started
- [ ] App launched on emulator
- [ ] Can search YouTube
- [ ] Can download audio
- [ ] Can play music

## Development Workflow

1. **On Pi** - Backend always running
   ```bash
   npm run backend
   ```

2. **On Local Machine** - Frontend development
   ```bash
   npm start  # Metro bundler
   # Press 'a' for emulator
   ```

3. **Make changes** - Edit on local machine
4. **Reload app** - Press 'r' in Metro terminal to reload
5. **Commit and push** - From local machine or Pi

---

Happy coding! 🚀
