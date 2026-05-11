import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { MainTabParamList } from '../types';
import { Colors } from '../constants';

import { HomeScreen } from '../screens/home';
import { MyBidsScreen, MyAuctionsScreen } from '../screens/activity';
import { ProfileScreen } from '../screens/profile';

const Tab = createBottomTabNavigator<MainTabParamList>();

export default function MainNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: Colors.accent,
        tabBarInactiveTintColor: Colors.tabInactive,
        tabBarStyle: {
          backgroundColor: Colors.tabBar,
          borderTopColor: Colors.border,
        },
      }}
    >
      <Tab.Screen
        name="Home"
        component={HomeScreen}
        options={{ tabBarLabel: 'Inicio' }}
      />
      <Tab.Screen
        name="MyBids"
        component={MyBidsScreen}
        options={{ tabBarLabel: 'Mis Pujas' }}
      />
      <Tab.Screen
        name="MyAuctions"
        component={MyAuctionsScreen}
        options={{ tabBarLabel: 'Mis Subastas' }}
      />
      <Tab.Screen
        name="Profile"
        component={ProfileScreen}
        options={{ tabBarLabel: 'Perfil' }}
      />
    </Tab.Navigator>
  );
}
