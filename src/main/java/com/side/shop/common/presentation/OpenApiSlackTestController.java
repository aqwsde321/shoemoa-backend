package com.side.shop.common.presentation;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiSlackTestController {

    @GetMapping("/api/openapi-slack-test")
    public Map<String, String> openApiSlackTest() {
        return Map.of("status", "openapi-slack-test");
    }
}
