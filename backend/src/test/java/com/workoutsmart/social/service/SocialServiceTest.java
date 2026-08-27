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
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.social.dto.FriendshipRequest;
import com.workoutsmart.social.entity.Friendship;
import com.workoutsmart.social.entity.LeaderboardEntry;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import com.workoutsmart.social.repository.ChallengeParticipantRepository;
import com.workoutsmart.social.repository.ChallengeRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import com.workoutsmart.social.repository.LeaderboardRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
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
    @Mock
    private WorkoutSessionRepository sessionRepository;

    private SocialService service;

    @BeforeEach
    void setUp() {
        service = new SocialService(userRepository, friendshipRepository, feedRepository,
                leaderboardRepository, challengeRepository, participantRepository, sessionRepository);
    }

    private User user(long id, String name, AccountStatus status) {
        return User.builder().id(id).email("u" + id + "@e.c").role("user")
                .displayName(name).accountStatus(status).build();
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

    // ---------- Leaderboard (giữ nguyên từ baseline) ----------

    @Test
    void leaderboardRanksByCurrentStreakAndExcludesInactive() {
        User strong = user(1L, "Mạnh", AccountStatus.ACTIVE);
        User weak = user(2L, "Yếu", AccountStatus.ACTIVE);
        User deleted = user(3L, "Đã xóa", AccountStatus.DELETED);
        when(userRepository.findAll()).thenReturn(List.of(strong, weak, deleted));

        List<WorkoutSession> strongSessions = List.of(
                WorkoutSession.builder().id(11L).userId(1L).status("completed")
                        .startTime(Instant.now().minus(1, ChronoUnit.HOURS)).build(),
                WorkoutSession.builder().id(12L).userId(1L).status("completed")
                        .startTime(Instant.now().minus(2, ChronoUnit.HOURS)).build(),
                WorkoutSession.builder().id(13L).userId(1L).status("completed")
                        .startTime(Instant.now().minus(3, ChronoUnit.HOURS)).build());
        when(sessionRepository.findByUserIdOrderByStartTimeDesc(eq(1L), any()))
                .thenReturn(new PageImpl<>(strongSessions));
        when(sessionRepository.findByUserIdOrderByStartTimeDesc(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(leaderboardRepository.findByUserId(1L))
                .thenReturn(Optional.of(LeaderboardEntry.builder().userId(1L).build()));
        when(leaderboardRepository.findByUserId(2L))
                .thenReturn(Optional.of(LeaderboardEntry.builder().userId(2L).build()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(strong));
        when(userRepository.findById(2L)).thenReturn(Optional.of(weak));

        var board = service.leaderboard();

        assertEquals(2, board.size());
        assertEquals(1, board.get(0).rank());
        assertEquals(1L, board.get(0).userId());
        assertEquals(1, board.get(0).currentStreakWeeks());
        assertEquals(2, board.get(1).rank());
        assertEquals(2L, board.get(1).userId());
        assertEquals(0, board.get(1).currentStreakWeeks());
    }
}
