import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import { AuthStackParamList } from '../types';

import {
  SplashScreen,
  AccessScreen,
  LoginScreen,
  ForgotPasswordScreen,
  RegisterStep1Screen,
  RegisterStep2Screen,
  RegisterStep3Screen,
  PendingApprovalScreen,
} from '../screens/auth';

const Stack = createStackNavigator<AuthStackParamList>();

export default function AuthNavigator() {
  return (
    <Stack.Navigator
      initialRouteName="Splash"
      screenOptions={{ headerShown: false }}
    >
      <Stack.Screen name="Splash" component={SplashScreen} />
      <Stack.Screen name="Access" component={AccessScreen} />
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
      <Stack.Screen name="RegisterStep1" component={RegisterStep1Screen} />
      <Stack.Screen name="RegisterStep2" component={RegisterStep2Screen} />
      <Stack.Screen name="RegisterStep3" component={RegisterStep3Screen} />
      <Stack.Screen name="PendingApproval" component={PendingApprovalScreen} />
    </Stack.Navigator>
  );
}
