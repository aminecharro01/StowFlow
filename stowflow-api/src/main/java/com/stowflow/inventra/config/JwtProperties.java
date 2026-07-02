package com.stowflow.inventra.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventra.jwt")
@Getter
@Setter
public class JwtProperties {

    /** Clé HMAC (≥ 32 caractères pour HS256). Surcharger via JWT_SECRET en production. */
    private String secret = "StowFlowDevSecretKey2026ChangeInProductionMustBe256BitsMinimum!!";

    private Duration accessTokenExpiration = Duration.ofMinutes(30);
}
