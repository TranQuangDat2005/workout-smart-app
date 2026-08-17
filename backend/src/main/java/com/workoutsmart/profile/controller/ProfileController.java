package com.workoutsmart.profile.controller;

import com.workoutsmart.profile.dto.MessageResponse;
import com.workoutsmart.profile.dto.ProfileResponse;
import com.workoutsmart.profile.dto.UpdateProfileRequest;
import com.workoutsmart.profile.dto.UpdateProfileResponse;
import com.workoutsmart.profile.dto.WorkoutSessionDetailResponse;
import com.workoutsmart.profile.dto.WorkoutSessionResponse;
import com.workoutsmart.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
