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

/** Lấy userId từ payload JWT (claim sub) — chỉ đọc, server vẫn kiểm tra quyền thực thi. */
export function getUserIdFromToken(token: string): number | null {
  try {
    const payload = token.split('.')[1];
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
    const decoded = JSON.parse(atob(padded));
    const sub = typeof decoded.sub === 'string' || typeof decoded.sub === 'number' ? Number(decoded.sub) : NaN;
    return Number.isFinite(sub) && sub > 0 ? sub : null;
  } catch {
    return null;
  }
}
