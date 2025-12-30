package io.github.gohoski.numai;

import java.io.InputStream;

class ApiResponse {
    private int statusCode;
    private InputStream body;
    private boolean successful;

    ApiResponse(int statusCode, InputStream body) {
        this.statusCode = statusCode;
        this.body = body;
        this.successful = (statusCode >= 200 && statusCode < 300);
    }

    // Getters
    int getStatusCode() { return statusCode; }
    InputStream getBody() { return body; }
    boolean isSuccessful() { return successful; }
}