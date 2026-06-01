import React, { useState } from 'react';
import {
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors, Fonts, FontSize } from '../../constants';

export interface DropdownOption {
  value: string;
  label: string;
}

interface Props {
  options: DropdownOption[];
  selectedValue: string;
  onValueChange: (value: string) => void;
}

export default function DropdownFilter({
  options,
  selectedValue,
  onValueChange,
}: Props) {
  const [isOpen, setIsOpen] = useState(false);

  const selectedOption =
    options.find((opt) => opt.value === selectedValue) || options[0];

  const handleSelect = (val: string) => {
    onValueChange(val);
    setIsOpen(false);
  };

  return (
    <View>
      <Pressable
        style={({ pressed }) => [styles.button, pressed && styles.buttonPressed]}
        onPress={() => setIsOpen(true)}
      >
        <Text style={styles.buttonText} numberOfLines={1}>
          {selectedOption.label}
        </Text>
        <Ionicons name="chevron-down" size={16} color={Colors.searchPlaceholder} />
      </Pressable>

      <Modal
        visible={isOpen}
        transparent
        animationType="fade"
        onRequestClose={() => setIsOpen(false)}
      >
        <Pressable style={styles.overlay} onPress={() => setIsOpen(false)}>
          <Pressable style={styles.menuContainer} onPress={() => {}}>
            {options.map((option, index) => {
              const isSelected = option.value === selectedValue;
              return (
                <View key={option.value}>
                  <Pressable
                    style={({ pressed }) => [
                      styles.menuItem,
                      pressed && styles.menuItemPressed,
                    ]}
                    onPress={() => handleSelect(option.value)}
                  >
                    <View style={styles.checkIconWrap}>
                      {isSelected ? (
                        <Ionicons
                          name="checkmark"
                          size={16}
                          color={Colors.accent}
                        />
                      ) : null}
                    </View>
                    <Text
                      style={[
                        styles.menuItemText,
                        isSelected && styles.menuItemTextActive,
                      ]}
                    >
                      {option.label}
                    </Text>
                  </Pressable>
                  {index < options.length - 1 ? (
                    <View style={styles.separator} />
                  ) : null}
                </View>
              );
            })}
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: Colors.white,
    borderRadius: 20,
    paddingHorizontal: 16,
    height: 40,
    minWidth: 168,
    maxWidth: '100%',
    gap: 8,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 4,
    elevation: 2,
  },
  buttonPressed: {
    opacity: 0.9,
  },
  buttonText: {
    flex: 1,
    fontFamily: Fonts.bodyBold,
    fontSize: FontSize.sm,
    color: Colors.black,
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(27, 32, 69, 0.35)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  menuContainer: {
    width: '100%',
    maxWidth: 320,
    backgroundColor: Colors.white,
    borderRadius: 16,
    paddingVertical: 8,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 5,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
  },
  menuItemPressed: {
    backgroundColor: Colors.surface,
  },
  checkIconWrap: {
    width: 24,
    alignItems: 'flex-start',
  },
  menuItemText: {
    fontFamily: Fonts.body,
    fontSize: FontSize.md,
    color: Colors.textPrimary,
  },
  menuItemTextActive: {
    fontFamily: Fonts.bodyBold,
    color: Colors.black,
  },
  separator: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: Colors.border,
    marginHorizontal: 16,
  },
});
