import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { SearchScreen } from './screens/SearchScreen';
import { LibraryScreen } from './screens/LibraryScreen';
import { PlayerScreen } from './screens/PlayerScreen';
import Ionicons from 'react-native-vector-icons/Ionicons';

const Tab = createBottomTabNavigator();

const tabNavigator = () => (
  <Tab.Navigator
    screenOptions={({ route }) => ({
      tabBarIcon: ({ focused, color, size }) => {
        let iconName;
        if (route.name === 'Search') {
          iconName = focused ? 'search' : 'search-outline';
        } else if (route.name === 'Library') {
          iconName = focused ? 'musical-notes' : 'musical-notes-outline';
        } else if (route.name === 'Player') {
          iconName = focused ? 'play-circle' : 'play-circle-outline';
        }
        return <Ionicons name={iconName} size={size} color={color} />;
      },
    })}
  >
    <Tab.Screen name="Search" component={SearchScreen} />
    <Tab.Screen name="Library" component={LibraryScreen} />
    <Tab.Screen name="Player" component={PlayerScreen} />
  </Tab.Navigator>
);

export default function App() {
  return (
    <NavigationContainer>
      {tabNavigator()}
    </NavigationContainer>
  );
}
