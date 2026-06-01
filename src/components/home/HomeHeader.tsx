import React, { useState } from 'react';
import {
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import BrandMark from '../auth/BrandMark';
import { Colors, Fonts, FontSize } from '../../constants';

type Props = {
  isLoggedIn: boolean;
  onIngresar?: () => void;
  onChatPress?: () => void;
  searchValue?: string;
  onSearchChange?: (value: string) => void;
};

export default function HomeHeader({
  isLoggedIn,
  onIngresar,
  onChatPress,
  searchValue = '',
  onSearchChange = () => {},
}: Props) {
  const [isFocused, setIsFocused] = useState(false);

  return (
    <View style={styles.wrap}>
      <View style={styles.topRow}>
        <BrandMark size={40} style={styles.logo} />

        <View style={styles.searchWrap}>
          <TextInput
            style={styles.search}
            placeholder={isFocused ? '' : 'Encontrá los mejores artículos'}
            placeholderTextColor={Colors.searchPlaceholder}
            value={searchValue}
            onChangeText={onSearchChange}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            autoCapitalize="none"
            autoCorrect={false}
          />
          <Ionicons
            name="search"
            size={18}
            color={Colors.searchPlaceholder}
            style={styles.searchIcon}
          />
        </View>

        {isLoggedIn ? (
          <View style={styles.actions}>
            <Pressable
              style={styles.iconBtn}
              onPress={onChatPress}
              hitSlop={8}
            >
              <Ionicons name="chatbubble-outline" size={20} color={Colors.black} />
            </Pressable>
          </View>
        ) : (
          <Pressable style={styles.ingresarBtn} onPress={onIngresar}>
            <Text style={styles.ingresarText}>Ingresar</Text>
          </Pressable>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 12,
    backgroundColor: Colors.homeBackground,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  logo: {
    marginRight: 10,
  },
  searchWrap: {
    flex: 1,
    position: 'relative',
    justifyContent: 'center',
  },
  search: {
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.white,
    paddingHorizontal: 16,
    paddingRight: 40,
    fontFamily: Fonts.body,
    fontSize: FontSize.sm,
    color: Colors.black,
  },
  searchIcon: {
    position: 'absolute',
    right: 14,
  },
  ingresarBtn: {
    width: 77,
    height: 29,
    borderRadius: 6,
    backgroundColor: Colors.loginButton,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 10,
  },
  ingresarText: {
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.sm,
    color: Colors.white,
  },
  actions: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 8,
  },
  iconBtn: {
    padding: 6,
    marginLeft: 2,
  },
});
