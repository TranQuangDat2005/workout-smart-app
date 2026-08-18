import { http } from './http';

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
    http.get<UserSearchItem[]>(`/users/search?q=${encodeURIComponent(q)}`).then((r) => r.data),

  sendFriendRequest: (targetUserId: number) =>
    http.post<FriendItem>('/friendships', { targetUserId }).then((r) => r.data),

  pendingRequests: () => http.get<FriendItem[]>('/friendships/pending').then((r) => r.data),

  accept: (id: number) => http.post(`/friendships/${id}/accept`).then((r) => r.data),

  reject: (id: number) => http.post(`/friendships/${id}/reject`).then((r) => r.data),

  unfriend: (id: number) => http.delete(`/friendships/${id}`).then((r) => r.data),

  friends: () => http.get<FriendItem[]>('/friends').then((r) => r.data),

  feed: () => http.get<FeedItem[]>('/feed').then((r) => r.data),

  leaderboard: () => http.get<LeaderboardItem[]>('/leaderboard').then((r) => r.data),

  friendsLeaderboard: () => http.get<LeaderboardItem[]>('/leaderboard/friends').then((r) => r.data),

  challenges: () => http.get<Challenge[]>('/challenges').then((r) => r.data),

  myChallenges: () => http.get<Challenge[]>('/challenges/mine').then((r) => r.data),

  joinChallenge: (id: number) => http.post(`/challenges/${id}/join`).then((r) => r.data),
};
