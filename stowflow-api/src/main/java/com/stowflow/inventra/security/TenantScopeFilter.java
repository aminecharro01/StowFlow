package com.stowflow.inventra.security;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.web.TenantHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Isole les données par tenant : l'en-tête {@value TenantHeaders#TENANT_SLUG} doit correspondre au tenant du
 * compte, sauf pour {@link AppRole#SUPER_ADMIN}.
 */
@Component
public class TenantScopeFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/api/")) {
            return true;
        }
        return uri.startsWith("/api/auth") || uri.startsWith("/api/platform");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken
                || !(auth.getPrincipal() instanceof InventraUserDetails principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (principal.getRole() == AppRole.SUPER_ADMIN) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerSlug = request.getHeader(TenantHeaders.TENANT_SLUG);
        if (headerSlug == null || headerSlug.isBlank()) {
            writeForbidden(response, "En-tête X-Tenant-Slug requis.");
            return;
        }

        String expected = principal.getTenantSlug();
        if (expected == null || !expected.equalsIgnoreCase(headerSlug.trim())) {
            writeForbidden(response, "Ce compte n'a pas accès à ce tenant.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = "{\"error\":\"" + message.replace("\"", "\\\"") + "\"}";
        response.getWriter().write(json);
    }
}
