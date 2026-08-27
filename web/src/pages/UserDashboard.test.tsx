import '@testing-library/jest-dom';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import UserDashboard from './UserDashboard';
import { socialApi } from '../services/socialApi';

jest.mock('../services/profileApi', () => ({
  profileApi: { getProfile: jest.fn().mockResolvedValue({ displayName: 'An', goalType: 'muscle_gain' }) },
}));
jest.mock('../services/planApi', () => ({
  planApi: { getActivePlan: jest.fn().mockResolvedValue({ id: 1, days: [] }) },
}));
jest.mock('../services/statsApi', () => ({
  statsApi: {
    dashboard: jest.fn().mockResolvedValue({
      streak: { currentStreakWeeks: 2, longestStreakWeeks: 5 },
      planCompletion: { completionPct: 40 },
      weight: [],
      volume: [],
      calories: [],
    }),
  },
}));
jest.mock('../services/nutritionApi', () => ({
  nutritionApi: { getSummary: jest.fn().mockResolvedValue({ targetCalories: 2000, totalCalories: 1200 }) },
}));
jest.mock('../services/socialApi', () => ({
  ...jest.requireActual('../services/socialApi'),
  socialApi: { feed: jest.fn() },
}));

describe('UserDashboard — Hoạt động bạn bè (FR-005/006)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (socialApi.feed as jest.Mock).mockResolvedValue([]);
  });

  it('hiển thị empty state khi chưa có hoạt động', async () => {
    render(<MemoryRouter><UserDashboard /></MemoryRouter>);
    await waitFor(() =>
      expect(screen.getByText(/Chưa có hoạt động mới từ bạn bè/)).toBeInTheDocument(),
    );
    expect(socialApi.feed).toHaveBeenCalled();
  });

  it('hiển thị sự kiện streak_milestone với label tiếng Việt', async () => {
    (socialApi.feed as jest.Mock).mockResolvedValue([
      {
        id: 1,
        friendId: 2,
        displayName: 'Bình',
        actionType: 'streak_milestone',
        detailsJson: '{"streakWeeks":3}',
        createdAt: new Date().toISOString(),
      },
    ]);

    render(<MemoryRouter><UserDashboard /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Bình')).toBeInTheDocument());
    expect(screen.getByText('đạt chuỗi 3 tuần')).toBeInTheDocument();
  });

  it('hiển thị sự kiện new_pr với tổng khối lượng', async () => {
    (socialApi.feed as jest.Mock).mockResolvedValue([
      {
        id: 2,
        friendId: 3,
        displayName: 'Châu',
        actionType: 'new_pr',
        detailsJson: '{"volumeKg":250}',
        createdAt: new Date().toISOString(),
      },
    ]);

    render(<MemoryRouter><UserDashboard /></MemoryRouter>);
    await waitFor(() =>
      expect(screen.getByText('phá kỷ lục: tổng khối lượng 250 kg')).toBeInTheDocument(),
    );
  });
});
