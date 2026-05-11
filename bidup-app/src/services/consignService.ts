import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de consignación de ítems
 */
export const consignService = {
  submitItem: async (data: FormData) => {
    // TODO: implementar
    return apiClient.post(Endpoints.CONSIGNMENT.SUBMIT_ITEM, data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  acceptConditions: async (id: string) => {
    // TODO: implementar
    return apiClient.post(Endpoints.CONSIGNMENT.ACCEPT_CONDITIONS(id));
  },

  rejectConditions: async (id: string) => {
    // TODO: implementar
    return apiClient.post(Endpoints.CONSIGNMENT.REJECT_CONDITIONS(id));
  },

  getItemLocation: async (id: string) => {
    // TODO: implementar
    return apiClient.get(Endpoints.CONSIGNMENT.ITEM_LOCATION(id));
  },

  getInsurancePolicy: async (id: string) => {
    // TODO: implementar
    return apiClient.get(Endpoints.CONSIGNMENT.INSURANCE_POLICY(id));
  },
};
