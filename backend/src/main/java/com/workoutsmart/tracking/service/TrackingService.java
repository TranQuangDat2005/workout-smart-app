package com.workoutsmart.tracking.service;

import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.common.SetType;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.dto.SetTargetResponse;
import com.workoutsmart.plan.entity.WorkoutPlan;
import com.workoutsmart.plan.entity.WorkoutPlanDay;
import com.workoutsmart.plan.entity.WorkoutPlanExercise;
import com.workoutsmart.plan.repository.WorkoutPlanDayRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseRepository;
import com.workoutsmart.plan.repository.WorkoutPlanExerciseSetRepository;
import com.workoutsmart.plan.repository.WorkoutPlanRepository;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.entity.WorkoutSet;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.tracking.dto.RecordSetRequest;
import com.workoutsmart.tracking.dto.SessionExerciseResponse;
import com.workoutsmart.tracking.dto.SessionResponse;
import com.workoutsmart.tracking.dto.SetResponse;
import com.workoutsmart.tracking.dto.SyncRequest;
import com.workoutsmart.tracking.dto.SyncResponse;
import com.workoutsmart.tracking.dto.SyncSessionRequest;
import com.workoutsmart.tracking.dto.SyncSetRequest;
import com.workoutsmart.tracking.entity.WorkoutSessionExercise;
import com.workoutsmart.tracking.entity.WorkoutSessionExerciseSet;
import com.workoutsmart.tracking.repository.WorkoutSessionExerciseRepository;
import com.workoutsmart.tracking.repository.WorkoutSessionExerciseSetRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ tracking buổi tập — FR-001, FR-006, FR-010 (009) + snapshot 013. */
@Service
public class TrackingService {

    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutSetRepository setRepository;
    private final WorkoutPlanRepository planRepository;
    private final WorkoutPlanDayRepository dayRepository;
    private final WorkoutPlanExerciseRepository planExerciseRepository;
    private final WorkoutPlanExerciseSetRepository planExerciseSetRepository;
    private final WorkoutSessionExerciseRepository sessionExerciseRepository;
    private final WorkoutSessionExerciseSetRepository sessionExerciseSetRepository;
    private final ExerciseRepository exerciseRepository;

