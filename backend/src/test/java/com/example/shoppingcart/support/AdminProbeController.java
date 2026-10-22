package com.example.shoppingcart.support;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint under /api/admin so authorization rules can be tested before the real admin endpoints exist.
 * It lives in src/test, so it is never part of the deployed application.
 */
@RestController
@RequestMapping("/api/admin/test-probe")
public class AdminProbeController {

    @GetMapping
    public Map<String, String> probe() {
        return Map.of("area", "admin");
    }
}
