package com.workoutsmart.social.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.social.entity.Challenge;
import com.workoutsmart.social.entity.ChallengeParticipant;
import com.workoutsmart.social.repository.ChallengeParticipantRepository;
import com.workoutsmart.social.repository.ChallengeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChallengeFinalizerTest {

    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private ChallengeParticipantRepository participantRepository;
    @Mock
    private WorkoutSessionRepository sessionRepository;

    private ChallengeFinalizer finalizer;

    @BeforeEach
    void setUp() {
        finalizer = new ChallengeFinalizer(challengeRepository, participantRepository, sessionRepository);
    }

    private Challenge challenge(long id, String status, LocalDate endDate) {
        return Challenge.builder()
                .id(id).name("C" + id).durationDays(7)
                .startDate(endDate.minusDays(7)).endDate(endDate)
                .status(status).createdBy(1L).build();
    }

    private ChallengeParticipant participant(long id, Long challengeId, Long userId) {
        return ChallengeParticipant.builder()
                .id(id).challengeId(challengeId).userId(userId).build();
    }

    /** Helper: tạo 3 buổi completed trong tuần này (streak=1). */
    private void stubSessionsThisWeek(Long userId) {
        Instant now = Instant.now();
        List<WorkoutSession> sessions = List.of(
                WorkoutSession.builder().userId(userId).status("completed")
                        .startTime(now.minus(0, ChronoUnit.HOURS)).build(),
                WorkoutSession.builder().userId(userId).status("completed")
                        .startTime(now.minus(1, ChronoUnit.HOURS)).build(),
                WorkoutSession.builder().userId(userId).status("completed")
                        .startTime(now.minus(2, ChronoUnit.HOURS)).build());
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(sessions);
    }

    @Test
    void finalize_transitionsToFinished() {
        Challenge c = challenge(1L, "open", LocalDate.now().minusDays(1));
        when(challengeRepository.findByStatus("open")).thenReturn(List.of(c));
        when(participantRepository.findByChallengeId(1L)).thenReturn(List.of());

        finalizer.run();

        assertEquals("finished", c.getStatus());
        verify(challengeRepository).save(c);
    }

    @Test
    void finalize_skipsNotYetExpired() {
        Challenge c = challenge(1L, "open", LocalDate.now().plusDays(5));
        when(challengeRepository.findByStatus("open")).thenReturn(List.of(c));

        finalizer.run();

        assertEquals("open", c.getStatus());
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void finalize_setsFinalRankPerStreak() {
        Challenge c = challenge(1L, "open", LocalDate.now().minusDays(1));
        ChallengeParticipant pA = participant(10L, 1L, 1L);
        ChallengeParticipant pB = participant(11L, 1L, 2L);
        ChallengeParticipant pC = participant(12L, 1L, 3L);

        when(challengeRepository.findByStatus("open")).thenReturn(List.of(c));
        when(participantRepository.findByChallengeId(1L)).thenReturn(List.of(pA, pB, pC));

        // A: 6 sessions → longest streak, B: 3 → streak=1, C: 0 → streak=0
        stubSessions(1L, 6);
        stubSessions(2L, 3);
        stubSessions(3L, 0);

        finalizer.run();

        assertEquals("finished", c.getStatus());
        // A: 6 sessions this week → streak = 1 (1 week with ≥3 sessions)
        assertEquals(1, pA.getFinalRank());
        // B: 3 sessions → streak = 1, tie-break: userId 2 > 1 → rank 2
        assertEquals(2, pB.getFinalRank());
        // C: 0 sessions → streak = 0 → rank 3
        assertEquals(3, pC.getFinalRank());
        assertNotNull(pA.getCompletedAt());
    }

    @Test
    void finalize_tieBreakByStreakStartWeek() {
        Challenge c = challenge(1L, "open", LocalDate.now().minusDays(1));
        ChallengeParticipant pA = participant(10L, 1L, 1L);
        ChallengeParticipant pB = participant(11L, 1L, 2L);

        when(challengeRepository.findByStatus("open")).thenReturn(List.of(c));
        when(participantRepository.findByChallengeId(1L)).thenReturn(List.of(pA, pB));

        // Both have 3 sessions this week (streak=1, same start week)
        // Tie-break goes to userId ASC → A first
        stubSessionsThisWeek(1L);
        stubSessionsThisWeek(2L);

        finalizer.run();

        assertEquals(1, pA.getFinalRank());
        assertEquals(2, pB.getFinalRank());
    }

    @Test
    void finalize_tieBreakByUserId() {
        Challenge c = challenge(1L, "open", LocalDate.now().minusDays(1));
        ChallengeParticipant pA = participant(10L, 1L, 1L);
        ChallengeParticipant pB = participant(11L, 1L, 2L);

        when(challengeRepository.findByStatus("open")).thenReturn(List.of(c));
        when(participantRepository.findByChallengeId(1L)).thenReturn(List.of(pA, pB));

        // Both 3 sessions this week, same streak, same startWeek → userId ASC
        stubSessionsThisWeek(1L);
        stubSessionsThisWeek(2L);

        finalizer.run();

        assertEquals(1, pA.getFinalRank());
        assertEquals(2, pB.getFinalRank());
    }

    @Test
    void finalize_idempotent() {
        Challenge c = challenge(1L, "finished", LocalDate.now().minusDays(1));
        when(challengeRepository.findByStatus("open")).thenReturn(List.of());

        // Already finished → not processed again
        finalizer.run();

        verify(challengeRepository, never()).save(any());
    }

    @Test
    void finalize_handlesEmptyParticipants() {
        Challenge c = challenge(1L, "open", LocalDate.now().minusDays(1));
        when(challengeRepository.findByStatus("open")).thenReturn(List.of(c));
        when(participantRepository.findByChallengeId(1L)).thenReturn(List.of());

        finalizer.run();

        assertEquals("finished", c.getStatus());
        verify(challengeRepository).save(c);
    }

    @Test
    void finalize_wrapsPerChallengeInTryCatch() {
        Challenge c1 = challenge(1L, "open", LocalDate.now().minusDays(1));
        Challenge c2 = challenge(2L, "open", LocalDate.now().minusDays(1));
        when(challengeRepository.findByStatus("open")).thenReturn(List.of(c1, c2));

        // c1 throws, c2 should still be processed
        when(participantRepository.findByChallengeId(1L)).thenThrow(new RuntimeException("DB error"));
        when(participantRepository.findByChallengeId(2L)).thenReturn(List.of());

        finalizer.run();

        assertEquals("open", c1.getStatus()); // failed
        assertEquals("finished", c2.getStatus()); // succeeded
    }

    @Test
    void finalize_multipleChallenges() {
        Challenge c1 = challenge(1L, "open", LocalDate.now().minusDays(2));
        Challenge c2 = challenge(2L, "open", LocalDate.now().minusDays(1));
        when(challengeRepository.findByStatus("open")).thenReturn(List.of(c1, c2));
        when(participantRepository.findByChallengeId(1L)).thenReturn(List.of());
        when(participantRepository.findByChallengeId(2L)).thenReturn(List.of());

        finalizer.run();

        assertEquals("finished", c1.getStatus());
        assertEquals("finished", c2.getStatus());
    }

    /** Helper: tạo n sessions completed trong tuần này. */
    private void stubSessions(Long userId, int count) {
        Instant now = Instant.now();
        List<WorkoutSession> sessions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sessions.add(WorkoutSession.builder()
                    .userId(userId).status("completed")
                    .startTime(now.minus(i, ChronoUnit.HOURS)).build());
        }
        when(sessionRepository.findByUserIdAndStatus(userId, "completed")).thenReturn(sessions);
    }
}
