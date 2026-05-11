/**
 * BidUp — Endpoints de la API REST
 *
 * Solo paths relativos; la baseURL se configura en apiClient.ts
 * Basado en subastas-api.yaml
 */
export const Endpoints = {
  // ── Auth ───────────────────────────────────────────────
  AUTH: {
    LOGIN: '/auth/login',
    LOGOUT: '/auth/logout',
    REGISTER_STEP1: '/auth/register/step1',
    REGISTER_STEP2: '/auth/register/step2',
    REGISTER_STEP3: '/auth/register/step3',
  },

  // ── Users ──────────────────────────────────────────────
  USERS: {
    PROFILE: '/users/profile',
    UPDATE_PROFILE: '/users/profile',
  },

  // ── Catalog ────────────────────────────────────────────
  CATALOG: {
    ITEMS: '/catalog/items',
    ITEM_DETAIL: (id: string) => `/catalog/items/${id}`,
    ITEM_IMAGES: (id: string) => `/catalog/items/${id}/images`,
  },

  // ── Auctions ───────────────────────────────────────────
  AUCTIONS: {
    LIST: '/auctions',
    DETAIL: (id: string) => `/auctions/${id}`,
    CONNECT: (id: string) => `/auctions/${id}/connect`,
    DISCONNECT: (id: string) => `/auctions/${id}/disconnect`,
  },

  // ── Bids ───────────────────────────────────────────────
  BIDS: {
    CURRENT: (auctionId: string) => `/auctions/${auctionId}/bids/current`,
    PLACE: (auctionId: string) => `/auctions/${auctionId}/bids`,
    HISTORY: (auctionId: string) => `/auctions/${auctionId}/bids/history`,
  },

  // ── Purchases ──────────────────────────────────────────
  PURCHASES: {
    DETAIL: (id: string) => `/purchases/${id}`,
  },

  // ── Payment Methods ────────────────────────────────────
  PAYMENTS: {
    METHODS: '/payment-methods',
    ADD_CARD: '/payment-methods/card',
    ADD_BANK_ACCOUNT: '/payment-methods/bank-account',
    ADD_CHECK: '/payment-methods/check',
    DELETE: (id: string) => `/payment-methods/${id}`,
  },

  // ── Consignment ────────────────────────────────────────
  CONSIGNMENT: {
    SUBMIT_ITEM: '/consignment/items',
    ACCEPT_CONDITIONS: (id: string) => `/consignment/items/${id}/accept`,
    REJECT_CONDITIONS: (id: string) => `/consignment/items/${id}/reject`,
    ITEM_LOCATION: (id: string) => `/consignment/items/${id}/location`,
    INSURANCE_POLICY: (id: string) => `/consignment/items/${id}/insurance`,
  },

  // ── Metrics ────────────────────────────────────────────
  METRICS: {
    STATS: '/metrics/stats',
    PARTICIPATION_HISTORY: '/metrics/participation',
    FINES: '/metrics/fines',
    PAY_FINE: (id: string) => `/metrics/fines/${id}/pay`,
  },
} as const;
