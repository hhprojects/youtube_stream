// API Configuration
// Uses EXPO_PUBLIC_API_URL from .env or EAS build env; fallback for Tailscale.
// - Tailscale IP 100.87.0.56 reaches the Pi from anywhere (phone must be on Tailscale).
// - Override via EXPO_PUBLIC_API_URL in .env for local dev (e.g. localhost or LAN IP).
const DEFAULT_API_URL = 'http://100.87.0.56:3001/api';

export const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || DEFAULT_API_URL;
