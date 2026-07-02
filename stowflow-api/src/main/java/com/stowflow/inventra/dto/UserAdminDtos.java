package com.stowflow.inventra.dto;

import com.stowflow.inventra.domain.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserAdminDtos {

    public record UserRow(Long id, String email, String role, boolean enabled) {}

    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 128) String password,
            @NotNull AppRole role
    ) {}

    public record UpdateUserRequest(AppRole role, Boolean enabled) {}

    private UserAdminDtos() {}
}
