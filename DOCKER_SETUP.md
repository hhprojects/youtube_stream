# Docker Setup Guide - Backend API

## Overview

Run the backend API server in a Docker container on your Raspberry Pi.

## Prerequisites

- Docker installed on Pi
- Docker Compose (optional, but recommended)

## Method 1: Using Docker Compose (Recommended)

### 1. Create docker-compose.yml

```yaml
version: '3.8'

services:
  backend:
    build: .
    container_name: youtube-stream-backend
    ports:
      - "3001:3001"
    volumes:
      - ./backend/downloads:/app/backend/downloads
      - ./backend:/app/backend
    environment:
      - NODE_ENV=production
      - PORT=3001
      - DOWNLOAD_DIR=/app/backend/downloads
    restart: always  # Auto-restart on boot
    command: node backend/server.js
```

### 2. Create Dockerfile

```dockerfile
FROM node:20-alpine

WORKDIR /app

# Install yt-dlp
RUN apk add --no-cache yt-dlp python3

# Copy backend files
COPY backend/ ./backend/
COPY package*.json ./

# Install dependencies
RUN npm install --production

# Expose port
EXPOSE 3001

# Start server
CMD ["node", "backend/server.js"]
```

### 3. Run Container

```bash
# Build and start
docker-compose up -d

# Stop
docker-compose down

# View logs
docker-compose logs -f
```

## Method 2: Using Docker CLI

### Build Image

```bash
docker build -t youtube-stream-backend .
```

### Run Container

```bash
docker run -d \
  --name youtube-stream-backend \
  --restart always \
  -p 3001:3001 \
  -v "$(pwd)/backend/downloads:/app/backend/downloads" \
  youtube-stream-backend
```

### Stop/Start Container

```bash
# Stop
docker stop youtube-stream-backend

# Start
docker start youtube-stream-backend

# Restart
docker restart youtube-stream-backend

# Remove
docker rm youtube-stream-backend
```

## Container Management

### View Running Containers

```bash
docker ps
```

### View Logs

```bash
docker logs youtube-stream-backend
```

### Access Container Shell

```bash
docker exec -it youtube-stream-backend /bin/sh
```

## Auto-Start on Boot

The container will auto-restart with `restart: always` or:
- Docker Desktop: Settings → Start Docker Desktop on login
- Systemd service: Create systemd service for Docker daemon

## Port Access

Backend runs on `http://<PI_IP>:3001`

Find Pi's IP:
```bash
hostname -I | awk '{print $1}'
```

## Frontend Connection

Update `.env` on your Windows machine:

```bash
REACT_APP_API_URL=http://<PI_IP>:3001/api
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker logs youtube-stream-backend

# Restart
docker restart youtube-stream-backend
```

### Port Already in Use

```bash
# Check what's using port 3001
sudo lsof -i :3001

# Or change port mapping
docker run -p 3002:3001 ...
```

### Volume Mount Issues

```bash
# Ensure downloads directory exists
mkdir -p backend/downloads

# Check permissions
ls -la backend/downloads
```

---

## Benefits of Docker

✅ **Always running** with `restart: always`
✅ Easy to stop/start
✅ Isolated environment
✅ Consistent across machines
✅ Easy to deploy

## Drawbacks of Docker

❌ **Frontend needs host Node.js** - Docker not for React Native development
❌ More complex setup
❌ Volume management
❌ Port mapping needed

---

**Recommendation**: Use Docker for backend, but install Node.js directly on Windows for frontend development.
