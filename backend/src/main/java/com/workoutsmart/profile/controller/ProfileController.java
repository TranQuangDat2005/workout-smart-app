package com.workoutsmart.profile.controller;

import com.workoutsmart.profile.dto.ExerciseProgressResponse;
import com.workoutsmart.profile.dto.MessageResponse;
import com.workoutsmart.profile.dto.ProfileResponse;
import com.workoutsmart.profile.dto.UpdateProfileRequest;
import com.workoutsmart.profile.dto.UpdateProfileResponse;
import com.workoutsmart.profile.dto.WorkoutSessionDetailResponse;
import com.workoutsmart.profile.dto.WorkoutSessionResponse;
import com.workoutsmart.profile.service.ProfileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

/** Controller mỏng — logic nằm ở ProfileService. */
@RestController
@RequestMapping("/api/v1")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ProfileResponse getProfile(Authentication auth) {
        return profileService.getProfile(currentUserId(auth));
    }

    @PutMapping("/profile")
    public UpdateProfileResponse updateProfile(Authentication auth,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(currentUserId(auth), request);
    }

    @DeleteMapping("/account")
    public MessageResponse deleteAccount(Authentication auth) {
        return profileService.deleteAccount(currentUserId(auth));
    }

    @GetMapping("/workout-sessions")
    public Page<WorkoutSessionResponse> getSessions(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return profileService.getSessions(currentUserId(auth), page, size);
    }

    @GetMapping("/workout-sessions/{id}")
    public WorkoutSessionDetailResponse getSessionDetail(Authentication auth, @PathVariable Long id) {
        return profileService.getSessionDetail(currentUserId(auth), id);
    }

    @GetMapping("/workout-sessions/progress")
    public List<ExerciseProgressResponse> getExerciseProgress(Authentication auth,
                                                               @RequestParam(defaultValue = "30") int days) {
        return profileService.getExerciseProgress(currentUserId(auth), days);
    }

    /** 018: xóa toàn bộ lịch sử tập (giữ buổi active). */
    @DeleteMapping("/workout-sessions/history")
    public MessageResponse clearHistory(Authentication auth) {
        return profileService.clearHistory(currentUserId(auth));
    }

    @PostMapping("/workout-sessions/{id}/complete")
    public MessageResponse completeSession(Authentication auth, @PathVariable Long id) {
        return profileService.completeSession(currentUserId(auth), id);
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
