package testsupport.tenant;

import com.gymflow.shared.tenant.AbstractTenantEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Existe solo para TenantIsolationIT: prueba el mecanismo de
 * AbstractTenantEntity/@TenantId de forma aislada, antes de que exista
 * ninguna entidad de negocio real que lo use (la primera es Fase 6+).
 *
 * Vive fuera de com.gymflow a propósito: si estuviera dentro, el entity
 * scan por defecto de Spring Boot la registraría en TODOS los contextos de
 * test (AuthFlowIT incluido), y esos otros contextos fallarían al
 * arrancar por no tener la tabla (solo TenantIsolationIT corre la
 * migración de test que la crea).
 */
@Entity
@Table(name = "tenant_isolation_fixture")
public class TenantIsolationFixture extends AbstractTenantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String label;

    protected TenantIsolationFixture() {
        // JPA
    }

    public TenantIsolationFixture(String label) {
        this.label = label;
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }
}
