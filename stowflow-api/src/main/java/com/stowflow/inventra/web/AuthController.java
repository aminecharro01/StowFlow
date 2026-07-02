package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.AuthDtos;
import com.stowflow.inventra.security.InventraUserDetails;
import com.stowflow.inventra.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        InventraUserDetails principal = (InventraUserDetails) auth.getPrincipal();
        String accessToken = jwtService.generateAccessToken(principal);
        return AuthDtos.LoginResponse.of(accessToken, jwtService.accessTokenExpiresInSeconds(), principal);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public AuthDtos.SessionResponse me(Authentication authentication) {
        return AuthDtos.SessionResponse.from((InventraUserDetails) authentication.getPrincipal());
    }
}
