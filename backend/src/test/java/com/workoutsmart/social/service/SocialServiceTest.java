package com.workoutsmart.social.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.social.dto.FriendshipRequest;
import com.workoutsmart.social.entity.Friendship;
import com.workoutsmart.social.entity.LeaderboardEntry;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import com.workoutsmart.social.repository.ChallengeParticipantRepository;
import com.workoutsmart.social.repository.ChallengeRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import com.workoutsmart.social.repository.LeaderboardRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SocialServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private ActivityFeedRepository feedRepository;
    @Mock
    private LeaderboardRepository leaderboardRepository;
    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private ChallengeParticipantRepository participantRepository;

    private SocialService service;

    @BeforeEach
    void setUp() {
        service = new SocialService(userRepository, friendshipRepository, feedRepository,
                leaderboardRepository, challengeRepository, participantRepository);
    }

    private User user(long id, String name, AccountStatus status) {
        return User.builder().id(id).email("u" + id + "@e.c").role("user")
                .displayName(name).accountStatus(status).isPrivate(false).build();
    }

    private Friendship friendship(Long id, Long u1, Long u2, String status, Long initiatedBy,
                                  Instant updatedAt) {
        Friendship f = Friendship.builder().id(id).userId1(u1).userId2(u2)
                .status(status).initiatedBy(initiatedBy).build();
        f.setCreatedAt(Instant.now());
        f.setUpdatedAt(updatedAt);
        return f;
    }

    // ---------- US1: sendRequest (T009) ----------

    @Test
    void sendRequest_rejectsSelfFriendship() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(1L)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void sendRequest_conflictsWhenAlreadyFriends() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(any())).thenReturn(0L);
        when(friendshipRepository.findBetween(1L, 2L))
                .thenReturn(List.of(friendship(9L, 1L, 2L, "accepted", 1L, Instant.now())));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void sendRequest_conflictsWhenOwnPendingRequestExists() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(any())).thenReturn(0L);
        when(friendshipRepository.findBetween(1L, 2L))
                .thenReturn(List.of(friendship(9L, 1L, 2L, "pending", 1L, Instant.now())));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void sendRequest_autoAcceptsCrossInvite() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(any())).thenReturn(0L);
        Friendship pendingFromOther = friendship(9L, 2L, 1L, "pending", 2L, Instant.now());
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(List.of(pendingFromOther));

        var res = service.sendRequest(1L, new FriendshipRequest(2L));

        assertEquals("accepted", res.status());
        assertEquals("accepted", pendingFromOther.getStatus());
        verify(friendshipRepository).save(pendingFromOther);
        verify(feedRepository, times(2)).save(any());
    }

    @Test
    void sendRequest_blocksWithinRejectCooldown() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(any())).thenReturn(0L);
        when(friendshipRepository.findBetween(1L, 2L))
                .thenReturn(List.of(friendship(9L, 1L, 2L, "rejected", 1L, Instant.now())));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    void sendRequest_reusesRejectedRowAfterCooldown() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(any())).thenReturn(0L);
        Friendship old = friendship(9L, 1L, 2L, "rejected", 1L,
                Instant.now().minus(31, ChronoUnit.DAYS));
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(List.of(old));

        var res = service.sendRequest(1L, new FriendshipRequest(2L));

        assertEquals("pending", res.status());
        assertEquals("pending", old.getStatus());
        assertEquals(1L, old.getInitiatedBy());
        verify(friendshipRepository).save(old);
        verify(friendshipRepository, never()).saveAndFlush(any());
    }

    @Test
    void sendRequest_blocksWhenSenderAtFriendCap() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(1L)).thenReturn(500L);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void sendRequest_blocksWhenTargetAtFriendCap() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(1L)).thenReturn(0L);
        when(friendshipRepository.countAcceptedFor(2L)).thenReturn(500L);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus());
    }

    @Test
    void sendRequest_rateLimitsAtFivePerDay() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(any())).thenReturn(0L);
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(List.of());
        when(friendshipRepository.countByInitiatedByAndCreatedAtAfter(eq(1L), any()))
                .thenReturn(5L);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    void sendRequest_createsPendingRequest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "Bình", AccountStatus.ACTIVE)));
        when(friendshipRepository.countAcceptedFor(any())).thenReturn(0L);
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(List.of());
        when(friendshipRepository.countByInitiatedByAndCreatedAtAfter(eq(1L), any()))
                .thenReturn(0L);
        when(friendshipRepository.saveAndFlush(any())).thenAnswer(inv -> {
            Friendship f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });

        var res = service.sendRequest(1L, new FriendshipRequest(2L));

        assertEquals("pending", res.status());
        assertEquals("Bình", res.displayName());
        verify(friendshipRepository).saveAndFlush(any());
    }

    // ---------- US1: searchUsers (T009) ----------

    @Test
    void searchUsers_returnsRelationshipStatusBatch() {
        User b = user(2L, "Bình", AccountStatus.ACTIVE);
        b.setEmail("binh@e.c");
        User c = user(3L, "Châu", AccountStatus.ACTIVE);
        c.setEmail("chau@e.c");
        when(userRepository.findByEmailContainingIgnoreCase("c")).thenReturn(List.of());
        when(userRepository.findByDisplayNameContainingIgnoreCase("c")).thenReturn(List.of(b, c));
        when(friendshipRepository.findWithAnyOf(1L, List.of(2L, 3L)))
                .thenReturn(List.of(
                        friendship(9L, 1L, 2L, "pending", 1L, Instant.now()),
                        friendship(10L, 3L, 1L, "accepted", 3L, Instant.now())));

        var results = service.searchUsers(1L, "c");

        assertEquals(2, results.size());
        var bResult = results.stream().filter(r -> r.id().equals(2L)).findFirst().orElseThrow();
        assertEquals("pending_sent", bResult.relationshipStatus());
        assertNull(bResult.email());
        var cResult = results.stream().filter(r -> r.id().equals(3L)).findFirst().orElseThrow();
        assertEquals("accepted", cResult.relationshipStatus());
        assertEquals("chau@e.c", cResult.email());
    }

    @Test
    void searchUsers_returnsNoneForStrangers() {
        User b = user(2L, "Bình", AccountStatus.ACTIVE);
        when(userRepository.findByEmailContainingIgnoreCase("b")).thenReturn(List.of());
        when(userRepository.findByDisplayNameContainingIgnoreCase("b")).thenReturn(List.of(b));
        when(friendshipRepository.findWithAnyOf(1L, List.of(2L))).thenReturn(List.of());

        var results = service.searchUsers(1L, "b");

        assertEquals(1, results.size());
        assertEquals("none", results.get(0).relationshipStatus());
        assertNull(results.get(0).email());
    }

    // ---------- Leaderboard (US3 — đọc từ leaderboard_entries, FR-007 tie-break) ----------

    @Test
    void leaderboard_readsFromEntries_andRanksByStreak() {
        User strong = user(1L, "Mạnh", AccountStatus.ACTIVE);
        User weak = user(2L, "Yếu", AccountStatus.ACTIVE);
        User deleted = user(3L, "Đã xóa", AccountStatus.DELETED);

        LeaderboardEntry entryStrong = LeaderboardEntry.builder()
                .userId(1L).currentStreakWeeks(5).longestStreakWeeks(8)
                .streakStartWeek(LocalDate.of(2026, 7, 6)).build();
        LeaderboardEntry entryWeak = LeaderboardEntry.builder()
                .userId(2L).currentStreakWeeks(2).longestStreakWeeks(3)
                .streakStartWeek(LocalDate.of(2026, 8, 3)).build();

        when(leaderboardRepository.findTop100ByOrderByCurrentStreakWeeksDescStreakStartWeekAscUserIdAsc())
                .thenReturn(List.of(entryStrong, entryWeak));
        when(leaderboardRepository.findByUserId(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(strong));
        when(userRepository.findById(2L)).thenReturn(Optional.of(weak));

        var board = service.leaderboard(99L); // viewer ngoài top

        assertEquals(2, board.size());
        assertEquals(1, board.get(0).rank());
        assertEquals(1L, board.get(0).userId());
        assertEquals(5, board.get(0).currentStreakWeeks());
        assertEquals(2, board.get(1).rank());
        assertEquals(2L, board.get(1).userId());
        assertEquals(2, board.get(1).currentStreakWeeks());
    }

    @Test
    void leaderboard_tieBreakByStreakStartWeek() {
        User a = user(1L, "An", AccountStatus.ACTIVE);
        User b = user(2L, "Bình", AccountStatus.ACTIVE);

        // Cả 2 cùng streak 3, nhưng A bắt đầu chuỗi sớm hơn (7/7 vs 14/7)
        LeaderboardEntry entryA = LeaderboardEntry.builder()
                .userId(1L).currentStreakWeeks(3).longestStreakWeeks(3)
                .streakStartWeek(LocalDate.of(2026, 7, 7)).build();
        LeaderboardEntry entryB = LeaderboardEntry.builder()
                .userId(2L).currentStreakWeeks(3).longestStreakWeeks(3)
                .streakStartWeek(LocalDate.of(2026, 7, 14)).build();

        when(leaderboardRepository.findTop100ByOrderByCurrentStreakWeeksDescStreakStartWeekAscUserIdAsc())
                .thenReturn(List.of(entryA, entryB));
        when(leaderboardRepository.findByUserId(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(a));
        when(userRepository.findById(2L)).thenReturn(Optional.of(b));

        var board = service.leaderboard(99L);

        assertEquals(1L, board.get(0).userId());
        assertEquals(2L, board.get(1).userId());
    }

    @Test
    void leaderboard_viewerOutsideTop100_pinned() {
        User viewer = user(1L, "Viewer", AccountStatus.ACTIVE);
        User top = user(2L, "Top", AccountStatus.ACTIVE);

        LeaderboardEntry entryTop = LeaderboardEntry.builder()
                .userId(2L).currentStreakWeeks(10).longestStreakWeeks(10)
                .streakStartWeek(LocalDate.of(2026, 5, 4)).build();
        LeaderboardEntry entryViewer = LeaderboardEntry.builder()
                .userId(1L).currentStreakWeeks(1).longestStreakWeeks(1)
                .streakStartWeek(LocalDate.of(2026, 8, 17)).build();

        when(leaderboardRepository.findTop100ByOrderByCurrentStreakWeeksDescStreakStartWeekAscUserIdAsc())
                .thenReturn(List.of(entryTop));
        when(leaderboardRepository.findByUserId(1L)).thenReturn(Optional.of(entryViewer));
        when(leaderboardRepository.findRankByStats(1, LocalDate.of(2026, 8, 17), 1L)).thenReturn(51);
        when(userRepository.findById(1L)).thenReturn(Optional.of(viewer));
        when(userRepository.findById(2L)).thenReturn(Optional.of(top));

        var board = service.leaderboard(1L);

        // Top 1 + viewer pinned
        assertEquals(2, board.size());
        assertEquals(2L, board.get(0).userId());
        assertEquals(1, board.get(0).rank());
        // Viewer appended with viewerRank
        assertEquals(1L, board.get(1).userId());
        assertEquals(51, board.get(1).viewerRank());
    }
}
