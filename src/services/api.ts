import axios from 'axios';

const API_BASE_URL = 'http://localhost:3001/api';

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

export interface SearchResult {
  id: string;
  title: string;
  channel: string;
  duration?: number;
  url?: string;
  thumbnail?: string;
}

export interface Song {
  id: string;
  title: string;
  artist: string;
  duration: string;
  path: string;
  size?: number;
  dateAdded?: Date;
}

// Search YouTube
export const searchVideos = async (query: string): Promise<SearchResult[]> => {
  const response = await api.post('/search', { query });
  return response.data.results;
};

// Download audio
export const downloadAudio = async (videoId: string, title: string): Promise<Song> => {
  const response = await api.post('/download', { videoId, title });
  return {
    id: response.data.filename,
    title: response.data.title,
    artist: 'Unknown',
    duration: 'Unknown',
    path: response.data.path,
    size: response.data.size,
  };
};

// Get library
export const getLibrary = async (): Promise<Song[]> => {
  const response = await api.get('/library');
  return response.data.songs;
};

// Delete song
export const deleteSong = async (filename: string): Promise<void> => {
  await api.delete(`/library/${filename}`);
};
