package com.stowflow.inventra.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TenantDtos {

    public record TenantRow(Long id, String slug, String name, int userCount, boolean ready) {}

    public record CreateTenantRequest(
            @NotBlank
            @Size(min = 2, max = 64)
            @Pattern(regexp = "[a-z0-9][a-z0-9-]*", message = "Slug : minuscules, chiffres et tirets uniquement")
            String slug,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Email String adminEmail,
            @NotBlank @Size(min = 6, max = 128) String adminPassword
    ) {}

    private TenantDtos() {}
}
