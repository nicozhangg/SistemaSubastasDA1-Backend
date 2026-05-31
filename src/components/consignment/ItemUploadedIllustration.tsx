import React from 'react';
import { Image, StyleSheet, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../constants';

export default function ItemUploadedIllustration() {
  return (
    <View style={styles.wrap}>
      <Image
        source={require('../../assets/logo.png')}
        style={styles.logo}
        resizeMode="contain"
      />
      <View style={styles.badge}>
        <Ionicons name="checkmark" size={28} color={Colors.white} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    alignItems: 'center',
    justifyContent: 'center',
    height: 160,
    marginVertical: 28,
    position: 'relative',
    width: '100%',
  },
  logo: {
    width: 120,
    height: 120,
  },
  badge: {
    position: 'absolute',
    right: '28%',
    bottom: 8,
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.success,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 3,
    borderColor: Colors.white,
  },
});
