import { create } from 'zustand';
import { MockAuctionItem } from '../data/mockActivity';

interface MyAuctionsStore {
  submissions: MockAuctionItem[];
  addSubmission: (item: Omit<MockAuctionItem, 'id' | 'moderationStatus' | 'timeRemaining'>) => void;
}

export const useMyAuctionsStore = create<MyAuctionsStore>((set) => ({
  submissions: [],

  addSubmission: (item) =>
    set((state) => ({
      submissions: [
        {
          ...item,
          id: `user-auc-${Date.now()}`,
          moderationStatus: 'pending',
          timeRemaining: 'En revisión',
        },
        ...state.submissions,
      ],
    })),
}));
