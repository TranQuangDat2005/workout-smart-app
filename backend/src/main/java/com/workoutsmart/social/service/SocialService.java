package com.workoutsmart.social.service;

import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.social.dto.ChallengeResponse;
import com.workoutsmart.social.dto.CreateChallengeRequest;
import com.workoutsmart.social.dto.FeedItemResponse;
import com.workoutsmart.social.dto.FriendshipRequest;
import com.workoutsmart.social.dto.FriendshipResponse;
import com.workoutsmart.social.dto.LeaderboardResponse;
import com.workoutsmart.social.dto.MessageResponse;
import com.workoutsmart.social.dto.UserSearchResponse;
import com.workoutsmart.social.entity.ActivityFeedItem;
import com.workoutsmart.social.entity.Challenge;
import com.workoutsmart.social.entity.ChallengeParticipant;
import com.workoutsmart.social.entity.Friendship;
import com.workoutsmart.social.entity.LeaderboardEntry;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import com.workoutsmart.social.repository.ChallengeParticipantRepository;
import com.workoutsmart.social.repository.ChallengeRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import com.workoutsmart.social.repository.LeaderboardRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ xã hội — spec 003-social-community. */
@Service
public class SocialService {

    private static final int MAX_REQUESTS_PER_DAY = 5;
    private static final int REJECT_COOLDOWN_DAYS = 30;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ActivityFeedRepository feedRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final WorkoutSessionRepository sessionRepository;

    public SocialService(UserRepository userRepository,
                         FriendshipRepository friendshipRepository,
                         ActivityFeedRepository feedRepository,
                         LeaderboardRepository leaderboardRepository,
                         ChallengeRepository challengeRepository,
                         ChallengeParticipantRepository participantRepository,
                         WorkoutSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.feedRepository = feedRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.challengeRepository = challengeRepository;
        this.participantRepository = participantRepository;
        this.sessionRepository = sessionRepository;
    }

    // ---------- Search ----------

