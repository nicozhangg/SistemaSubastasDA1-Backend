import React, { useMemo, useRef, useState } from 'react';
import {
  Animated,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import {
  AuctionBottomBar,
  AuctionImageHeader,
  AuctionProductInfo,
  AuctionStatsCards,
  BidHistoryRow,
  ConfirmBidModal,
} from '../../components/auction';
import { Colors, Fonts, FontSize } from '../../constants';
import { MOCK_AUCTION_DETAIL } from '../../data/mockAuctionDetail';

const IMAGE_HEIGHT = 280;
const DESCRIPTION_MAX_HEIGHT = 400;

export default function AuctionDetailScreen() {
  const navigation = useNavigation();
  const auction = MOCK_AUCTION_DETAIL;

  const [descriptionOpen, setDescriptionOpen] = useState(false);
  const [selectedAmount, setSelectedAmount] = useState(
    auction.quickBidAmounts[0]
  );
  const [customBidMode, setCustomBidMode] = useState(false);
  const [customBidValue, setCustomBidValue] = useState('');
  const [confirmVisible, setConfirmVisible] = useState(false);
  const [pendingAmount, setPendingAmount] = useState(selectedAmount);

  // Animated values
  const descHeight = useRef(new Animated.Value(0)).current;
  const chevronRotation = useRef(new Animated.Value(0)).current;

  const toggleDescription = () => {
    const opening = !descriptionOpen;
    setDescriptionOpen(opening);
    Animated.parallel([
      Animated.spring(descHeight, {
        toValue: opening ? DESCRIPTION_MAX_HEIGHT : 0,
        useNativeDriver: false,
        bounciness: 4,
      }),
      Animated.timing(chevronRotation, {
        toValue: opening ? 1 : 0,
        duration: 250,
        useNativeDriver: true,
      }),
    ]).start();
  };

  const chevronDeg = chevronRotation.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '180deg'],
  });

  const effectiveCustomAmount = useMemo(() => {
    const parsed = parseInt(customBidValue.replace(/\D/g, ''), 10);
    return Number.isFinite(parsed) ? parsed : 0;
  }, [customBidValue]);

  const minBidAmount = auction.lastBid;
  const isCustomBidValid = effectiveCustomAmount > minBidAmount;

  const handlePlaceBid = () => {
    setPendingAmount(selectedAmount);
    setConfirmVisible(true);
  };

  const handleOpenCustomBid = () => {
    setCustomBidMode(true);
    setCustomBidValue(String(minBidAmount + 1));
  };

  const handleCustomBidChange = (value: string) => {
    setCustomBidValue(value.replace(/\D/g, ''));
  };

  const handleConfirmCustomBid = () => {
    if (!isCustomBidValid) {
      return;
    }
    setSelectedAmount(effectiveCustomAmount);
    setCustomBidMode(false);
    setCustomBidValue('');
  };

  const handleConfirmBid = () => {
    setConfirmVisible(false);
    navigation.goBack();
  };

  return (
    <SafeAreaView style={styles.safe} edges={[]}>
      <View style={styles.container}>
        {/* Scrollable Container with Parallax-like Overlapping Sheets */}
        <ScrollView
          style={styles.mainScroll}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={false}
        >
          {/* Product Image Header */}
          <AuctionImageHeader
            imageUrl={auction.imageUrl}
            height={IMAGE_HEIGHT}
            onBack={() => navigation.goBack()}
          />

          {/* Dark Blue Card — tapping it toggles the description */}
          <Pressable style={styles.blueCard} onPress={toggleDescription}>
            <AuctionProductInfo
              title={auction.title}
              sellerName={auction.sellerName}
              sellerAvatarColor={auction.sellerAvatarColor}
              status={auction.status}
              lotNumber="#029"
              titleSize={FontSize.xxl}
            />

            {/* Collapsible description with animated height */}
            <Animated.View style={[styles.descriptionWrapper, { maxHeight: descHeight }]}>
              <Text style={styles.descriptionHeading}>Descripción</Text>
              <Text style={styles.descriptionBody}>{auction.description}</Text>
            </Animated.View>
          </Pressable>

          {/* Floating chevron straddling the seam between the two cards */}
          <Pressable style={styles.floatingChevronContainer} onPress={toggleDescription}>
            <View style={styles.chevronPill}>
              <Animated.View style={{ transform: [{ rotate: chevronDeg }] }}>
                <Ionicons name="chevron-down" size={20} color={Colors.auctionViolet} />
              </Animated.View>
            </View>
          </Pressable>

          {/* White Card (Bid Stats & Bidders History List) */}
          <View style={styles.whiteCard}>
            <AuctionStatsCards
              initialPrice={auction.initialPrice}
              lastBid={auction.lastBid}
              currency={auction.currency}
              timeRemaining={auction.timeRemaining}
            />

            <View style={styles.liveHeader}>
              <View style={styles.liveTitleRow}>
                <View style={styles.liveDot} />
                <Text style={styles.liveTitle}>Subasta en vivo</Text>
              </View>
              <Text style={styles.offerCount}>
                {auction.offerCount} Ofertas
              </Text>
            </View>

            {auction.bids.map((bid) => (
              <BidHistoryRow key={bid.id} bid={bid} variant="light" />
            ))}
          </View>

          {/* Large bottom spacer to prevent sticky bottom bar occlusion */}
          <View style={styles.bottomSpacer} />
        </ScrollView>

        {customBidMode ? (
          <View style={styles.dimOverlay} pointerEvents="none" />
        ) : null}

        {/* Fixed Floating Bottom Bar */}
        <View style={styles.fixedBottomBar}>
          <AuctionBottomBar
            quickBidAmounts={auction.quickBidAmounts}
            selectedAmount={selectedAmount}
            customBidMode={customBidMode}
            customBidValue={customBidValue}
            currency={auction.currency}
            minBidAmount={minBidAmount}
            isCustomBidValid={isCustomBidValid}
            onSelectQuickBid={setSelectedAmount}
            onToggleCustomBid={handleOpenCustomBid}
            onCustomBidChange={handleCustomBidChange}
            onConfirmCustomBid={handleConfirmCustomBid}
            onCancelCustomBid={() => {
              setCustomBidMode(false);
              setCustomBidValue('');
            }}
            onPlaceBid={handlePlaceBid}
          />
        </View>
      </View>

      <ConfirmBidModal
        visible={confirmVisible}
        amount={pendingAmount}
        currency={auction.currency}
        onConfirm={handleConfirmBid}
        onCancel={() => setConfirmVisible(false)}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    minHeight: 0,
    backgroundColor: '#F5EEF0', // Matches the light background of the image header for seamless status bar bleed
  },
  container: {
    flex: 1,
    minHeight: 0,
    backgroundColor: '#F5EEF0',
    position: 'relative', // Ensures absolute children position correctly
  },
  mainScroll: {
    flex: 1,
    minHeight: 0,
    ...Platform.select({
      web: {
        overflow: 'scroll',
      },
    }),
  },
  scrollContent: {
    paddingBottom: 24,
  },
  blueCard: {
    backgroundColor: Colors.auctionViolet,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: 20,
    paddingTop: 24,
    paddingBottom: 8,
    marginTop: -20,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 4,
  },
  floatingChevronContainer: {
    alignItems: 'center',
    marginTop: -16, // Half of pill height — straddles the seam between cards
    zIndex: 10,
  },
  whiteCard: {
    backgroundColor: Colors.white,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: 20,
    paddingTop: 32, // Extra top padding to visually clear the floating pill above
    paddingBottom: 24,
    marginTop: -16, // Slides under the bottom half of the pill
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.05,
    shadowRadius: 8,
    elevation: 4,
  },
  descriptionWrapper: {
    overflow: 'hidden', // Clips content during animation
  },
  chevronRow: {
    alignItems: 'center',
    marginTop: 12,
  },
  chevronPill: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.white, // White pill floats visibly at the seam
    borderRadius: 20,
    paddingHorizontal: 28,
    paddingVertical: 7,
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.12,
    shadowRadius: 6,
    elevation: 6,
  },
  descriptionHeading: {
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.lg,
    color: Colors.white,
    marginTop: 16,
    marginBottom: 8,
  },
  descriptionBody: {
    fontFamily: Fonts.sora,
    fontSize: FontSize.md, // 14px — balanced and readable on mobile
    color: 'rgba(255,255,255,0.9)',
    lineHeight: 22,
    marginBottom: 20,
  },
  liveHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
    marginTop: 8,
  },
  liveTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: Colors.accent,
    marginRight: 8,
  },
  liveTitle: {
    fontFamily: Fonts.soraBold,
    fontSize: FontSize.base,
    color: Colors.black,
  },
  offerCount: {
    fontFamily: Fonts.sora,
    fontSize: FontSize.sm,
    color: Colors.textSecondary,
  },
  bottomSpacer: {
    height: 120, // Clean minimal padding so scroll finishes elegantly above the bar
  },
  dimOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.35)',
    bottom: 160,
    zIndex: 15,
  },
  fixedBottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    zIndex: 20,
    backgroundColor: Colors.white,
    borderTopWidth: 1,
    borderTopColor: '#EAEAEA',
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.08,
    shadowRadius: 10,
    elevation: 10,
  },
});
