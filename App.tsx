import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import SearchScreen from './frontend/src/screens/SearchScreen';
import LibraryScreen from './frontend/src/screens/LibraryScreen';
import PlayerScreen from './frontend/src/screens/PlayerScreen';
import Ionicons from '@expo/vector-icons/Ionicons';
import { AudioProvider } from './frontend/src/services/audioPlayer';

const Tab = createBottomTabNavigator();

const tabNavigator = () => (
  <Tab.Navigator
    id="MainTabs"
    screenOptions={({ route }) => ({
      tabBarIcon: ({ focused, color, size }) => {
        let iconName: any;
        if (route.name === 'Search') {
          iconName = focused ? 'search' : 'search-outline';
        } else if (route.name === 'Library') {
          iconName = focused ? 'musical-notes' : 'musical-notes-outline';
        } else if (route.name === 'Player') {
          iconName = focused ? 'play-circle' : 'play-circle-outline';
        }
        return <Ionicons name={iconName} size={size} color={color} />;
      },
      tabBarActiveTintColor: '#FF0000',
      tabBarInactiveTintColor: '#999',
    })}
  >
    <Tab.Screen name="Search" component={SearchScreen} />
    <Tab.Screen name="Library" component={LibraryScreen} />
    <Tab.Screen name="Player" component={PlayerScreen} />
  </Tab.Navigator>
);

export default function App() {
  return (
    <AudioProvider>
      <NavigationContainer>
        {tabNavigator()}
      </NavigationContainer>
    </AudioProvider>
  );
}
