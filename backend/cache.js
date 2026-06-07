/** Tiny in-memory TTL cache. `now` is injectable so TTL expiry is unit-testable. */
function createCache({ ttlMs, now = Date.now } = {}) {
  const store = new Map();
  return {
    get(key) {
      const e = store.get(key);
      if (!e) return undefined;
      if (now() >= e.expiresAt) { store.delete(key); return undefined; }
      return e.value;
    },
    set(key, value) { store.set(key, { value, expiresAt: now() + ttlMs }); },
    clear() { store.clear(); },
    get size() { return store.size; },
  };
}

module.exports = { createCache };
