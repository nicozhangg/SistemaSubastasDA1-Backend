import React from 'react';
import { ScrollView, StyleSheet } from 'react-native';
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
      />

      <ConsignPromoBanner onPress={() => navigation.navigate('UploadItem', { returnTo: 'home' })} />

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {MOCK_HOME_CATEGORIES.map((category) => (
          <CategorySection
            key={category.id}
            category={category}
            showPrice={showPrice}
            onLotPress={openLot}
            onItemPress={openAuction}
          />
        ))}
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
});
