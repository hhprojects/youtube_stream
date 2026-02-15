#!/usr/bin/env node
require('dotenv').config();

const axios = require('axios');
const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || process.env.REACT_APP_API_URL || 'http://localhost:3001/api';
const DOWNLOADS_BASE = API_BASE_URL.replace(/\/api\/?$/, '').replace(/\/$/, '') + '/downloads';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

async function testConnection() {
  let passed = 0;
  let failed = 0;

  console.log('Test 1: GET /api/library');
  try {
    const response = await api.get('/library');
    console.log('  OK -', response.data.songs?.length ?? 0, 'songs\n');
    passed++;
  } catch (error) {
    console.log('  FAIL -', error.message, '\n');
    failed++;
  }

  console.log('Test 2: POST /api/search');
  try {
    const response = await api.post('/search', { query: 'test music' });
    console.log('  OK -', response.data.results?.length ?? 0, 'results\n');
    passed++;
  } catch (error) {
    console.log('  FAIL -', error.message, '\n');
    failed++;
  }

  console.log('Test 3: GET /downloads (static mount)');
  try {
    await axios.get(`${DOWNLOADS_BASE}/_nonexistent.m4a`, { responseType: 'head', timeout: 5000 });
    console.log('  OK\n');
    passed++;
  } catch (error) {
    if (error.response?.status === 404) {
      console.log('  OK (404 for missing file)\n');
      passed++;
    } else {
      console.log('  FAIL -', error.message, '\n');
      failed++;
    }
  }

  console.log('Test 4: POST /api/download (invalid ID)');
  try {
    await api.post('/download', { videoId: 'invalid' });
    console.log('  FAIL - expected 400/500\n');
    failed++;
  } catch (error) {
    if (error.response && (error.response.status === 400 || error.response.status === 500)) {
      console.log('  OK - endpoint exists\n');
      passed++;
    } else {
      console.log('  FAIL -', error.message, '\n');
      failed++;
    }
  }

  console.log('---');
  console.log('Passed:', passed, '/ 4');
  process.exit(failed === 0 ? 0 : 1);
}

testConnection().catch((error) => {
  console.error('Error:', error.message);
  process.exit(1);
});
