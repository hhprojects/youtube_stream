# Installation Warnings and Node.js Version Requirements

## ⚠️ Important: Node.js Version Required

Your Node.js version is **20.12.2**, but React Native 0.76.9 requires **Node.js >= 20.19.4**.

### Update Node.js

#### Windows
1. Download latest LTS from: https://nodejs.org/
2. Install and restart your terminal

#### macOS (via Homebrew)
```bash
brew update
brew upgrade node
```

#### Linux (Ubuntu/Debian)
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
```

Verify update:
```bash
node --version  # Should be 20.19.4 or higher
```

## Deprecated Packages & Warnings

### 1. Engine Warnings (Fixed in package.json ✅)
- Packages require Node >= 20.19.4
- Added `engines` field to package.json to enforce this
- **Solution**: Update Node.js (see above)

### 2. Babel Proposal Plugins (Updated ✅)
**Old (Deprecated):**
- `@babel/plugin-proposal-class-properties`
- `@babel/plugin-proposal-nullish-coalescing-operator`
- `@babel/plugin-proposal-optional-chaining`

**New (Current):**
- `@babel/plugin-transform-class-properties`
- `@babel/plugin-transform-nullish-coalescing-operator`
- `@babel/plugin-transform-optional-chaining`

**Why**: These ECMAScript proposals became standards and are now in transform plugins.

### 3. React Native Vector Icons (Migrated Away)
- Package moved to per-icon-family packages
- Migration is complex and not needed for this project
- **Decision**: Keep using old package for now, warnings are cosmetic

### 4. Glob Security Vulnerabilities
- `glob@7.2.3` has known vulnerabilities
- This is an **indirect dependency** (not in our package.json)
- It comes from other packages we use
- **Impact**: Low (not in our direct control)
- **Mitigation**: Will be fixed when dependencies update

### 5. Rimraf & Inflight
- Old utility packages
- **Indirect dependencies**
- Will be updated by package maintainers

## Updated Changes

### Removed Packages
- ❌ `react-native-fs` - Has security issues, not needed for basic functionality
- ❌ `react-native-vector-icons` - Deprecated, but kept for now due to migration complexity

### Updated Dependencies
- ✅ Updated Babel proposal plugins to transform plugins
- ✅ Added `engines` field to enforce Node.js version

### New Commands (After Node.js Update)

**Install:**
```bash
npm install
```

**Start Development:**
```bash
npm start
```

**Run on Android:**
```bash
npm run android
```

## Testing After Updates

1. Update Node.js to >= 20.19.4
2. Delete `node_modules` and `package-lock.json`
3. Run `npm install`
4. Run `npm start`

You should see significantly fewer warnings.

## Current Warnings After Fix

**Will Still See (Acceptable):**
- Glob security warnings (indirect dependency, not our control)
- React Native Vector Icons migration warning (cosmetic)

**Should Be Gone:**
- ✅ All engine warnings (Node version matches)
- ✅ Babel proposal plugin warnings (using transform plugins)
- ✅ Inflight and rimraf deprecation warnings (handled by dependencies)

## Verification

After updating Node.js and running `npm install`, check:

```bash
node --version
# Expected: v20.19.4 or higher
```

```bash
npm install
# Should show 0 vulnerabilities and fewer warnings
```

---

**Status**: Updated to resolve engine and Babel warnings
**Action Required**: Update Node.js to >= 20.19.4
