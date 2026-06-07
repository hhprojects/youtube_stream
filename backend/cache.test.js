const { test } = require('node:test');
const assert = require('node:assert/strict');
const { createCache } = require('./cache');

test('returns undefined for a missing key', () => {
  const c = createCache({ ttlMs: 1000, now: () => 0 });
  assert.equal(c.get('x'), undefined);
});

test('returns a set value before expiry', () => {
  let t = 0;
  const c = createCache({ ttlMs: 1000, now: () => t });
  c.set('x', { a: 1 });
  t = 999;
  assert.deepEqual(c.get('x'), { a: 1 });
});

test('expires a value at/after ttl and forgets it', () => {
  let t = 0;
  const c = createCache({ ttlMs: 1000, now: () => t });
  c.set('x', 42);
  t = 1000;
  assert.equal(c.get('x'), undefined);
  assert.equal(c.size, 0);
});
