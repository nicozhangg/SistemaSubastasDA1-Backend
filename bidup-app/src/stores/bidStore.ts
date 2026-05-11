import { create } from 'zustand';
import { Bid } from '../types';

interface BidStore {
  bids: Bid[];
  currentBid: Bid | null;

  setBids: (bids: Bid[]) => void;
  setCurrentBid: (bid: Bid | null) => void;
  placeBid: (bid: Bid) => void;
}

export const useBidStore = create<BidStore>((set) => ({
  bids: [],
  currentBid: null,

  setBids: (bids) => set({ bids }),
  setCurrentBid: (bid) => set({ currentBid: bid }),
  placeBid: (bid) =>
    set((state) => ({
      bids: [bid, ...state.bids],
      currentBid: bid,
    })),
}));
