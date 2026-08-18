/** Giải mã payload JWT (base64url) để lấy role — chỉ đọc, không verify (server đã verify). */
export function getRoleFromToken(token: string): string | null {
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const decoded = JSON.parse(atob(padded));
    return typeof decoded.role === 'string' ? decoded.role : null;
  } catch {
    return null;
  }
}
