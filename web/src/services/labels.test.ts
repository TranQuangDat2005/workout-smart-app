import { ACCOUNT_STATUS_LABELS, CHALLENGE_STATUS_LABELS, label } from './labels';

describe('label', () => {
  it('maps a known value to its Vietnamese label', () => {
    expect(label(ACCOUNT_STATUS_LABELS, 'BANNED')).toBe('Bị khóa');
    expect(label(CHALLENGE_STATUS_LABELS, 'finished')).toBe('Đã kết thúc');
  });

  it('returns the original value when unmapped', () => {
    expect(label(ACCOUNT_STATUS_LABELS, 'UNKNOWN')).toBe('UNKNOWN');
  });

  it('returns fallback for null or empty value', () => {
    expect(label(CHALLENGE_STATUS_LABELS, null)).toBe('—');
    expect(label(CHALLENGE_STATUS_LABELS, '')).toBe('—');
  });
});
