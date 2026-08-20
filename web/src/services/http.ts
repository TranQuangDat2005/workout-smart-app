import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { tokenStorage } from './tokenStorage';

/**
 * HTTP client dùng chung cho toàn bộ Web App.
 * - Request: tự gắn access token (nếu có).
 * - Response: khi nhận 401 và còn refresh token thì silent refresh 1 lần rồi thử lại request gốc (FR-008).
 * - KHÔNG ép Content-Type mặc định: axios tự set application/json cho object payload,
 *   còn FormData cần để browser tự sinh boundary multipart (axios 1.19 sẽ convert
 *   FormData thành JSON nếu header mặc định là application/json — lỗi 500 khi đăng bài).
 */
export const http = axios.create({
  baseURL: '/api/v1',
});

http.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise: Promise<string> | null = null;

/** Gọi endpoint refresh bằng axios thuần để tránh lọt vào chính interceptor phản hồi. */
function refreshTokens(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    return Promise.reject(new Error('Missing refresh token'));
  }
  return axios
    .post<{ accessToken: string; refreshToken: string }>('/api/v1/auth/refresh', { refreshToken })
    .then((res) => {
      tokenStorage.setAccessToken(res.data.accessToken);
      tokenStorage.setRefreshToken(res.data.refreshToken);
      return res.data.accessToken;
    });
}

http.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retried?: boolean }) | undefined;
    const refreshToken = tokenStorage.getRefreshToken();

    if (error.response?.status === 401 && refreshToken && original && !original._retried) {
      original._retried = true;
      try {
        refreshPromise ??= refreshTokens();
        await refreshPromise;
        refreshPromise = null;
        return http(original);
      } catch {
        refreshPromise = null;
        tokenStorage.clear();
        window.location.href = '/login';
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  },
);

export default http;
