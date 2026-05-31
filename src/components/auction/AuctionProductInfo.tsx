import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Colors, Fonts, FontSize } from '../../constants';
import { getInitials } from '../../utils/format';

type Props = {
  title: string;
  sellerName: string;
  sellerAvatarColor: string;
  status: string;
  categories?: string[];
  lotNumber?: string;
  titleSize?: number;
};

export default function AuctionProductInfo({
  title,
  sellerName,
  sellerAvatarColor,
  status,
  categories,
  lotNumber = '#029',
  titleSize = FontSize.xxxl,
}: Props) {
  return (
    <>
      <Text style={[styles.title, { fontSize: titleSize }]}>{title}</Text>

      <View style={styles.sellerRow}>
        <View style={[styles.sellerAvatar, { backgroundColor: sellerAvatarColor }]}>
          <Text style={styles.sellerInitials}>{getInitials(sellerName)}</Text>
        </View>
        <Text style={styles.sellerName}>{sellerName}</Text>
      </View>

      <Text style={styles.metaLine}>
        <Text style={styles.metaBold}>Estado: </Text>
        {status}
      </Text>

      <View style={styles.tagsRow}>
        <Text style={styles.metaBold}>Lote: </Text>
        <View style={styles.tag}>
          <Text style={styles.tagText}>{lotNumber}</Text>
        </View>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  title: {
    fontFamily: Fonts.soraBold,
    color: Colors.white,
    marginBottom: 12,
  },
  sellerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
  },
  sellerAvatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 10,
  },
  sellerInitials: {
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.md, // Increased to md (14px)
    color: Colors.white,
  },
  sellerName: {
    fontFamily: Fonts.sora,
    fontSize: FontSize.base, // Increased to base (16px)
    color: Colors.white,
  },
  metaLine: {
    fontFamily: Fonts.sora,
    fontSize: FontSize.md, // Increased to md (14px)
    color: Colors.white,
    marginBottom: 10,
  },
  metaBold: {
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.md, // Increased to md (14px)
    color: Colors.white,
  },
  tagsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 4,
  },
  tag: {
    backgroundColor: 'rgba(255,255,255,0.18)',
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 5,
  },
  tagText: {
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.md, // Increased to md (14px) to match "Estado" and look highly readable
    color: Colors.white,
  },
});
