import { create } from 'zustand';

interface AuthState {
  token: string | null;
  userInfo: any;
  setToken: (token: string) => void;
  setUserInfo: (info: any) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('token'),
  userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
  
  setToken: (token) => {
    localStorage.setItem('token', token);
    set({ token });
  },
  
  setUserInfo: (userInfo) => {
    localStorage.setItem('userInfo', JSON.stringify(userInfo));
    set({ userInfo });
  },
  
  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    set({ token: null, userInfo: null });
  },
}));
