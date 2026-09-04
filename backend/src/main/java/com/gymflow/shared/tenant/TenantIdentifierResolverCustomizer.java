package com.gymflow.shared.tenant;

import java.util.Map;
import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/** Registra el resolver en Hibernate. Solo hace falta esta property — el modo "database/schema per tenant" necesitaría además un MultiTenantConnectionProvider, pero con columna discriminadora no. */
@Component
public class TenantIdentifierResolverCustomizer implements HibernatePropertiesCustomizer {

    private final GymTenantIdentifierResolver resolver;

    public TenantIdentifierResolverCustomizer(GymTenantIdentifierResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
