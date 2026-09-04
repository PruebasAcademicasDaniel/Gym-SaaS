package com.gymflow.member.infrastructure.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email String email,
        String phone) {
}
