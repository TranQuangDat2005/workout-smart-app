import { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi } from '../services/authApi';
import { tokenStorage } from '../services/tokenStorage';
import { getRoleFromToken } from '../services/jwt';
import { AuthContext } from './AuthContextValue';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);

  const applySession = (accessToken: string, refreshToken: string) => {
    tokenStorage.setAccessToken(accessToken);
    tokenStorage.setRefreshToken(refreshToken);
    setIsAuthenticated(true);
    setIsAdmin(getRoleFromToken(accessToken) === 'admin');
  };

  // Khôi phục phiên: nếu có refresh token thì silent refresh (FR-008)
  useEffect(() => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (!refreshToken) return;
    authApi
      .refresh(refreshToken)
      .then((res) => applySession(res.accessToken, res.refreshToken))
      .catch(() => {
        tokenStorage.clear();
        setIsAuthenticated(false);
        setIsAdmin(false);
      });
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    applySession(res.accessToken, res.refreshToken);
  }, []);

  const logout = useCallback(() => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (refreshToken) {
      authApi.logout(refreshToken).catch(() => undefined);
    }
    tokenStorage.clear();
    setIsAuthenticated(false);
    setIsAdmin(false);
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, isAdmin, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
