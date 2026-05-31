import React from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { CompositeNavigationProp } from '@react-navigation/native';
import type { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import type { StackNavigationProp } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { CategorySection, HomeHeader } from '../../components/home';
import { Colors, Fonts, FontSize, Layout } from '../../constants';
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

  const openAuction = (itemId: string) => {
    navigation.getParent()?.getParent()?.navigate('AuctionDetail', {
      auctionId: itemId,
    });
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <HomeHeader
        isLoggedIn={isAuthenticated}
        onIngresar={logout}
        onChatPress={() => navigation.navigate('ChatList')}
      />

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
            onItemPress={openAuction}
          />
        ))}

        <Pressable
          style={({ pressed }) => [
            styles.consignBtn,
            pressed && styles.consignPressed,
          ]}
          onPress={() => navigation.navigate('UploadItem')}
        >
          <Text style={styles.consignText}>Subastá tu artículo</Text>
        </Pressable>
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
    paddingBottom: 16,
  },
  consignBtn: {
    height: Layout.buttonHeight,
    borderRadius: Layout.buttonBorderRadius,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
    marginBottom: 8,
    marginHorizontal: 4,
  },
  consignPressed: {
    opacity: 0.9,
  },
  consignText: {
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.base,
    color: Colors.white,
  },
});
