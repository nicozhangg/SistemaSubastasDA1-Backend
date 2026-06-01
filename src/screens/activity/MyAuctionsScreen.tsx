import React, { useMemo, useState } from 'react';
import { FlatList, StyleSheet, Text, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';

import { Colors, Fonts, FontSize } from '../../constants';
import { useAuthStore, useMyAuctionsStore } from '../../stores';
import HomeHeader from '../../components/home/HomeHeader';
import ConsignPromoBanner from '../../components/home/ConsignPromoBanner';
import {
  ActivityItemCard,
  ActivityBadgeType,
  ActivitySectionHeader,
  ActivityToolbar,
  DropdownFilter,
  DropdownOption,
} from '../../components/activity';
import { MOCK_AUCTIONS, MockAuctionItem } from '../../data/mockActivity';
import type { MyAuctionsStackParamList } from '../../types';

const FILTER_OPTIONS: DropdownOption[] = [
  { value: 'all', label: 'Todas mis Subastas' },
  { value: 'pending', label: 'Pendientes' },
  { value: 'approved_pending_lot', label: 'Sin lote' },
  { value: 'published', label: 'Publicadas' },
  { value: 'rejected', label: 'Rechazadas' },
  { value: 'finished', label: 'Finalizadas' },
];

type Nav = StackNavigationProp<MyAuctionsStackParamList, 'MyAuctionsMain'>;

function getModerationBadge(
  moderationStatus: MockAuctionItem['moderationStatus']
): ActivityBadgeType {
  if (moderationStatus === 'pending') return 'pending';
  if (moderationStatus === 'approved_pending_lot') return 'approved_pending_lot';
  if (moderationStatus === 'published') return 'published';
  if (moderationStatus === 'rejected') return 'rejected';
  return 'pending';
}

export default function MyAuctionsScreen() {
  const navigation = useNavigation<Nav>();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const logout = useAuthStore((s) => s.logout);
  const userSubmissions = useMyAuctionsStore((s) => s.submissions);

  const [filter, setFilter] = useState('all');

  const allAuctions = useMemo(
    () => [...userSubmissions, ...MOCK_AUCTIONS],
    [userSubmissions]
  );

  const filteredAuctions = allAuctions.filter((auction) => {
    if (filter === 'all') return true;
    if (filter === 'pending') return auction.moderationStatus === 'pending';
    if (filter === 'approved_pending_lot') {
      return auction.moderationStatus === 'approved_pending_lot';
    }
    if (filter === 'published') return auction.moderationStatus === 'published';
    if (filter === 'rejected') return auction.moderationStatus === 'rejected';
    if (filter === 'finished') return auction.status === 'finished';
    return true;
  });

  const goHome = () => {
    navigation.getParent()?.navigate('Home');
  };

  const handleChatPress = () => {
    navigation.getParent()?.navigate('Home', { screen: 'ChatList' });
  };

  const handleCreateAuction = () => {
    navigation.navigate('UploadItem', { returnTo: 'myAuctions' });
  };

  const handleItemPress = (auction: MockAuctionItem) => {
    if (auction.moderationStatus !== 'published') {
      return;
    }
    navigation.getParent()?.getParent()?.navigate('AuctionDetail', {
      auctionId: auction.id,
    });
  };

  const renderAuctionItem = ({ item }: { item: MockAuctionItem }) => (
    <ActivityItemCard
      title={item.title}
      imageUrl={item.imageUrl}
      timeRemaining={item.timeRemaining}
      primaryPrice={item.currentPrice}
      secondaryPrice={
        item.moderationStatus === 'published' ? 'Puja máxima' : undefined
      }
      badgeType={getModerationBadge(item.moderationStatus)}
      statusNote={
        item.moderationStatus === 'rejected' && item.rejectionReason
          ? `Motivo: ${item.rejectionReason}`
          : item.moderationStatus === 'approved_pending_lot'
            ? 'Pendiente de subir a un lote'
            : undefined
      }
      onPress={() => handleItemPress(item)}
    />
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <HomeHeader
        isLoggedIn={isAuthenticated}
        onIngresar={logout}
        onChatPress={handleChatPress}
      />

      <ConsignPromoBanner onPress={handleCreateAuction} />

      <ActivitySectionHeader title="Mis subastas" />

      <ActivityToolbar onBack={goHome}>
        <DropdownFilter
          options={FILTER_OPTIONS}
          selectedValue={filter}
          onValueChange={setFilter}
        />
      </ActivityToolbar>

      <FlatList
        data={filteredAuctions}
        keyExtractor={(item) => item.id}
        renderItem={renderAuctionItem}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          <View style={styles.emptyCard}>
            <Ionicons name="albums-outline" size={40} color={Colors.cardTime} />
            <Text style={styles.emptyText}>No tenés subastas en esta categoría.</Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: Colors.homeBackground,
  },
  listContent: {
    paddingTop: 4,
    paddingBottom: 24,
  },
  emptyCard: {
    marginHorizontal: 12,
    marginTop: 8,
    paddingVertical: 48,
    paddingHorizontal: 24,
    backgroundColor: Colors.white,
    borderRadius: 12,
    alignItems: 'center',
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 6,
    elevation: 2,
  },
  emptyText: {
    fontFamily: Fonts.body,
    fontSize: FontSize.md,
    color: Colors.cardTime,
    marginTop: 12,
    textAlign: 'center',
  },
});
