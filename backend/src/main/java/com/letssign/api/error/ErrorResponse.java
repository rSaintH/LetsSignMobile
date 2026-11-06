package com.letssign.api.error;

import java.util.Map;

/** Corpo padrão de erro: o app só precisa exibir "message". */
public record ErrorResponse(int status, String error, String message, Map<String, String> fields, long timestamp) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, null, System.currentTimeMillis());
    }
}
