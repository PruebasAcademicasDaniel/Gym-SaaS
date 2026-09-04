package com.gymflow.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import testsupport.tenant.TenantIsolationFixture;
import testsupport.tenant.TenantIsolationFixtureRepository;

/**
 * El test más importante del proyecto (Fase 0, sección 13): prueba que el
 * mecanismo de aislamiento de tenant (AbstractTenantEntity + @TenantId +
 * GymTenantIdentifierResolver) realmente impide que un gimnasio vea datos
 * de otro — no solo en listados, también en lookup directo por id (la
 * forma más común de IDOR entre tenants).
 *
 * Usa un fixture de test en vez de una entidad de negocio real porque
 * todavía no existe ninguna (la primera, Member, llega en la Fase 6) —
 * cuando exista, su propio test de isolation ejercita este mismo mecanismo
 * ya probado acá.
 */
@SpringBootTest
@EntityScan(basePackages = {"com.gymflow", "testsupport.tenant"})
@EnableJpaRepositories(basePackages = {"com.gymflow", "testsupport.tenant"})
@TestPropertySource(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/migration-test")
@Testcontainers
class TenantIsolationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TenantIsolationFixtureRepository repository;

    private final UUID gymA = UUID.randomUUID();
    private final UUID gymB = UUID.randomUUID();

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void newRecordIsAutoStampedWithTheCurrentTenant_applicationCodeCannotOverrideIt() {
        TenantContext.setCurrentTenantId(gymA);

        TenantIsolationFixture saved = repository.saveAndFlush(new TenantIsolationFixture("gym-a-record"));

        assertThat(saved.getGymId()).isEqualTo(gymA);
    }

    @Test
    void aTenantNeverSeesAnotherTenantsRows_inListingsOrDirectIdLookup() {
        TenantContext.setCurrentTenantId(gymA);
        TenantIsolationFixture recordA = repository.saveAndFlush(new TenantIsolationFixture("gym-a-record"));

        TenantContext.setCurrentTenantId(gymB);
        TenantIsolationFixture recordB = repository.saveAndFlush(new TenantIsolationFixture("gym-b-record"));

        TenantContext.setCurrentTenantId(gymA);
        assertThat(repository.findAll()).extracting(TenantIsolationFixture::getId).containsExactly(recordA.getId());
        // IDOR entre tenants: conocer el id de otro gimnasio no alcanza para leerlo.
        assertThat(repository.findById(recordB.getId())).isEmpty();

        TenantContext.setCurrentTenantId(gymB);
        assertThat(repository.findAll()).extracting(TenantIsolationFixture::getId).containsExactly(recordB.getId());
        assertThat(repository.findById(recordA.getId())).isEmpty();
    }

    @Test
    void withNoTenantInContext_noRowsAreVisible_failsClosedNotOpen() {
        // Cubre el arranque de la app y a SUPER_ADMIN (sin gymId): ninguno
        // de los dos debería heredar acceso a datos de gimnasios reales
        // solo porque no hay un tenant resuelto. Un panel cross-tenant
        // para SUPER_ADMIN necesitaría su propio mecanismo explícito — no
        // este, que deliberadamente no deja pasar nada.
        TenantContext.setCurrentTenantId(gymA);
        repository.saveAndFlush(new TenantIsolationFixture("gym-a-record"));
        TenantContext.setCurrentTenantId(gymB);
        repository.saveAndFlush(new TenantIsolationFixture("gym-b-record"));

        TenantContext.clear();

        assertThat(repository.findAll()).isEmpty();
    }
}
