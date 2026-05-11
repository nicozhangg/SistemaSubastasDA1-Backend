import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de usuario / perfil
 */
export const userService = {
  getProfile: async () => {
    // TODO: implementar
    return apiClient.get(Endpoints.USERS.PROFILE);
  },

  updateProfile: async (data: {
    firstName?: string;
    lastName?: string;
    phone?: string;
  }) => {
    // TODO: implementar
    return apiClient.put(Endpoints.USERS.UPDATE_PROFILE, data);
  },
};
