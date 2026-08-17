package com.workoutsmart;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoint protected đơn giản để kiểm chứng JwtAuthFilter + ban-check (FR-013). */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
