import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { RootStackParamList } from '../types';
import { useAuthStore } from '../stores';

import AuthNavigator from './AuthNavigator';
import MainNavigator from './MainNavigator';

// Modal screens
import { AuctionDetailScreen, ConfirmBidScreen, LoginWallScreen } from '../screens/auction';
import { ChatListScreen, ChatDetailScreen } from '../screens/chat';
import { UploadItemScreen, ItemUploadedScreen } from '../screens/consignment';
import { AddressListScreen, AddAddressScreen, PaymentMethodsScreen, AddCardScreen, AddCheckScreen } from '../screens/profile';

const Stack = createStackNavigator<RootStackParamList>();

export default function RootNavigator() {
  const { isAuthenticated, isGuest } = useAuthStore();

  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      {!isAuthenticated && !isGuest ? (
        // Auth flow
        <Stack.Screen name="Auth" component={AuthNavigator} />
      ) : (
        // Main app flow
        <>
          <Stack.Screen name="Main" component={MainNavigator} />

          {/* Modal screens (stack sobre Main) */}
          <Stack.Group screenOptions={{ presentation: 'modal' }}>
            <Stack.Screen name="AuctionDetail" component={AuctionDetailScreen} />
            <Stack.Screen name="ConfirmBid" component={ConfirmBidScreen} />
            <Stack.Screen name="LoginWall" component={LoginWallScreen} />
            <Stack.Screen name="ChatList" component={ChatListScreen} />
            <Stack.Screen name="ChatDetail" component={ChatDetailScreen} />
            <Stack.Screen name="UploadItem" component={UploadItemScreen} />
            <Stack.Screen name="ItemUploaded" component={ItemUploadedScreen} />
            <Stack.Screen name="AddressList" component={AddressListScreen} />
            <Stack.Screen name="AddAddress" component={AddAddressScreen} />
            <Stack.Screen name="PaymentMethods" component={PaymentMethodsScreen} />
            <Stack.Screen name="AddCard" component={AddCardScreen} />
            <Stack.Screen name="AddCheck" component={AddCheckScreen} />
          </Stack.Group>
        </>
      )}
    </Stack.Navigator>
  );
}
