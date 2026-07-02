package com.stowflow.inventra.dto;

import com.stowflow.inventra.security.InventraUserDetails;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record SessionResponse(String email, String role, String tenantSlug) {

        public static SessionResponse from(InventraUserDetails u) {
            return new SessionResponse(u.getUsername(), u.getRole().name(), u.getTenantSlug());
        }
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            String email,
            String role,
            String tenantSlug
    ) {

        public static LoginResponse of(String accessToken, long expiresIn, InventraUserDetails u) {
            return new LoginResponse(
                    accessToken,
                    "Bearer",
                    expiresIn,
                    u.getUsername(),
                    u.getRole().name(),
                    u.getTenantSlug());
        }
    }

    private AuthDtos() {}
}
