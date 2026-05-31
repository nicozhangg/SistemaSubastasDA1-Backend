import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { PrimaryButton } from '../../components/auth';
import { ConsignmentScreenShell } from '../../components/consignment';
import ItemUploadedIllustration from '../../components/consignment/ItemUploadedIllustration';
import { Colors, Fonts, FontSize } from '../../constants';
import type { HomeStackParamList } from '../../types';

type Nav = StackNavigationProp<HomeStackParamList, 'ItemUploaded'>;

export default function ItemUploadedScreen() {
  const navigation = useNavigation<Nav>();

  const goHome = () => {
    navigation.popToTop();
  };

  return (
    <ConsignmentScreenShell
      contentStyle={styles.content}
      footer={
        <PrimaryButton label="Accedé a tu subasta" onPress={goHome} />
      }
    >
      <View style={styles.card}>
        <Text style={styles.title}>Artículo subido</Text>

        <ItemUploadedIllustration />

        <Text style={styles.message}>
          Añadimos tu artículo a nuestro catálogo y pronto será subastado.
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
