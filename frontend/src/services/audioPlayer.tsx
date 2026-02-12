import React, { createContext, useContext, ReactNode, useRef, useEffect } from 'react';
import { useAudioPlayerStatus, createAudioPlayer, AudioPlayer as ExpoAudioPlayer, setAudioModeAsync } from 'expo-audio';

export type PlaybackStatus = {
  isPlaying: boolean;
  isBuffering: boolean;
  positionMillis: number;
  durationMillis: number;
  didJustFinish: boolean;
};

interface AudioContextType {
  playSong: (url: string) => Promise<void>;
  pause: () => Promise<void>;
  play: () => Promise<void>;
  stop: () => Promise<void>;
  seek: (positionMillis: number) => Promise<void>;
  playerState: PlaybackStatus;
  currentUrl: string | null;
}

const AudioContext = createContext<AudioContextType | null>(null);

export const useGlobalAudio = () => {
  const context = useContext(AudioContext);
  if (!context) {
    throw new Error('useGlobalAudio must be used within AudioProvider');
  }
  return context;
};

export const AudioProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  // Create an initial player immediately to avoid undefined errors
  const initialPlayer = createAudioPlayer('', { updateInterval: 1000 });
  const player = useRef<ExpoAudioPlayer>(initialPlayer);
  const [currentUrl, setCurrentUrl] = React.useState<string | null>(null);

  // Default state when no player exists
  const [playerState, setPlayerState] = React.useState<PlaybackStatus>({
    isPlaying: false,
    isBuffering: false,
    positionMillis: 0,
    durationMillis: 0,
    didJustFinish: false,
  });

  // Setup audio mode
  useEffect(() => {
    (async () => {
      try {
        await setAudioModeAsync({
          playsInSilentMode: true,
        });
        console.log('Audio mode configured successfully');
      } catch (error) {
        console.error('Failed to setup audio:', error);
      }
    })();
  }, []);

  // Use useAudioPlayerStatus hook with the player
  const playerStatus = useAudioPlayerStatus(player.current);

  // Update playerState when playerStatus changes
  useEffect(() => {
    if (playerStatus) {
      setPlayerState({
        isPlaying: playerStatus.playing || false,
        isBuffering: playerStatus.isBuffering || false,
        positionMillis: (playerStatus.currentTime || 0) * 1000,
        durationMillis: (playerStatus.duration || 0) * 1000,
        didJustFinish: playerStatus.didJustFinish || false,
      });
    }
  }, [playerStatus]);

  const playSong = async (url: string): Promise<void> => {
    try {
      // Stop current player if playing
      try {
        await player.current.pause();
      } catch (e) {
        console.error('Error stopping old player:', e);
      }

      // Create new player with source
      const newPlayer = createAudioPlayer(url, {
        updateInterval: 1000,
      });

      player.current = newPlayer;
      setCurrentUrl(url);

      console.log('Audio loaded:', url);
    } catch (error) {
      console.error('Failed to play song:', error);
      throw error;
    }
  };

  const play = async (): Promise<void> => {
    if (!currentUrl) {
      throw new Error('No audio loaded');
    }

    try {
      await player.current.play();
      // Player will update automatically via useAudioPlayerStatus hook
    } catch (error) {
      console.error('Failed to play audio:', error);
      throw error;
    }
  };

  const pause = async (): Promise<void> => {
    try {
      await player.current.pause();
      // Player will update automatically via useAudioPlayerStatus hook
    } catch (error) {
      console.error('Failed to pause audio:', error);
    }
  };

  const stop = async (): Promise<void> => {
    try {
      // In expo-audio v55, there's no stop() method
      // We pause and reset position by seeking to 0
      await player.current.pause();
      await player.current.seekTo(0);
      // Player will update automatically via useAudioPlayerStatus hook
    } catch (error) {
      console.error('Failed to stop audio:', error);
    }
  };

  const seek = async (positionMillis: number): Promise<void> => {
    try {
      const positionSeconds = positionMillis / 1000;
      await player.current.seekTo(positionSeconds);
      // Player will update automatically via useAudioPlayerStatus hook
    } catch (error) {
      console.error('Failed to seek audio:', error);
    }
  };

  const value: AudioContextType = {
    playSong,
    play,
    pause,
    stop,
    seek,
    playerState,
    currentUrl,
  };

  return (
    <AudioContext.Provider value={value}>
      {children}
    </AudioContext.Provider>
  );
};

// Legacy export for backward compatibility
const audioContext = {
  playSong: () => Promise.resolve(),
  play: () => Promise.resolve(),
  pause: () => Promise.resolve(),
  stop: () => Promise.resolve(),
  seek: () => Promise.resolve(),
  playerState: {
    isPlaying: false,
    isBuffering: false,
    positionMillis: 0,
    durationMillis: 0,
    didJustFinish: false,
  },
  currentUrl: null,
};

export const audioPlayer = {
  ...audioContext,
  addListener: (listener: (status: PlaybackStatus) => void) => {
    console.warn('addListener is deprecated, use useGlobalAudio hook instead');
  },
  removeListener: (listener: (status: PlaybackStatus) => void) => {
    console.warn('removeListener is deprecated, use useGlobalAudio hook instead');
  },
  getCurrentUrl: () => audioContext.currentUrl,
};

export const setupAudio = async (): Promise<void> => {
  // Audio mode is now set in AudioProvider
  console.log('setupAudio() called - audio mode configured in provider');
};
