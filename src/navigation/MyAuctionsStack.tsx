import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { MyAuctionsStackParamList } from '../types';
import { MyAuctionsScreen } from '../screens/activity';
import { UploadItemScreen, ItemUploadedScreen } from '../screens/consignment';

const Stack = createStackNavigator<MyAuctionsStackParamList>();

export default function MyAuctionsStack() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="MyAuctionsMain" component={MyAuctionsScreen} />
      <Stack.Screen name="UploadItem" component={UploadItemScreen} />
      <Stack.Screen name="ItemUploaded" component={ItemUploadedScreen} />
    </Stack.Navigator>
  );
}
