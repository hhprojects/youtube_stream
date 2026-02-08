import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, FlatList, ActivityIndicator } from 'react-native';
import { StyleSheet } from 'react-native';

export default function SearchScreen({ navigation }) {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState([]);

  const handleSearch = async () => {
    if (!query.trim()) return;
    
    setLoading(true);
    // TODO: Call YouTube API / yt-dlp service to search
    // This will be implemented in a separate backend service
    
    // Placeholder for now
    setTimeout(() => {
      setResults([
        { id: '1', title: `${query} - Official Video`, channel: 'OfficialArtist' },
        { id: '2', title: `${query} - Cover`, channel: 'CoverArtist' },
      ]);
      setLoading(false);
    }, 1000);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Search YouTube</Text>
      
      <TextInput
        style={styles.input}
        placeholder="Search videos or music..."
        value={query}
        onChangeText={setQuery}
        onSubmitEditing={handleSearch}
        autoCapitalize="none"
      />
      
      {loading ? (
        <ActivityIndicator size="large" color="#FF0000" />
      ) : (
        <FlatList
          data={results}
          keyExtractor={(item) => item.id}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.item}
              onPress={() => navigation.navigate('Player', { video: item })}
            >
              <Text style={styles.itemTitle}>{item.title}</Text>
              <Text style={styles.itemChannel}>{item.channel}</Text>
            </TouchableOpacity>
          )}
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
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 15,
    fontSize: 16,
    marginBottom: 20,
  },
  item: {
    padding: 15,
    backgroundColor: '#f5f5f5',
    marginBottom: 10,
    borderRadius: 8,
  },
  itemTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 5,
  },
  itemChannel: {
    fontSize: 14,
    color: '#666',
  },
});
