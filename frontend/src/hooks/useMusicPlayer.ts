import { useCallback } from 'react';
import { useGlobalAudio } from '../services/audioPlayer';
import { Song } from '../types';

export const useMusicPlayer = () => {
  const {
    isPlayerReady,
    currentSong,
    playlist,
    currentIndex,
    isPlaying,
    position,
    duration,
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
  } = useGlobalAudio();

  // Start a new playlist (from Library/Search screens)
  const playSong = useCallback((song: Song, songPlaylist?: Song[], index?: number) => {
    if (!isPlayerReady) return;
    playSongFromPlaylist(song, songPlaylist || [song], index ?? 0);
  }, [isPlayerReady, playSongFromPlaylist]);

  return {
    playerState: {
      isPlaying,
      currentSong,
      playlist,
      currentIndex,
      repeatMode,
      isShuffled,
      position,
      duration,
    },
    playSong,
    playSongAtIndex,
    togglePlayPause,
    playNext,
    playPrevious,
    seek,
    toggleShuffle,
    cycleRepeatMode,
  };
};
