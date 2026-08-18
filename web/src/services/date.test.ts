import { todayLocalISO } from './date';

describe('todayLocalISO', () => {
  it('returns a YYYY-MM-DD string', () => {
    expect(todayLocalISO()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});
