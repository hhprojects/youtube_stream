import { useState, useEffect, useCallback } from 'react';
import { Song } from '../types';
import { useGlobalAudio } from '../services/audioPlayer';

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
  const { playSong, play, pause, seek, playerState, currentUrl } = useGlobalAudio();
  const [localPlayerState, setLocalPlayerState] = useState<PlayerState>({
    isPlaying: false,
    currentSong: null,
    playlist: [],
    currentIndex: -1,
    isLooping: false,
    isShuffled: false,
    position: 0,
    duration: 0,
  });

  // Sync global player state with local state
  useEffect(() => {
    setLocalPlayerState(prev => ({
      ...prev,
      isPlaying: playerState.isPlaying,
      position: playerState.positionMillis,
      duration: playerState.durationMillis,
    }));
  }, [playerState]);

  const playSongLocal = useCallback(async (song: Song, playlist?: Song[], index?: number) => {
    try {
      await playSong(song.path);

      setLocalPlayerState(prev => ({
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
  }, [playSong]);

  const playNext = useCallback(() => {
    const { playlist, currentIndex, isLooping, isShuffled } = localPlayerState;
    if (playlist.length === 0) return;

    let nextIndex = currentIndex + 1;

    if (isShuffled) {
      nextIndex = Math.floor(Math.random() * playlist.length);
    } else if (nextIndex >= playlist.length) {
      if (isLooping) {
        nextIndex = 0;
      } else {
        return;
      }
    }

    playSongLocal(playlist[nextIndex], playlist, nextIndex);
  }, [localPlayerState, playSongLocal]);

  const playPrevious = useCallback(() => {
    const { playlist, currentIndex, isLooping, isShuffled } = localPlayerState;
    if (playlist.length === 0) return;

    let prevIndex = currentIndex - 1;

    if (isShuffled) {
      prevIndex = Math.floor(Math.random() * playlist.length);
    } else if (prevIndex < 0) {
      if (isLooping) {
        prevIndex = playlist.length - 1;
      } else {
        return;
      }
    }

    playSongLocal(playlist[prevIndex], playlist, prevIndex);
  }, [localPlayerState, playSongLocal]);

  // Check for song finish and auto-play next
  useEffect(() => {
    const { playlist, isLooping, isPlaying } = localPlayerState;
    if (!isPlaying || playlist.length === 0) return;

    // Check if song finished (position near duration)
    if (playerState.positionMillis > 0 &&
        playerState.durationMillis > 0 &&
        Math.abs(playerState.positionMillis - playerState.durationMillis) < 100 &&
        !playerState.isPlaying) {
      if (isLooping && playlist.length === 1) {
        seek(0);
        play();
      } else {
        playNext();
      }
    }
  }, [playerState, localPlayerState, playNext, seek, play]);

  const togglePlayPause = useCallback(async () => {
    const { currentSong } = localPlayerState;
    if (!currentSong) return;

    if (localPlayerState.isPlaying) {
      await pause();
    } else {
      await play();
    }
  }, [localPlayerState, pause, play]);

  const toggleLoop = useCallback(() => {
    setLocalPlayerState(prev => ({ ...prev, isLooping: !prev.isLooping }));
  }, []);

  const toggleShuffle = useCallback(() => {
    setLocalPlayerState(prev => ({ ...prev, isShuffled: !prev.isShuffled }));
  }, []);

  return {
    playerState: localPlayerState,
    playSong: playSongLocal,
    togglePlayPause,
    playNext,
    playPrevious,
    seek,
    toggleLoop,
    toggleShuffle,
  };
};