    public TrackingService(WorkoutSessionRepository sessionRepository,
                           WorkoutSetRepository setRepository,
                           WorkoutPlanRepository planRepository,
                           WorkoutPlanDayRepository dayRepository,
                           WorkoutPlanExerciseRepository planExerciseRepository,
                           WorkoutPlanExerciseSetRepository planExerciseSetRepository,
                           WorkoutSessionExerciseRepository sessionExerciseRepository,
                           WorkoutSessionExerciseSetRepository sessionExerciseSetRepository,
                           ExerciseRepository exerciseRepository) {
        this.sessionRepository = sessionRepository;
        this.setRepository = setRepository;
        this.planRepository = planRepository;
        this.dayRepository = dayRepository;
        this.planExerciseRepository = planExerciseRepository;
        this.planExerciseSetRepository = planExerciseSetRepository;
        this.sessionExerciseRepository = sessionExerciseRepository;
        this.sessionExerciseSetRepository = sessionExerciseSetRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional
    public SessionResponse startSession(Long userId, Long planId, Instant startTime) {
        List<WorkoutSession> active = sessionRepository.findByUserIdAndStatus(userId, "active");
        if (!active.isEmpty()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Bạn có một buổi tập chưa hoàn thành. Tiếp tục hay kết thúc?");
        }
        Long resolvedPlanId = planId != null
                ? planId
                : planRepository.findByUserIdAndStatus(userId, "active").map(WorkoutPlan::getId).orElse(null);
        WorkoutSession session = WorkoutSession.builder()
                .userId(userId)
                .planId(resolvedPlanId)
                .status("active")
                .startTime(startTime != null ? startTime : Instant.now())
                .build();
        session = sessionRepository.save(session);
        boolean hasSnapshot = snapshotToday(session, resolvedPlanId);
        if (resolvedPlanId != null && !hasSnapshot) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Hôm nay không có bài tập trong lộ trình");
        }
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public SessionResponse getActiveSession(Long userId) {
        return sessionRepository.findByUserIdAndStatus(userId, "active").stream()
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không có buổi tập đang diễn ra"));
    }

    @Transactional
    public SetResponse recordSet(Long userId, Long sessionId, RecordSetRequest request) {
        WorkoutSession session = requireOwnedActiveSession(userId, sessionId);
        WorkoutSet set;
        if (request.sessionExerciseId() != null) {
            requireSnapshotBelongs(sessionId, request.sessionExerciseId());
            set = setRepository.findBySessionIdAndSessionExerciseIdAndSetNumber(
                            sessionId, request.sessionExerciseId(), request.setNumber())
                    .orElseGet(() -> WorkoutSet.builder()
                            .sessionId(sessionId)
                            .sessionExerciseId(request.sessionExerciseId())
                            .setNumber(request.setNumber())
                            .build());
        } else {
            set = setRepository.findBySessionIdAndSetNumber(sessionId, request.setNumber())
                    .orElseGet(() -> WorkoutSet.builder()
                            .sessionId(sessionId)
                            .setNumber(request.setNumber())
                            .build());
        }
        set.setExerciseId(request.exerciseId());
        set.setSessionExerciseId(request.sessionExerciseId());
        set.setRepsCompleted(request.repsCompleted());
        set.setWeightUsed(request.weightUsed());
        set.setDurationSeconds(request.durationSeconds());
        String setType = resolveSetType(request.setType(), request.sessionExerciseId(), request.setNumber());
        set.setSetType(setType);
        set.setRestTimeSeconds(SetType.DROP_SET.value().equals(setType) ? Integer.valueOf(0) : request.restTimeSeconds());
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

    /** FR-007 (016): xóa hiệp vừa ghi (undo) — chỉ khi session thuộc user và còn active. */
    @Transactional
    public void deleteSet(Long userId, Long sessionId, Long setId) {
        WorkoutSession session = requireOwnedActiveSession(userId, sessionId);
        WorkoutSet set = setRepository.findById(setId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hiệp tập không tồn tại"));
        if (!session.getId().equals(set.getSessionId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Hiệp tập không tồn tại");
        }
        setRepository.delete(set);
    }

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

    @Transactional
    public SyncResponse sync(Long userId, SyncRequest request) {
        List<Long> rejectedSetIds = new ArrayList<>();
        int acceptedSessions = 0;
        int acceptedSets = 0;
        LocalDate today = LocalDate.now();

        for (SyncSessionRequest syncSession : request.sessions()) {
            WorkoutSession session;
            if (syncSession.serverSessionId() != null) {
                session = sessionRepository.findById(syncSession.serverSessionId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại"));
                if (!session.getUserId().equals(userId)) {
                    throw new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại");
                }
                if (session.getStartTime() != null
                        && session.getStartTime().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(today)) {
                    syncSession.sets().forEach(s -> rejectedSetIds.add(s.setNumber().longValue()));
                    continue;
                }
            } else {
                session = sessionRepository.save(WorkoutSession.builder()
                        .userId(userId)
                        .planId(syncSession.planId())
                        .status("active")
                        .startTime(syncSession.startTime() != null ? syncSession.startTime() : Instant.now())
                        .build());
                acceptedSessions++;
            }

            for (SyncSetRequest setReq : syncSession.sets()) {
                Long sessionExerciseId = resolveSyncSessionExerciseId(session.getId(), setReq.sessionExerciseId());
                WorkoutSet set = sessionExerciseId != null
                        ? setRepository.findBySessionIdAndSessionExerciseIdAndSetNumber(
                                session.getId(), sessionExerciseId, setReq.setNumber())
                        .orElse(null)
                        : setRepository.findBySessionIdAndSetNumber(session.getId(), setReq.setNumber())
                        .orElse(null);
                if (set != null && set.getClientTimestamp() != null && setReq.clientTimestamp() != null
                        && set.getClientTimestamp().isAfter(setReq.clientTimestamp())) {
                    continue;
                }
                if (set == null) {
                    set = WorkoutSet.builder().sessionId(session.getId()).setNumber(setReq.setNumber()).build();
                }
                set.setSessionExerciseId(sessionExerciseId);
                set.setExerciseId(setReq.exerciseId());
                set.setRepsCompleted(setReq.repsCompleted());
                set.setWeightUsed(setReq.weightUsed());
                set.setDurationSeconds(setReq.durationSeconds());
                set.setSetType(SetType.normalize(setReq.setType()));
                set.setRestTimeSeconds(SetType.DROP_SET.value().equals(set.getSetType())
                        ? Integer.valueOf(0)
                        : setReq.restTimeSeconds());
                set.setClientTimestamp(setReq.clientTimestamp());
                setRepository.save(set);
                acceptedSets++;
            }
        }
        return new SyncResponse(acceptedSessions, acceptedSets, rejectedSetIds);
    }

    /** Chỉ dùng session_exercise_id khi snapshot tương ứng tồn tại trong session; nếu không thì rơi về UPSERT (session_id, set_number). */
    private Long resolveSyncSessionExerciseId(Long sessionId, Long sessionExerciseId) {
        if (sessionExerciseId == null) {
            return null;
        }
        return sessionExerciseRepository.findById(sessionExerciseId)
                .filter(row -> sessionId.equals(row.getSessionId()))
                .map(WorkoutSessionExercise::getId)
                .orElse(null);
    }

    private boolean snapshotToday(WorkoutSession session, Long planId) {
        if (planId == null) {
            return false;
        }
        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue() % 7;
        WorkoutPlanDay day = dayRepository.findByPlanIdAndDayOfWeek(planId, dayOfWeek).orElse(null);
        if (day == null) {
            return false;
        }
        List<WorkoutPlanExercise> items = planExerciseRepository.findByDayIdOrderBySortOrderAscIdAsc(day.getId());
        if (items.isEmpty()) {
            return false;
        }
        copyDay(session, items);
        return true;
    }

    private void copyDay(WorkoutSession session, List<WorkoutPlanExercise> items) {
        int order = 0;
        for (WorkoutPlanExercise item : items) {
            Exercise exercise = exerciseRepository.findById(item.getExerciseId()).orElse(null);
            WorkoutSessionExercise saved = sessionExerciseRepository.save(WorkoutSessionExercise.builder()
                    .sessionId(session.getId())
                    .exerciseId(item.getExerciseId())
                    .sortOrder(order++)
                    .targetSets(item.getTargetSets())
                    .targetReps(item.getTargetReps())
                    .targetDurationSeconds(item.getTargetDurationSeconds())
                    .restTimeSeconds(item.getRestTimeSeconds())
                    .exerciseName(exercise != null ? exercise.getName() : null)
                    .measureType(item.getMeasureType() != null && !item.getMeasureType().isBlank()
                            ? item.getMeasureType()
                            : exercise != null ? exercise.getMeasureType() : "reps_weight")
                    .build());
            copySetTargets(saved.getId(), item.getId());
        }
    }

    private void copySetTargets(Long sessionExerciseId, Long planExerciseId) {
        planExerciseSetRepository.findByPlanExerciseIdOrderBySetNumberAsc(planExerciseId).stream()
                .map(s -> WorkoutSessionExerciseSet.builder()
                        .sessionExerciseId(sessionExerciseId)
                        .setNumber(s.getSetNumber())
                        .targetReps(s.getTargetReps())
                        .targetWeight(s.getTargetWeight())
                        .setType(s.getSetType())
                        .targetDurationSeconds(s.getTargetDurationSeconds())
                        .build())
                .forEach(sessionExerciseSetRepository::save);
    }

    private void requireSnapshotBelongs(Long sessionId, Long sessionExerciseId) {
        WorkoutSessionExercise row = sessionExerciseRepository.findById(sessionExerciseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bài tập trong buổi không tồn tại"));
        if (!sessionId.equals(row.getSessionId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bài tập trong buổi không tồn tại");
        }
    }

    private WorkoutSession requireOwnedActiveSession(Long userId, Long sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại"));
        if (!session.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại");
        }
        if (!"active".equals(session.getStatus())) {
            String message = "expired".equals(session.getStatus())
                    ? "Buổi tập đã hết hạn vì sang ngày mới"
                    : "Buổi tập đã kết thúc";
            throw new ApiException(HttpStatus.CONFLICT, message);
        }
        return session;
    }

    private SessionResponse toResponse(WorkoutSession session) {
        Map<Long, String> mediaByExercise = new HashMap<>();
        List<SessionExerciseResponse> exercises = session.getId() == null
                ? List.of()
                : sessionExerciseRepository.findBySessionIdOrderBySortOrderAscIdAsc(session.getId()).stream()
                .map(e -> new SessionExerciseResponse(e.getId(), e.getExerciseId(), e.getExerciseName(),
                        e.getSortOrder(), e.getTargetSets(), e.getTargetReps(), e.getRestTimeSeconds(),
                        sessionExerciseSetRepository.findBySessionExerciseIdOrderBySetNumberAsc(e.getId()).stream()
                                .map(s -> new SetTargetResponse(s.getSetNumber(), s.getTargetReps(),
                                        s.getTargetWeight(), s.getSetType(), s.getTargetDurationSeconds()))
                                .toList(),
                        e.getTargetDurationSeconds(),
                        e.getMeasureType(),
                        mediaByExercise.computeIfAbsent(e.getExerciseId(), this::resolveMediaUrl)))
                .toList();
        List<SetResponse> sets = session.getId() == null
                ? List.of()
                : setRepository.findBySessionIdOrderBySetNumberAsc(session.getId()).stream()
                        .map(this::toResponse)
                        .toList();
        return new SessionResponse(session.getId(), session.getStatus(), session.getStartTime(),
                session.getEndTime(), session.getFocusInterruptionsCount(), session.getPlanId(), exercises, sets);
    }

    /** Lấy path media của bài tập (GIF ưu tiên, nếu không thì ảnh) — join một lần cho toàn session, tránh N+1. */
    private String resolveMediaUrl(Long exerciseId) {
        if (exerciseId == null) {
            return null;
        }
        Exercise exercise = exerciseRepository.findById(exerciseId).orElse(null);
        if (exercise == null) {
            return null;
        }
        return exercise.getGifUrl() != null && !exercise.getGifUrl().isBlank()
                ? exercise.getGifUrl()
                : exercise.getImage();
    }

    private SetResponse toResponse(WorkoutSet set) {
        return new SetResponse(set.getId(), set.getSessionId(), set.getExerciseId(), set.getSetNumber(),
                set.getRepsCompleted(), set.getWeightUsed(), set.getRestTimeSeconds(), set.getSessionExerciseId(),
                set.getSetType(), set.getDurationSeconds());
    }

    private String resolveSetType(String requested, Long sessionExerciseId, int setNumber) {
        if (SetType.isValid(requested)) {
            return requested;
        }
        if (sessionExerciseId != null) {
            return sessionExerciseSetRepository
                    .findBySessionExerciseIdAndSetNumber(sessionExerciseId, setNumber)
                    .map(WorkoutSessionExerciseSet::getSetType)
                    .orElse(SetType.NORMAL.value());
        }
        return SetType.NORMAL.value();
    }
}
