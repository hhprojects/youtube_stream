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

export enum AppRepeatMode {
  Off = 'off',
  Track = 'track',
  Queue = 'queue',
}

export interface PlayerState {
  isPlaying: boolean;
  currentSong: Song | null;
  playlist: Song[];
  currentIndex: number;
  repeatMode: AppRepeatMode;
  isShuffled: boolean;
  position: number;
  duration: number;
}

export interface DownloadProgress {
  videoId: string;
  title: string;
  progress: number;
  status: 'downloading' | 'completed' | 'error';
}
