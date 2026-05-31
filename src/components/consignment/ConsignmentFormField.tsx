import React from 'react';
import { StyleSheet, TextInput, TextInputProps } from 'react-native';
import { Colors, Fonts, FontSize, Layout } from '../../constants';

type Props = TextInputProps;

export default function ConsignmentFormField({
  placeholderTextColor = Colors.searchPlaceholder,
  style,
  ...props
}: Props) {
  return (
    <TextInput
      style={[styles.input, style]}
      placeholderTextColor={placeholderTextColor}
      {...props}
    />
  );
}

const styles = StyleSheet.create({
  input: {
    minHeight: Layout.inputMinHeight,
    borderRadius: Layout.inputBorderRadius,
    backgroundColor: Layout.inputBackground,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontFamily: Fonts.input,
    fontSize: FontSize.base,
    color: Colors.black,
    marginBottom: 12,
  },
});