    /** Tìm user theo email/displayName; email chỉ lộ cho bạn bè/chính mình (privacy). */
    public List<UserSearchResponse> searchUsers(Long userId, String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return List.of();
        }
        var byEmail = userRepository.findByEmailContainingIgnoreCase(q);
        var byName = userRepository.findByDisplayNameContainingIgnoreCase(q);
        var results = new ArrayList<User>();
        var seen = new java.util.HashSet<Long>();
        for (User u : byEmail) {
            if (seen.add(u.getId())) results.add(u);
        }
        for (User u : byName) {
            if (seen.add(u.getId())) results.add(u);
        }
        return results.stream()
                .filter(u -> !u.getId().equals(userId))
                .limit(20)
                .map(u -> new UserSearchResponse(u.getId(), u.getDisplayName(), u.getAvatarUrl(),
                        isFriend(userId, u.getId()) ? u.getEmail() : null))
                .toList();
    }

    // ---------- Friendship ----------

    /** FR-002: gửi lời mời (max 5/ngày, chặn pending, cooldown 30 ngày sau khi bị từ chối). */
    @Transactional
    public FriendshipResponse sendRequest(Long userId, FriendshipRequest request) {
        Long targetId = request.targetUserId();
        if (targetId.equals(userId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Không thể kết bạn với chính mình");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        // Lời mời chéo: đối phương đã gửi pending cho mình → accept luôn
        Optional<Friendship> existing = friendshipRepository.findBetween(userId, targetId);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if ("accepted".equals(f.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "Đã là bạn bè");
            }
            if ("pending".equals(f.getStatus())) {
                if (f.getInitiatedBy().equals(userId)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Lời mời đang chờ phản hồi");
                }
                // Đối phương đã gửi → auto accept (ai nhấn trước là người gửi)
                f.setStatus("accepted");
                friendshipRepository.save(f);
                addFeed(f.getUserId1(), f.getUserId2(), "friendship_created");
                return toFriendshipResponse(f, userId, target);
            }
            // rejected → cooldown check
            Instant since = Instant.now().minus(REJECT_COOLDOWN_DAYS, ChronoUnit.DAYS);
            if (f.getUpdatedAt() != null && f.getUpdatedAt().isAfter(since)) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Đã bị từ chối gần đây, vui lòng thử lại sau");
            }
        }

        Instant dayStart = Instant.now().atZone(ZONE).toLocalDate().atStartOfDay(ZONE).toInstant();
        if (friendshipRepository.countByInitiatedByAndCreatedAtAfter(userId, dayStart) >= MAX_REQUESTS_PER_DAY) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Đã đạt giới hạn 5 lời mời/ngày");
        }

        Friendship friendship = friendshipRepository.save(Friendship.builder()
                .userId1(userId)
                .userId2(targetId)
                .status("pending")
                .initiatedBy(userId)
                .build());
        return toFriendshipResponse(friendship, userId, target);
    }

    public List<FriendshipResponse> pendingRequests(Long userId) {
        return friendshipRepository.findPendingFor(userId).stream()
                .map(f -> {
                    User sender = userRepository.findById(f.getUserId1()).orElse(null);
                    return new FriendshipResponse(f.getId(), f.getUserId1(),
                            sender != null ? sender.getDisplayName() : "?",
                            sender != null ? sender.getAvatarUrl() : null, f.getStatus());
                })
                .toList();
    }

    @Transactional
    public MessageResponse accept(Long userId, Long friendshipId) {
        Friendship f = requireFriendship(friendshipId);
        if (!f.getUserId2().equals(userId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Chỉ người nhận mới được chấp nhận");
        }
        f.setStatus("accepted");
        friendshipRepository.save(f);
        addFeed(f.getUserId1(), f.getUserId2(), "friendship_created");
        return new MessageResponse("Đã chấp nhận kết bạn");
    }

    @Transactional
    public MessageResponse reject(Long userId, Long friendshipId) {
        Friendship f = requireFriendship(friendshipId);
        if (!f.getUserId2().equals(userId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Chỉ người nhận mới được từ chối");
        }
        f.setStatus("rejected");
        friendshipRepository.save(f);
        return new MessageResponse("Đã từ chối lời mời");
    }

    /** FR-004b: hủy kết bạn — xóa quan hệ, ngừng hiển thị feed của nhau. */
    @Transactional
    public MessageResponse unfriend(Long userId, Long friendshipId) {
        Friendship f = requireFriendship(friendshipId);
        if (!"accepted".equals(f.getStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Không có quan hệ bạn bè");
        }
        if (!f.getUserId1().equals(userId) && !f.getUserId2().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy quan hệ");
        }
        friendshipRepository.delete(f);
        return new MessageResponse("Đã hủy kết bạn");
    }

    public List<FriendshipResponse> friends(Long userId) {
        return friendshipRepository.findAcceptedFor(userId).stream()
                .map(f -> {
                    Long friendId = f.getUserId1().equals(userId) ? f.getUserId2() : f.getUserId1();
                    User friend = userRepository.findById(friendId).orElse(null);
                    return new FriendshipResponse(f.getId(), friendId,
                            friend != null ? friend.getDisplayName() : "?",
                            friend != null ? friend.getAvatarUrl() : null, "accepted");
                })
                .toList();
    }

    // ---------- Feed ----------

    public List<FeedItemResponse> feed(Long userId) {
        List<Long> friendIds = friendshipRepository.findAcceptedFor(userId).stream()
                .map(f -> f.getUserId1().equals(userId) ? f.getUserId2() : f.getUserId1())
                .toList();
        if (friendIds.isEmpty()) {
            return List.of();
        }
        return feedRepository.findTop50ByUserIdInOrderByCreatedAtDesc(friendIds).stream()
                .map(item -> {
                    User u = userRepository.findById(item.getUserId()).orElse(null);
                    return new FeedItemResponse(item.getId(), item.getUserId(),
                            u != null ? u.getDisplayName() : "?", item.getActionType(),
                            item.getDetailsJson(), item.getCreatedAt());
                })
                .toList();
    }

    // ---------- Leaderboard (kỳ thi vô tận) ----------

    @Transactional
    public List<LeaderboardResponse> leaderboard() {
        return computeLeaderboard(userRepository.findAll(), 100);
    }

    /** Bảng xếp hạng nhóm bạn bè (bao gồm chính mình) — FR-007 (003). */
    @Transactional
    public List<LeaderboardResponse> friendsLeaderboard(Long userId) {
        List<Long> friendIds = friendshipRepository.findAcceptedFor(userId).stream()
                .map(f -> f.getUserId1().equals(userId) ? f.getUserId2() : f.getUserId1())
                .toList();
        List<Long> ids = new ArrayList<>();
        ids.add(userId);
        ids.addAll(friendIds);
        return computeLeaderboard(userRepository.findAllById(ids), ids.size());
    }

    private List<LeaderboardResponse> computeLeaderboard(List<User> users, int limit) {
        for (User user : users) {
            List<Instant> starts = sessionRepository
                    .findByUserIdOrderByStartTimeDesc(user.getId(), org.springframework.data.domain.PageRequest.of(0, 1000))
                    .stream()
                    .filter(s -> "completed".equals(s.getStatus()))
                    .map(WorkoutSession::getStartTime)
                    .toList();
            StreakCalculator.StreakResult streak = new StreakCalculator().calculate(starts, ZONE);
            LeaderboardEntry entry = leaderboardRepository.findByUserId(user.getId()).orElseGet(() ->
                    LeaderboardEntry.builder().userId(user.getId()).build());
            entry.setCurrentStreakWeeks(streak.currentStreakWeeks());
            entry.setLongestStreakWeeks(streak.longestStreakWeeks());
            leaderboardRepository.save(entry);
        }

        List<LeaderboardEntry> entries = users.stream()
                .map(u -> leaderboardRepository.findByUserId(u.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator
                        .comparingInt(LeaderboardEntry::getCurrentStreakWeeks).reversed()
                        .thenComparing(Comparator.comparingInt(LeaderboardEntry::getLongestStreakWeeks).reversed())
                        .thenComparingLong(LeaderboardEntry::getUserId))
                .limit(limit)
                .toList();

        List<LeaderboardResponse> result = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry e : entries) {
            User u = userRepository.findById(e.getUserId()).orElse(null);
            if (u == null) continue;
            result.add(new LeaderboardResponse(rank++, e.getUserId(),
                    u.getDisplayName() != null ? u.getDisplayName() : "Người dùng #" + u.getId(),
                    e.getCurrentStreakWeeks(), e.getLongestStreakWeeks()));
        }
        return result;
    }

    // ---------- Challenge ----------

    public List<ChallengeResponse> challenges(Long userId) {
        return challengeRepository.findByStatusOrderByStartDateAsc("open").stream()
                .map(c -> toChallengeResponse(c, userId))
                .toList();
    }

    public List<ChallengeResponse> myChallenges(Long userId) {
        return participantRepository.findByUserId(userId).stream()
                .map(p -> challengeRepository.findById(p.getChallengeId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(c -> toChallengeResponse(c, userId))
                .toList();
    }

    @Transactional
    public ChallengeResponse createChallenge(Long adminId, CreateChallengeRequest request) {
        LocalDate start = request.startDate() != null ? request.startDate() : LocalDate.now(ZONE);
        Challenge challenge = challengeRepository.save(Challenge.builder()
                .name(request.name())
                .goalType(request.goalType())
                .durationDays(request.durationDays())
                .startDate(start)
                .endDate(start.plusDays(request.durationDays()))
                .status("open")
                .createdBy(adminId)
                .build());
        return toChallengeResponse(challenge, adminId);
    }

    @Transactional
    public MessageResponse joinChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Thử thách không tồn tại"));
        if (!"open".equals(challenge.getStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Thử thách không còn mở đăng ký");
        }
        if (participantRepository.findByChallengeIdAndUserId(challengeId, userId).isPresent()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Bạn đã tham gia thử thách này");
        }
        participantRepository.save(ChallengeParticipant.builder()
                .challengeId(challengeId)
                .userId(userId)
                .build());
        return new MessageResponse("Đã tham gia thử thách");
    }

    // ---------- helpers ----------

    private boolean isFriend(Long userId, Long otherId) {
        return friendshipRepository.findBetween(userId, otherId)
                .map(f -> "accepted".equals(f.getStatus()))
                .orElse(false);
    }

    private Friendship requireFriendship(Long id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy lời mời kết bạn"));
    }

    private FriendshipResponse toFriendshipResponse(Friendship f, Long viewerId, User target) {
        return new FriendshipResponse(f.getId(), target.getId(), target.getDisplayName(),
                target.getAvatarUrl(), f.getStatus());
    }

    private void addFeed(Long userA, Long userB, String actionType) {
        feedRepository.save(ActivityFeedItem.builder()
                .userId(userA).actionType(actionType)
                .detailsJson("{\"friendId\":" + userB + "}")
                .build());
        feedRepository.save(ActivityFeedItem.builder()
                .userId(userB).actionType(actionType)
                .detailsJson("{\"friendId\":" + userA + "}")
                .build());
    }

    private ChallengeResponse toChallengeResponse(Challenge c, Long viewerId) {
        boolean joined = participantRepository.findByChallengeIdAndUserId(c.getId(), viewerId).isPresent();
        return new ChallengeResponse(c.getId(), c.getName(), c.getGoalType(), c.getDurationDays(),
                c.getStartDate(), c.getEndDate(), c.getStatus(), joined);
    }
}
