package com.workoutsmart.social.service;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.social.dto.ChallengeResponse;
import com.workoutsmart.social.dto.ChallengeResultResponse;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ xã hội — spec 003-social-community. */
@Service
public class SocialService {

    private static final int REJECT_COOLDOWN_DAYS = 30;
    private static final int MAX_FRIENDS = 500;
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final ActivityFeedRepository feedRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;

    public SocialService(UserRepository userRepository,
                         FriendshipRepository friendshipRepository,
                         ActivityFeedRepository feedRepository,
                         LeaderboardRepository leaderboardRepository,
                         ChallengeRepository challengeRepository,
                         ChallengeParticipantRepository participantRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.feedRepository = feedRepository;
        this.leaderboardRepository = leaderboardRepository;
        this.challengeRepository = challengeRepository;
        this.participantRepository = participantRepository;
    }

    // ---------- Search ----------

    /** Tìm user theo email/displayName; email chỉ lộ cho bạn bè (privacy); kèm trạng thái quan hệ (FR-004). */
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
        List<User> limited = results.stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE) // FR-VISIBILITY
                .filter(u -> !u.getId().equals(userId))
                .limit(20)
                .toList();

        // 1 query batch lấy trạng thái quan hệ (R12 — tránh N+1)
        Map<Long, Friendship> rels = friendshipRepository
                .findWithAnyOf(userId, limited.stream().map(User::getId).toList()).stream()
                .collect(Collectors.toMap(
                        f -> f.getUserId1().equals(userId) ? f.getUserId2() : f.getUserId1(),
                        Function.identity(),
                        (a, b) -> a));

        return limited.stream()
                .map(u -> {
                    Friendship f = rels.get(u.getId());
                    boolean friend = f != null && "accepted".equals(f.getStatus());
                    // FR-013/014: private profile — ẩn email nếu viewer không phải bạn; badge "private"
                    boolean privateProfile = u.isPrivate() && !friend;
                    return new UserSearchResponse(u.getId(), u.getDisplayName(), u.getAvatarUrl(),
                            privateProfile ? null : (friend ? u.getEmail() : null),
                            privateProfile ? "private" : relationshipStatus(userId, f),
                            f == null ? null : f.getId());
                })
                .toList();
    }

    // ---------- Friendship ----------

    /** FR-001/002/003: gửi lời mời (cap 500 bạn, reuse row sau cooldown, chéo → auto-accept, không giới hạn/ngày). */
    @Transactional
    public FriendshipResponse sendRequest(Long userId, FriendshipRequest request) {
        Long targetId = request.targetUserId();
        if (targetId.equals(userId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Không thể kết bạn với chính mình");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        // Cap 500 bạn (FR-003) — chặn khi một trong hai bên đã đủ
        if (friendshipRepository.countAcceptedFor(userId) >= MAX_FRIENDS
                || friendshipRepository.countAcceptedFor(targetId) >= MAX_FRIENDS) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Đã đạt giới hạn 500 bạn bè");
        }

        // Quan hệ hiện có (chỉ bản ghi hoạt động — sau V20 tối đa 1/pair)
        List<Friendship> existing = friendshipRepository.findBetween(userId, targetId);
        if (!existing.isEmpty()) {
            Friendship f = existing.get(0);
            if ("accepted".equals(f.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, "Đã là bạn bè");
            }
            if ("pending".equals(f.getStatus())) {
                if (f.getInitiatedBy().equals(userId)) {
                    throw new ApiException(HttpStatus.CONFLICT, "Lời mời đang chờ phản hồi");
                }
                // Đối phương đã gửi → auto accept (FR-001, Clarifications Q1)
                f.setStatus("accepted");
                friendshipRepository.save(f);
                addFeed(f.getUserId1(), f.getUserId2(), "friendship_created");
                return toFriendshipResponse(f, userId, target);
            }
            // rejected → cooldown 30 ngày; hết hạn thì TÁI SỬ DỤNG row (FR-002 — không tạo mới)
            Instant since = Instant.now().minus(REJECT_COOLDOWN_DAYS, ChronoUnit.DAYS);
            if (f.getUpdatedAt() != null && f.getUpdatedAt().isAfter(since)) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "Đã bị từ chối gần đây, vui lòng thử lại sau");
            }
            f.setStatus("pending");
            f.setInitiatedBy(userId);
            friendshipRepository.save(f);
            return toFriendshipResponse(f, userId, target);
        }

        Friendship friendship = Friendship.builder()
                .userId1(userId)
                .userId2(targetId)
                .status("pending")
                .initiatedBy(userId)
                .build();
        try {
            friendshipRepository.saveAndFlush(friendship);
        } catch (DataIntegrityViolationException e) {
            // Race đồng thời (FR-001): đối phương tạo lời mời giữa lúc check và insert
            List<Friendship> raced = friendshipRepository.findBetween(userId, targetId);
            if (!raced.isEmpty()) {
                Friendship f = raced.get(0);
                if ("pending".equals(f.getStatus()) && !f.getInitiatedBy().equals(userId)) {
                    f.setStatus("accepted");
                    friendshipRepository.save(f);
                    addFeed(f.getUserId1(), f.getUserId2(), "friendship_created");
                    return toFriendshipResponse(f, userId, target);
                }
                throw new ApiException(HttpStatus.CONFLICT, "Lời mời đã tồn tại");
            }
            throw e;
        }
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

    /**
     * FR-004b: hủy kết bạn (accepted) — xóa quan hệ, ngừng hiển thị feed của nhau.
     * FR-WITHDRAW: rút lời mời pending do chính mình gửi — xóa row, không phát feed.
     */
    @Transactional
    public MessageResponse unfriend(Long userId, Long friendshipId) {
        Friendship f = requireFriendship(friendshipId);
        if ("pending".equals(f.getStatus())) {
            if (!f.getInitiatedBy().equals(userId)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Chỉ người gửi mới có thể rút lại lời mời");
            }
            friendshipRepository.delete(f);
            return new MessageResponse("Đã rút lại lời mời");
        }
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
        // FR-006: chỉ sự kiện của bạn bè trong 7 ngày gần nhất, tối đa 50, mới nhất trước
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        List<ActivityFeedItem> items = feedRepository
                .findTop50ByUserIdInAndCreatedAtAfterOrderByCreatedAtDesc(friendIds, since);
        if (items.isEmpty()) {
            return List.of();
        }
        // FR-VISIBILITY: loại tác giả BANNED/DELETED khỏi feed (batch 1 query — tránh N+1)
        Set<Long> authorIds = items.stream().map(ActivityFeedItem::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> users = userRepository.findAllById(authorIds).stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return items.stream()
                .filter(item -> users.containsKey(item.getUserId()))
                .map(item -> new FeedItemResponse(item.getId(), item.getUserId(),
                        users.get(item.getUserId()).getDisplayName(), item.getActionType(),
                        item.getDetailsJson(), item.getCreatedAt()))
                .toList();
    }

    // ---------- Leaderboard (kỳ thi vô tận) ----------

    /** FR-008: luôn trả về vị trí cá nhân (pin) kể cả ngoài top 100; user chưa có entry → unranked. */
    @Transactional
    public List<LeaderboardResponse> leaderboard(Long viewerId) {
        List<LeaderboardEntry> top100 = leaderboardRepository
                .findTop100ByOrderByCurrentStreakWeeksDescStreakStartWeekAscUserIdAsc();

        boolean viewerInTop100 = top100.stream().anyMatch(e -> e.getUserId().equals(viewerId));

        List<LeaderboardResponse> result = mapToResponse(top100);

        if (!viewerInTop100) {
            // Viewer ngoài top 100 → tính rank riêng + append
            LeaderboardEntry viewerEntry = leaderboardRepository.findByUserId(viewerId).orElse(null);
            User viewerUser = userRepository.findById(viewerId).orElse(null);
            if (viewerUser != null) {
                if (viewerEntry != null) {
                    int viewerRank = leaderboardRepository.findRankByStats(
                            viewerEntry.getCurrentStreakWeeks(),
                            viewerEntry.getStreakStartWeek(),
                            viewerId);
                    result.add(new LeaderboardResponse(viewerRank, viewerId, displayName(viewerUser),
                            viewerEntry.getCurrentStreakWeeks(), viewerEntry.getLongestStreakWeeks(),
                            viewerRank));
                } else {
                    // FR-008: chưa có bản ghi xếp hạng → ghim cuối bảng với "—" (rank 0 = unranked)
                    result.add(new LeaderboardResponse(0, viewerId, displayName(viewerUser),
                            0, 0, null));
                }
            }
        }

        return result;
    }

    /** Bảng xếp hạng nhóm bạn bè (bao gồm chính mình) — FR-007 (003) + FR-008 unranked. */
    @Transactional
    public List<LeaderboardResponse> friendsLeaderboard(Long userId) {
        List<Long> friendIds = friendshipRepository.findAcceptedFor(userId).stream()
                .map(f -> f.getUserId1().equals(userId) ? f.getUserId2() : f.getUserId1())
                .toList();
        List<Long> ids = new ArrayList<>();
        ids.add(userId);
        ids.addAll(friendIds);
        if (ids.isEmpty()) {
            return List.of();
        }

        // Batch load user — FR-VISIBILITY: chỉ ACTIVE, map id → user
        Map<Long, User> users = userRepository.findAllById(ids).stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                .collect(Collectors.toMap(User::getId, Function.identity()));
        if (users.isEmpty()) {
            return List.of();
        }
        Set<Long> activeIds = users.keySet();

        List<LeaderboardEntry> entries = leaderboardRepository.findAllById(activeIds).stream()
                .filter(e -> activeIds.contains(e.getUserId()))
                .sorted(Comparator
                        .comparingInt(LeaderboardEntry::getCurrentStreakWeeks).reversed()
                        .thenComparing(Comparator.comparing(LeaderboardEntry::getStreakStartWeek,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .thenComparingLong(LeaderboardEntry::getUserId))
                .toList();

        List<LeaderboardResponse> result = new ArrayList<>();
        int rank = 1;
        Set<Long> rankedIds = new java.util.HashSet<>();
        for (LeaderboardEntry e : entries) {
            User u = users.get(e.getUserId());
            if (u == null) continue;
            rankedIds.add(e.getUserId());
            result.add(new LeaderboardResponse(rank++, e.getUserId(), displayName(u),
                    e.getCurrentStreakWeeks(), e.getLongestStreakWeeks(), null));
        }

        // FR-008: bạn bè + chính mình chưa có entry → cuối bảng, hạng "—" (rank 0 = unranked)
        activeIds.stream().sorted().filter(id -> !rankedIds.contains(id)).forEach(id -> {
            result.add(new LeaderboardResponse(0, id, displayName(users.get(id)), 0, 0, null));
        });
        return result;
    }

    private List<LeaderboardResponse> mapToResponse(List<LeaderboardEntry> entries) {
        List<LeaderboardResponse> result = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry e : entries) {
            User u = userRepository.findById(e.getUserId()).orElse(null);
            if (u == null) continue;
            result.add(new LeaderboardResponse(rank++, e.getUserId(), displayName(u),
                    e.getCurrentStreakWeeks(), e.getLongestStreakWeeks(), null));
        }
        return result;
    }

    private String displayName(User u) {
        return u.getDisplayName() != null ? u.getDisplayName() : "Người dùng #" + u.getId();
    }

    // ---------- Challenge ----------

    public List<ChallengeResponse> challenges(Long userId) {
        List<Challenge> all = new java.util.ArrayList<>();
        all.addAll(challengeRepository.findByStatusOrderByStartDateAsc("open"));
        all.addAll(challengeRepository.findByStatusOrderByStartDateAsc("finished"));
        return all.stream()
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
        if (challenge.getEndDate() != null && LocalDate.now(ZONE).isAfter(challenge.getEndDate())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Thử thách đã kết thúc");
        }
        if (participantRepository.findByChallengeIdAndUserId(challengeId, userId).isPresent()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Bạn đã tham gia thử thách này");
        }
        try {
            participantRepository.save(ChallengeParticipant.builder()
                    .challengeId(challengeId)
                    .userId(userId)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Race đồng thời trùng (challenge_id, user_id) — unique index V24
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Bạn đã tham gia thử thách này");
        }
        return new MessageResponse("Đã tham gia thử thách");
    }

    /** Kết quả chung cuộc của challenge (FR-011). */
    public List<ChallengeResultResponse> results(Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Thử thách không tồn tại"));
        if (!"finished".equals(challenge.getStatus())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Thử thách chưa tổng kết");
        }
        return participantRepository.findByChallengeId(challengeId).stream()
                .filter(p -> p.getFinalRank() != null)
                .sorted(Comparator.comparingInt(ChallengeParticipant::getFinalRank))
                .map(p -> {
                    User u = userRepository.findById(p.getUserId()).orElse(null);
                    return new ChallengeResultResponse(
                            p.getUserId(),
                            u != null ? u.getDisplayName() : "Người dùng #" + p.getUserId(),
                            p.getFinalRank(),
                            0, // streak đã được lưu ý ở time finalize, không cần hiện lại
                            null);
                })
                .toList();
    }

    // ---------- helpers ----------

    private boolean isFriend(Long userId, Long otherId) {
        return friendshipRepository.findBetween(userId, otherId).stream()
                .anyMatch(f -> "accepted".equals(f.getStatus()));
    }

    private String relationshipStatus(Long viewerId, Friendship f) {
        if (f == null || "rejected".equals(f.getStatus())) {
            return "none";
        }
        if ("accepted".equals(f.getStatus())) {
            return "accepted";
        }
        return f.getInitiatedBy().equals(viewerId) ? "pending_sent" : "pending_received";
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
        int participantCount = participantRepository.findByChallengeId(c.getId()).size();

        // B1: "closed" là trạng thái DERIVED — open đã quá end_date hiển thị như closed
        // (job tổng kết sẽ chuyển finished trong ≤1 phút; không ghi closed vào DB).
        String status = c.getStatus();
        if ("open".equals(status) && c.getEndDate() != null
                && LocalDate.now(ZONE).isAfter(c.getEndDate())) {
            status = "closed";
        }

        // Tìm participant của viewer (nếu có) để lấy finalRank
        ChallengeParticipant viewerParticipant = participantRepository
                .findByChallengeIdAndUserId(c.getId(), viewerId).orElse(null);

        return new ChallengeResponse(c.getId(), c.getName(), c.getGoalType(), c.getDurationDays(),
                c.getStartDate(), c.getEndDate(), status, joined,
                participantCount,
                viewerParticipant != null ? viewerParticipant.getCompletedAt() : null,
                viewerParticipant != null ? viewerParticipant.getFinalRank() : null);
    }
}
