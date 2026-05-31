import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { Colors, Fonts, FontSize } from '../../constants';

type Props = {
  onPress: () => void;
};

export default function ConsignPromoBanner({ onPress }: Props) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.wrap, pressed && styles.pressed]}
      accessibilityRole="button"
      accessibilityLabel="Subastá tu artículo"
    >
      <LinearGradient
        colors={['#FFD166', '#FC9905', '#F57F17']}
        start={{ x: 0, y: 0.5 }}
        end={{ x: 1, y: 0.5 }}
        style={styles.gradient}
      >
        <View style={styles.shine} pointerEvents="none" />
        <View style={styles.decorLarge} pointerEvents="none" />
        <View style={styles.decorSmall} pointerEvents="none" />

        <View style={styles.row}>
          <View style={styles.iconRing}>
            <Ionicons name="hammer" size={26} color={Colors.white} />
          </View>

          <View style={styles.copy}>
            <Text style={styles.eyebrow}>¿Tenés algo para vender?</Text>
            <Text style={styles.title}>Subastá tu artículo</Text>
            <Text style={styles.subtitle}>Solicitá tu publicación</Text>
          </View>

          <View style={styles.cta}>
            <Ionicons name="arrow-forward" size={22} color={Colors.white} />
          </View>
        </View>
      </LinearGradient>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  wrap: {
    marginHorizontal: 12,
    marginBottom: 14,
    borderRadius: 18,
    shadowColor: '#C45A00',
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.35,
    shadowRadius: 10,
    elevation: 8,
  },
  pressed: {
    opacity: 0.94,
    transform: [{ scale: 0.985 }],
  },
  gradient: {
    borderRadius: 18,
    overflow: 'hidden',
    minHeight: 88,
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.35)',
  },
  shine: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: '45%',
    backgroundColor: 'rgba(255,255,255,0.18)',
    borderBottomLeftRadius: 80,
    borderBottomRightRadius: 80,
  },
  decorLarge: {
    position: 'absolute',
    right: -24,
    top: -20,
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: 'rgba(255,255,255,0.12)',
  },
  decorSmall: {
    position: 'absolute',
    right: 48,
    bottom: -16,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: 'rgba(255,255,255,0.1)',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 16,
    paddingHorizontal: 16,
  },
  iconRing: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: 'rgba(255,255,255,0.28)',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 2,
    borderColor: 'rgba(255,255,255,0.45)',
  },
  copy: {
    flex: 1,
    marginLeft: 14,
    marginRight: 8,
  },
  eyebrow: {
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.xs,
    color: 'rgba(255,255,255,0.92)',
    textTransform: 'uppercase',
    letterSpacing: 0.6,
    marginBottom: 2,
  },
  title: {
    fontFamily: Fonts.title,
    fontSize: FontSize.lg,
    color: Colors.white,
    lineHeight: 24,
  },
  subtitle: {
    fontFamily: Fonts.body,
    fontSize: FontSize.xs,
    color: 'rgba(255,255,255,0.9)',
    marginTop: 2,
  },
  cta: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(0,0,0,0.15)',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
