import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { HomeStackParamList } from '../types';
import { HomeScreen, LotDetailScreen } from '../screens/home';
import { LoginWallScreen } from '../screens/auction';
import { ChatListScreen, ChatDetailScreen } from '../screens/chat';
import { UploadItemScreen, ItemUploadedScreen } from '../screens/consignment';

const Stack = createStackNavigator<HomeStackParamList>();

export default function HomeStack() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="HomeMain" component={HomeScreen} />
      <Stack.Screen name="UploadItem" component={UploadItemScreen} />
      <Stack.Screen name="ItemUploaded" component={ItemUploadedScreen} />
      <Stack.Screen name="ChatList" component={ChatListScreen} />
      <Stack.Screen name="ChatDetail" component={ChatDetailScreen} />
      <Stack.Screen name="LoginWall" component={LoginWallScreen} />
      <Stack.Screen name="LotDetail" component={LotDetailScreen} />
    </Stack.Navigator>
  );
}
