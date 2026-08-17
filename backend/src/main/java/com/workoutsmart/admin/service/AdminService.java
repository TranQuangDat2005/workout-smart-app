package com.workoutsmart.admin.service;

import com.workoutsmart.admin.entity.AuditLog;
import com.workoutsmart.admin.repository.AuditLogRepository;
import com.workoutsmart.auth.exception.ApiException;
import com.workoutsmart.exercise.entity.Exercise;
import com.workoutsmart.exercise.repository.ExerciseRepository;
import com.workoutsmart.plan.service.DraftExerciseService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nghiệp vụ quản trị bài tập — UC-19 (ẩn/hiện exercise + audit log). */
@Service
public class AdminService {

    private final ExerciseRepository exerciseRepository;
    private final AuditLogRepository auditLogRepository;
    private final DraftExerciseService draftExerciseService;

    public AdminService(ExerciseRepository exerciseRepository,
                        AuditLogRepository auditLogRepository,
                        DraftExerciseService draftExerciseService) {
        this.exerciseRepository = exerciseRepository;
        this.auditLogRepository = auditLogRepository;
        this.draftExerciseService = draftExerciseService;
    }

    @Transactional
    public void setExerciseStatus(Long exerciseId, String status, String reason, Long adminId) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bài tập không tồn tại"));
        exercise.setStatus(status);
        exerciseRepository.save(exercise);

        if ("inactive".equals(status)) {
            draftExerciseService.handleExerciseHidden(exerciseId);
        }

        auditLogRepository.save(AuditLog.builder()
                .adminId(adminId)
                .actionType("inactive".equals(status) ? "deactivate_exercise" : "activate_exercise")
                .targetType("exercise")
                .targetId(exerciseId)
                .reason(reason)
                .build());
    }
}
