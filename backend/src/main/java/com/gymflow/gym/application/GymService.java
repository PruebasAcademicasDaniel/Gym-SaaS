package com.gymflow.gym.application;

import com.gymflow.gym.domain.Gym;
import com.gymflow.gym.infrastructure.persistence.GymRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GymService {

    private final GymRepository gymRepository;

    public GymService(GymRepository gymRepository) {
        this.gymRepository = gymRepository;
    }

    @Transactional
    public Gym create(String name, String slug) {
        if (gymRepository.existsBySlug(slug)) {
            throw new SlugAlreadyInUseException(slug);
        }
        return gymRepository.save(new Gym(name, slug));
    }

    public Gym getById(UUID id) {
        return gymRepository.findById(id).orElseThrow(() -> new GymNotFoundException(id));
    }
}
