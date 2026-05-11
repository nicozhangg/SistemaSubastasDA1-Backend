import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de autenticación
 */
export const authService = {
  login: async (email: string, password: string) => {
    // TODO: implementar
    return apiClient.post(Endpoints.AUTH.LOGIN, { email, password });
  },

  logout: async () => {
    // TODO: implementar
    return apiClient.post(Endpoints.AUTH.LOGOUT);
  },

  registerStep1: async (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }) => {
    // TODO: implementar
    return apiClient.post(Endpoints.AUTH.REGISTER_STEP1, data);
  },

  registerStep2: async (data: {
    dni: string;
    phone: string;
    address: string;
  }) => {
    // TODO: implementar
    return apiClient.post(Endpoints.AUTH.REGISTER_STEP2, data);
  },

  registerStep3: async (data: { paymentMethod: unknown }) => {
    // TODO: implementar
    return apiClient.post(Endpoints.AUTH.REGISTER_STEP3, data);
  },
};
