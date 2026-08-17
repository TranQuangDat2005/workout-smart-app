package com.workoutsmart.admin.controller;

import com.workoutsmart.admin.dto.AdminExerciseResponse;
import com.workoutsmart.admin.dto.AdminUserDetailResponse;
import com.workoutsmart.admin.dto.AdminUserResponse;
import com.workoutsmart.admin.dto.BanUserRequest;
import com.workoutsmart.admin.dto.CreateExerciseRequest;
import com.workoutsmart.admin.dto.ExerciseImportRequest;
import com.workoutsmart.admin.dto.ExerciseImportResponse;
import com.workoutsmart.admin.dto.SetExerciseStatusRequest;
import com.workoutsmart.admin.dto.UpdateExerciseRequest;
import com.workoutsmart.admin.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Controller mỏng cho Admin — bảo vệ bởi SecurityConfig (/api/v1/admin/** = ADMIN). */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> searchUsers(Authentication auth,
                                               @RequestParam(defaultValue = "") String q) {
        return adminService.searchUsers(q);
    }

    @GetMapping("/users/{id}")
    public AdminUserDetailResponse getUserDetail(@PathVariable Long id) {
        return adminService.getUserDetail(id);
    }

    @PostMapping("/users/{id}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void banUser(Authentication auth,
                        @PathVariable Long id,
                        @Valid @RequestBody BanUserRequest request) {
        adminService.banUser(id, request.reason(), (Long) auth.getPrincipal());
    }

    @PostMapping("/users/{id}/unban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbanUser(Authentication auth, @PathVariable Long id) {
        adminService.unbanUser(id, (Long) auth.getPrincipal());
    }

    @GetMapping("/exercises")
    public List<AdminExerciseResponse> listExercises(@RequestParam(defaultValue = "") String q) {
        return adminService.listExercises(q);
    }

    @PostMapping("/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public void createExercise(Authentication auth,
                               @Valid @RequestBody CreateExerciseRequest request) {
        adminService.createExercise(request, (Long) auth.getPrincipal());
    }

    @PutMapping("/exercises/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateExercise(Authentication auth,
                               @PathVariable Long id,
                               @Valid @RequestBody UpdateExerciseRequest request) {
        adminService.updateExercise(id, request, (Long) auth.getPrincipal());
    }

    @PostMapping("/exercises/import")
    public ExerciseImportResponse importExercises(Authentication auth,
                                                  @Valid @RequestBody ExerciseImportRequest request) {
        return adminService.importExercises(request.items(), (Long) auth.getPrincipal());
    }

    @PatchMapping("/exercises/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setExerciseStatus(Authentication auth,
                                  @PathVariable Long id,
                                  @Valid @RequestBody SetExerciseStatusRequest request) {
        adminService.setExerciseStatus(id, request.status(), request.reason(),
                (Long) auth.getPrincipal());
    }
}
