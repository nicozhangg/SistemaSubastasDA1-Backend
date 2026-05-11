import { create } from 'zustand';
import { Auction } from '../types';

interface AuctionStore {
  auctions: Auction[];
  currentAuction: Auction | null;
  sessionId: string | null;

  setAuctions: (auctions: Auction[]) => void;
  setCurrentAuction: (auction: Auction | null) => void;
  setSessionId: (sessionId: string | null) => void;
}

export const useAuctionStore = create<AuctionStore>((set) => ({
  auctions: [],
  currentAuction: null,
  sessionId: null,

  setAuctions: (auctions) => set({ auctions }),
  setCurrentAuction: (auction) => set({ currentAuction: auction }),
  setSessionId: (sessionId) => set({ sessionId }),
}));
