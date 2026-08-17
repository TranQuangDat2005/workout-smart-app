package com.workoutsmart.profile.service;

import com.workoutsmart.auth.entity.AccountStatus;
import com.workoutsmart.auth.entity.User;
import com.workoutsmart.auth.repository.UserRepository;
import com.workoutsmart.auth.security.JwtAuthFilter;
import com.workoutsmart.auth.service.TokenService;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
    private final TokenService tokenService;
    private final JwtAuthFilter jwtAuthFilter;
    private final DraftExerciseService draftExerciseService;

    public ProfileService(UserRepository userRepository,
                          WorkoutSessionRepository sessionRepository,
                          WorkoutSetRepository setRepository,
                          TokenService tokenService,
                          JwtAuthFilter jwtAuthFilter,
                          DraftExerciseService draftExerciseService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.setRepository = setRepository;
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
                user.getAccountStatus().name(),
                user.isEmailVerified(),
                user.getCreatedAt());
    }
}
