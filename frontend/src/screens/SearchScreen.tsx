import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  ActivityIndicator,
  Alert,
  Image,
} from 'react-native';
import { StyleSheet } from 'react-native';
import { searchVideos, downloadAudio } from '../services/api';
import { SearchResult } from '../types';
import Ionicons from '@expo/vector-icons/Ionicons';
import { MiniPlayer } from '../components/MiniPlayer';

export default function SearchScreen({ navigation }: any) {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [downloading, setDownloading] = useState<string | null>(null);
  const [results, setResults] = useState<SearchResult[]>([]);

  const handleSearch = async () => {
    if (!query.trim()) return;

    setLoading(true);
    try {
      const videos = await searchVideos(query);
      setResults(videos);
    } catch (error: any) {
      console.error('Search error:', error);
      const msg = error?.response?.data?.error || 'Failed to search. Please try again.';
      Alert.alert('Error', msg);
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = async (video: SearchResult) => {
    setDownloading(video.id);
    try {
      const song = await downloadAudio(video.id, video.title);
      Alert.alert(
        'Success',
        `"${video.title}" downloaded successfully!`,
        [
          {
            text: 'OK',
            onPress: () => navigation.navigate('Library'),
          },
        ]
      );
    } catch (error: any) {
      console.error('Download error:', error);
      const msg = error?.response?.data?.error || 'Failed to download. Please try again.';
      Alert.alert('Error', msg);
    } finally {
      setDownloading(null);
    }
  };

  const renderItem = ({ item }: { item: SearchResult }) => (
    <View
      style={styles.item}
    >
      <Image
        source={{ uri: item.thumbnail || 'https://via.placeholder.com/120' }}
        style={styles.thumbnail}
      />
      <View style={styles.itemContent}>
        <Text style={styles.itemTitle} numberOfLines={2}>{item.title}</Text>
        <Text style={styles.itemChannel}>{item.channel}</Text>
        {item.duration && (
          <Text style={styles.itemDuration}>{Math.floor(item.duration / 60)}:{(item.duration % 60).toString().padStart(2, '0')}</Text>
        )}
      </View>
      <TouchableOpacity
        style={styles.downloadButton}
        onPress={() => handleDownload(item)}
        disabled={downloading === item.id}
      >
        {downloading === item.id ? (
          <ActivityIndicator size="small" color="#FF0000" />
        ) : (
          <Ionicons name="download-outline" size={24} color="#FF0000" />
        )}
      </TouchableOpacity>
    </View>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Search YouTube</Text>

      <View style={styles.searchContainer}>
        <TextInput
          style={styles.input}
          placeholder="Search videos or music..."
          value={query}
          onChangeText={setQuery}
          onSubmitEditing={handleSearch}
          autoCapitalize="none"
        />
        <TouchableOpacity
          style={styles.searchButton}
          onPress={handleSearch}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator size="small" color="#fff" />
          ) : (
            <Ionicons name="search" size={24} color="#fff" />
          )}
        </TouchableOpacity>
      </View>

      <FlatList
        data={results}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Ionicons name="search-outline" size={64} color="#ccc" />
            <Text style={styles.emptyText}>
              Search for your favorite music
            </Text>
          </View>
        }
        contentContainerStyle={styles.listContent}
      />
      <MiniPlayer navigation={navigation} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  searchContainer: {
    flexDirection: 'row',
    marginBottom: 20,
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 15,
    fontSize: 16,
    marginRight: 10,
  },
  searchButton: {
    backgroundColor: '#FF0000',
    borderRadius: 8,
    padding: 15,
    justifyContent: 'center',
    alignItems: 'center',
  },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 10,
    backgroundColor: '#f5f5f5',
    marginBottom: 10,
    borderRadius: 8,
  },
  thumbnail: {
    width: 80,
    height: 45,
    borderRadius: 4,
    marginRight: 10,
  },
  itemContent: {
    flex: 1,
  },
  itemTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    marginBottom: 2,
  },
  itemChannel: {
    fontSize: 12,
    color: '#666',
    marginBottom: 2,
  },
  itemDuration: {
    fontSize: 12,
    color: '#999',
  },
  downloadButton: {
    padding: 8,
    marginLeft: 8,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 60,
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
    marginTop: 10,
  },
  listContent: {
    paddingBottom: 100, // Space for mini player
  },
});
