package com.stowflow.inventra.security;

import com.stowflow.inventra.repo.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventraUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return appUserRepository
                .findByEmailIgnoreCase(username)
                .map(InventraUserDetails::fromEntity)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu: " + username));
    }
}
