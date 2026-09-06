package com.gymflow.attendance.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CheckInRequest(@NotNull UUID memberId) {
}
