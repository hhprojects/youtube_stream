export interface Video {
  id: string;
  title: string;
  channel: string;
  duration?: number;
  url?: string;
  thumbnail?: string;
}

export interface Song extends Video {
  path: string;
  size?: number;
  dateAdded?: Date;
}

export interface PlayerState {
  isPlaying: boolean;
  currentSong: Song | null;
  playlist: Song[];
  currentIndex: number;
  isLooping: boolean;
  isShuffled: boolean;
}

export interface DownloadProgress {
  videoId: string;
  title: string;
  progress: number;
  status: 'downloading' | 'completed' | 'error';
}
