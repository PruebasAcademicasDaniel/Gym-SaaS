package com.gymflow.gym.infrastructure.web;

import com.gymflow.gym.domain.Gym;
import java.time.Instant;
import java.util.UUID;

public record GymResponse(UUID id, String name, String slug, Instant createdAt) {

    public static GymResponse from(Gym gym) {
        return new GymResponse(gym.getId(), gym.getName(), gym.getSlug(), gym.getCreatedAt());
    }
}
