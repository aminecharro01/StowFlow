package com.stowflow.inventra.bootstrap;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.AppUser;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.repo.AppUserRepository;
import com.stowflow.inventra.repo.TenantRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "inventra", name = "seed-demo", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "password";

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoScenarioSeeder demoScenarioSeeder;

    @Override
    public void run(String... args) {
        if (tenantRepository.count() == 0) {
            demoScenarioSeeder.seedFresh();
        } else {
            demoScenarioSeeder.enrichIfNeeded();
        }
        ensureDemoUsers();
    }

    private void ensureDemoUsers() {
        Tenant tenant = tenantRepository
                .findBySlug("default")
                .orElseGet(() -> {
                    log.warn("Tenant 'default' absent — initialisation des données de démo.");
                    demoScenarioSeeder.seedFresh();
                    return tenantRepository
                            .findBySlug("default")
                            .orElseThrow(() -> new IllegalStateException("Impossible de créer le tenant default."));
                });

        String enc = passwordEncoder.encode(DEMO_PASSWORD);

        upsertDemoUser("superadmin@stowflow.demo", AppRole.SUPER_ADMIN, null, null, enc);
        upsertDemoUser("admin@default.demo", AppRole.TENANT_ADMIN, tenant, null, enc);
        upsertDemoUser("stock@default.demo", AppRole.STOCK_MANAGER, tenant, null, enc);
        upsertDemoUser("sales@default.demo", AppRole.SALES, tenant, null, enc);
        upsertDemoUser("manager@default.demo", AppRole.MANAGER, tenant, null, enc);

        log.info(
                "Comptes démo prêts (mot de passe : « {} ») — ex. admin@default.demo, stock@default.demo",
                DEMO_PASSWORD);
    }

    private void upsertDemoUser(
            String email,
            AppRole role,
            Tenant tenant,
            com.stowflow.inventra.domain.Supplier supplier,
            String passwordHash
    ) {
        Optional<AppUser> existing = appUserRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            AppUser u = existing.get();
            u.setPasswordHash(passwordHash);
            u.setRole(role);
            u.setTenant(tenant);
            u.setSupplier(supplier);
            u.setEnabled(true);
            appUserRepository.save(u);
            return;
        }
        appUserRepository.save(AppUser.builder()
                .email(email)
                .passwordHash(passwordHash)
                .role(role)
                .tenant(tenant)
                .supplier(supplier)
                .enabled(true)
                .build());
    }
}
