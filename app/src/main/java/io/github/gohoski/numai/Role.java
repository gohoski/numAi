package io.github.gohoski.numai;

/**
 * Created by Gleb on 21.08.2025.
 */

enum Role {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}