/**
 * Lưu token (research R7 - Web):
 * - Access token: giữ trong memory (không để JS khác/extension đọc dễ dàng)
 * - Refresh token: localStorage (MVP; nâng cấp httpOnly cookie ở phase sau)
 */
let accessToken: string | null = null;

const REFRESH_KEY = 'ws.refresh_token';

export const tokenStorage = {
  getAccessToken: (): string | null => accessToken,
  setAccessToken: (token: string): void => {
    accessToken = token;
  },
  getRefreshToken: (): string | null => localStorage.getItem(REFRESH_KEY),
  setRefreshToken: (token: string): void => {
    localStorage.setItem(REFRESH_KEY, token);
  },
  clear: (): void => {
    accessToken = null;
    localStorage.removeItem(REFRESH_KEY);
  },
};
