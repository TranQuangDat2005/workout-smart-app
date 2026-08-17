package com.workoutsmart.admin.service;

import com.workoutsmart.admin.dto.AdminUserDetailResponse;
import com.workoutsmart.admin.dto.AdminUserResponse;
import com.workoutsmart.admin.dto.CreateExerciseRequest;
import com.workoutsmart.admin.dto.ExerciseImportResponse;
import com.workoutsmart.admin.dto.UpdateExerciseRequest;
import com.workoutsmart.admin.entity.AuditLog;
import com.workoutsmart.admin.repository.AuditLogRepository;
import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtAuthFilter;
import com.workoutsmart.auth.service.TokenService;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.service.DraftExerciseService;
import com.workoutsmart.profile.dto.WorkoutSessionResponse;
import com.workoutsmart.profile.entity.WorkoutSession;
import com.workoutsmart.profile.repository.WorkoutSessionRepository;
import com.workoutsmart.profile.service.ProfileService;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ quản trị — UC-18 (user) + UC-19 (exercise) + audit log (constitution §5). */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final AuditLogRepository auditLogRepository;
    private final DraftExerciseService draftExerciseService;
    private final ProfileService profileService;
    private final TokenService tokenService;
    private final JwtAuthFilter jwtAuthFilter;
    private final WorkoutSessionRepository sessionRepository;

    public AdminService(UserRepository userRepository,
                        ExerciseRepository exerciseRepository,
                        AuditLogRepository auditLogRepository,
                        DraftExerciseService draftExerciseService,
                        ProfileService profileService,
                        TokenService tokenService,
                        JwtAuthFilter jwtAuthFilter,
                        WorkoutSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.exerciseRepository = exerciseRepository;
        this.auditLogRepository = auditLogRepository;
        this.draftExerciseService = draftExerciseService;
        this.profileService = profileService;
        this.tokenService = tokenService;
        this.jwtAuthFilter = jwtAuthFilter;
        this.sessionRepository = sessionRepository;
    }

    // ---------- User management (UC-18) ----------

    public List<AdminUserResponse> searchUsers(String q) {
        if (q == null || q.isBlank()) {
            return userRepository.findAll().stream()
                    .limit(50)
                    .map(this::toAdminUser)
                    .toList();
        }
        Map<Long, User> byId = new LinkedHashMap<>();
        if (q.matches("\\d+")) {
            userRepository.findById(Long.valueOf(q)).ifPresent(u -> byId.put(u.getId(), u));
        }
        userRepository.findByEmailContainingIgnoreCase(q).forEach(u -> byId.put(u.getId(), u));
        userRepository.findByDisplayNameContainingIgnoreCase(q).forEach(u -> byId.put(u.getId(), u));
        return byId.values().stream().map(this::toAdminUser).toList();
    }

    public AdminUserDetailResponse getUserDetail(Long userId) {
        var profile = profileService.getProfile(userId);
        List<WorkoutSessionResponse> sessions = profileService.getSessions(userId, 0, 20).getContent();
        return new AdminUserDetailResponse(profile, sessions);
    }

    @Transactional
    public void banUser(Long userId, String reason, Long adminId) {
        User user = requireUser(userId);
        user.setAccountStatus(AccountStatus.BANNED);
        userRepository.save(user);
        tokenService.revokeAll(userId);
        jwtAuthFilter.invalidate(userId);
        // FR-007 (009): giữ dữ liệu đã ghi, đánh dấu session đang tập thành interrupted
        sessionRepository.findByUserIdAndStatus(userId, "active").forEach(session -> {
            session.setStatus("interrupted");
            sessionRepository.save(session);
        });
        audit(adminId, "ban_user", "user", userId, reason);
    }

    @Transactional
    public void unbanUser(Long userId, Long adminId) {
        User user = requireUser(userId);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        jwtAuthFilter.invalidate(userId);
        audit(adminId, "unban_user", "user", userId, null);
    }

    // ---------- Exercise management (UC-19) ----------

    @Transactional
    public Exercise createExercise(CreateExerciseRequest request, Long adminId) {
        Exercise exercise = exerciseRepository.save(toExercise(request));
        audit(adminId, "create_exercise", "exercise", exercise.getId(), null);
        return exercise;
    }

    @Transactional
    public void updateExercise(Long exerciseId, UpdateExerciseRequest request, Long adminId) {
        Exercise exercise = requireExercise(exerciseId);
        applyUpdate(exercise, request);
        exerciseRepository.save(exercise);
        audit(adminId, "update_exercise", "exercise", exerciseId, null);
    }

    @Transactional
    public void setExerciseStatus(Long exerciseId, String status, String reason, Long adminId) {
        Exercise exercise = requireExercise(exerciseId);
        exercise.setStatus(status);
        exerciseRepository.save(exercise);

        if ("inactive".equals(status)) {
            draftExerciseService.handleExerciseHidden(exerciseId);
        }

        audit(adminId, "inactive".equals(status) ? "deactivate_exercise" : "activate_exercise",
                "exercise", exerciseId, reason);
    }

    @Transactional
    public ExerciseImportResponse importExercises(List<CreateExerciseRequest> items, Long adminId) {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;
        Set<String> seen = new HashSet<>();

        for (CreateExerciseRequest item : items) {
            String key = normalize(item.name()) + "|" + normalize(item.equipment());
            if (!seen.add(key)) {
                skipped++;
                continue;
            }
            Optional<Exercise> existing = exerciseRepository
                    .findByNameIgnoreCaseAndEquipmentIgnoreCase(item.name().trim(), item.equipment().trim());
            if (existing.isPresent()) {
                Exercise exercise = existing.get();
                applyUpdate(exercise, toUpdate(item));
                exerciseRepository.save(exercise);
                updated++;
            } else {
                exerciseRepository.save(toExercise(item));
                inserted++;
            }
        }

        audit(adminId, "import_exercises", "exercise", null,
                "inserted=" + inserted + ", updated=" + updated + ", skipped=" + skipped);
        return new ExerciseImportResponse(inserted, updated, skipped);
    }

    // ---------- Helpers ----------

    private Exercise toExercise(CreateExerciseRequest req) {
        return Exercise.builder()
                .name(req.name().trim())
                .category(req.category())
                .bodyPart(req.bodyPart())
                .equipment(req.equipment().trim())
                .target(req.target())
                .muscleGroup(req.muscleGroup())
                .image(req.image())
                .gifUrl(req.gifUrl())
                .instructions(req.instructions())
                .status("active")
                .build();
    }

    private UpdateExerciseRequest toUpdate(CreateExerciseRequest req) {
        return new UpdateExerciseRequest(req.name(), req.category(), req.bodyPart(), req.equipment(),
                req.target(), req.muscleGroup(), req.image(), req.gifUrl(), req.instructions());
    }

    private void applyUpdate(Exercise exercise, UpdateExerciseRequest req) {
        if (req.name() != null) exercise.setName(req.name().trim());
        if (req.category() != null) exercise.setCategory(req.category());
        if (req.bodyPart() != null) exercise.setBodyPart(req.bodyPart());
        if (req.equipment() != null) exercise.setEquipment(req.equipment().trim());
        if (req.target() != null) exercise.setTarget(req.target());
        if (req.muscleGroup() != null) exercise.setMuscleGroup(req.muscleGroup());
        if (req.image() != null) exercise.setImage(req.image());
        if (req.gifUrl() != null) exercise.setGifUrl(req.gifUrl());
        if (req.instructions() != null) exercise.setInstructions(req.instructions());
    }

    private void audit(Long adminId, String actionType, String targetType, Long targetId, String reason) {
        auditLogRepository.save(AuditLog.builder()
                .adminId(adminId)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .build());
    }

    private AdminUserResponse toAdminUser(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getAccountStatus().name(), user.isEmailVerified());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tài khoản không tồn tại"));
    }

    private Exercise requireExercise(Long exerciseId) {
        return exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
