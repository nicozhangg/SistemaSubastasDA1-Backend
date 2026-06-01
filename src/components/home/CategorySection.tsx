import React from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors, Fonts, FontSize } from '../../constants';
import { CatalogCategory } from '../../types/catalog';
import AuctionItemCard, { AUCTION_CARD_WIDTH } from './AuctionItemCard';

type Props = {
  category: CatalogCategory;
  showPrice: boolean;
  onLotPress?: (lotId: string) => void;
  onItemPress?: (itemId: string) => void;
};

export default function CategorySection({
  category,
  showPrice,
  onLotPress,
  onItemPress,
}: Props) {
  return (
    <View style={styles.section}>
      <Pressable
        style={({ pressed }) => [styles.lotButton, pressed && styles.lotButtonPressed]}
        onPress={() => onLotPress?.(category.id)}
        accessibilityRole="button"
        accessibilityLabel={`Ver ${category.name}`}
      >
        <Text style={styles.lotTitle}>{category.name}</Text>
        <Ionicons name="chevron-forward" size={16} color={Colors.accent} />
      </Pressable>
      <View style={styles.underline} />
      <FlatList
        data={category.items}
        keyExtractor={(item) => item.id}
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <AuctionItemCard
            item={item}
            showPrice={showPrice}
            onPress={() => onItemPress?.(item.id)}
          />
        )}
        getItemLayout={(_, index) => ({
          length: AUCTION_CARD_WIDTH + 12,
          offset: (AUCTION_CARD_WIDTH + 12) * index,
          index,
        })}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  section: {
    marginBottom: 20,
  },
  lotButton: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'center',
    gap: 6,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: Colors.white,
    borderWidth: 1.5,
    borderColor: Colors.accent,
    marginBottom: 6,
  },
  lotButtonPressed: {
    opacity: 0.85,
    backgroundColor: '#FFF8EE',
  },
  lotTitle: {
    fontFamily: Fonts.title,
    fontSize: FontSize.category,
    color: Colors.accent,
    textDecorationLine: 'underline',
  },
  underline: {
    alignSelf: 'center',
    width: 120,
    height: 3,
    backgroundColor: Colors.accent,
    borderRadius: 2,
    marginBottom: 14,
  },
  list: {
    paddingHorizontal: 4,
  },
});
