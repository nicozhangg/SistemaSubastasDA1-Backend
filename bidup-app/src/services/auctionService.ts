import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de subastas
 */
export const auctionService = {
  getAuctions: async () => {
    // TODO: implementar
    return apiClient.get(Endpoints.AUCTIONS.LIST);
  },

  getAuctionDetail: async (id: string) => {
    // TODO: implementar
    return apiClient.get(Endpoints.AUCTIONS.DETAIL(id));
  },

  connectToAuction: async (id: string) => {
    // TODO: implementar
    return apiClient.post(Endpoints.AUCTIONS.CONNECT(id));
  },

  disconnectFromAuction: async (id: string) => {
    // TODO: implementar
    return apiClient.post(Endpoints.AUCTIONS.DISCONNECT(id));
  },
};
