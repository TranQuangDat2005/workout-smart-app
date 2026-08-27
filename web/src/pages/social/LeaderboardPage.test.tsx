import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LeaderboardPage from './LeaderboardPage';
import { socialApi } from '../../services/socialApi';
import { profileApi } from '../../services/profileApi';

jest.mock('../../services/socialApi');
jest.mock('../../services/profileApi');

const mockedSocialApi = jest.mocked(socialApi);
const mockedProfileApi = jest.mocked(profileApi);

function wrapper({ children }: { children: React.ReactNode }) {
  return <MemoryRouter>{children}</MemoryRouter>;
}

beforeEach(() => {
  jest.clearAllMocks();
  mockedSocialApi.challenges.mockResolvedValue([]);
  mockedSocialApi.myChallenges.mockResolvedValue([]);
  mockedProfileApi.getProfile.mockResolvedValue({ id: 1 } as never);
});

describe('LeaderboardPage', () => {
  it('renders empty state when no data', async () => {
    mockedSocialApi.leaderboard.mockResolvedValue([]);
    render(<LeaderboardPage />, { wrapper });
    expect(await screen.findByText(/Chưa có dữ liệu xếp hạng/)).toBeInTheDocument();
  });

  it('renders pin card when viewer is in top 100', async () => {
    mockedSocialApi.leaderboard.mockResolvedValue([
      { rank: 1, userId: 1, displayName: 'An', currentStreakWeeks: 5, longestStreakWeeks: 8, viewerRank: null },
    ]);
    render(<LeaderboardPage />, { wrapper });
    expect(await screen.findByText(/Vị trí của bạn/)).toBeInTheDocument();
    expect(screen.getAllByText('An').length).toBeGreaterThanOrEqual(1);
  });

  it('renders pin card with viewerRank when viewer is outside top 100', async () => {
    mockedSocialApi.leaderboard.mockResolvedValue([
      { rank: 1, userId: 2, displayName: 'Top', currentStreakWeeks: 10, longestStreakWeeks: 10, viewerRank: null },
      { rank: 2, userId: 3, displayName: 'Second', currentStreakWeeks: 8, longestStreakWeeks: 8, viewerRank: null },
      // Viewer (userId=1) is not in the list but has viewerRank
      { rank: 101, userId: 1, displayName: 'Viewer', currentStreakWeeks: 1, longestStreakWeeks: 1, viewerRank: 101 },
    ]);
    render(<LeaderboardPage />, { wrapper });
    expect(await screen.findByText(/Vị trí của bạn/)).toBeInTheDocument();
    expect(screen.getAllByText(/#101/).length).toBeGreaterThanOrEqual(1);
  });

  it('renders finished challenges with results button', async () => {
    mockedSocialApi.leaderboard.mockResolvedValue([]);
    mockedSocialApi.challenges.mockResolvedValue([
      { id: 1, name: '30 ngày plank', goalType: 'endurance', durationDays: 30, startDate: '2026-07-01', endDate: '2026-07-31', status: 'finished', joined: true, participantCount: 5, completedAt: '2026-07-31T23:59:59Z', finalRank: 2 },
    ]);
    render(<LeaderboardPage />, { wrapper });
    expect(await screen.findByText('30 ngày plank')).toBeInTheDocument();
    expect(screen.getByText('Xem kết quả')).toBeInTheDocument();
    expect(screen.getByText(/Hạng của bạn: #2/)).toBeInTheDocument();
  });

  it('renders open challenges section', async () => {
    mockedSocialApi.leaderboard.mockResolvedValue([]);
    mockedSocialApi.challenges.mockResolvedValue([
      { id: 2, name: '14 ngày cardio', goalType: 'cardio', durationDays: 14, startDate: '2026-08-20', endDate: '2026-09-03', status: 'open', joined: false, participantCount: 3, completedAt: null, finalRank: null },
    ]);
    render(<LeaderboardPage />, { wrapper });
    expect(await screen.findByText('14 ngày cardio')).toBeInTheDocument();
    expect(screen.getByText('Tham gia')).toBeInTheDocument();
  });
});
