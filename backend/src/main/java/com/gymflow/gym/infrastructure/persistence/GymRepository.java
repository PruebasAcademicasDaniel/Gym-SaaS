package com.gymflow.gym.infrastructure.persistence;

import com.gymflow.gym.domain.Gym;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymRepository extends JpaRepository<Gym, UUID> {

    boolean existsBySlug(String slug);
}
