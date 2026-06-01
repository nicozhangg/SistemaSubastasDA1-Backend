import React, { useState } from 'react';
import {
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors, Fonts, FontSize, Layout } from '../../constants';

export type SelectOption = { value: string; label: string };

type Props = {
  placeholder: string;
  options: SelectOption[];
  value: string | null;
  onValueChange: (value: string) => void;
};

export default function FormSelect({
  placeholder,
  options,
  value,
  onValueChange,
}: Props) {
  const [open, setOpen] = useState(false);
  const selected = options.find((o) => o.value === value);

  return (
    <View style={styles.wrap}>
      <Pressable
        style={({ pressed }) => [styles.field, pressed && styles.pressed]}
        onPress={() => setOpen(true)}
      >
        <Text
          style={[styles.text, !selected && styles.placeholder]}
          numberOfLines={1}
        >
          {selected ? selected.label : placeholder}
        </Text>
        <Ionicons name="chevron-down" size={18} color={Colors.searchPlaceholder} />
      </Pressable>

      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable style={styles.overlay} onPress={() => setOpen(false)}>
          <Pressable style={styles.menu} onPress={() => {}}>
            {options.map((option, index) => {
              const isSelected = option.value === value;
              return (
                <View key={option.value}>
                  <Pressable
                    style={({ pressed }) => [styles.item, pressed && styles.itemPressed]}
                    onPress={() => {
                      onValueChange(option.value);
                      setOpen(false);
                    }}
                  >
                    <View style={styles.check}>
                      {isSelected ? (
                        <Ionicons name="checkmark" size={16} color={Colors.accent} />
                      ) : null}
                    </View>
                    <Text style={[styles.itemText, isSelected && styles.itemTextActive]}>
                      {option.label}
                    </Text>
                  </Pressable>
                  {index < options.length - 1 ? <View style={styles.separator} /> : null}
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
  wrap: {
    marginBottom: 12,
  },
  field: {
    minHeight: Layout.inputMinHeight,
    borderRadius: Layout.inputBorderRadius,
    backgroundColor: Layout.inputBackground,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  pressed: {
    opacity: 0.92,
  },
  text: {
    flex: 1,
    fontFamily: Fonts.input,
    fontSize: FontSize.base,
    color: Colors.black,
    marginRight: 8,
  },
  placeholder: {
    fontFamily: Fonts.body,
    color: Colors.searchPlaceholder,
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(27, 32, 69, 0.35)',
    justifyContent: 'center',
    paddingHorizontal: 24,
  },
  menu: {
    backgroundColor: Colors.white,
    borderRadius: 16,
    paddingVertical: 8,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.12,
    shadowRadius: 12,
    elevation: 5,
  },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 14,
    paddingHorizontal: 16,
  },
  itemPressed: {
    backgroundColor: Colors.surface,
  },
  check: {
    width: 24,
  },
  itemText: {
    fontFamily: Fonts.body,
    fontSize: FontSize.md,
    color: Colors.textPrimary,
  },
  itemTextActive: {
    fontFamily: Fonts.bodyBold,
  },
  separator: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: Colors.border,
    marginHorizontal: 16,
  },
});
