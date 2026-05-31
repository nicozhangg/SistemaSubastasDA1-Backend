import React, { useMemo } from 'react';
import {
  Image,
  Pressable,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors, Fonts, FontSize, Layout } from '../../constants';

export const MIN_CONSIGNMENT_PHOTOS = 6;
const SLOT_COUNT = 6;
const COLS = 3;
const SLOT_GAP = 10;
/** Scroll padding (12×2) + photos card padding (16×2) */
const GRID_HORIZONTAL_INSET = 56;

type Props = {
  photos: (string | null)[];
  onChange: (photos: (string | null)[]) => void;
};

const DEMO_PHOTO =
  'https://images.unsplash.com/photo-1614728263952-84ea256f9679?w=200&q=80';

export default function PhotoUploadGrid({ photos, onChange }: Props) {
  const { width: windowWidth } = useWindowDimensions();
  const slotSize = useMemo(() => {
    const available = windowWidth - GRID_HORIZONTAL_INSET - SLOT_GAP * (COLS - 1);
    return Math.floor(available / COLS);
  }, [windowWidth]);

  const filledCount = photos.filter(Boolean).length;

  const addNextPhoto = () => {
    const index = photos.findIndex((p) => !p);
    if (index === -1) return;
    const next = [...photos];
    next[index] = DEMO_PHOTO;
    onChange(next);
  };

  const removePhoto = (index: number) => {
    const next = [...photos];
    next[index] = null;
    onChange(next);
  };

  return (
    <View style={styles.wrap}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>Adjunta fotos del artículo</Text>
        <Pressable onPress={addNextPhoto} hitSlop={8} style={styles.uploadBtn}>
          <Ionicons name="cloud-upload-outline" size={22} color={Colors.accent} />
        </Pressable>
      </View>

      <Text style={styles.hint}>
        Debés adjuntar como mínimo {MIN_CONSIGNMENT_PHOTOS} (seis) fotos.
        {filledCount > 0 ? ` · ${filledCount}/${SLOT_COUNT}` : ''}
      </Text>

      <View style={[styles.grid, { gap: SLOT_GAP }]}>
        {photos.map((uri, index) => (
          <Pressable
            key={`photo-slot-${index}`}
            style={({ pressed }) => [
              styles.slot,
              { width: slotSize, height: slotSize },
              pressed && styles.slotPressed,
            ]}
            onPress={() => (uri ? removePhoto(index) : addNextPhoto())}
          >
            {uri ? (
              <View style={styles.slotFill}>
                <Image
                  source={{ uri }}
                  style={styles.image}
                  resizeMode="cover"
                />
                <View style={styles.removeBadge}>
                  <Ionicons name="close" size={14} color={Colors.white} />
                </View>
              </View>
            ) : (
              <Ionicons name="image-outline" size={22} color={Colors.cardTime} />
            )}
          </Pressable>
        ))}
      </View>
    </View>
  );
}

export function createEmptyPhotoSlots(): (string | null)[] {
  return Array.from({ length: SLOT_COUNT }, () => null);
}

const styles = StyleSheet.create({
  wrap: {
    marginTop: 4,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 6,
  },
  headerTitle: {
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.md,
    color: Colors.black,
    flex: 1,
  },
  uploadBtn: {
    padding: 4,
  },
  hint: {
    fontFamily: Fonts.body,
    fontSize: FontSize.xs,
    color: Colors.cardTime,
    marginBottom: 12,
    lineHeight: 16,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  slot: {
    borderRadius: 10,
    backgroundColor: Layout.inputBackground,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  slotPressed: {
    opacity: 0.9,
  },
  slotFill: {
    ...StyleSheet.absoluteFillObject,
  },
  image: {
    width: '100%',
    height: '100%',
  },
  removeBadge: {
    position: 'absolute',
    top: 6,
    right: 6,
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: 'rgba(0,0,0,0.55)',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
