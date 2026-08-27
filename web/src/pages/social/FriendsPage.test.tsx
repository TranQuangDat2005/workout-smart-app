import '@testing-library/jest-dom';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FriendsPage from './FriendsPage';
import { socialApi } from '../../services/socialApi';
import type { UserSearchItem } from '../../services/socialApi';

jest.mock('../../services/socialApi', () => ({
  ...jest.requireActual('../../services/socialApi'),
  socialApi: {
    friends: jest.fn().mockResolvedValue([]),
    pendingRequests: jest.fn().mockResolvedValue([]),
    searchUsers: jest.fn(),
    sendFriendRequest: jest.fn(),
    accept: jest.fn(),
    reject: jest.fn(),
    unfriend: jest.fn(),
  },
}));

const searchItem = (overrides: Partial<UserSearchItem>): UserSearchItem => ({
  id: 7,
  displayName: 'Bình',
  avatarUrl: null,
  email: null,
  relationshipStatus: 'none',
  friendshipId: null,
  ...overrides,
});

describe('FriendsPage — nút theo trạng thái quan hệ (FR-004)', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('hiển thị nút + Kết bạn khi chưa có quan hệ', async () => {
    (socialApi.searchUsers as jest.Mock).mockResolvedValue([searchItem({})]);

    render(<FriendsPage />);
    await userEvent.type(screen.getByLabelText(/Tên hoặc email/i), 'Bình');
    await userEvent.click(screen.getByRole('button', { name: /Tìm/ }));

    await waitFor(() =>
      expect(screen.getByRole('button', { name: /\+ Kết bạn/ })).toBeInTheDocument(),
    );
  });

  it('hiển thị nút Rút lời mời khi pending_sent và gọi unfriend đúng id', async () => {
    (socialApi.searchUsers as jest.Mock).mockResolvedValue([
      searchItem({ relationshipStatus: 'pending_sent', friendshipId: 11 }),
    ]);

    render(<FriendsPage />);
    await userEvent.type(screen.getByLabelText(/Tên hoặc email/i), 'Bình');
    await userEvent.click(screen.getByRole('button', { name: /Tìm/ }));

    await waitFor(() => {
      const btn = screen.getByRole('button', { name: /Rút lời mời/ });
      expect(btn).toBeInTheDocument();
      expect(btn).not.toBeDisabled();
    });
    await userEvent.click(screen.getByRole('button', { name: /Rút lời mời/ }));
    await waitFor(() => expect(socialApi.unfriend).toHaveBeenCalledWith(11));
  });

  it('hiển thị Chấp nhận/Từ chối khi pending_received và gọi accept đúng id', async () => {
    (socialApi.searchUsers as jest.Mock).mockResolvedValue([
      searchItem({ relationshipStatus: 'pending_received', friendshipId: 22 }),
    ]);

    render(<FriendsPage />);
    await userEvent.type(screen.getByLabelText(/Tên hoặc email/i), 'Bình');
    await userEvent.click(screen.getByRole('button', { name: /Tìm/ }));

    const acceptBtn = await screen.findByRole('button', { name: /Chấp nhận/ });
    expect(screen.getByRole('button', { name: /Từ chối/ })).toBeInTheDocument();
    await userEvent.click(acceptBtn);
    await waitFor(() => expect(socialApi.accept).toHaveBeenCalledWith(22));
  });

  it('hiển thị badge Bạn bè khi accepted', async () => {
    (socialApi.searchUsers as jest.Mock).mockResolvedValue([
      searchItem({ relationshipStatus: 'accepted', friendshipId: 33 }),
    ]);

    render(<FriendsPage />);
    await userEvent.type(screen.getByLabelText(/Tên hoặc email/i), 'Bình');
    await userEvent.click(screen.getByRole('button', { name: /Tìm/ }));

    await waitFor(() =>
      expect(screen.getByText('Bạn bè', { selector: 'span.badge' })).toBeInTheDocument(),
    );
  });
});
