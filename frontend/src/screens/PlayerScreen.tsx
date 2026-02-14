import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
} from 'react-native';
import Slider from '@react-native-community/slider';
import Ionicons from '@expo/vector-icons/Ionicons';
import { useMusicPlayer } from '../hooks/useMusicPlayer';
import { SearchResult, Song, AppRepeatMode } from '../types';

export default function PlayerScreen({ route, navigation }: any) {
  const video: SearchResult | Song = route.params?.video || route.params?.song;
  const { playerState, togglePlayPause, playNext, playPrevious, seek, cycleRepeatMode, toggleShuffle, playSongAtIndex } = useMusicPlayer();

  const [sliderValue, setSliderValue] = useState(0);
  const [seeking, setSeeking] = useState(false);
  const [queueExpanded, setQueueExpanded] = useState(false);

  useEffect(() => {
    if (!seeking) {
      setSliderValue(playerState.position);
    }
  }, [playerState.position, seeking]);

  const handleSliderChange = (value: number) => {
    setSliderValue(value);
    setSeeking(true);
  };

  const handleSliderComplete = async (value: number) => {
    await seek(value);
    setSeeking(false);
  };

  const formatTime = (millis: number): string => {
    const totalSeconds = Math.floor(millis / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const currentTrack = playerState.currentSong || video;

  // Helper to safely get artist/channel from SearchResult | Song
  const getArtist = (track: SearchResult | Song | null | undefined): string => {
    if (!track) return 'Unknown Artist';
    if ('artist' in track) return track.artist;
    if ('channel' in track) return track.channel;
    return 'Unknown Artist';
  };

  const getRepeatIcon = (): { name: string; color: string; badge?: string } => {
    switch (playerState.repeatMode) {
      case AppRepeatMode.Track:
        return { name: 'repeat', color: '#FF0000', badge: '1' };
      case AppRepeatMode.Queue:
        return { name: 'repeat', color: '#FF0000' };
      case AppRepeatMode.Off:
      default:
        return { name: 'repeat-outline', color: '#999' };
    }
  };

  const repeatIcon = getRepeatIcon();

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#333" />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Now Playing</Text>
        <View style={styles.headerSpacer} />
      </View>

      <View style={styles.trackInfoContainer}>
        <View style={styles.thumbnailPlaceholder}>
          <Ionicons name="musical-notes" size={80} color="#FF0000" />
        </View>

        <Text style={styles.trackTitle} numberOfLines={2}>
          {currentTrack?.title || 'No track selected'}
        </Text>

        <Text style={styles.trackArtist} numberOfLines={1}>
          {getArtist(currentTrack)}
        </Text>
      </View>

      <View style={styles.progressContainer}>
        <Text style={styles.timeText}>{formatTime(sliderValue)}</Text>
        <View style={styles.sliderContainer}>
          <Slider
            style={styles.slider}
            minimumValue={0}
            maximumValue={playerState.duration}
            value={sliderValue}
            onValueChange={handleSliderChange}
            onSlidingComplete={handleSliderComplete}
            minimumTrackTintColor="#FF0000"
            maximumTrackTintColor="#ccc"
            thumbTintColor="#FF0000"
          />
        </View>
        <Text style={styles.timeText}>{formatTime(playerState.duration)}</Text>
      </View>

      <View style={styles.controlsContainer}>
        <TouchableOpacity
          style={styles.controlButton}
          onPress={toggleShuffle}
        >
          <Ionicons
            name={playerState.isShuffled ? 'shuffle' : 'shuffle-outline'}
            size={24}
            color={playerState.isShuffled ? '#FF0000' : '#999'}
          />
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.mainControlButton}
          onPress={playPrevious}
        >
          <Ionicons name="play-skip-back" size={32} color="#333" />
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.playButton, playerState.isPlaying && styles.playButtonActive]}
          onPress={togglePlayPause}
          disabled={!currentTrack}
        >
          <Ionicons
            name={playerState.isPlaying ? 'pause' : 'play'}
            size={32}
            color="#fff"
          />
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.mainControlButton}
          onPress={playNext}
        >
          <Ionicons name="play-skip-forward" size={32} color="#333" />
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.controlButton}
          onPress={cycleRepeatMode}
        >
          <View>
            <Ionicons
              name={repeatIcon.name as any}
              size={24}
              color={repeatIcon.color}
            />
            {repeatIcon.badge && (
              <View style={styles.repeatBadgeContainer}>
                <Text style={styles.repeatBadgeText}>{repeatIcon.badge}</Text>
              </View>
            )}
          </View>
        </TouchableOpacity>
      </View>

      {playerState.playlist.length > 0 && (
        <View style={styles.queueContainer}>
          <TouchableOpacity
            style={styles.queueHeader}
            onPress={() => setQueueExpanded(!queueExpanded)}
            activeOpacity={0.7}
          >
            <Text style={styles.queueTitle}>
              Up Next ({playerState.playlist.length})
            </Text>
            <Ionicons
              name={queueExpanded ? 'chevron-up' : 'chevron-down'}
              size={20}
              color="#666"
            />
          </TouchableOpacity>

          {queueExpanded && (
            <ScrollView style={styles.queueList} nestedScrollEnabled>
              {playerState.playlist.map((song, index) => {
                if (!song || !song.id) return null;

                return (
                  <TouchableOpacity
                    key={song.id}
                    style={[
                      styles.queueItem,
                      index === playerState.currentIndex && styles.queueItemActive,
                    ]}
                    onPress={() => playSongAtIndex(index)}
                  >
                    <Text
                      style={[
                        styles.queueItemText,
                        index === playerState.currentIndex && styles.queueItemTextActive,
                      ]}
                      numberOfLines={1}
                    >
                      {index + 1}. {song.title}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </ScrollView>
          )}
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 20,
    paddingTop: 40,
  },
  backButton: {
    padding: 5,
  },
  headerTitle: {
    flex: 1,
    fontSize: 18,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  headerSpacer: {
    width: 34,
  },
  trackInfoContainer: {
    alignItems: 'center',
    padding: 20,
  },
  thumbnailPlaceholder: {
    width: 200,
    height: 200,
    borderRadius: 100,
    backgroundColor: '#f5f5f5',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 30,
  },
  trackTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 10,
    paddingHorizontal: 20,
  },
  trackArtist: {
    fontSize: 16,
    color: '#666',
    textAlign: 'center',
  },
  progressContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 30,
    marginTop: 20,
  },
  timeText: {
    fontSize: 12,
    color: '#999',
    width: 40,
  },
  sliderContainer: {
    flex: 1,
    marginHorizontal: 10,
  },
  slider: {
    width: '100%',
    height: 40,
  },
  controlsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    alignItems: 'center',
    marginTop: 40,
    paddingHorizontal: 20,
  },
  controlButton: {
    padding: 10,
  },
  mainControlButton: {
    padding: 15,
  },
  playButton: {
    width: 70,
    height: 70,
    borderRadius: 35,
    backgroundColor: '#FF0000',
    justifyContent: 'center',
    alignItems: 'center',
  },
  playButtonActive: {
    backgroundColor: '#cc0000',
  },
  repeatBadgeContainer: {
    position: 'absolute',
    top: -4,
    right: -6,
    backgroundColor: '#FF0000',
    borderRadius: 6,
    width: 12,
    height: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  repeatBadgeText: {
    color: '#fff',
    fontSize: 8,
    fontWeight: 'bold',
  },
  queueContainer: {
    marginTop: 40,
    paddingHorizontal: 20,
    paddingBottom: 20,
  },
  queueHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 10,
  },
  queueTitle: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  queueList: {
    maxHeight: 200,
    marginTop: 10,
  },
  queueItem: {
    padding: 12,
    backgroundColor: '#f5f5f5',
    marginBottom: 8,
    borderRadius: 8,
  },
  queueItemActive: {
    backgroundColor: '#fff0f0',
    borderWidth: 1,
    borderColor: '#FF0000',
  },
  queueItemText: {
    fontSize: 14,
    color: '#333',
  },
  queueItemTextActive: {
    color: '#FF0000',
    fontWeight: 'bold',
  },
});
