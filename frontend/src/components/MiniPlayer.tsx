import React from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Image,
} from 'react-native';
import { useMusicPlayer } from '../hooks/useMusicPlayer';
import { Song } from '../types';
import Ionicons from '@expo/vector-icons/Ionicons';

interface MiniPlayerProps {
  navigation: any;
}

export const MiniPlayer: React.FC<MiniPlayerProps> = ({ navigation }) => {
  const { playerState, togglePlayPause, playNext } = useMusicPlayer();

  if (!playerState.currentSong) {
    return null;
  }

  const handlePress = () => {
    navigation.navigate('Player');
  };

  const formatTime = (millis: number): string => {
    const totalSeconds = Math.floor(millis / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const getArtist = (song: Song): string => {
    if (!song) return 'Unknown Artist';
    return song.artist || 'Unknown Artist';
  };

  return (
    <TouchableOpacity
      style={styles.container}
      onPress={handlePress}
      activeOpacity={0.9}
    >
      <View style={styles.thumbnailContainer}>
        <Ionicons name="musical-notes" size={20} color="#FF0000" />
      </View>

      <View style={styles.infoContainer}>
        <Text style={styles.title} numberOfLines={1}>
          {playerState.currentSong.title}
        </Text>
        <Text style={styles.artist} numberOfLines={1}>
          {getArtist(playerState.currentSong)}
        </Text>
      </View>

      <Text style={styles.time}>
        {formatTime(playerState.position)} / {formatTime(playerState.duration)}
      </Text>

      <TouchableOpacity
        style={styles.playButton}
        onPress={(e) => {
          e.stopPropagation();
          togglePlayPause();
        }}
      >
        <Ionicons
          name={playerState.isPlaying ? 'pause' : 'play'}
          size={20}
          color="#FF0000"
        />
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.nextButton}
        onPress={(e) => {
          e.stopPropagation();
          playNext();
        }}
      >
        <Ionicons name="play-skip-forward" size={20} color="#999" />
      </TouchableOpacity>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    paddingBottom: 20,
    elevation: 8,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  thumbnailContainer: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#f5f5f5',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  infoContainer: {
    flex: 1,
    marginRight: 12,
  },
  title: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 2,
  },
  artist: {
    fontSize: 12,
    color: '#666',
  },
  time: {
    fontSize: 11,
    color: '#999',
    marginRight: 12,
  },
  playButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: '#FF0000',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 8,
  },
  nextButton: {
    padding: 4,
  },
});
