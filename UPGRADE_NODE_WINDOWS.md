# How to Upgrade Node.js on Windows

## Current Status

- **Your Node.js**: 20.12.2
- **Required for React Native 0.76.9**: >= 20.19.4

You need to upgrade Node.js to fix engine warnings.

---

## Method 1: Official Installer (Recommended)

### Steps

1. **Download Node.js LTS**
   - Go to: https://nodejs.org/
   - Download the latest **LTS** version (22.x recommended)
   - Choose **.msi** (Windows Installer)

2. **Run the Installer**
   - Double-click the downloaded `.msi` file
   - Follow the installation wizard
   - Select "Add to PATH" (default)
   - Complete the installation

3. **Restart PowerShell/Command Prompt**
   - Close and reopen to update PATH

4. **Verify Installation**
   ```powershell
   node --version
   # Should show: v22.x.x or v20.19.4+
   ```

---

## Method 2: Using nvm-windows (Best for Developers)

**Why nvm-windows?** Allows switching between Node.js versions easily.

### Installation

1. **Install nvm-windows** (PowerShell as Administrator)
   ```powershell
   winget install CoreyButler.NVMforWindows
   ```

   Or download from: https://github.com/coreybutler/nvm-windows/releases

2. **Restart PowerShell** to use nvm

### Use nvm to Install Node.js

```powershell
# List available versions
nvm list available

# Install specific version
nvm install 22.14.0

# Or install latest LTS
nvm install lts

# Use the installed version
nvm use 22.14.0

# Set as default
nvm use 22.14.0 default
```

3. **Verify**
   ```powershell
   node --version
   npm --version
   ```

---

## Method 3: Using Chocolatey (Package Manager)

**If you have Chocolatey installed:**

```powershell
# Check current version
choco list nodejs

# Upgrade to latest
choco upgrade nodejs

# Verify
node --version
```

---

## After Upgrading Node.js

### 1. Pull Latest Changes from GitHub

```powershell
git pull
```

### 2. Clean and Reinstall Dependencies

```powershell
# Remove old modules
Remove-Item -Recurse -Force node_modules
Remove-Item package-lock.json

# Reinstall with new Node.js
npm install
```

### 3. Verify No Warnings

```powershell
npm install
```

**Expected output:**
```
added 972 packages, and audited 972 packages in XXs
found 0 vulnerabilities
```

**NO engine warnings should appear!**

---

## Version Comparison

| Node Version | React Native 0.74 | React Native 0.76 |
|--------------|---------------------|---------------------|
| 20.12.2 | ✅ Compatible | ❌ Engine warnings |
| 20.19.4+ | ✅ Compatible | ✅ Compatible |
| 22.x (LTS) | ✅ Compatible | ✅ Compatible |

---

## Troubleshooting

### Node Command Not Found After Upgrade

**PowerShell:**
```powershell
# Refresh environment variables
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")

# Restart PowerShell
```

### npm Still Uses Old Node

```powershell
# Check which node npm is using
npm config get prefix

# Check Node location
where node
```

If multiple Node installations, uninstall old versions from:
```
C:\Program Files\nodejs\
C:\Users\<username>\AppData\Roaming\nvm\
```

---

## Recommendation

**Use Method 1 (Official Installer)** for simplicity:
- Easy and straightforward
- No additional tools needed
- Works for everyone

**Use Method 2 (nvm-windows)** if you're a developer:
- Switch between Node versions
- Useful for testing different React Native versions
- Good for multiple projects

---

## What's Next?

After upgrading Node.js:

1. ✅ All engine warnings will be gone
2. ✅ You can use latest React Native (0.76.9)
3. ✅ Latest Node.js features and performance
4. ✅ Better security patches

Current project (React Native 0.76.9) is already compatible with Node.js >= 20.19.4. Just upgrade Node.js and you're good to go!

---

**Last Updated**: 2026-02-08
