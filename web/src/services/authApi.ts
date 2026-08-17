import axios from 'axios';
import { tokenStorage } from './tokenStorage';

/** HTTP client — access token trong memory, silent refresh khi 401 (FR-008). */
const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise: Promise<string> | null = null;

client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const refreshToken = tokenStorage.getRefreshToken();
    if (error.response?.status === 401 && refreshToken && !original._retried) {
      original._retried = true;
      try {
        refreshPromise ??= authApi.refresh(refreshToken).then((res) => {
          tokenStorage.setAccessToken(res.accessToken);
          tokenStorage.setRefreshToken(res.refreshToken);
          return res.accessToken;
        });
        await refreshPromise;
        refreshPromise = null;
        return client(original);
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

interface AuthResponseDto {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
}

interface VerifyOtpResponseDto {
  verified: boolean;
  restoreRequired: boolean;
  message: string;
}

export const authApi = {
  register: (email: string, password: string) =>
    client.post<{ message: string }>('/auth/register', { email, password }).then((r) => r.data),

  verifyOtp: (email: string, code: string) =>
    client.post<VerifyOtpResponseDto>('/auth/verify-otp', { email, code }).then((r) => r.data),

  resendOtp: (email: string) =>
    client.post<{ message: string }>('/auth/resend-otp', { email }).then((r) => r.data),

  login: (email: string, password: string) =>
    client.post<AuthResponseDto>('/auth/login', { email, password }).then((r) => r.data),

  refresh: (refreshToken: string) =>
    client
      .post<AuthResponseDto>('/auth/refresh', { refreshToken }, { headers: { Authorization: undefined } })
      .then((r) => r.data),

  logout: (refreshToken: string) =>
    client.post<void>('/auth/logout', { refreshToken }, { headers: { Authorization: undefined } }),

  forgotPassword: (email: string) =>
    client.post<{ message: string }>('/auth/forgot-password', { email }).then((r) => r.data),

  resetPassword: (email: string, code: string, newPassword: string) =>
    client
      .post<{ message: string }>('/auth/reset-password', { email, code, newPassword })
      .then((r) => r.data),

  restorePassword: (email: string, code: string, newPassword: string) =>
    client
      .post<{ message: string }>('/auth/restore-password', { email, code, newPassword })
      .then((r) => r.data),
};

export default client;
