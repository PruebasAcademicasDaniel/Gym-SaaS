package testsupport.tenant;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantIsolationFixtureRepository extends JpaRepository<TenantIsolationFixture, UUID> {
}
