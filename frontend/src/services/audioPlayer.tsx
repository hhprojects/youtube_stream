import React, { createContext, useContext, ReactNode, useRef } from 'react';
import { useAudioPlayer, useAudioPlayerStatus, createAudioPlayer, AudioPlayer as ExpoAudioPlayer, setAudioModeAsync } from 'expo-audio';

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
  const player = useRef<ExpoAudioPlayer | null>(null);
  const listeners = useRef<Array<(status: PlaybackStatus) => void>>([]);
  const [playerState, setPlayerState] = React.useState<PlaybackStatus>({
    isPlaying: false,
    isBuffering: false,
    positionMillis: 0,
    durationMillis: 0,
    didJustFinish: false,
  });
  const [currentUrl, setCurrentUrl] = React.useState<string | null>(null);
  const statusUpdateInterval = useRef<NodeJS.Timeout | null>(null);

  // Setup audio mode
  React.useEffect(() => {
    (async () => {
      try {
        await setAudioModeAsync({
          playsInSilentModeIOS: true,
          staysActiveInBackground: true,
          shouldDuckAndroid: true,
        });
        console.log('Audio mode configured successfully');
      } catch (error) {
        console.error('Failed to setup audio:', error);
      }
    })();
  }, []);

  const playSong = async (url: string): Promise<void> => {
    try {
      // Create new player with source
      const newPlayer = createAudioPlayer(url, {
        updateInterval: 1000,
      });

      if (player.current) {
        try {
          player.current.stop();
        } catch (e) {
          console.error('Error stopping old player:', e);
        }
      }

      player.current = newPlayer;
      setCurrentUrl(url);

      // Start polling for status
      if (statusUpdateInterval.current) {
        clearInterval(statusUpdateInterval.current);
      }

      statusUpdateInterval.current = setInterval(() => {
        if (newPlayer) {
          try {
            const status = newPlayer.getStatus();
            setPlayerState({
              isPlaying: status.playing,
              isBuffering: status.buffering || false,
              positionMillis: status.currentTime * 1000,
              durationMillis: status.duration ? status.duration * 1000 : 0,
              didJustFinish: false,
            });
          } catch (error) {
            console.error('Error polling status:', error);
          }
        }
      }, 1000);

      console.log('Audio loaded:', url);
    } catch (error) {
      console.error('Failed to play song:', error);
      throw error;
    }
  };

  const play = async (): Promise<void> => {
    if (!player.current) {
      throw new Error('No audio loaded');
    }

    try {
      await player.current.play();
      setPlayerState(prev => ({ ...prev, isPlaying: true }));
    } catch (error) {
      console.error('Failed to play audio:', error);
      throw error;
    }
  };

  const pause = async (): Promise<void> => {
    if (!player.current) return;

    try {
      await player.current.pause();
      setPlayerState(prev => ({ ...prev, isPlaying: false }));
    } catch (error) {
      console.error('Failed to pause audio:', error);
    }
  };

  const stop = async (): Promise<void> => {
    if (!player.current) return;

    try {
      await player.current.stop();
      setPlayerState(prev => ({
        ...prev,
        isPlaying: false,
        positionMillis: 0,
      }));
    } catch (error) {
      console.error('Failed to stop audio:', error);
    }
  };

  const seek = async (positionMillis: number): Promise<void> => {
    if (!player.current) return;

    try {
      const positionSeconds = positionMillis / 1000;
      await player.current.seekTo(positionSeconds);
      setPlayerState(prev => ({ ...prev, positionMillis }));
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
