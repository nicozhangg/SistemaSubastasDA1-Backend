import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de métodos de pago
 */
export const paymentService = {
  getPaymentMethods: async () => {
    // TODO: implementar
    return apiClient.get(Endpoints.PAYMENTS.METHODS);
  },

  addCard: async (data: {
    cardNumber: string;
    cardHolder: string;
    expirationDate: string;
    cvv: string;
  }) => {
    // TODO: implementar
    return apiClient.post(Endpoints.PAYMENTS.ADD_CARD, data);
  },

  addBankAccount: async (data: {
    bankName: string;
    accountNumber: string;
    routingNumber: string;
  }) => {
    // TODO: implementar
    return apiClient.post(Endpoints.PAYMENTS.ADD_BANK_ACCOUNT, data);
  },

  addCheck: async (data: {
    bankName: string;
    checkNumber: string;
    amount: number;
  }) => {
    // TODO: implementar
    return apiClient.post(Endpoints.PAYMENTS.ADD_CHECK, data);
  },

  deletePaymentMethod: async (id: string) => {
    // TODO: implementar
    return apiClient.delete(Endpoints.PAYMENTS.DELETE(id));
  },
};
