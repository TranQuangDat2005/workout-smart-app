package com.workoutsmart.tracking.controller;

import com.workoutsmart.tracking.dto.RecordSetRequest;
import com.workoutsmart.tracking.dto.SessionResponse;
import com.workoutsmart.tracking.dto.SetResponse;
import com.workoutsmart.tracking.dto.StartSessionRequest;
import com.workoutsmart.tracking.dto.SyncRequest;
import com.workoutsmart.tracking.dto.SyncResponse;
import com.workoutsmart.tracking.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Controller mỏng cho workout tracking — FR-001, FR-006 (009). */
@RestController
@RequestMapping("/api/v1/workout-sessions")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/active")
    public SessionResponse getActive(Authentication auth) {
        return trackingService.getActiveSession(currentUserId(auth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse startSession(Authentication auth,
                                        @RequestBody(required = false) StartSessionRequest request) {
        Long planId = request != null ? request.planId() : null;
        var startTime = request != null ? request.startTime() : null;
        return trackingService.startSession(currentUserId(auth), planId, startTime);
    }

    @PostMapping("/{id}/sets")
    public SetResponse recordSet(Authentication auth,
                                 @PathVariable Long id,
                                 @Valid @RequestBody RecordSetRequest request) {
        return trackingService.recordSet(currentUserId(auth), id, request);
    }

    @PostMapping("/{id}/focus-interruption")
    public SessionResponse incrementFocus(Authentication auth, @PathVariable Long id) {
        return trackingService.incrementFocus(currentUserId(auth), id);
    }

    /** FR-007 (016): hoàn tác hiệp vừa ghi — 204, 404 (không tồn tại/không thuộc), 409 (session không active). */
    @DeleteMapping("/{id}/sets/{setId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSet(Authentication auth, @PathVariable Long id, @PathVariable Long setId) {
        trackingService.deleteSet(currentUserId(auth), id, setId);
    }

    @PostMapping("/sync")
    public SyncResponse sync(Authentication auth, @Valid @RequestBody SyncRequest request) {
        return trackingService.sync(currentUserId(auth), request);
    }

    private Long currentUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
