package com.stowflow.inventra.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start in production when JWT secret is missing or still the dev default.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class JwtSecretStartupValidator {

    static final String DEV_DEFAULT_SECRET =
            "StowFlowDevSecretKey2026ChangeInProductionMustBe256BitsMinimum!!";

    private final JwtProperties jwtProperties;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        String secret = jwtProperties.getSecret();
        String envSecret = environment.getProperty("JWT_SECRET");

        if (envSecret == null || envSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET environment variable is required in production profile.");
        }
        if (DEV_DEFAULT_SECRET.equals(secret) || DEV_DEFAULT_SECRET.equals(envSecret)) {
            throw new IllegalStateException(
                    "JWT_SECRET must not use the development default in production profile.");
        }
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters in production.");
        }
        log.info("JWT secret validated for production profile.");
    }
}
