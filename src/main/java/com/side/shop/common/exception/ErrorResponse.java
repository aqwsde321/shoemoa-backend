package com.side.shop.common.exception;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ErrorResponse {

    private String code;
    private String message;
    private Map<String, String> details;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public ErrorResponse(String code, String message, Map<String, String> details) {
        this.code = code;
        this.message = message;
        this.details = details;
    }
}
