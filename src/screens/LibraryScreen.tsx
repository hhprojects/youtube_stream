import React from 'react';
import { View, Text, FlatList, TouchableOpacity } from 'react-native';
import { StyleSheet } from 'react-native';
import Ionicons from 'react-native-vector-icons/Ionicons';

export default function LibraryScreen({ navigation }) {
  const [songs, setSongs] = useState([
    { id: '1', title: 'Song 1', artist: 'Artist 1', duration: '3:45' },
    { id: '2', title: 'Song 2', artist: 'Artist 2', duration: '4:12' },
    { id: '3', title: 'Song 3', artist: 'Artist 3', duration: '2:58' },
  ]);

  const renderSong = ({ item }) => (
    <TouchableOpacity
      style={styles.songItem}
      onPress={() => navigation.navigate('Player', { song: item })}
    >
      <View style={styles.songInfo}>
        <View style={styles.songDetails}>
          <Text style={styles.songTitle}>{item.title}</Text>
          <Text style={styles.songArtist}>{item.artist}</Text>
        </View>
        <Text style={styles.songDuration}>{item.duration}</Text>
      </View>
      <Ionicons name="play-circle-outline" size={20} color="#FF0000" />
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>My Library</Text>
      <FlatList
        data={songs}
        keyExtractor={(item) => item.id}
        renderItem={renderSong}
        ItemSeparatorComponent={() => <View style={styles.separator} />}
      />
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
    fontWeight: ' Native',
    marginBottom: 20,
    textAlign: 'center',
  },
  songItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 15,
    backgroundColor: '#f5f5f5',
    marginBottom: 10,
    borderRadius: 8,
  },
  songInfo: {
    flex: 1,
  marginLeft: 15,
  },
  songDetails: {
    justifyContent: 'center',
  },
  songTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 5,
  },
  songArtist: {
    fontSize: 14,
    color: '#666',
  },
  songDuration: {
    fontSize: 14,
    color: '#999',
  },
  separator: {
    height: 1,
    backgroundColor: '#e0e0e0',
  },
});
