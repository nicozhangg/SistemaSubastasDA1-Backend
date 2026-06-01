import React, { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { CompositeNavigationProp } from '@react-navigation/native';
import type { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import type { StackNavigationProp } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  CategorySection,
  ConsignPromoBanner,
  HomeHeader,
} from '../../components/home';
import { Colors } from '../../constants';
import { MOCK_HOME_CATEGORIES } from '../../data/mockHomeCatalog';
import { useAuthStore } from '../../stores';
import type {
  HomeStackParamList,
  MainTabParamList,
  RootStackParamList,
} from '../../types';

type HomeNav = CompositeNavigationProp<
  StackNavigationProp<HomeStackParamList, 'HomeMain'>,
  CompositeNavigationProp<
    BottomTabNavigationProp<MainTabParamList>,
    StackNavigationProp<RootStackParamList>
  >
>;

export default function HomeScreen() {
  const navigation = useNavigation<HomeNav>();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const logout = useAuthStore((s) => s.logout);
  const [searchQuery, setSearchQuery] = useState('');

  const showPrice = isAuthenticated;

  const requireAuth = (action: () => void) => {
    if (isAuthenticated) {
      action();
      return;
    }
    navigation.navigate('LoginWall');
  };

  const openLot = (lotId: string) => {
    requireAuth(() => navigation.navigate('LotDetail', { lotId }));
  };

  const filteredCategories = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();

    if (!normalizedQuery) {
      return MOCK_HOME_CATEGORIES;
    }

    return MOCK_HOME_CATEGORIES
      .map((category) => ({
        ...category,
        items: category.items.filter((item) => {
          const searchText = [category.name, item.title, item.price, item.timeRemaining]
            .join(' ')
            .toLowerCase();

          return searchText.includes(normalizedQuery);
        }),
      }))
      .filter((category) => category.items.length > 0);
  }, [searchQuery]);

  const openAuction = (itemId: string) => {
    requireAuth(() => {
      navigation.getParent()?.getParent()?.navigate('AuctionDetail', {
        auctionId: itemId,
      });
    });
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <HomeHeader
        isLoggedIn={isAuthenticated}
        onIngresar={logout}
        onChatPress={() => navigation.navigate('ChatList')}
        searchValue={searchQuery}
        onSearchChange={setSearchQuery}
      />

      <ConsignPromoBanner onPress={() => navigation.navigate('UploadItem', { returnTo: 'home' })} />

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {filteredCategories.length > 0 ? (
          filteredCategories.map((category) => (
            <CategorySection
              key={category.id}
              category={category}
              showPrice={showPrice}
              onLotPress={openLot}
              onItemPress={openAuction}
            />
          ))
        ) : (
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No encontramos resultados</Text>
            <Text style={styles.emptyText}>
              Probá con otro nombre de artículo o categoría.
            </Text>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: Colors.homeBackground,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 12,
    paddingTop: 10,
    paddingBottom: 16,
  },
  emptyState: {
    backgroundColor: '#fff',
    borderRadius: 12,
    paddingHorizontal: 20,
    paddingVertical: 28,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 12,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.black,
    marginBottom: 6,
    textAlign: 'center',
  },
  emptyText: {
    fontSize: 14,
    color: Colors.cardTime,
    textAlign: 'center',
    lineHeight: 20,
  },
});
