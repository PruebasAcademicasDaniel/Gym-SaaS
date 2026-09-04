package com.gymflow.membership.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateMembershipRequest(@NotNull UUID memberId, @NotNull UUID planId) {
}
