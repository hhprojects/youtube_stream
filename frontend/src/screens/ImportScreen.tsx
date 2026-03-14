import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { PiSong } from '../types';
import { getPiLibrary } from '../services/api';
import { getLocalFilenames, downloadToDevice } from '../services/localLibrary';
import Ionicons from '@expo/vector-icons/Ionicons';

export default function ImportScreen({ navigation }: any) {
  const insets = useSafeAreaInsets();
  const [piSongs, setPiSongs] = useState<PiSong[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [downloading, setDownloading] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState<Record<string, number>>({});

  useEffect(() => {
    loadPiSongs();
  }, []);

  const loadPiSongs = async () => {
    try {
      const allPiSongs = await getPiLibrary();
      const localFilenames = await getLocalFilenames();
      const notDownloaded = allPiSongs.filter(
        (s) => !localFilenames.has(s.filename)
      );
      setPiSongs(notDownloaded);
    } catch (error: any) {
      const msg = error?.response?.data?.error || error?.message || 'Connection failed';
      Alert.alert('Connection Error', `Could not reach Pi: ${msg}`);
    } finally {
      setLoading(false);
    }
  };

  const toggleSelect = (filename: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(filename)) {
        next.delete(filename);
      } else {
        next.add(filename);
      }
      return next;
    });
  };

  const selectAll = () => {
    if (selected.size === piSongs.length) {
      setSelected(new Set());
    } else {
      setSelected(new Set(piSongs.map((s) => s.filename)));
    }
  };

  const handleDownloadSelected = async () => {
    if (selected.size === 0) return;

    setDownloading(true);
    const toDownload = piSongs.filter((s) => selected.has(s.filename));
    let succeeded = 0;
    let failed = 0;

    for (const song of toDownload) {
      try {
        await downloadToDevice(
          song.downloadUrl,
          song.filename,
          song.title,
          song.artist,
          song.size,
          (progress) => {
            setDownloadProgress((prev) => ({
              ...prev,
              [song.filename]: progress,
            }));
          }
        );
        succeeded++;
      } catch {
        failed++;
      }
    }

    setDownloading(false);
    setDownloadProgress({});

    const msg = failed > 0
      ? `${succeeded} downloaded, ${failed} failed`
      : `${succeeded} songs downloaded`;

    Alert.alert('Import Complete', msg, [
      { text: 'OK', onPress: () => navigation.goBack() },
    ]);
  };

  const formatSize = (bytes: number): string => {
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const renderSong = ({ item }: { item: PiSong }) => {
    const isSelected = selected.has(item.filename);
    const progress = downloadProgress[item.filename];

    return (
      <TouchableOpacity
        style={[styles.songItem, isSelected && styles.songItemSelected]}
        onPress={() => !downloading && toggleSelect(item.filename)}
        disabled={downloading}
      >
        <View style={styles.checkbox}>
          <Ionicons
            name={isSelected ? 'checkbox' : 'square-outline'}
            size={24}
            color={isSelected ? '#FF0000' : '#999'}
          />
        </View>
        <View style={styles.songInfo}>
          <Text style={styles.songTitle} numberOfLines={1}>
            {item.title}
          </Text>
          <Text style={styles.songMeta} numberOfLines={1}>
            {item.artist} · {formatSize(item.size)}
          </Text>
        </View>
        {progress !== undefined && (
          <Text style={styles.progressText}>{Math.round(progress * 100)}%</Text>
        )}
      </TouchableOpacity>
    );
  };

  if (loading) {
    return (
      <View style={[styles.container, { paddingTop: insets.top }]}>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color="#FF0000" />
          <Text style={styles.loadingText}>Connecting to Pi...</Text>
        </View>
      </View>
    );
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Ionicons name="arrow-back" size={24} color="#333" />
        </TouchableOpacity>
        <Text style={styles.title}>Import from Pi</Text>
        <TouchableOpacity onPress={selectAll} style={styles.selectAllButton}>
          <Text style={styles.selectAllText}>
            {selected.size === piSongs.length ? 'Deselect All' : 'Select All'}
          </Text>
        </TouchableOpacity>
      </View>

      {piSongs.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Ionicons name="checkmark-circle-outline" size={64} color="#ccc" />
          <Text style={styles.emptyText}>All songs are on your device</Text>
        </View>
      ) : (
        <>
          <FlatList
            data={piSongs}
            keyExtractor={(item) => item.filename}
            renderItem={renderSong}
            contentContainerStyle={styles.listContent}
          />
          {selected.size > 0 && !downloading && (
            <TouchableOpacity
              style={styles.downloadBar}
              onPress={handleDownloadSelected}
            >
              <Ionicons name="download" size={20} color="#fff" />
              <Text style={styles.downloadBarText}>
                Download {selected.size} song{selected.size !== 1 ? 's' : ''}
              </Text>
            </TouchableOpacity>
          )}
          {downloading && (
            <View style={styles.downloadBar}>
              <ActivityIndicator size="small" color="#fff" />
              <Text style={styles.downloadBarText}>Downloading...</Text>
            </View>
          )}
        </>
      )}
    </View>
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
    paddingBottom: 10,
  },
  backButton: {
    marginRight: 12,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    flex: 1,
  },
  selectAllButton: {
    padding: 8,
  },
  selectAllText: {
    color: '#FF0000',
    fontSize: 14,
    fontWeight: '600',
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 10,
    color: '#999',
  },
  songItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 15,
    marginHorizontal: 20,
    marginBottom: 8,
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
  },
  songItemSelected: {
    backgroundColor: '#fff0f0',
    borderWidth: 1,
    borderColor: '#FF0000',
  },
  checkbox: {
    marginRight: 12,
  },
  songInfo: {
    flex: 1,
  },
  songTitle: {
    fontSize: 15,
    fontWeight: 'bold',
    marginBottom: 3,
  },
  songMeta: {
    fontSize: 13,
    color: '#666',
  },
  progressText: {
    fontSize: 13,
    color: '#FF0000',
    fontWeight: '600',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 16,
    color: '#999',
    marginTop: 10,
  },
  listContent: {
    paddingBottom: 80,
  },
  downloadBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: '#FF0000',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
    paddingBottom: 24,
    gap: 8,
  },
  downloadBarText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});
