import { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { authApi } from '../services/authApi';
import { tokenStorage } from '../services/tokenStorage';
import { AuthContext } from './AuthContextValue';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Khôi phục phiên: nếu có refresh token thì silent refresh (FR-008)
  useEffect(() => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (!refreshToken) return;
    authApi
      .refresh(refreshToken)
      .then((res) => {
        tokenStorage.setAccessToken(res.accessToken);
        tokenStorage.setRefreshToken(res.refreshToken);
        setIsAuthenticated(true);
      })
      .catch(() => {
        tokenStorage.clear();
        setIsAuthenticated(false);
      });
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    tokenStorage.setAccessToken(res.accessToken);
    tokenStorage.setRefreshToken(res.refreshToken);
    setIsAuthenticated(true);
  }, []);

  const logout = useCallback(() => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (refreshToken) {
      authApi.logout(refreshToken).catch(() => undefined);
    }
    tokenStorage.clear();
    setIsAuthenticated(false);
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
