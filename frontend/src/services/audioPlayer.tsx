import React, { createContext, useContext, ReactNode, useState, useEffect, useCallback, useRef } from 'react';
import { PermissionsAndroid, Platform } from 'react-native';
import { useAudioPlayer, setAudioModeAsync } from 'expo-audio';
import { Song, AppRepeatMode } from '../types';

interface AudioContextType {
  isPlayerReady: boolean;
  currentSong: Song | null;
  playlist: Song[];
  currentIndex: number;
  isPlaying: boolean;
  position: number;
  duration: number;
  originalPlaylist: Song[];
  isShuffled: boolean;
  repeatMode: AppRepeatMode;
  playSongFromPlaylist: (song: Song, playlist: Song[], index: number) => void;
  playSongAtIndex: (index: number) => void;
  togglePlayPause: () => void;
  playNext: () => void;
  playPrevious: () => void;
  seek: (positionMillis: number) => void;
  toggleShuffle: () => void;
  cycleRepeatMode: () => void;
}

const AudioContext = createContext<AudioContextType | null>(null);

export const useGlobalAudio = () => {
  const context = useContext(AudioContext);
  if (!context) {
    throw new Error('useGlobalAudio must be used within AudioProvider');
  }
  return context;
};

// Fisher-Yates shuffle, keeps the item at keepIndex at the front
function shuffleArray<T>(arr: T[], keepIndex?: number): T[] {
  const result = [...arr];
  for (let i = result.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  if (keepIndex !== undefined && keepIndex >= 0 && keepIndex < arr.length) {
    const item = arr[keepIndex];
    const idx = result.findIndex(x => x === item);
    if (idx > 0) {
      result.splice(idx, 1);
      result.unshift(item);
    }
  }
  return result;
}

export const AudioProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const player = useAudioPlayer(null, { updateInterval: 500 });

  const [isPlayerReady, setIsPlayerReady] = useState(false);
  const [currentSong, setCurrentSong] = useState<Song | null>(null);
  const [playlist, setPlaylist] = useState<Song[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [originalPlaylist, setOriginalPlaylist] = useState<Song[]>([]);
  const [isShuffled, setIsShuffled] = useState(false);
  const [repeatMode, setRepeatMode] = useState<AppRepeatMode>(AppRepeatMode.Off);
  const [isPlaying, setIsPlaying] = useState(false);
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(0);

  // Refs for use inside event callbacks (avoids stale closures)
  const playlistRef = useRef<Song[]>([]);
  const currentIndexRef = useRef(0);
  const repeatModeRef = useRef<AppRepeatMode>(AppRepeatMode.Off);
  const handlingEndRef = useRef(false);
  const positionRef = useRef(0);

  useEffect(() => { playlistRef.current = playlist; }, [playlist]);
  useEffect(() => { currentIndexRef.current = currentIndex; }, [currentIndex]);
  useEffect(() => { repeatModeRef.current = repeatMode; }, [repeatMode]);
  useEffect(() => { positionRef.current = position; }, [position]);

  // Configure audio mode for background playback
  useEffect(() => {
    async function setup() {
      try {
        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (Platform.OS === 'android' && Platform.Version >= 33) {
          await PermissionsAndroid.request(
            PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS
          );
        }

        await setAudioModeAsync({
          shouldPlayInBackground: true,
          playsInSilentMode: true,
          interruptionMode: 'doNotMix',
        });
        setIsPlayerReady(true);
        console.log('Audio mode setup complete');
      } catch (error) {
        console.error('Failed to setup audio mode:', error);
      }
    }
    setup();
  }, []);

  // Sync player.loop with repeat mode
  useEffect(() => {
    if (player) {
      player.loop = repeatMode === AppRepeatMode.Track;
    }
  }, [player, repeatMode]);

  // Load and play a song (internal helper)
  const loadAndPlaySong = useCallback((song: Song, index: number) => {
    setCurrentSong(song);
    setCurrentIndex(index);
    currentIndexRef.current = index;
    handlingEndRef.current = false;

    player.replace({ uri: song.path });
    player.play();

    try {
      if (typeof player.setActiveForLockScreen === 'function') {
        player.setActiveForLockScreen(true, {
          title: song.title,
          artist: song.artist || 'Unknown',
        }, {
          showNext: true,
          showPrevious: true,
        });
      }
    } catch (e) {
      console.log('Lock screen controls not available:', e);
    }
  }, [player]);

  // Listen for playback status updates (position, duration, end-of-track)
  useEffect(() => {
    const subscription = player.addListener('playbackStatusUpdate', (status) => {
      setIsPlaying(status.playing);
      setPosition(status.currentTime * 1000);
      setDuration(status.duration * 1000);

      // Auto-advance when track finishes (Track repeat is handled by player.loop)
      if (status.didJustFinish && !handlingEndRef.current && repeatModeRef.current !== AppRepeatMode.Track) {
        handlingEndRef.current = true;
        const pl = playlistRef.current;
        const idx = currentIndexRef.current;
        const rm = repeatModeRef.current;

        if (idx < pl.length - 1) {
          // Play next track
          loadAndPlaySong(pl[idx + 1], idx + 1);
        } else if (rm === AppRepeatMode.Queue) {
          // Wrap around to first track
          loadAndPlaySong(pl[0], 0);
        }
        // Off mode at end of playlist: just stop
      }
    });

    return () => subscription.remove();
  }, [player, loadAndPlaySong]);

  // Listen for next/previous track events from notification controls
  const playNextRef = useRef<() => void>(() => {});
  const playPreviousRef = useRef<() => void>(() => {});

  useEffect(() => {
    const nextSub = player.addListener('onNextTrack', () => {
      playNextRef.current();
    });
    const prevSub = player.addListener('onPreviousTrack', () => {
      playPreviousRef.current();
    });
    return () => {
      nextSub.remove();
      prevSub.remove();
    };
  }, [player]);

  // Start a new playlist (called from Library/Search screens)
  const playSongFromPlaylist = useCallback((song: Song, songPlaylist: Song[], index: number) => {
    if (!isPlayerReady) return;

    setOriginalPlaylist(songPlaylist);

    let activePlaylist = songPlaylist;
    let activeIndex = index;

    if (isShuffled) {
      activePlaylist = shuffleArray(songPlaylist, index);
      activeIndex = 0;
    }

    setPlaylist(activePlaylist);
    playlistRef.current = activePlaylist;

    loadAndPlaySong(activePlaylist[activeIndex], activeIndex);
  }, [isPlayerReady, isShuffled, loadAndPlaySong]);

  // Jump to a specific index in the current playlist (for queue item clicks)
  const playSongAtIndex = useCallback((index: number) => {
    if (playlist.length === 0 || index < 0 || index >= playlist.length) return;
    loadAndPlaySong(playlist[index], index);
  }, [playlist, loadAndPlaySong]);

  const togglePlayPause = useCallback(() => {
    if (!currentSong) return;
    if (isPlaying) {
      player.pause();
    } else {
      player.play();
    }
  }, [currentSong, player, isPlaying]);

  const playNext = useCallback(() => {
    if (playlist.length === 0) return;
    let nextIndex = currentIndex + 1;
    if (nextIndex >= playlist.length) {
      if (repeatMode === AppRepeatMode.Queue) {
        nextIndex = 0;
      } else {
        return;
      }
    }
    loadAndPlaySong(playlist[nextIndex], nextIndex);
  }, [playlist, currentIndex, repeatMode, loadAndPlaySong]);

  const playPrevious = useCallback(() => {
    if (playlist.length === 0) return;
    // If more than 3 seconds in, restart current track
    if (position > 3000) {
      player.seekTo(0);
      return;
    }
    let prevIndex = currentIndex - 1;
    if (prevIndex < 0) {
      if (repeatMode === AppRepeatMode.Queue) {
        prevIndex = playlist.length - 1;
      } else {
        player.seekTo(0);
        return;
      }
    }
    loadAndPlaySong(playlist[prevIndex], prevIndex);
  }, [playlist, currentIndex, position, repeatMode, player, loadAndPlaySong]);

  // Keep refs in sync for notification event handlers
  useEffect(() => { playNextRef.current = playNext; }, [playNext]);
  useEffect(() => { playPreviousRef.current = playPrevious; }, [playPrevious]);

  const seek = useCallback((positionMillis: number) => {
    player.seekTo(positionMillis / 1000);
  }, [player]);

  const toggleShuffle = useCallback(() => {
    const newShuffled = !isShuffled;
    setIsShuffled(newShuffled);

    if (!currentSong || originalPlaylist.length === 0) return;

    if (newShuffled) {
      const currentIdx = originalPlaylist.findIndex(s => s.id === currentSong.id);
      const shuffled = shuffleArray(originalPlaylist, currentIdx);
      setPlaylist(shuffled);
      playlistRef.current = shuffled;
      setCurrentIndex(0);
      currentIndexRef.current = 0;
    } else {
      const restoredIndex = originalPlaylist.findIndex(s => s.id === currentSong.id);
      setPlaylist(originalPlaylist);
      playlistRef.current = originalPlaylist;
      setCurrentIndex(Math.max(0, restoredIndex));
      currentIndexRef.current = Math.max(0, restoredIndex);
    }
  }, [isShuffled, currentSong, originalPlaylist]);

  const cycleRepeatMode = useCallback(() => {
    let nextMode: AppRepeatMode;
    if (repeatMode === AppRepeatMode.Off) {
      nextMode = AppRepeatMode.Track;
    } else if (repeatMode === AppRepeatMode.Track) {
      nextMode = AppRepeatMode.Queue;
    } else {
      nextMode = AppRepeatMode.Off;
    }
    setRepeatMode(nextMode);
    repeatModeRef.current = nextMode;
  }, [repeatMode]);

  const value: AudioContextType = {
    isPlayerReady,
    currentSong,
    playlist,
    currentIndex,
    isPlaying,
    position,
    duration,
    originalPlaylist,
    isShuffled,
    repeatMode,
    playSongFromPlaylist,
    playSongAtIndex,
    togglePlayPause,
    playNext,
    playPrevious,
    seek,
    toggleShuffle,
    cycleRepeatMode,
  };

  return (
    <AudioContext.Provider value={value}>
      {children}
    </AudioContext.Provider>
  );
};
