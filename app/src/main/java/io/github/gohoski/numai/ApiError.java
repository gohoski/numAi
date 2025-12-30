package io.github.gohoski.numai;

/**
 * Created by Gleb on 21.08.2025.
 */

class ApiError extends Exception {
    private final String message;

    ApiError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
