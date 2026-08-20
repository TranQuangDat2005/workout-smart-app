package com.workoutsmart.profile.service;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtAuthFilter;
import com.workoutsmart.auth.service.TokenService;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.profile.dto.ExerciseProgressResponse;
import com.workoutsmart.profile.dto.MessageResponse;
import com.workoutsmart.profile.dto.ProfileResponse;
import com.workoutsmart.profile.dto.UpdateProfileRequest;
import com.workoutsmart.profile.dto.UpdateProfileResponse;
import com.workoutsmart.profile.dto.WorkoutSessionDetailResponse;
import com.workoutsmart.profile.dto.WorkoutSessionResponse;
import com.workoutsmart.profile.dto.WorkoutSetResponse;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.repository.WorkoutSetRepository;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.plan.service.DraftExerciseService;
import com.workoutsmart.tracking.repository.WorkoutSessionExerciseRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ hồ sơ & lịch sử tập — spec 001-profile-history. */
@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final WorkoutSessionRepository sessionRepository;
    private final WorkoutSetRepository setRepository;
    private final WorkoutSessionExerciseRepository sessionExerciseRepository;
    private final ExerciseRepository exerciseRepository;
    private final TokenService tokenService;
    private final JwtAuthFilter jwtAuthFilter;
    private final DraftExerciseService draftExerciseService;

    public ProfileService(UserRepository userRepository,
                          WorkoutSessionRepository sessionRepository,
                          WorkoutSetRepository setRepository,
                          WorkoutSessionExerciseRepository sessionExerciseRepository,
                          ExerciseRepository exerciseRepository,
                          TokenService tokenService,
                          JwtAuthFilter jwtAuthFilter,
                          DraftExerciseService draftExerciseService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.setRepository = setRepository;
        this.sessionExerciseRepository = sessionExerciseRepository;
        this.exerciseRepository = exerciseRepository;
        this.tokenService = tokenService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.draftExerciseService = draftExerciseService;
    }

    public ProfileResponse getProfile(Long userId) {
        return toProfileResponse(requireUser(userId));
    }

    /** FR-002/003/004: sửa hồ sơ (không sửa cân nặng); đổi mục tiêu → goalChanged=true. */
    @Transactional
    public UpdateProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        boolean goalChanged = false;

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        if (request.heightCm() != null) {
            user.setHeightCm(request.heightCm());
        }
        // 019: TDEE đồng bộ hồ sơ — cho phép sửa cân nặng/giới tính/mức vận động từ màn Chỉ số cơ thể.
        if (request.weightKg() != null) {
            user.setWeightKg(request.weightKg());
        }
        if (request.sex() != null) {
            user.setSex(request.sex());
        }
        if (request.activityLevel() != null) {
            user.setActivityLevel(request.activityLevel());
        }
        if (request.calorieGoal() != null) {
            user.setCalorieGoal(request.calorieGoal());
        }
        if (request.customCalorieOffset() != null) {
            user.setCustomCalorieOffset(request.customCalorieOffset());
        }
        if (request.goalType() != null && !request.goalType().equals(user.getGoalType())) {
            user.setGoalType(request.goalType());
            goalChanged = true;
        }
        userRepository.save(user);
        return new UpdateProfileResponse(toProfileResponse(user), goalChanged);
    }

    /** FR-005: soft-delete tài khoản — giữ nguyên dữ liệu, chỉ đổi trạng thái. */
    @Transactional
    public MessageResponse deleteAccount(Long userId) {
        User user = requireUser(userId);
        user.setAccountStatus(AccountStatus.DELETED);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
        tokenService.revokeAll(userId);
        jwtAuthFilter.invalidate(userId);
        return new MessageResponse("Tài khoản đã được xóa. Bạn có thể khôi phục trong 30 ngày.");
    }

    /** FR-006/008: lịch sử buổi tập phân trang (20/trang, mới nhất trước). */
    public Page<WorkoutSessionResponse> getSessions(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return sessionRepository
                .findByUserIdOrderByStartTimeDesc(userId, PageRequest.of(Math.max(page, 0), safeSize))
                .map(session -> {
                    List<com.workoutsmart.profile.entity.WorkoutSet> sets =
                            setRepository.findBySessionIdOrderBySetNumberAsc(session.getId());
                    BigDecimal volume = sets.stream()
                            .filter(s -> s.getWeightUsed() != null && s.getRepsCompleted() != null)
                            .map(s -> s.getWeightUsed().multiply(BigDecimal.valueOf(s.getRepsCompleted())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new WorkoutSessionResponse(
                            session.getId(),
                            session.getStartTime(),
                            session.getEndTime(),
                            session.getStatus(),
                            session.getFocusInterruptionsCount(),
                            sets.size(),
                            volume);
                });
    }

    /** FR-007: chi tiết 1 buổi tập với từng hiệp. */
    public WorkoutSessionDetailResponse getSessionDetail(Long userId, Long sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại"));
        if (!session.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại");
        }
        List<WorkoutSetResponse> sets = setRepository.findBySessionIdOrderBySetNumberAsc(sessionId)
                .stream()
                .map(s -> new WorkoutSetResponse(
                        s.getId(), s.getSetNumber(), s.getRepsCompleted(),
                        s.getWeightUsed(), s.getRestTimeSeconds()))
                .toList();
        return new WorkoutSessionDetailResponse(
                session.getId(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                session.getFocusInterruptionsCount(),
                sets);
    }

    /** Tiến bộ tạ/rep theo từng bài tập trong khoảng thời gian (mặc định 30 ngày). */
    public List<ExerciseProgressResponse> getExerciseProgress(Long userId, int days) {
        int safeDays = Math.min(Math.max(days, 7), 365);
        Instant to = Instant.now();
        Instant from = to.minus(Duration.ofDays(safeDays));
        List<WorkoutSession> sessions = new ArrayList<>(sessionRepository
                .findByUserIdAndStatusAndStartTimeBetween(userId, "completed", from, to));
        sessions.sort(Comparator.comparing(WorkoutSession::getStartTime));
        if (sessions.isEmpty()) {
            return List.of();
        }

        Map<Long, Instant> sessionTimes = sessions.stream()
                .collect(Collectors.toMap(WorkoutSession::getId, WorkoutSession::getStartTime, (a, b) -> a));
        List<Long> sessionIds = sessions.stream().map(WorkoutSession::getId).toList();
        List<com.workoutsmart.profile.entity.WorkoutSet> sets = setRepository.findBySessionIdIn(sessionIds);

        Map<Long, String> names = new HashMap<>();
        Map<Long, List<Point>> byExercise = new HashMap<>();
        for (com.workoutsmart.profile.entity.WorkoutSet set : sets) {
            if ("warm_up".equals(set.getSetType())) {
                continue;
            }
            Long exerciseId = set.getExerciseId();
            if (exerciseId == null) {
                continue;
            }
            if (set.getWeightUsed() == null && set.getRepsCompleted() == null) {
                continue;
            }
            names.computeIfAbsent(exerciseId, id -> {
                Exercise exercise = exerciseRepository.findById(id).orElse(null);
                return exercise != null ? exercise.getName() : "Bài tập";
            });
            Instant time = sessionTimes.get(set.getSessionId());
            if (time == null) {
                continue;
            }
            byExercise.computeIfAbsent(exerciseId, k -> new ArrayList<>())
                    .add(new Point(time, set.getWeightUsed(), set.getRepsCompleted()));
        }

        List<ExerciseProgressResponse> result = new ArrayList<>();
        for (Map.Entry<Long, List<Point>> entry : byExercise.entrySet()) {
            List<Point> points = entry.getValue();
            points.sort(Comparator.comparing(Point::time));
            BigDecimal firstWeight = null;
            BigDecimal lastWeight = null;
            Integer firstReps = null;
            Integer lastReps = null;
            for (Point p : points) {
                if (firstWeight == null && p.weight() != null) {
                    firstWeight = p.weight();
                }
                if (p.weight() != null) {
                    lastWeight = p.weight();
                }
                if (firstReps == null && p.reps() != null) {
                    firstReps = p.reps();
                }
                if (p.reps() != null) {
                    lastReps = p.reps();
                }
            }
            result.add(new ExerciseProgressResponse(
                    entry.getKey(),
                    names.get(entry.getKey()),
                    firstWeight,
                    lastWeight,
                    delta(firstWeight, lastWeight),
                    firstReps,
                    lastReps,
                    deltaInt(firstReps, lastReps)));
        }
        result.sort(Comparator.comparing(ExerciseProgressResponse::exerciseName,
                Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private BigDecimal delta(BigDecimal first, BigDecimal last) {
        if (first == null || last == null) {
            return null;
        }
        return last.subtract(first);
    }

    private Integer deltaInt(Integer first, Integer last) {
        if (first == null || last == null) {
            return null;
        }
        return last - first;
    }

    private record Point(Instant time, BigDecimal weight, Integer reps) {}

    /** FR-010 (009): kết thúc buổi tập → dọn draft queue của session. */
    @Transactional
    public MessageResponse completeSession(Long userId, Long sessionId) {
        WorkoutSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại"));
        if (!session.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Buổi tập không tồn tại");
        }
        if (!"active".equals(session.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Buổi tập đã kết thúc");
        }
        session.setStatus("completed");
        session.setEndTime(Instant.now());
        sessionRepository.save(session);
        draftExerciseService.cleanupBySession(sessionId);
        return new MessageResponse("Buổi tập đã hoàn thành");
    }

    /** 018: xóa toàn bộ lịch sử tập của User (giữ buổi active) — thứ tự con → cha, 1 transaction. */
    @Transactional
    public MessageResponse clearHistory(Long userId) {
        List<WorkoutSession> toDelete = sessionRepository.findByUserIdAndStatusNot(userId, "active");
        if (toDelete.isEmpty()) {
            return new MessageResponse("Không có lịch sử để xóa");
        }
        List<Long> sessionIds = toDelete.stream().map(WorkoutSession::getId).toList();
        setRepository.deleteBySessionIdIn(sessionIds);
        draftExerciseService.cleanupBySessions(sessionIds);
        // workout_session_exercise_sets cascade theo workout_session_exercises.
        sessionExerciseRepository.deleteBySessionIdIn(sessionIds);
        long deleted = sessionRepository.deleteByUserIdAndStatusNot(userId, "active");
        return new MessageResponse("Đã xóa " + deleted + " buổi tập khỏi lịch sử");
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
    }

    private ProfileResponse toProfileResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getAge(),
                user.getWeightKg(),
                user.getHeightCm(),
                user.getGoalType(),
                user.getFitnessLevel(),
                user.getSex(),
                user.getActivityLevel(),
                user.getCalorieGoal(),
                user.getCustomCalorieOffset(),
                user.getAccountStatus().name(),
                user.isEmailVerified(),
                user.getCreatedAt());
    }
}
