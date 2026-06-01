import React, { useMemo, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { StackNavigationProp } from '@react-navigation/stack';
import {
  AuthLink,
  AuthScreen,
  AuthTitle,
  BidUpTextField,
  BrandMark,
  PrimaryButton,
} from '../../components/auth';
import { Colors, Fonts, FontSize } from '../../constants';
import { useAuthStore } from '../../stores';
import type { AuthStackParamList } from '../../types';

type Nav = StackNavigationProp<AuthStackParamList, 'Login'>;

export default function LoginScreen() {
  const navigation = useNavigation<Nav>();
  const login = useAuthStore((s) => s.login);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [touched, setTouched] = useState({
    username: false,
    password: false,
  });
  const [submitAttempted, setSubmitAttempted] = useState(false);

  const usernameValid = username.trim().length > 0;
  const passwordValid = password.trim().length > 0;
  const isFormValid = useMemo(
    () => usernameValid && passwordValid,
    [usernameValid, passwordValid]
  );

  const handleContinue = () => {
    setSubmitAttempted(true);
    setTouched({ username: true, password: true });

    if (!isFormValid) {
      return;
    }

    login(
      {
        id: '1',
        email: username.trim(),
        firstName: 'Usuario',
        lastName: 'Demo',
        dni: '',
        status: 'approved',
      },
      'demo-token'
    );
  };

  return (
    <AuthScreen
      footer={
        <View style={styles.footer}>
          <Text style={styles.footerText}>¿No tenés una cuenta? </Text>
          <AuthLink onPress={() => navigation.navigate('RegisterStep1')}>
            Registrate
          </AuthLink>
        </View>
      }
    >
      <BrandMark />
      <AuthTitle>Iniciar sesión</AuthTitle>

      <BidUpTextField
        placeholder="Usuario"
        value={username}
        onChangeText={setUsername}
        autoCapitalize="none"
        onBlur={() => setTouched((state) => ({ ...state, username: true }))}
      />
      {(submitAttempted || touched.username) && !usernameValid ? (
        <Text style={styles.errorText}>El usuario es obligatorio.</Text>
      ) : null}
      <BidUpTextField
        placeholder="Contraseña"
        value={password}
        onChangeText={setPassword}
        secureTextEntry
        onBlur={() => setTouched((state) => ({ ...state, password: true }))}
      />
      {(submitAttempted || touched.password) && !passwordValid ? (
        <Text style={styles.errorText}>La contraseña es obligatoria.</Text>
      ) : null}

      <View style={styles.recoverRow}>
        <Text style={styles.recoverPrefix}>¿Olvidaste tu contraseña? </Text>
        <AuthLink onPress={() => navigation.navigate('ForgotPassword')}>
          Recuperar
        </AuthLink>
      </View>

      <View style={styles.spacer} />
      <PrimaryButton
        label="Continuar"
        onPress={handleContinue}
        disabled={!isFormValid}
      />
    </AuthScreen>
  );
}

const styles = StyleSheet.create({
  recoverRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    marginTop: 4,
    marginBottom: 8,
  },
  recoverPrefix: {
    fontFamily: Fonts.body,
    fontSize: FontSize.sm,
    color: Colors.black,
  },
  spacer: {
    flex: 1,
    minHeight: 24,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  footerText: {
    fontFamily: Fonts.body,
    fontSize: FontSize.sm,
    color: Colors.black,
  },
  errorText: {
    fontFamily: Fonts.body,
    fontSize: FontSize.xs,
    color: '#FF3B30',
    marginTop: -6,
    marginBottom: 8,
    lineHeight: 16,
  },
});
