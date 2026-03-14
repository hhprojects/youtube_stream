import axios from 'axios';
import { API_BASE_URL } from '../config/apiConfig';
import { SearchResult, Song, PiSong } from '../types';
import { downloadToDevice } from './localLibrary';

export type { SearchResult, Song };

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

export const searchVideos = async (query: string): Promise<SearchResult[]> => {
  const response = await api.post('/search', { query });
  return response.data.results;
};

export const downloadAudio = async (
  videoId: string,
  title: string,
  onProgress?: (progress: number) => void
): Promise<Song> => {
  // Step 1: Pi downloads from YouTube
  const response = await api.post('/download', { videoId, title }, { timeout: 300000 });
  const { filename, downloadUrl, title: parsedTitle, artist, size } = response.data;

  // Step 2: Download file from Pi to device
  const song = await downloadToDevice(
    downloadUrl,
    filename,
    parsedTitle,
    artist,
    size,
    onProgress
  );

  return song;
};

export const getPiLibrary = async (): Promise<PiSong[]> => {
  const response = await api.get('/library');
  return response.data.songs;
};

export const deleteSongFromPi = async (filename: string): Promise<void> => {
  await api.delete(`/library/${encodeURIComponent(filename)}`);
};
