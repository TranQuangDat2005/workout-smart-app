import { http } from './http';

export type FeedTab = 'friends' | 'discover';

export interface CommentItem {
  id: number;
  userId: number;
  displayName: string | null;
  avatarUrl: string | null;
  content: string;
  createdAt: string;
}

export interface PostItem {
  id: number;
  userId: number;
  displayName: string | null;
  avatarUrl: string | null;
  content: string | null;
  mediaType: 'image' | null;
  mediaUrl: string | null;
  gifUrl: string | null;
  audience: string;
  createdAt: string;
  likeCount: number;
  likedByMe: boolean;
  comments: CommentItem[];
}

export interface FeedPage {
  items: PostItem[];
  nextCursor: number | null;
}

export interface LikeResult {
  liked: boolean;
  likeCount: number;
}

export const feedApi = {
  feed: (tab: FeedTab, cursor?: number) =>
    http.get<FeedPage>('/feed/posts', { params: { tab, cursor } }).then((r) => r.data),

  createPost: (payload: { content?: string; audience?: string; media?: File | null; gifUrl?: string | null }) => {
    const form = new FormData();
    if (payload.content) form.append('content', payload.content);
    form.append('audience', payload.audience ?? 'public');
    if (payload.media) form.append('media', payload.media);
    if (payload.gifUrl) form.append('gifUrl', payload.gifUrl);
    return http.post<PostItem>('/feed/posts', form).then((r) => r.data);
  },

  toggleLike: (postId: number) =>
    http.post<LikeResult>(`/feed/posts/${postId}/like`).then((r) => r.data),

  addComment: (postId: number, content: string) =>
    http.post<CommentItem>(`/feed/posts/${postId}/comments`, { content }).then((r) => r.data),

  deletePost: (postId: number) => http.delete(`/feed/posts/${postId}`).then(() => undefined),
};

/** URL hiển thị media — backend 302 redirect sang SeaweedFS (hỗ trợ stream video). */
export function postMediaUrl(mediaUrl: string | null): string | null {
  if (!mediaUrl) return null;
  return `/api/v1/feed/media/${mediaUrl}`;
}

/** Thời gian tương đối kiểu "vừa xong / 5 phút trước / 2 giờ trước / 3 ngày trước". */
export function timeAgo(iso: string, now: number = Date.now()): string {
  const diff = now - new Date(iso).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'vừa xong';
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days} ngày trước`;
  return new Date(iso).toLocaleDateString('vi-VN');
}
