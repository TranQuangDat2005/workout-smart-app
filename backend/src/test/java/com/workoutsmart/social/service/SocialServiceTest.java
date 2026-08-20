package com.workoutsmart.social.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
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

    @Test
    void leaderboardRanksByCurrentStreakAndExcludesInactive() {
        User strong = user(1L, "Mạnh", AccountStatus.ACTIVE);
        User weak = user(2L, "Yếu", AccountStatus.ACTIVE);
        User deleted = user(3L, "Đã xóa", AccountStatus.DELETED);
        when(userRepository.findAll()).thenReturn(List.of(strong, weak, deleted));

        // Mạnh: 3 buổi completed tuần này → streak 1; Yếu: 0 buổi → 0.
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
