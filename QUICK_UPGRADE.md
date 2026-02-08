# Quick Upgrade Guide - Node.js on Windows

## Current Situation

- **Your Node.js**: 20.12.2
- **Required**: >= 20.19.4
- **Status**: Engine warnings

## Fastest Way (5 minutes)

### 1. Download Node.js
Go to: https://nodejs.org/
Download: **LTS version (.msi installer)**

### 2. Install
- Double-click the `.msi` file
- Follow the wizard (click Next, Next, Finish)

### 3. Restart Terminal
Close and reopen PowerShell or Command Prompt.

### 4. Verify
```powershell
node --version
# Should show v22.x.x or v20.19.4+
```

### 5. Update Project
```powershell
cd C:\Users\lhanh\OneDrive\Documents\Projects\Personal\apps\youtube_stream

# Pull latest changes
git pull

# Reinstall dependencies
npm install
```

**Expected**: No engine warnings! ✅

---

## Alternative: nvm-windows (10 minutes)

Best if you want to switch between versions.

```powershell
# Install nvm-windows (run as Administrator)
winget install CoreyButler.NVMforWindows

# Restart PowerShell, then:
nvm install lts
nvm use lts
```

---

## After Upgrading

✅ Engine warnings GONE
✅ Can use React Native 0.76.9 (latest)
✅ Latest Node.js features
✅ Better performance and security

## Full Guide

See `UPGRADE_NODE_WINDOWS.md` for detailed instructions with:
- All 3 upgrade methods
- Troubleshooting steps
- Version comparison tables

---

**Ready to upgrade? Go!** 🚀
