package com.gymflow.gym.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateGymRequest(
        @NotBlank String name,
        @NotBlank
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "solo minúsculas, números y guiones, sin espacios")
        String slug) {
}
