import { useContext } from 'react';
import type { AuthContextValue } from './AuthContextValue';
import { AuthContext } from './AuthContextValue';

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth phải được dùng trong AuthProvider');
  return ctx;
}
