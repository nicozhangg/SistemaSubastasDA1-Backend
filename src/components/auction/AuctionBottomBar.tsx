import React from 'react';
import {
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors, Fonts, FontSize, Layout } from '../../constants';
import { formatCurrency } from '../../utils/format';

type Props = {
  quickBidAmounts: number[];
  selectedAmount: number;
  customBidMode: boolean;
  customBidValue: string;
  currency: string;
  minBidAmount: number;
  isCustomBidValid: boolean;
  onSelectQuickBid: (amount: number) => void;
  onToggleCustomBid: () => void;
  onCustomBidChange: (value: string) => void;
  onConfirmCustomBid: () => void;
  onCancelCustomBid: () => void;
  onPlaceBid: () => void;
};

export default function AuctionBottomBar({
  quickBidAmounts,
  selectedAmount,
  customBidMode,
  customBidValue,
  currency,
  minBidAmount,
  isCustomBidValid,
  onSelectQuickBid,
  onToggleCustomBid,
  onCustomBidChange,
  onConfirmCustomBid,
  onCancelCustomBid,
  onPlaceBid,
}: Props) {
  if (customBidMode) {
    return (
      <View style={styles.wrap}>
        <Text style={styles.customHint}>
          Mínimo: {formatCurrency(minBidAmount, currency, true)} — ingresá un monto mayor
        </Text>
        <View style={styles.customRow}>
          <TextInput
            style={[
              styles.customInput,
              !isCustomBidValid && customBidValue.length > 0 && styles.customInputInvalid,
            ]}
            placeholder={String(minBidAmount + 1)}
            placeholderTextColor={Colors.searchPlaceholder}
            value={customBidValue}
            onChangeText={onCustomBidChange}
            keyboardType="numeric"
            autoFocus
          />
          <Pressable
            style={[
              styles.iconAction,
              styles.confirmAction,
              !isCustomBidValid && styles.iconActionDisabled,
            ]}
            onPress={onConfirmCustomBid}
            disabled={!isCustomBidValid}
          >
            <Ionicons name="checkmark" size={22} color={Colors.white} />
          </Pressable>
          <Pressable
            style={[styles.iconAction, styles.cancelAction]}
            onPress={onCancelCustomBid}
          >
            <Ionicons name="close" size={22} color={Colors.black} />
          </Pressable>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.wrap}>
      <View style={styles.bidRow}>
        {quickBidAmounts.map((amount) => {
          const active = amount === selectedAmount;
          return (
            <Pressable
              key={amount}
              style={[styles.quickBtn, active && styles.quickBtnActive]}
              onPress={() => onSelectQuickBid(amount)}
            >
              <Text style={[styles.quickText, active && styles.quickTextActive]}>
                {formatCurrency(amount, currency, true)}
              </Text>
            </Pressable>
          );
        })}
        <Pressable style={styles.customBidBtn} onPress={onToggleCustomBid}>
          <Text style={styles.customBidText}>Puja{'\n'}personalizada</Text>
        </Pressable>
      </View>

      <Pressable
        style={({ pressed }) => [styles.placeBidBtn, pressed && styles.pressed]}
        onPress={onPlaceBid}
      >
        <Text style={styles.placeBidText}>
          Pujar por {formatCurrency(selectedAmount, currency, true)}
        </Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    backgroundColor: Colors.white,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 16,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: Colors.border,
  },
  bidRow: {
    flexDirection: 'row',
    alignItems: 'stretch',
    marginBottom: 10,
  },
  quickBtn: {
    flex: 1,
    marginRight: 4,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: Colors.border,
    backgroundColor: Colors.white,
    alignItems: 'center',
    justifyContent: 'center',
  },
  quickBtnActive: {
    backgroundColor: Colors.auctionViolet,
    borderColor: Colors.auctionViolet,
  },
  quickText: {
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.sm,
    color: Colors.black,
  },
  quickTextActive: {
    color: Colors.white,
  },
  customBidBtn: {
    width: 88,
    marginLeft: 4,
    borderRadius: 8,
    backgroundColor: Colors.auctionViolet,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 4,
  },
  customBidText: {
    fontFamily: Fonts.soraBold,
    fontSize: 10,
    color: Colors.white,
    textAlign: 'center',
    lineHeight: 13,
  },
  placeBidBtn: {
    height: Layout.buttonHeight,
    borderRadius: Layout.buttonBorderRadius,
    backgroundColor: Colors.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeBidText: {
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.base,
    color: Colors.white,
  },
  customRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  customHint: {
    fontFamily: Fonts.sora,
    fontSize: FontSize.xs,
    color: Colors.textSecondary,
    marginBottom: 8,
  },
  customInput: {
    flex: 1,
    height: 44,
    borderRadius: 8,
    backgroundColor: Colors.surface,
    paddingHorizontal: 14,
    fontFamily: Fonts.sora,
    fontSize: FontSize.base,
    color: Colors.black,
    marginRight: 8,
  },
  customInputInvalid: {
    borderWidth: 1,
    borderColor: Colors.error,
  },
  iconAction: {
    width: 44,
    height: 44,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    marginLeft: 4,
  },
  confirmAction: {
    backgroundColor: Colors.accent,
  },
  iconActionDisabled: {
    opacity: 0.45,
  },
  cancelAction: {
    backgroundColor: Colors.white,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  pressed: {
    opacity: 0.92,
  },
});
