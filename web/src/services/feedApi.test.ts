import { postMediaUrl, timeAgo } from './feedApi';

describe('postMediaUrl', () => {
  it('trả null khi không có key', () => {
    expect(postMediaUrl(null)).toBeNull();
    expect(postMediaUrl('')).toBeNull();
  });

  it('ghép key vào endpoint media', () => {
    expect(postMediaUrl('workoutsmart-media/abc.png')).toBe('/api/v1/feed/media/workoutsmart-media/abc.png');
  });
});

describe('timeAgo', () => {
  const now = Date.parse('2026-08-19T12:00:00Z');

  it('hiển thị vừa xong dưới 1 phút', () => {
    expect(timeAgo('2026-08-19T11:59:30Z', now)).toBe('vừa xong');
  });

  it('hiển thị phút', () => {
    expect(timeAgo('2026-08-19T11:55:00Z', now)).toBe('5 phút trước');
  });

  it('hiển thị giờ', () => {
    expect(timeAgo('2026-08-19T10:00:00Z', now)).toBe('2 giờ trước');
  });

  it('hiển thị ngày', () => {
    expect(timeAgo('2026-08-16T12:00:00Z', now)).toBe('3 ngày trước');
  });

  it('quá 30 ngày hiển thị ngày cụ thể', () => {
    const result = timeAgo('2026-07-01T12:00:00Z', now);
    expect(result).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/);
  });
});
