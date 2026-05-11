import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Colors, FontSize } from '../../constants';

export default function SplashScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>SplashScreen</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.primary,
  },
  title: {
    fontSize: FontSize.xxxl,
    fontWeight: '700',
    color: Colors.white,
  },
});
