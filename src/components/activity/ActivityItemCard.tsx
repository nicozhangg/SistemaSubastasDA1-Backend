import React from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors, Fonts, FontSize } from '../../constants';

export type ActivityBadgeType =
  | 'winning'
  | 'losing'
  | 'won'
  | 'lost'
  | 'soon'
  | 'finished'
  | 'canceled'
  | 'pending'
  | 'approved_pending_lot'
  | 'published'
  | 'rejected';

interface Props {
  title: string;
  imageUrl: string;
  timeRemaining: string;
  primaryPrice: string;
  secondaryPrice?: string;
  badgeType: ActivityBadgeType;
  statusNote?: string;
  onPress?: () => void;
}

export default function ActivityItemCard({
  title,
  imageUrl,
  timeRemaining,
  primaryPrice,
  secondaryPrice,
  badgeType,
  statusNote,
  onPress,
}: Props) {
  const badgeConfig = getBadgeConfig(badgeType);

  return (
    <Pressable
      style={({ pressed }) => [styles.card, pressed && styles.pressed]}
      onPress={onPress}
    >
      <View style={styles.imageWrap}>
        {imageUrl ? (
          <Image source={{ uri: imageUrl }} style={styles.image} />
        ) : (
          <View style={[styles.image, styles.fallbackImage]}>
            <Ionicons name="hammer-outline" size={26} color={Colors.cardTime} />
          </View>
        )}
      </View>

      <View style={styles.details}>
        <Text style={styles.title} numberOfLines={2}>
          {title}
        </Text>
        <View style={styles.timeRow}>
          <Ionicons name="time-outline" size={12} color={Colors.cardTime} />
          <Text style={styles.timeText}>{timeRemaining}</Text>
        </View>
        <View style={styles.priceRow}>
          <View style={styles.priceTag}>
            <Text style={styles.primaryPrice}>{primaryPrice}</Text>
          </View>
          {secondaryPrice ? (
            <Text style={styles.secondaryPrice}>{secondaryPrice}</Text>
          ) : null}
        </View>
      </View>

      <View style={styles.badgeWrap}>
        <View style={[styles.badge, { backgroundColor: badgeConfig.bg }]}>
          {badgeConfig.icon ? (
            <Ionicons
              name={badgeConfig.icon as keyof typeof Ionicons.glyphMap}
              size={12}
              color={badgeConfig.color}
              style={styles.badgeIcon}
            />
          ) : null}
          <Text style={[styles.badgeText, { color: badgeConfig.color }]}>
            {badgeConfig.text}
          </Text>
        </View>
        {statusNote ? (
          <Text
            style={[
              styles.statusNote,
              badgeType === 'rejected' ? styles.statusNoteError : styles.statusNoteInfo,
            ]}
            numberOfLines={3}
          >
            {statusNote}
          </Text>
        ) : null}
      </View>
    </Pressable>
  );
}

function getBadgeConfig(type: ActivityBadgeType) {
  switch (type) {
    case 'winning':
      return {
        text: 'Estás Ganando!',
        bg: '#8CA73A',
        color: Colors.white,
        icon: 'checkmark-circle-outline' as const,
      };
    case 'losing':
      return {
        text: 'Estás Perdiendo!',
        bg: Colors.accent,
        color: Colors.white,
        icon: 'alert-circle-outline' as const,
      };
    case 'won':
      return {
        text: 'Ganaste!',
        bg: Colors.success,
        color: Colors.white,
        icon: 'trophy-outline' as const,
      };
    case 'lost':
      return {
        text: 'Perdiste',
        bg: Colors.error,
        color: Colors.white,
        icon: 'close-circle-outline' as const,
      };
    case 'soon':
      return {
        text: 'Falta Poco!',
        bg: Colors.accent,
        color: Colors.white,
        icon: 'hourglass-outline' as const,
      };
    case 'finished':
      return {
        text: 'Finalizada!',
        bg: Colors.success,
        color: Colors.white,
        icon: 'checkmark-circle-outline' as const,
      };
    case 'canceled':
      return {
        text: 'Cancelada',
        bg: Colors.error,
        color: Colors.white,
        icon: 'ban-outline' as const,
      };
    case 'pending':
      return {
        text: 'Pendiente',
        bg: Colors.warning,
        color: Colors.white,
        icon: 'time-outline' as const,
      };
    case 'approved_pending_lot':
      return {
        text: 'Aprobada',
        bg: '#5B8DEF',
        color: Colors.white,
        icon: 'checkmark-outline' as const,
      };
    case 'published':
      return {
        text: 'Publicado',
        bg: Colors.success,
        color: Colors.white,
        icon: 'checkmark-circle-outline' as const,
      };
    case 'rejected':
      return {
        text: 'Rechazado',
        bg: Colors.error,
        color: Colors.white,
        icon: 'close-circle-outline' as const,
      };
    default:
      return { text: '', bg: Colors.border, color: Colors.black, icon: undefined };
  }
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    backgroundColor: Colors.white,
    borderRadius: 12,
    marginHorizontal: 12,
    marginBottom: 12,
    padding: 12,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
  },
  pressed: {
    opacity: 0.94,
  },
  imageWrap: {
    width: 72,
    height: 72,
    borderRadius: 10,
    overflow: 'hidden',
    backgroundColor: Colors.surface,
  },
  image: {
    width: '100%',
    height: '100%',
  },
  fallbackImage: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  details: {
    flex: 1,
    paddingLeft: 12,
    paddingRight: 8,
    minHeight: 72,
    justifyContent: 'center',
  },
  title: {
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.sm,
    color: Colors.black,
    lineHeight: 17,
    marginBottom: 6,
  },
  timeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  timeText: {
    fontFamily: Fonts.body,
    fontSize: FontSize.xs,
    color: Colors.cardTime,
    marginLeft: 4,
  },
  priceRow: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: 8,
  },
  priceTag: {
    backgroundColor: Colors.cardPrice,
    borderRadius: 4,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  primaryPrice: {
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.sm,
    color: Colors.white,
  },
  secondaryPrice: {
    fontFamily: Fonts.body,
    fontSize: FontSize.xs,
    color: Colors.cardTime,
  },
  badgeWrap: {
    justifyContent: 'flex-start',
    maxWidth: 108,
  },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    paddingVertical: 5,
    borderRadius: 12,
  },
  badgeIcon: {
    marginRight: 3,
  },
  badgeText: {
    fontFamily: Fonts.bodyBold,
    fontSize: 9,
    flexShrink: 1,
  },
  statusNote: {
    fontFamily: Fonts.body,
    fontSize: 9,
    marginTop: 6,
    lineHeight: 13,
  },
  statusNoteError: {
    color: Colors.error,
  },
  statusNoteInfo: {
    color: Colors.textSecondary,
  },
});
