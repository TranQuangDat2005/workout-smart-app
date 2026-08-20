import '@testing-library/jest-dom';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CommunityFeedPage from './CommunityFeedPage';
import { feedApi } from '../../services/feedApi';
import type { PostItem } from '../../services/feedApi';

jest.mock('../../services/feedApi', () => ({
  ...jest.requireActual('../../services/feedApi'),
  feedApi: {
    feed: jest.fn().mockResolvedValue({ items: [], nextCursor: null }),
    createPost: jest.fn(),
    toggleLike: jest.fn(),
    addComment: jest.fn(),
    deletePost: jest.fn(),
  },
}));

jest.mock('../../services/tokenStorage', () => ({
  tokenStorage: { getAccessToken: () => null },
}));

class MockIntersectionObserver {
  observe = jest.fn();
  unobserve = jest.fn();
  disconnect = jest.fn();
}
(global as { IntersectionObserver?: unknown }).IntersectionObserver = MockIntersectionObserver;

const createdPost: PostItem = {
  id: 99,
  userId: 7,
  displayName: 'Người test',
  avatarUrl: null,
  content: 'Bài đầu tiên của tôi',
  mediaType: null,
  mediaUrl: null,
  audience: 'public',
  createdAt: '2026-08-19T12:00:00Z',
  likeCount: 0,
  likedByMe: false,
  comments: [],
};

describe('CommunityFeedPage — đăng bài', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('gõ text + bấm Đăng bài → gọi createPost và hiển thị bài mới', async () => {
    (feedApi.createPost as jest.Mock).mockResolvedValue(createdPost);

    render(<CommunityFeedPage />);

    const textarea = screen.getByPlaceholderText(/Chia sẻ buổi tập/);
    await userEvent.type(textarea, 'Bài đầu tiên của tôi');

    const postButton = screen.getByRole('button', { name: /Đăng bài/ });
    expect(postButton).toBeEnabled();
    await userEvent.click(postButton);

    await waitFor(() => expect(feedApi.createPost).toHaveBeenCalledTimes(1));
    const payload = (feedApi.createPost as jest.Mock).mock.calls[0][0];
    expect(payload.content).toBe('Bài đầu tiên của tôi');
    expect(payload.audience).toBe('public');
    expect(payload.media).toBeNull();

    // Bài mới xuất hiện trên feed sau khi đăng thành công
    await waitFor(() => expect(screen.getByText('Bài đầu tiên của tôi')).toBeInTheDocument());
    // Ô nhập được reset
    expect(textarea).toHaveValue('');
  });

  it('không cho bấm Đăng bài khi chưa có nội dung và chưa có media', () => {
    render(<CommunityFeedPage />);
    expect(screen.getByRole('button', { name: /Đăng bài/ })).toBeDisabled();
  });

  it('hiển thị lỗi server khi đăng thất bại', async () => {
    (feedApi.createPost as jest.Mock).mockRejectedValue({
      response: { data: { message: 'Nội dung bài đăng tối đa 2000 ký tự' } },
    });

    render(<CommunityFeedPage />);
    await userEvent.type(screen.getByPlaceholderText(/Chia sẻ buổi tập/), 'quá dài');
    await userEvent.click(screen.getByRole('button', { name: /Đăng bài/ }));

    await waitFor(() =>
      expect(screen.getByText('Nội dung bài đăng tối đa 2000 ký tự')).toBeInTheDocument(),
    );
  });
});
