import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de catálogo
 */
export const catalogService = {
  getItems: async () => {
    // TODO: implementar
    return apiClient.get(Endpoints.CATALOG.ITEMS);
  },

  getItemDetail: async (id: string) => {
    // TODO: implementar
    return apiClient.get(Endpoints.CATALOG.ITEM_DETAIL(id));
  },

  getItemImages: async (id: string) => {
    // TODO: implementar
    return apiClient.get(Endpoints.CATALOG.ITEM_IMAGES(id));
  },
};
