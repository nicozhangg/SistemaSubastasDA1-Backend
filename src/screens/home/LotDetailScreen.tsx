import React, { useEffect } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { RouteProp } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AuctionItemCard } from '../../components/home';
import ProfileHeaderBar from '../../components/profile/ProfileHeaderBar';
import { Colors, Fonts, FontSize } from '../../constants';
import { getLotById } from '../../data/mockHomeCatalog';
import { useAuthStore } from '../../stores';
import type { HomeStackParamList, RootStackParamList } from '../../types';

type Route = RouteProp<HomeStackParamList, 'LotDetail'>;
type Nav = StackNavigationProp<HomeStackParamList, 'LotDetail'>;

export default function LotDetailScreen() {
  const navigation = useNavigation<Nav>();
  const route = useRoute<Route>();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const lot = getLotById(route.params.lotId);

  useEffect(() => {
    if (!isAuthenticated) {
      navigation.replace('LoginWall');
    }
  }, [isAuthenticated, navigation]);

  if (!lot) {
    return (
      <SafeAreaView style={styles.safe} edges={['top']}>
        <ProfileHeaderBar title="Lote" onBack={() => navigation.goBack()} />
        <View style={styles.center}>
          <Text style={styles.errorText}>No encontramos este lote.</Text>
        </View>
      </SafeAreaView>
    );
  }

  const openProduct = (itemId: string) => {
    navigation
      .getParent()
      ?.getParent()
      ?.navigate('AuctionDetail' as keyof RootStackParamList, {
        auctionId: itemId,
      });
  };

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={styles.header}>
        <ProfileHeaderBar title={lot.name} onBack={() => navigation.goBack()} />
      </View>

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        <Text style={styles.sectionLabel}>Descripción del lote</Text>
        <Text style={styles.description}>{lot.description}</Text>

        <Text style={styles.sectionLabel}>
          Productos ({lot.items.length})
        </Text>

        {lot.items.map((item) => (
          <AuctionItemCard
            key={item.id}
            item={item}
            showPrice
            onPress={() => openProduct(item.id)}
            style={styles.productCard}
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
  header: {
    paddingHorizontal: 16,
    paddingTop: 4,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingBottom: 24,
  },
  sectionLabel: {
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.base,
    color: Colors.textPrimary,
    marginBottom: 8,
    marginTop: 4,
  },
  description: {
    fontFamily: Fonts.body,
    fontSize: FontSize.md,
    color: Colors.textSecondary,
    lineHeight: 22,
    marginBottom: 20,
  },
  productCard: {
    width: '100%',
    marginRight: 0,
    marginBottom: 14,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  errorText: {
    fontFamily: Fonts.body,
    fontSize: FontSize.base,
    color: Colors.textSecondary,
    textAlign: 'center',
  },
});
