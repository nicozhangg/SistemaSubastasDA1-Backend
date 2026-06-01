import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useNavigation, useRoute } from '@react-navigation/native';
import type { RouteProp } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { PrimaryButton } from '../../components/auth';
import { ConsignmentScreenShell } from '../../components/consignment';
import ItemUploadedIllustration from '../../components/consignment/ItemUploadedIllustration';
import { Colors, Fonts, FontSize } from '../../constants';
import type { HomeStackParamList, MyAuctionsStackParamList } from '../../types';

type ItemUploadedRoute = RouteProp<
  HomeStackParamList | MyAuctionsStackParamList,
  'ItemUploaded'
>;
type Nav = StackNavigationProp<
  HomeStackParamList | MyAuctionsStackParamList,
  'ItemUploaded'
>;

export default function ItemUploadedScreen() {
  const navigation = useNavigation<Nav>();
  const route = useRoute<ItemUploadedRoute>();
  const returnTo = route.params?.returnTo ?? 'home';

  const goBack = () => {
    if (returnTo === 'myAuctions') {
      navigation.popToTop();
      return;
    }
    navigation.popToTop();
  };

  return (
    <ConsignmentScreenShell
      contentStyle={styles.content}
      footer={
        <PrimaryButton
          label={
            returnTo === 'myAuctions'
              ? 'Ver mis subastas'
              : 'Accedé a tu subasta'
          }
          onPress={goBack}
        />
      }
    >
      <View style={styles.card}>
        <Text style={styles.title}>Solicitud enviada</Text>

        <ItemUploadedIllustration />

        <Text style={styles.message}>
          Recibimos tu artículo. Un administrador debe revisarlo y confirmarlo
          antes de que quede publicado en el catálogo.
        </Text>
      </View>
    </ConsignmentScreenShell>
  );
}

const styles = StyleSheet.create({
  content: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingTop: 24,
  },
  card: {
    backgroundColor: Colors.white,
    borderRadius: 12,
    paddingHorizontal: 24,
    paddingVertical: 32,
    alignItems: 'center',
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
  },
  title: {
    fontFamily: Fonts.title,
    fontSize: FontSize.xxl,
    color: Colors.black,
    textAlign: 'center',
    marginBottom: 8,
  },
  message: {
    fontFamily: Fonts.body,
    fontSize: FontSize.base,
    color: Colors.cardTime,
    textAlign: 'center',
    lineHeight: 22,
    paddingHorizontal: 8,
  },
});
