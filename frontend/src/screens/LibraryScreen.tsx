import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from 'react-native';
import { StyleSheet } from 'react-native';
import { getLibrary, deleteSong, Song } from '../services/api';
import { useMusicPlayer } from '../hooks/useMusicPlayer';
import Ionicons from '@expo/vector-icons/Ionicons';

export default function LibraryScreen({ navigation }: any) {
  const [songs, setSongs] = useState<Song[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const { playSong, playerState } = useMusicPlayer();

  const fetchLibrary = async () => {
    try {
      const library = await getLibrary();
      setSongs(library);
    } catch (error) {
      console.error('Library error:', error);
      Alert.alert('Error', 'Failed to load library. Make sure the backend is running.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchLibrary();
  }, []);

  const handleRefresh = () => {
    setRefreshing(true);
    fetchLibrary();
  };

  const handleSongPress = (song: Song, index: number) => {
    playSong(song, songs, index);
    navigation.navigate('Player');
  };

  const handleDelete = (song: Song) => {
    Alert.alert(
      'Delete Song',
      `Are you sure you want to delete "${song.title}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            try {
              await deleteSong(song.id);
              // If deleted song is currently playing, stop it
              if (playerState.currentSong?.id === song.id) {
                // Stop playback logic could go here
              }
              fetchLibrary();
            } catch (error) {
              console.error('Delete error:', error);
              Alert.alert('Error', 'Failed to delete song.');
            }
          },
        },
      ]
    );
  };

  const renderSong = ({ item, index }: { item: Song; index: number }) => {
    const isPlaying = playerState.currentSong?.id === item.id && playerState.isPlaying;
    
    return (
      <TouchableOpacity
        style={[styles.songItem, isPlaying && styles.songItemPlaying]}
        onPress={() => handleSongPress(item, index)}
      >
        <View style={styles.songIcon}>
          {isPlaying ? (
            <Ionicons name="musical-notes" size={24} color="#FF0000" />
          ) : (
            <Ionicons name="play-circle-outline" size={24} color="#FF0000" />
          )}
        </View>
        <View style={styles.songInfo}>
          <Text style={[styles.songTitle, isPlaying && styles.songTitlePlaying]} numberOfLines={1}>
            {item.title}
          </Text>
          <Text style={styles.songArtist} numberOfLines={1}>{item.artist}</Text>
        </View>
        <View style={styles.songRight}>
          <Text style={styles.songDuration}>{item.duration}</Text>
          <TouchableOpacity
            style={styles.deleteButton}
            onPress={() => handleDelete(item)}
          >
            <Ionicons name="trash-outline" size={20} color="#FF0000" />
          </TouchableOpacity>
        </View>
      </TouchableOpacity>
    );
  };

  if (loading) {
    return (
      <View style={styles.container}>
        <Text style={styles.title}>My Library</Text>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#FF0000" />
        </View>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>My Library</Text>
        <TouchableOpacity onPress={handleRefresh} style={styles.refreshButton}>
          <Ionicons name="refresh" size={24} color="#FF0000" />
        </TouchableOpacity>
      </View>
      
      {songs.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Ionicons name="musical-notes-outline" size={64} color="#ccc" />
          <Text style={styles.emptyText}>
            No songs yet
          </Text>
          <Text style={styles.emptySubtext}>
            Search and download songs to build your library
          </Text>
        </View>
      ) : (
        <FlatList
          data={songs}
          keyExtractor={(item) => item.id}
          renderItem={renderSong}
          ItemSeparatorComponent={() => <View style={styles.separator} />}
          refreshing={refreshing}
          onRefresh={handleRefresh}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  refreshButton: {
    padding: 8,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  songItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 15,
    backgroundColor: '#f5f5f5',
    marginBottom: 10,
    borderRadius: 8,
  },
  songItemPlaying: {
    backgroundColor: '#fff0f0',
    borderWidth: 1,
    borderColor: '#FF0000',
  },
  songIcon: {
    marginRight: 15,
  },
  songInfo: {
    flex: 1,
  },
  songTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 5,
  },
  songTitlePlaying: {
    color: '#FF0000',
  },
  songArtist: {
    fontSize: 14,
    color: '#666',
  },
  songRight: {
    alignItems: 'center',
    flexDirection: 'row',
  },
  songDuration: {
    fontSize: 14,
    color: '#999',
    marginRight: 10,
  },
  deleteButton: {
    padding: 5,
  },
  separator: {
    height: 1,
    backgroundColor: '#e0e0e0',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 60,
  },
  emptyText: {
    fontSize: 18,
    color: '#666',
    marginTop: 10,
    marginBottom: 5,
  },
  emptySubtext: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
  },
});
