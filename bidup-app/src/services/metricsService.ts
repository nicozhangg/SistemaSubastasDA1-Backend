import apiClient from './apiClient';
import { Endpoints } from '../constants';

/**
 * Servicio de métricas y multas
 */
export const metricsService = {
  getStats: async () => {
    // TODO: implementar
    return apiClient.get(Endpoints.METRICS.STATS);
  },

  getParticipationHistory: async () => {
    // TODO: implementar
    return apiClient.get(Endpoints.METRICS.PARTICIPATION_HISTORY);
  },

  getFines: async () => {
    // TODO: implementar
    return apiClient.get(Endpoints.METRICS.FINES);
  },

  payFine: async (id: string) => {
    // TODO: implementar
    return apiClient.post(Endpoints.METRICS.PAY_FINE(id));
  },
};
