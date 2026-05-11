import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Colors, FontSize } from '../../constants';

export default function UploadItemScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>UploadItemScreen</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.background,
  },
  title: {
    fontSize: FontSize.xxl,
    fontWeight: '700',
    color: Colors.textPrimary,
  },
});
