package com.gymflow.gym.application;

import com.gymflow.gym.domain.Gym;
import com.gymflow.gym.infrastructure.persistence.GymRepository;
import java.util.List;
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

    /**
     * Sin restricción de tenant (Gym no es @TenantId — es la raíz). Uso
     * interno solo: el scheduler de recordatorios (Fase 12) es el único
     * consumidor, para iterar todos los gimnasios y procesarlos uno por
     * uno. No hay endpoint HTTP que exponga esto — listar todos los
     * gimnasios es una operación de plataforma, no de un gimnasio.
     */
    public List<Gym> listAll() {
        return gymRepository.findAll();
    }
}
