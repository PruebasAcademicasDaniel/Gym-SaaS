package com.gymflow.auth.infrastructure.web;

import com.gymflow.auth.domain.User;
import java.util.UUID;

public record UserResponse(UUID id, String email, String role, boolean enabled) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().name(), user.isEnabled());
    }
}
