package com.side.shop.common.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Value("${RENDER_GIT_COMMIT:}")
    private String renderGitCommit;

    @GetMapping("/healthz")
    public Map<String, String> healthz() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "ok");

        if (!renderGitCommit.isBlank()) {
            response.put("commit", renderGitCommit);
        }

        return response;
    }
}
