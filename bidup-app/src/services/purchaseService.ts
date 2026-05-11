import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de compras
 */
export const purchaseService = {
  getPurchaseDetail: async (id: string) => {
    // TODO: implementar
    return apiClient.get(Endpoints.PURCHASES.DETAIL(id));
  },
};
