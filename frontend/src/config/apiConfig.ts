// API Configuration
// Update this to match your backend server's address
// - For development on same machine: 'http://localhost:3001/api'
// - For testing on real device: Use your Pi's IP address, e.g., 'http://192.168.1.11:3001/api'
// - For production: Use your backend server's domain/IP

export const API_BASE_URL = __DEV__
  ? 'http://192.168.1.11:3001/api'  // Use Pi's IP for device testing
  : 'http://192.168.1.11:3001/api'; // Production URL
