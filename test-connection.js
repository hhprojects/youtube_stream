#!/usr/bin/env node
/**
 * Frontend-Backend Connection Test Script
 * Tests all API endpoints to verify frontend-backend connectivity
 */

const axios = require('axios');
const API_BASE_URL = 'http://192.168.1.11:3001/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

async function testConnection() {
  console.log('🧪 Testing Frontend-Backend Connection\n');
  console.log(`📍 API Base URL: ${API_BASE_URL}\n`);

  let passed = 0;
  let failed = 0;

  // Test 1: Health Check (via library endpoint)
  console.log('Test 1: Health Check (GET /api/library)');
  try {
    const response = await api.get('/library');
    console.log('✅ PASSED - Library endpoint reachable');
    console.log(`   Found ${response.data.songs.length} songs\n`);
    passed++;
  } catch (error) {
    console.log('❌ FAILED - Library endpoint unreachable');
    console.log(`   Error: ${error.message}\n`);
    failed++;
  }

  // Test 2: YouTube Search
  console.log('Test 2: YouTube Search (POST /api/search)');
  try {
    const response = await api.post('/search', { query: 'test music' });
    console.log('✅ PASSED - Search endpoint working');
    console.log(`   Found ${response.data.results.length} results\n`);
    passed++;
  } catch (error) {
    console.log('❌ FAILED - Search endpoint not working');
    console.log(`   Error: ${error.message}\n`);
    failed++;
  }

  // Test 3: Static File Access
  console.log('Test 3: Static File Access (GET /downloads/...)');
  try {
    const response = await axios.get('http://192.168.1.11:3001/downloads/Dancing_With_A_Stranger.m4a', {
      responseType: 'head',
      timeout: 5000
    });
    console.log('✅ PASSED - Static files accessible');
    console.log(`   Content-Type: ${response.headers['content-type']}\n`);
    passed++;
  } catch (error) {
    console.log('❌ FAILED - Static files not accessible');
    console.log(`   Error: ${error.message}\n`);
    failed++;
  }

  // Test 4: Download Endpoint (simulate)
  console.log('Test 4: Download Endpoint Configuration (POST /api/download)');
  try {
    // Just check if endpoint exists, don't actually download
    const response = await api.post('/download', { videoId: 'test' });
    console.log('❌ PASSED - Download endpoint responded (expected to fail with invalid ID)\n');
    passed++;
  } catch (error) {
    if (error.response && error.response.status === 500) {
      console.log('✅ PASSED - Download endpoint exists and processes requests');
      console.log(`   Expected error: Invalid video ID\n`);
      passed++;
    } else {
      console.log('❌ FAILED - Download endpoint not responding correctly');
      console.log(`   Error: ${error.message}\n`);
      failed++;
    }
  }

  // Summary
  console.log('='.repeat(50));
  console.log('📊 Test Summary');
  console.log('='.repeat(50));
  console.log(`✅ Passed: ${passed}/4`);
  console.log(`❌ Failed: ${failed}/4`);

  if (failed === 0) {
    console.log('\n🎉 All tests passed! Frontend-backend connection is working.');
    console.log('\n📱 To run the React Native app:');
    console.log('   npm start          # Start Metro bundler');
    console.log('   npm run android    # Run on Android device');
    console.log('   npm run ios        # Run on iOS device (Mac only)');
    console.log('\n💡 Make sure your mobile device is on the same WiFi network as the Pi.');
  } else {
    console.log('\n⚠️  Some tests failed. Check backend server logs.');
  }

  process.exit(failed === 0 ? 0 : 1);
}

testConnection().catch(error => {
  console.error('❌ Test script error:', error.message);
  process.exit(1);
});
