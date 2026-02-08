import axios from 'axios';
import { API_BASE_URL } from '../config/apiConfig';
import { SearchResult, Song } from '../types';

export type { SearchResult, Song };

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

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
