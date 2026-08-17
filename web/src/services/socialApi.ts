import axios from 'axios';
import { tokenStorage } from './tokenStorage';

const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export interface UserSearchItem {
  id: number;
  displayName: string | null;
  avatarUrl: string | null;
  email: string | null;
}

export interface FriendItem {
  id: number;
  friendId: number;
  displayName: string | null;
  avatarUrl: string | null;
  status: string;
}

export interface FeedItem {
  id: number;
  friendId: number;
  displayName: string | null;
  actionType: string;
  detailsJson: string | null;
  createdAt: string;
}

export interface LeaderboardItem {
  rank: number;
  userId: number;
  displayName: string;
  currentStreakWeeks: number;
  longestStreakWeeks: number;
}

export interface Challenge {
  id: number;
  name: string;
  goalType: string | null;
  durationDays: number;
  startDate: string | null;
  endDate: string | null;
  status: string;
  joined: boolean;
}

export const socialApi = {
  searchUsers: (q: string) =>
    client.get<UserSearchItem[]>(`/users/search?q=${encodeURIComponent(q)}`).then((r) => r.data),

  sendFriendRequest: (targetUserId: number) =>
    client.post<FriendItem>('/friendships', { targetUserId }).then((r) => r.data),

  pendingRequests: () => client.get<FriendItem[]>('/friendships/pending').then((r) => r.data),

  accept: (id: number) => client.post(`/friendships/${id}/accept`).then((r) => r.data),

  reject: (id: number) => client.post(`/friendships/${id}/reject`).then((r) => r.data),

  unfriend: (id: number) => client.delete(`/friendships/${id}`).then((r) => r.data),

  friends: () => client.get<FriendItem[]>('/friends').then((r) => r.data),

  feed: () => client.get<FeedItem[]>('/feed').then((r) => r.data),

  leaderboard: () => client.get<LeaderboardItem[]>('/leaderboard').then((r) => r.data),

  challenges: () => client.get<Challenge[]>('/challenges').then((r) => r.data),

  myChallenges: () => client.get<Challenge[]>('/challenges/mine').then((r) => r.data),

  joinChallenge: (id: number) => client.post(`/challenges/${id}/join`).then((r) => r.data),
};
