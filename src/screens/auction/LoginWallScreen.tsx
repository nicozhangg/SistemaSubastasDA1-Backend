import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { PrimaryButton } from '../../components/auth';
import ProfileHeaderBar from '../../components/profile/ProfileHeaderBar';
import { Colors, Fonts, FontSize } from '../../constants';
import { useAuthStore } from '../../stores';
import type { HomeStackParamList } from '../../types';

type Nav = StackNavigationProp<HomeStackParamList, 'LoginWall'>;

export default function LoginWallScreen() {
  const navigation = useNavigation<Nav>();
  const logout = useAuthStore((s) => s.logout);

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
      <View style={styles.header}>
        <ProfileHeaderBar title="Acceso requerido" onBack={() => navigation.goBack()} />
      </View>

      <View style={styles.content}>
        <Text style={styles.title}>Iniciá sesión para continuar</Text>
        <Text style={styles.body}>
          Para ver precios, explorar lotes y acceder a los productos necesitás
          una cuenta en BidUp.
        </Text>
        <PrimaryButton label="Iniciar sesión" onPress={logout} pill />
      </View>
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
  content: {
    flex: 1,
    paddingHorizontal: 24,
    justifyContent: 'center',
    paddingBottom: 48,
  },
  title: {
    fontFamily: Fonts.title,
    fontSize: FontSize.xxl,
    color: Colors.textPrimary,
    marginBottom: 12,
    textAlign: 'center',
  },
  body: {
    fontFamily: Fonts.body,
    fontSize: FontSize.base,
    color: Colors.textSecondary,
    lineHeight: 22,
    textAlign: 'center',
    marginBottom: 28,
  },
});
