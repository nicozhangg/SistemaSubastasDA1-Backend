import React from 'react';
import { Platform } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MainTabParamList } from '../types';
import { Colors, Fonts, FontSize } from '../constants';

import HomeStack from './HomeStack';
import ProfileStack from './ProfileStack';
import MyAuctionsStack from './MyAuctionsStack';
import { MyBidsScreen } from '../screens/activity';

const Tab = createBottomTabNavigator<MainTabParamList>();

type TabIconName = React.ComponentProps<typeof Ionicons>['name'];

const TAB_ICONS: Record<keyof MainTabParamList, { active: TabIconName; inactive: TabIconName }> = {
  Home: { active: 'home', inactive: 'home-outline' },
  MyBids: { active: 'hammer', inactive: 'hammer-outline' },
  MyAuctions: { active: 'albums', inactive: 'albums-outline' },
  Profile: { active: 'person', inactive: 'person-outline' },
};

const TAB_BAR_BASE_HEIGHT = Platform.OS === 'ios' ? 56 : 60;

export default function MainNavigator() {
  const insets = useSafeAreaInsets();
  const bottomInset = Math.max(insets.bottom, Platform.OS === 'android' ? 8 : 0);

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: Colors.accent,
        tabBarInactiveTintColor: Colors.tabInactive,
        tabBarHideOnKeyboard: true,
        tabBarLabelStyle: {
          fontFamily: Fonts.sora,
          fontSize: FontSize.xs,
          marginTop: 2,
        },
        tabBarStyle: {
          backgroundColor: Colors.tabBar,
          borderTopColor: Colors.border,
          borderTopWidth: 1,
          height: TAB_BAR_BASE_HEIGHT + bottomInset,
          paddingTop: 6,
          paddingBottom: bottomInset,
        },
        tabBarIcon: ({ focused, color, size }) => {
          const icons = TAB_ICONS[route.name];
          const iconName = focused ? icons.active : icons.inactive;
          return <Ionicons name={iconName} size={size} color={color} />;
        },
      })}
    >
      <Tab.Screen
        name="Home"
        component={HomeStack}
        options={{ tabBarLabel: 'Inicio' }}
      />
      <Tab.Screen
        name="MyBids"
        component={MyBidsScreen}
        options={{ tabBarLabel: 'Mis Pujas' }}
      />
      <Tab.Screen
        name="MyAuctions"
        component={MyAuctionsStack}
        options={{ tabBarLabel: 'Mis Subastas' }}
      />
      <Tab.Screen
        name="Profile"
        component={ProfileStack}
        options={{ tabBarLabel: 'Perfil' }}
      />
    </Tab.Navigator>
  );
}
