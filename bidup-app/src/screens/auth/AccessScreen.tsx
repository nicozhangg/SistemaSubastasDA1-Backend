import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Colors, FontSize } from '../../constants';

export default function AccessScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>AccessScreen</Text>
      <Text style={styles.subtitle}>Iniciar sesión</Text>
      <Text style={styles.subtitle}>Registrarse</Text>
      <Text style={styles.subtitle}>Ingresar como invitado</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.background,
  },
  title: {
    fontSize: FontSize.xxl,
    fontWeight: '700',
    color: Colors.textPrimary,
    marginBottom: 24,
  },
  subtitle: {
    fontSize: FontSize.base,
    color: Colors.accent,
    marginVertical: 8,
  },
});
