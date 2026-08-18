import { http } from './http';

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
    http.post<{ message: string }>('/auth/register', { email, password }).then((r) => r.data),

  verifyOtp: (email: string, code: string) =>
    http.post<VerifyOtpResponseDto>('/auth/verify-otp', { email, code }).then((r) => r.data),

  resendOtp: (email: string) =>
    http.post<{ message: string }>('/auth/resend-otp', { email }).then((r) => r.data),

  login: (email: string, password: string) =>
    http.post<AuthResponseDto>('/auth/login', { email, password }).then((r) => r.data),

  refresh: (refreshToken: string) =>
    http
      .post<AuthResponseDto>('/auth/refresh', { refreshToken }, { headers: { Authorization: undefined } })
      .then((r) => r.data),

  logout: (refreshToken: string) =>
    http.post<void>('/auth/logout', { refreshToken }, { headers: { Authorization: undefined } }),

  forgotPassword: (email: string) =>
    http.post<{ message: string }>('/auth/forgot-password', { email }).then((r) => r.data),

  resetPassword: (email: string, code: string, newPassword: string) =>
    http
      .post<{ message: string }>('/auth/reset-password', { email, code, newPassword })
      .then((r) => r.data),

  restorePassword: (email: string, code: string, newPassword: string) =>
    http
      .post<{ message: string }>('/auth/restore-password', { email, code, newPassword })
      .then((r) => r.data),
};
