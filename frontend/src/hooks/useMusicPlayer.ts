import { useState, useEffect, useCallback } from 'react';
import { Song } from '../types';
import { audioPlayer, setupAudio, PlaybackStatus } from '../services/audioPlayer';

export interface PlayerState {
  isPlaying: boolean;
  currentSong: Song | null;
  playlist: Song[];
  currentIndex: number;
  isLooping: boolean;
  isShuffled: boolean;
  position: number;
  duration: number;
}

export const useMusicPlayer = () => {
  const [playerState, setPlayerState] = useState<PlayerState>({
    isPlaying: false,
    currentSong: null,
    playlist: [],
    currentIndex: -1,
    isLooping: false,
    isShuffled: false,
    position: 0,
    duration: 0,
  });

  useEffect(() => {
    setupAudio();

    const listener = (status: PlaybackStatus) => {
      setPlayerState(prev => ({
        ...prev,
        isPlaying: status.isPlaying,
        position: status.positionMillis,
        duration: status.durationMillis,
      }));

      if (status.didJustFinish) {
        if (playerState.isLooping) {
          audioPlayer.seek(0);
          audioPlayer.play();
        } else {
          playNext();
        }
      }
    };

    audioPlayer.addListener(listener);

    return () => {
      audioPlayer.removeListener(listener);
      audioPlayer.unload();
    };
  }, []);

  const playSong = useCallback(async (song: Song, playlist?: Song[], index?: number) => {
    try {
      await audioPlayer.load(song.path);
      await audioPlayer.play();
      
      setPlayerState(prev => ({
        ...prev,
        currentSong: song,
        playlist: playlist || [],
        currentIndex: index !== undefined ? index : -1,
        isPlaying: true,
        position: 0,
        duration: 0,
      }));
    } catch (error) {
      console.error('Failed to play song:', error);
    }
  }, []);

  const togglePlayPause = useCallback(async () => {
    if (!playerState.currentSong) return;

    if (playerState.isPlaying) {
      await audioPlayer.pause();
    } else {
      await audioPlayer.play();
    }
  }, [playerState.currentSong, playerState.isPlaying]);

  const playNext = useCallback(() => {
    if (playerState.playlist.length === 0) return;

    let nextIndex = playerState.currentIndex + 1;
    
    if (playerState.isShuffled) {
      nextIndex = Math.floor(Math.random() * playerState.playlist.length);
    } else if (nextIndex >= playerState.playlist.length) {
      if (playerState.isLooping) {
        nextIndex = 0;
      } else {
        return;
      }
    }

    playSong(playerState.playlist[nextIndex], playerState.playlist, nextIndex);
  }, [playerState.playlist, playerState.currentIndex, playerState.isShuffled, playerState.isLooping, playSong]);

  const playPrevious = useCallback(() => {
    if (playerState.playlist.length === 0) return;

    let prevIndex = playerState.currentIndex - 1;
    
    if (playerState.isShuffled) {
      prevIndex = Math.floor(Math.random() * playerState.playlist.length);
    } else if (prevIndex < 0) {
      if (playerState.isLooping) {
        prevIndex = playerState.playlist.length - 1;
      } else {
        return;
      }
    }

    playSong(playerState.playlist[prevIndex], playerState.playlist, prevIndex);
  }, [playerState.playlist, playerState.currentIndex, playerState.isShuffled, playerState.isLooping, playSong]);

  const seek = useCallback(async (position: number) => {
    await audioPlayer.seek(position);
  }, []);

  const toggleLoop = useCallback(() => {
    setPlayerState(prev => ({ ...prev, isLooping: !prev.isLooping }));
  }, []);

  const toggleShuffle = useCallback(() => {
    setPlayerState(prev => ({ ...prev, isShuffled: !prev.isShuffled }));
  }, []);

  return {
    playerState,
    playSong,
    togglePlayPause,
    playNext,
    playPrevious,
    seek,
    toggleLoop,
    toggleShuffle,
  };
};
