package com.workoutsmart.admin.controller;

import com.workoutsmart.admin.dto.SetExerciseStatusRequest;
import com.workoutsmart.admin.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Controller mỏng cho Admin — được bảo vệ bởi SecurityConfig (/api/v1/admin/** = ADMIN). */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
