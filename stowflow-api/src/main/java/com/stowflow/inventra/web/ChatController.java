package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.ChatDtos;
import com.stowflow.inventra.security.InventraUserDetails;
import com.stowflow.inventra.service.ChatService;
import com.stowflow.inventra.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final TenantService tenantService;

    @GetMapping("/bootstrap")
    @PreAuthorize("isAuthenticated()")
    public ChatDtos.ChatBootstrapResponse bootstrap(Authentication authentication) {
        InventraUserDetails user = (InventraUserDetails) authentication.getPrincipal();
        return chatService.bootstrap(user);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ChatDtos.ChatResponse chat(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @Valid @RequestBody ChatDtos.ChatRequest body,
            Authentication authentication
    ) {
        InventraUserDetails user = (InventraUserDetails) authentication.getPrincipal();
        return chatService.handle(tenantService.requireBySlug(tenantSlug), user, body.message());
    }
}
