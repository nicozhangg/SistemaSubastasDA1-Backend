import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de pujas (bids)
 */
export const bidService = {
  getCurrentBid: async (auctionId: string) => {
    // TODO: implementar
    return apiClient.get(Endpoints.BIDS.CURRENT(auctionId));
  },

  placeBid: async (auctionId: string, amount: number) => {
    // TODO: implementar
    return apiClient.post(Endpoints.BIDS.PLACE(auctionId), { amount });
  },

  getBidHistory: async (auctionId: string) => {
    // TODO: implementar
    return apiClient.get(Endpoints.BIDS.HISTORY(auctionId));
  },
};
