import React, { useState } from 'react';
import { View, Text, TouchableOpacity, ScrollView } from 'react-native';
import { StyleSheet } from 'react-native';
import Ionicons from 'react-native-vector-icons/Ionicons';
import Audio from 'react-native-voice'; // Note: This will need to be installed

export default function PlayerScreen({ route, navigation }) {
  const video = route.params?.video || route.params?.song;
  const [isPlaying, setIsPlaying] = useState(false);
  const [isLooping, setIsLooping] = useState(false);
  const [shuffle, setShuffle] = useState(false);

  const togglePlayPause = () => {
    if (isPlaying) {
      Audio.stop();
      setIsPlaying(false);
    } else {
      // TODO: Implement play logic using react-native-voice or expo-av
      setIsPlaying(true);
    }
  };

  const handleNext = () => {
    // TODO: Play next item in queue
    console.log('Next');
  };

  const handlePrevious = () => {
    // TODO: Play previous item in queue
    console.log('Previous');
  };

  const toggleLoop = () => {
    setIsLooping(!isLooping);
    console.log('Loop:', !isLooping);
  };

  const toggleShuffle = () => {
    setShuffle(!shuffle);
    console.log('Shuffle:', !shuffle);
  };

  const handleQueue = () => {
    navigation.navigate('Library');
  };

  return (
    <ScrollView style={styles.container}>
      <Text style={styles.title}>{video?.title || 'No track selected'}</Text>
      
      <View style={styles.playerControls}>
        <TouchableOpacity style={styles.controlButton} onPress={handlePrevious}>
          <Ionicons name="play-skip-back" size={30} color="#FF0000" />
        </TouchableOpacity>
        
        <TouchableOpacity style={[styles.playButton, isPlaying && styles.playButtonActive]} onPress={togglePlayPause}>
          <Ionicons 
            name={isPlaying ? 'pause-circle' : 'play-circle'} 
            size={50} 
            color={isPlaying ? '#FF0000' : '#FF0000'} 
          />
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.controlButton} onPress={handleNext}>
          <Ionicons name="play-skip-forward" size={30} color="#FF0000" />
        </TouchableOpacity>
      </View>
      
      <View style={styles.extraControls}>
        <TouchableOpacity style={[styles.extraButton, isLooping && styles.extraButtonActive]} onPress={toggleLoop}>
          <Text style={styles.extraButtonText}>🔁 Loop</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={[styles.extraButton, shuffle && styles.extraButtonActive]} onPress={toggleShuffle}>
          <Text style={styles.extraButtonText}>🔀 Shuffle</Text>
        </TouchableOpacity>
        
        <TouchableOpacity style={styles.queueButton} onPress={handleQueue}>
          <Ionicons name="list-outline" size={24} color="#FF0000" />
          <Text style={styles.queueButtonText}>Queue</Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
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
    marginBottom: 30,
    textAlign: 'center',
  },
  playerControls: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 30,
  },
  controlButton: {
    padding: 15,
    backgroundColor: '#f5f5f5',
    borderRadius: 50,
    marginHorizontal: 10,
  },
  playButton: {
    padding: 20,
    backgroundColor: '#FF0000',
    borderRadius: 50,
  },
  playButtonActive: {
    backgroundColor: '#333',
  },
  extraControls: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    padding: 20,
  },
  extraButton: {
    padding: 12,
    backgroundColor: '#f5f5f5',
    borderRadius: 25,
  },
  extraButtonActive: {
    backgroundColor: '#333',
  },
  extraButtonText: {
    color: '#333',
    fontSize: 12,
    fontWeight: 'bold',
  },
  queueButton: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 12,
    backgroundColor: '#e0e0e0',
    borderRadius: 25,
  },
  queueButtonText: {
    color: '#333',
    fontSize: 14,
    marginLeft: 8,
    fontWeight: 'bold',
  },
});
