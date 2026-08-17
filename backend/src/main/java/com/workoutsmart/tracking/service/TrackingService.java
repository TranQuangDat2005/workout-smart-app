package com.workoutsmart.tracking.service;

import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.tracking.dto.RecordSetRequest;
import com.workoutsmart.tracking.dto.SessionResponse;
import com.workoutsmart.tracking.dto.SetResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ tracking buổi tập — FR-001, FR-006, FR-010 (009). */
@Service
public class TrackingService {

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutSetRepository setRepository;

    public TrackingService(WorkoutSessionRepository sessionRepository,
                           WorkoutSetRepository setRepository) {
        this.sessionRepository = sessionRepository;
        this.setRepository = setRepository;
    }

    @Transactional
    public SessionResponse startSession(Long userId, Long planId, Instant startTime) {
        List<WorkoutSession> active = sessionRepository.findByUserIdAndStatus(userId, "active");
        if (!active.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bạn có một buổi tập chưa hoàn thành. Tiếp tục hay kết thúc?");
        }
        WorkoutSession session = WorkoutSession.builder()
                .userId(userId)
                .planId(planId)
                .status("active")
                .startTime(startTime != null ? startTime : Instant.now())
                .build();
        sessionRepository.save(session);
        return toResponse(session);
    }

    @Transactional
    public SetResponse recordSet(Long userId, Long sessionId, RecordSetRequest request) {
        WorkoutSession session = requireOwnedActiveSession(userId, sessionId);
        WorkoutSet set = setRepository.findBySessionIdAndSetNumber(sessionId, request.setNumber())
                .orElseGet(() -> WorkoutSet.builder()
                        .sessionId(sessionId)
                        .setNumber(request.setNumber())
                        .build());
        set.setExerciseId(request.exerciseId());
        set.setRepsCompleted(request.repsCompleted());
        set.setWeightUsed(request.weightUsed());
        set.setRestTimeSeconds(request.restTimeSeconds());
        setRepository.save(set);
        return toResponse(set);
    }

    @Transactional
    public SessionResponse incrementFocus(Long userId, Long sessionId) {
        WorkoutSession session = requireOwnedActiveSession(userId, sessionId);
        session.setFocusInterruptionsCount(session.getFocusInterruptionsCount() + 1);
        sessionRepository.save(session);
        return toResponse(session);
    }

    /** FR-010: mỗi ngày mới → đánh dấu session active cũ thành expired (theo timezone server). */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireStaleSessions() {
        LocalDate today = LocalDate.now();
        sessionRepository.findByStatus("active").stream()
                .filter(s -> s.getStartTime() != null)
                .filter(s -> s.getStartTime().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(today))
                .forEach(s -> {
                    s.setStatus("expired");
                    sessionRepository.save(s);
                });
    }

    private WorkoutSession requireOwnedActiveSession(Long userId, Long sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại"));
        if (!session.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại");
        }
        if (!"active".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Buổi tập đã kết thúc");
        }
        return session;
    }

    private SessionResponse toResponse(WorkoutSession session) {
        return new SessionResponse(session.getId(), session.getStatus(), session.getStartTime(),
                session.getEndTime(), session.getFocusInterruptionsCount());
    }

    private SetResponse toResponse(WorkoutSet set) {
        return new SetResponse(set.getId(), set.getSessionId(), set.getExerciseId(), set.getSetNumber(),
                set.getRepsCompleted(), set.getWeightUsed(), set.getRestTimeSeconds());
    }
}
