package com.side.shop.common.presentation;

import java.util.LinkedHashMap;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Value("${RENDER_GIT_COMMIT:}")
    private String renderGitCommit;

    @Operation(summary = "서비스 상태 확인", description = "서비스 상태와 Render 배포 커밋 정보를 반환합니다.")
    @GetMapping("/healthz")
    public Map<String, String> healthz(
            @Parameter(description = "상세 정보 포함 여부") @RequestParam(defaultValue = "false") boolean verbose,
            @Parameter(description = "OpenAPI Slack 알림 검증용 선택 값") @RequestParam(required = false) String reportMode) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "ok");

        if (!renderGitCommit.isBlank()) {
            response.put("commit", renderGitCommit);
        }

        if (verbose) {
            response.put("service", "shoemoa-backend");
        }

        if (reportMode != null && !reportMode.isBlank()) {
            response.put("reportMode", reportMode);
        }

        return response;
    }
}
