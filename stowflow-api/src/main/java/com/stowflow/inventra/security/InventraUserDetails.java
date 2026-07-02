package com.stowflow.inventra.security;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.AppUser;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class InventraUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String passwordHash;
    private final AppRole role;
    /** Slug tenant ; null si {@link AppRole#SUPER_ADMIN}. */
    private final String tenantSlug;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public InventraUserDetails(
            Long userId,
            String email,
            String passwordHash,
            AppRole role,
            String tenantSlug,
            boolean enabled
    ) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.tenantSlug = tenantSlug;
        this.enabled = enabled;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static InventraUserDetails fromEntity(AppUser u) {
        String slug = u.getTenant() != null ? u.getTenant().getSlug() : null;
        return new InventraUserDetails(
                u.getId(),
                u.getEmail(),
                u.getPasswordHash(),
                u.getRole(),
                slug,
                u.isEnabled()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
