package com.stowflow.inventra.web;

import com.stowflow.inventra.dto.ArticleDtos;
import com.stowflow.inventra.dto.PageDtos;
import com.stowflow.inventra.security.InventraPolicies;
import com.stowflow.inventra.service.ArticleService;
import com.stowflow.inventra.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize(InventraPolicies.ARTICLES_READ)
    public PageDtos.PageResponse<ArticleDtos.ArticleResponse> list(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return articleService.list(tenantService.requireBySlug(tenantSlug), q, status, category, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(InventraPolicies.ARTICLES_READ)
    public ArticleDtos.ArticleResponse get(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id
    ) {
        return articleService.get(tenantService.requireBySlug(tenantSlug), id);
    }

    @PostMapping
    @PreAuthorize(InventraPolicies.ARTICLES_WRITE)
    public ArticleDtos.ArticleResponse create(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @Valid @RequestBody ArticleDtos.CreateArticleRequest body
    ) {
        return articleService.create(tenantService.requireBySlug(tenantSlug), body);
    }

    @PutMapping("/{id}")
    @PreAuthorize(InventraPolicies.ARTICLES_WRITE)
    public ArticleDtos.ArticleResponse update(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id,
            @Valid @RequestBody ArticleDtos.UpdateArticleRequest body
    ) {
        return articleService.update(tenantService.requireBySlug(tenantSlug), id, body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(InventraPolicies.ARTICLES_WRITE)
    public void archive(
            @RequestHeader(value = TenantHeaders.TENANT_SLUG, defaultValue = "default") String tenantSlug,
            @PathVariable Long id
    ) {
        articleService.archive(tenantService.requireBySlug(tenantSlug), id);
    }
}
