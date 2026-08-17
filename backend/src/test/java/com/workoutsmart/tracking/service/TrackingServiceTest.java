package com.workoutsmart.tracking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.tracking.dto.RecordSetRequest;
import com.workoutsmart.tracking.dto.SessionResponse;
import com.workoutsmart.tracking.dto.SetResponse;
import com.workoutsmart.tracking.dto.SyncRequest;
import com.workoutsmart.tracking.dto.SyncResponse;
import com.workoutsmart.tracking.dto.SyncSessionRequest;
import com.workoutsmart.tracking.dto.SyncSetRequest;
import java.math.BigDecimal;
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
class TrackingServiceTest {

    @Mock
    private WorkoutSessionRepository sessionRepository;
    @Mock
    private WorkoutSetRepository setRepository;

    private TrackingService service;

    @BeforeEach
    void setUp() {
        service = new TrackingService(sessionRepository, setRepository);
    }

    private WorkoutSession activeSession() {
        return WorkoutSession.builder().id(10L).userId(1L).status("active").build();
    }

    @Test
    void startSessionCreatesActiveWhenNoActive() {
        when(sessionRepository.findByUserIdAndStatus(1L, "active")).thenReturn(List.of());
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(i -> i.getArgument(0));

        SessionResponse res = service.startSession(1L, null, null);

        assertEquals("active", res.status());
    }

    @Test
    void startSessionThrows409WhenActiveExists() {
        when(sessionRepository.findByUserIdAndStatus(1L, "active"))
                .thenReturn(List.of(activeSession()));

        ApiException ex = assertThrows(ApiException.class, () -> service.startSession(1L, null, null));

        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void recordSetInsertsNewSet() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(activeSession()));
        when(setRepository.findBySessionIdAndSetNumber(10L, 1)).thenReturn(Optional.empty());
        when(setRepository.save(any(WorkoutSet.class))).thenAnswer(i -> i.getArgument(0));

        SetResponse res = service.recordSet(1L, 10L,
                new RecordSetRequest(null, 1, 10, BigDecimal.valueOf(50), 60));

        assertEquals(1, res.setNumber());
        assertEquals(10, res.repsCompleted());
    }

    @Test
    void recordSetUpsertsExistingSet() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(activeSession()));
        when(setRepository.findBySessionIdAndSetNumber(10L, 1)).thenReturn(Optional.of(
                WorkoutSet.builder().id(5L).sessionId(10L).setNumber(1).repsCompleted(5).build()));
        when(setRepository.save(any(WorkoutSet.class))).thenAnswer(i -> i.getArgument(0));

        SetResponse res = service.recordSet(1L, 10L,
                new RecordSetRequest(null, 1, 12, null, null));

        assertEquals(12, res.repsCompleted());
    }

    @Test
    void recordSetRejectsNonOwner() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(
                WorkoutSession.builder().id(10L).userId(2L).status("active").build()));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.recordSet(1L, 10L, new RecordSetRequest(null, 1, 10, null, null)));

        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void incrementFocusIncrements() {
        WorkoutSession session = activeSession();
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(i -> i.getArgument(0));

        SessionResponse res = service.incrementFocus(1L, 10L);

        assertEquals(1, res.focusInterruptionsCount());
    }

    @Test
    void syncRejectsExpiredSession() {
        WorkoutSession expired = WorkoutSession.builder().id(10L).userId(1L).status("expired")
                .startTime(Instant.now().minus(2, ChronoUnit.DAYS)).build();
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(expired));

        SyncResponse res = service.sync(1L, new SyncRequest(List.of(new SyncSessionRequest(10L, null, null, List.of(
                new SyncSetRequest(null, 1, 10, null, null, Instant.now()))))));

        assertEquals(0, res.acceptedSets());
        assertEquals(1, res.rejectedSetIds().size());
    }

    @Test
    void syncCreatesNewSessionAndSets() {
        when(sessionRepository.save(any(WorkoutSession.class))).thenAnswer(i -> {
            WorkoutSession s = i.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(setRepository.findBySessionIdAndSetNumber(100L, 1)).thenReturn(Optional.empty());
        when(setRepository.save(any(WorkoutSet.class))).thenAnswer(i -> i.getArgument(0));

        SyncResponse res = service.sync(1L, new SyncRequest(List.of(new SyncSessionRequest(null, null, Instant.now(), List.of(
                new SyncSetRequest(null, 1, 10, null, null, Instant.now()))))));

        assertEquals(1, res.acceptedSessions());
        assertEquals(1, res.acceptedSets());
    }
}
