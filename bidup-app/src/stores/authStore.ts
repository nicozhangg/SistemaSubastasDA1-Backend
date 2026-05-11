import { create } from 'zustand';
import { User } from '../types';

interface AuthStore {
  user: User | null;
  token: string | null;
  isGuest: boolean;
  isAuthenticated: boolean;

  login: (user: User, token: string) => void;
  logout: () => void;
  setGuest: () => void;
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  token: null,
  isGuest: false,
  isAuthenticated: false,

  login: (user, token) =>
    set({
      user,
      token,
      isGuest: false,
      isAuthenticated: true,
    }),

  logout: () =>
    set({
      user: null,
      token: null,
      isGuest: false,
      isAuthenticated: false,
    }),

  setGuest: () =>
    set({
      user: null,
      token: null,
      isGuest: true,
      isAuthenticated: false,
    }),
}));
