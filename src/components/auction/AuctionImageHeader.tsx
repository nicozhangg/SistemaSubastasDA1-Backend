import React from 'react';
import { Image, Modal, Pressable, StyleSheet, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import { Colors } from '../../constants';

type Props = {
  imageUrl: string;
  height: number;
  activeDot?: number;
  dotCount?: number;
  onBack: () => void;
};

export default function AuctionImageHeader({
  imageUrl,
  height,
  activeDot = 0,
  dotCount = 3,
  onBack,
}: Props) {
  const [fullscreen, setFullscreen] = React.useState(false);
  const insets = useSafeAreaInsets();

  const dynamicTop = insets.top > 0 ? insets.top + 8 : 12;

  return (
    <>
      <View style={[styles.wrap, { height }]}>
        <Image source={{ uri: imageUrl }} style={styles.image} resizeMode="cover" />

        {/* Real high-fidelity Linear Gradient Overlay */}
        <LinearGradient
          colors={['rgba(0, 0, 0, 0.45)', 'rgba(0, 0, 0, 0.15)', 'transparent']}
          style={styles.gradient}
        />

        <Pressable 
          style={[styles.backBtn, { top: dynamicTop }]} 
          onPress={onBack} 
          hitSlop={8}
        >
          <Ionicons name="chevron-back" size={22} color={Colors.black} />
        </Pressable>

        <View style={styles.dotsRow}>
          {Array.from({ length: dotCount }).map((_, index) => (
            <View
              key={index}
              style={[styles.dot, index === activeDot && styles.dotActive]}
            />
          ))}
        </View>

        <Pressable
          style={styles.expandBtn}
          onPress={() => setFullscreen(true)}
          hitSlop={8}
        >
          <Ionicons name="expand-outline" size={20} color={Colors.black} />
        </Pressable>
      </View>

      <Modal visible={fullscreen} transparent animationType="fade">
        <View style={styles.fullscreenBackdrop}>
          <Pressable style={styles.fullscreenClose} onPress={() => setFullscreen(false)}>
            <Ionicons name="close" size={28} color={Colors.white} />
          </Pressable>
          <Image
            source={{ uri: imageUrl }}
            style={styles.fullscreenImage}
            resizeMode="contain"
          />
        </View>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  wrap: {
    backgroundColor: '#F5EEF0',
    position: 'relative',
  },
  image: {
    width: '100%',
    height: '100%',
  },
  gradient: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 90,
  },
  backBtn: {
    position: 'absolute',
    left: 16,
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 15,
  },
  dotsRow: {
    position: 'absolute',
    bottom: 36,
    left: 16,
    flexDirection: 'row',
    alignItems: 'center',
  },
  dot: {
    width: 7,
    height: 7,
    borderRadius: 4,
    backgroundColor: '#D0D0D0',
    marginRight: 6,
  },
  dotActive: {
    backgroundColor: Colors.black,
    width: 8,
    height: 8,
  },
  expandBtn: {
    position: 'absolute',
    bottom: 36,
    right: 16,
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
  },
  fullscreenBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.92)',
    justifyContent: 'center',
  },
  fullscreenClose: {
    position: 'absolute',
    top: 48,
    right: 20,
    zIndex: 2,
    padding: 8,
  },
  fullscreenImage: {
    width: '100%',
    height: '70%',
  },
});
