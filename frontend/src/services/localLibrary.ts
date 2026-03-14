import * as FileSystem from 'expo-file-system/legacy';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Song } from '../types';

const SONGS_DIR = `${FileSystem.documentDirectory}songs/`;
const LIBRARY_KEY = '@local_library';

export async function ensureSongsDir(): Promise<void> {
  const info = await FileSystem.getInfoAsync(SONGS_DIR);
  if (!info.exists) {
    await FileSystem.makeDirectoryAsync(SONGS_DIR, { intermediates: true });
  }
}

export async function getLocalLibrary(): Promise<Song[]> {
  const json = await AsyncStorage.getItem(LIBRARY_KEY);
  if (!json) return [];
  return JSON.parse(json);
}

async function saveLibrary(songs: Song[]): Promise<void> {
  await AsyncStorage.setItem(LIBRARY_KEY, JSON.stringify(songs));
}

export async function downloadToDevice(
  downloadUrl: string,
  filename: string,
  title: string,
  artist: string,
  size: number,
  onProgress?: (progress: number) => void
): Promise<Song> {
  await ensureSongsDir();
  const localPath = `${SONGS_DIR}${filename}`;

  const downloadResumable = FileSystem.createDownloadResumable(
    downloadUrl,
    localPath,
    {},
    (downloadProgress) => {
      if (onProgress && downloadProgress.totalBytesExpectedToWrite > 0) {
        const progress =
          downloadProgress.totalBytesWritten /
          downloadProgress.totalBytesExpectedToWrite;
        onProgress(progress);
      }
    }
  );

  const result = await downloadResumable.downloadAsync();
  if (!result) {
    throw new Error('Download failed');
  }

  const song: Song = {
    id: filename,
    title,
    artist,
    duration: 'Unknown',
    path: downloadUrl,
    localPath,
    filename,
    size,
    dateAdded: new Date().toISOString(),
  };

  const library = await getLocalLibrary();
  const existing = library.findIndex((s) => s.filename === filename);
  if (existing >= 0) {
    library[existing] = song;
  } else {
    library.unshift(song);
  }
  await saveLibrary(library);

  return song;
}

export async function deleteLocalSong(filename: string): Promise<void> {
  const localPath = `${SONGS_DIR}${filename}`;
  const info = await FileSystem.getInfoAsync(localPath);
  if (info.exists) {
    await FileSystem.deleteAsync(localPath);
  }
  const library = await getLocalLibrary();
  const filtered = library.filter((s) => s.filename !== filename);
  await saveLibrary(filtered);
}

export async function isDownloaded(filename: string): Promise<boolean> {
  const library = await getLocalLibrary();
  return library.some((s) => s.filename === filename);
}

export async function getLocalFilenames(): Promise<Set<string>> {
  const library = await getLocalLibrary();
  return new Set(library.map((s) => s.filename));
}
