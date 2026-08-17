package com.workoutsmart.social.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.social.dto.FriendshipRequest;
import com.workoutsmart.social.dto.FriendshipResponse;
import com.workoutsmart.social.dto.MessageResponse;
import com.workoutsmart.social.entity.Challenge;
import com.workoutsmart.social.entity.Friendship;
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

    private User user(Long id, String name) {
        return User.builder().id(id).email("u" + id + "@e.c").displayName(name)
                .accountStatus(com.workoutsmart.auth.entity.AccountStatus.ACTIVE).build();
    }

    @Test
    void sendRequestToSelfReturns422() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(1L)));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void sendRequestToUnknownUserReturns404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(99L)));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void sendRequestWhenAlreadyFriendsReturns409() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "B")));
        Friendship existing = Friendship.builder()
                .id(1L).userId1(1L).userId2(2L).status("accepted").initiatedBy(1L).build();
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void sendRequestWhenMyPendingExistsReturns409() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "B")));
        Friendship pending = Friendship.builder()
                .id(1L).userId1(1L).userId2(2L).status("pending").initiatedBy(1L).build();
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.of(pending));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void crossRequestAutoAccepts() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "B")));
        // B đã gửi pending cho A (initiatedBy = B = 2)
        Friendship pending = Friendship.builder()
                .id(1L).userId1(2L).userId2(1L).status("pending").initiatedBy(2L).build();
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.of(pending));

        FriendshipResponse res = service.sendRequest(1L, new FriendshipRequest(2L));

        assertEquals("accepted", res.status());
        verify(friendshipRepository).save(pending);
        verify(feedRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void sendRequestAfterRecentRejectionReturns429() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "B")));
        Friendship rejected = Friendship.builder()
                .id(1L).userId1(1L).userId2(2L).status("rejected").initiatedBy(1L)
                .updatedAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .build();
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.of(rejected));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(429, ex.getStatus().value());
    }

    @Test
    void sendRequestExceedingDailyLimitReturns429() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "B")));
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());
        when(friendshipRepository.countByInitiatedByAndCreatedAtAfter(any(), any())).thenReturn(5L);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.sendRequest(1L, new FriendshipRequest(2L)));
        assertEquals(429, ex.getStatus().value());
        verify(friendshipRepository, never()).save(any(Friendship.class));
    }

    @Test
    void sendRequestHappyPathCreatesPending() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "B")));
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());
        when(friendshipRepository.countByInitiatedByAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendshipResponse res = service.sendRequest(1L, new FriendshipRequest(2L));

        assertEquals("pending", res.status());
    }

    @Test
    void acceptByWrongPersonReturns422() {
        Friendship f = Friendship.builder()
                .id(1L).userId1(1L).userId2(2L).status("pending").initiatedBy(1L).build();
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(f));

        ApiException ex = assertThrows(ApiException.class, () -> service.accept(1L, 1L));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void acceptHappyPath() {
        Friendship f = Friendship.builder()
                .id(1L).userId1(1L).userId2(2L).status("pending").initiatedBy(1L).build();
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(f));

        MessageResponse res = service.accept(2L, 1L);

        assertEquals("accepted", f.getStatus());
        assertNotNull(res.message());
    }

    @Test
    void unfriendNonAcceptedReturns422() {
        Friendship f = Friendship.builder()
                .id(1L).userId1(1L).userId2(2L).status("pending").initiatedBy(1L).build();
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(f));

        ApiException ex = assertThrows(ApiException.class, () -> service.unfriend(1L, 1L));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void unfriendHappyPathDeletes() {
        Friendship f = Friendship.builder()
                .id(1L).userId1(1L).userId2(2L).status("accepted").initiatedBy(1L).build();
        when(friendshipRepository.findById(1L)).thenReturn(Optional.of(f));

        service.unfriend(1L, 1L);
        verify(friendshipRepository).delete(f);
    }

    @Test
    void joinChallengeHappyPath() {
        Challenge c = Challenge.builder().id(1L).name("180 ngày").status("open").build();
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(c));
        when(participantRepository.findByChallengeIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        MessageResponse res = service.joinChallenge(1L, 1L);
        assertNotNull(res.message());
        verify(participantRepository).save(any());
    }

    @Test
    void joinChallengeAlreadyJoinedReturns422() {
        Challenge c = Challenge.builder().id(1L).name("180 ngày").status("open").build();
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(c));
        when(participantRepository.findByChallengeIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(com.workoutsmart.social.entity.ChallengeParticipant.builder()
                        .id(1L).challengeId(1L).userId(1L).build()));

        ApiException ex = assertThrows(ApiException.class, () -> service.joinChallenge(1L, 1L));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void joinClosedChallengeReturns422() {
        Challenge c = Challenge.builder().id(1L).name("Đã đóng").status("finished").build();
        when(challengeRepository.findById(1L)).thenReturn(Optional.of(c));

        ApiException ex = assertThrows(ApiException.class, () -> service.joinChallenge(1L, 1L));
        assertEquals(422, ex.getStatus().value());
    }

    @Test
    void searchUsersHidesEmailFromStrangers() {
        when(userRepository.findByEmailContainingIgnoreCase("an")).thenReturn(List.of(user(2L, "An")));
        when(userRepository.findByDisplayNameContainingIgnoreCase("an")).thenReturn(List.of());
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());

        var results = service.searchUsers(1L, "an");
        assertEquals(1, results.size());
        org.junit.jupiter.api.Assertions.assertNull(results.get(0).email());
    }

    @Test
    void searchUsersShowsEmailToFriends() {
        when(userRepository.findByEmailContainingIgnoreCase("an")).thenReturn(List.of(user(2L, "An")));
        when(userRepository.findByDisplayNameContainingIgnoreCase("an")).thenReturn(List.of());
        Friendship f = Friendship.builder().id(1L).userId1(1L).userId2(2L)
                .status("accepted").initiatedBy(1L).build();
        when(friendshipRepository.findBetween(1L, 2L)).thenReturn(Optional.of(f));

        var results = service.searchUsers(1L, "an");
        assertEquals("u2@e.c", results.get(0).email());
    }

    @Test
    void emptyQueryReturnsEmptyList() {
        assertEquals(List.of(), service.searchUsers(1L, ""));
    }
}
