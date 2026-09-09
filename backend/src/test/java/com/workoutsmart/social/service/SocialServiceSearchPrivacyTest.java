package com.workoutsmart.social.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.social.dto.UserSearchResponse;
import com.workoutsmart.social.entity.Friendship;
import com.workoutsmart.social.repository.ActivityFeedRepository;
import com.workoutsmart.social.repository.ChallengeParticipantRepository;
import com.workoutsmart.social.repository.ChallengeRepository;
import com.workoutsmart.social.repository.FriendshipRepository;
import com.workoutsmart.social.repository.LeaderboardRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialServiceSearchPrivacyTest {

    @Mock private UserRepository userRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private ActivityFeedRepository feedRepository;
    @Mock private LeaderboardRepository leaderboardRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private ChallengeParticipantRepository participantRepository;

    private SocialService service;

    @BeforeEach
    void setUp() {
        service = new SocialService(userRepository, friendshipRepository, feedRepository,
                leaderboardRepository, challengeRepository, participantRepository);
    }

    private User privateUser(long id, String name) {
        return User.builder().id(id).email("u" + id + "@e.c").role("user")
                .displayName(name).avatarUrl("http://img/" + id + ".png")
                .accountStatus(AccountStatus.ACTIVE).isPrivate(true).build();
    }

    private User publicUser(long id, String name) {
        return User.builder().id(id).email("u" + id + "@e.c").role("user")
                .displayName(name).avatarUrl("http://img/" + id + ".png")
                .accountStatus(AccountStatus.ACTIVE).isPrivate(false).build();
    }

    @Test
    void searchUsers_privateUser_stranger_seesNameAndAvatarOnly() {
        User me = publicUser(1L, "Me");
        User target = privateUser(2L, "Alice");

        when(userRepository.findByEmailContainingIgnoreCase("alice"))
                .thenReturn(List.of(target));
        when(userRepository.findByDisplayNameContainingIgnoreCase("alice"))
                .thenReturn(List.of());
        when(friendshipRepository.findWithAnyOf(1L, List.of(2L)))
                .thenReturn(List.of());

        List<UserSearchResponse> results = service.searchUsers(1L, "alice");

        assertEquals(1, results.size());
        assertEquals("Alice", results.get(0).displayName());
        assertEquals("http://img/2.png", results.get(0).avatarUrl());
        assertNull(results.get(0).email());
        assertEquals("private", results.get(0).relationshipStatus());
    }

    @Test
    void searchUsers_privateUser_friend_seesFullProfile() {
        User me = publicUser(1L, "Me");
        User target = privateUser(2L, "Alice");

        when(userRepository.findByEmailContainingIgnoreCase("alice"))
                .thenReturn(List.of(target));
        when(userRepository.findByDisplayNameContainingIgnoreCase("alice"))
                .thenReturn(List.of());

        Friendship f = Friendship.builder().id(10L).userId1(1L).userId2(2L)
                .status("accepted").initiatedBy(1L).build();
        when(friendshipRepository.findWithAnyOf(1L, List.of(2L)))
                .thenReturn(List.of(f));

        List<UserSearchResponse> results = service.searchUsers(1L, "alice");

        assertEquals(1, results.size());
        assertEquals("u2@e.c", results.get(0).email());
        assertEquals("accepted", results.get(0).relationshipStatus());
    }

    @Test
    void searchUsers_publicUser_stranger_seesNameAndAvatar() {
        User me = publicUser(1L, "Me");
        User target = publicUser(2L, "Bob");

        when(userRepository.findByEmailContainingIgnoreCase("bob"))
                .thenReturn(List.of());
        when(userRepository.findByDisplayNameContainingIgnoreCase("bob"))
                .thenReturn(List.of(target));
        when(friendshipRepository.findWithAnyOf(1L, List.of(2L)))
                .thenReturn(List.of());

        List<UserSearchResponse> results = service.searchUsers(1L, "bob");

        assertEquals(1, results.size());
        assertEquals("Bob", results.get(0).displayName());
        assertEquals("http://img/2.png", results.get(0).avatarUrl());
        assertNull(results.get(0).email());
        assertEquals("none", results.get(0).relationshipStatus());
    }
}
