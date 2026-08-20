import { http } from './http';

/**
 * Regression: http instance từng set Content-Type: application/json mặc định.
 * axios 1.19 convert FormData → JSON khi thấy header này → backend multipart
 * trả 415 (nuốt thành 500) → không đăng được bài. Không được set lại.
 */
describe('http instance', () => {
  it('không ép Content-Type mặc định (để FormData gửi đúng multipart có boundary)', () => {
    const headers = http.defaults.headers as Record<string, unknown>;
    const common = (headers.common ?? {}) as Record<string, unknown>;
    expect(headers['Content-Type']).toBeUndefined();
    expect(headers['content-type']).toBeUndefined();
    expect(common['Content-Type'] ?? undefined).toBeUndefined();
  });
});
