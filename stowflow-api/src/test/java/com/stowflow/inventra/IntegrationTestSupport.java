package com.stowflow.inventra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.AppUser;
import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.AuthDtos;
import com.stowflow.inventra.repo.AppUserRepository;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.repo.TenantRepository;
import com.stowflow.inventra.web.TenantHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

public abstract class IntegrationTestSupport extends AbstractIntegrationTest {

    protected static final String PASSWORD = "password";

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TenantRepository tenantRepository;

    @Autowired
    protected AppUserRepository appUserRepository;

    @Autowired
    protected ArticleRepository articleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected Tenant defaultTenant;
    protected Tenant otherTenant;
    protected AppUser stockUser;
    protected AppUser salesUser;

    @BeforeEach
    void seedBaseData() {
        jdbcTemplate.execute(
                """
                TRUNCATE audit_events, stock_movements, sale_lines, sales,
                replenishment_requests, purchase_order_lines, purchase_orders,
                app_users, articles, suppliers, tenants RESTART IDENTITY CASCADE
                """);
        defaultTenant = tenantRepository.save(Tenant.builder().slug("default").name("Default Tenant").build());
        otherTenant = tenantRepository.save(Tenant.builder().slug("other").name("Other Tenant").build());

        String hash = passwordEncoder.encode(PASSWORD);
        stockUser = appUserRepository.save(AppUser.builder()
                .email("stock@default.demo")
                .passwordHash(hash)
                .role(AppRole.STOCK_MANAGER)
                .tenant(defaultTenant)
                .enabled(true)
                .build());
        salesUser = appUserRepository.save(AppUser.builder()
                .email("sales@default.demo")
                .passwordHash(hash)
                .role(AppRole.SALES)
                .tenant(defaultTenant)
                .enabled(true)
                .build());
        appUserRepository.save(AppUser.builder()
                .email("stock@other.demo")
                .passwordHash(hash)
                .role(AppRole.STOCK_MANAGER)
                .tenant(otherTenant)
                .enabled(true)
                .build());
    }

    protected String loginToken(String email) throws Exception {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest(email, PASSWORD);
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    protected Article saveArticle(Tenant tenant, String sku, int qty) {
        return articleRepository.save(Article.builder()
                .tenant(tenant)
                .sku(sku)
                .name("Test " + sku)
                .category("Test")
                .salePrice(java.math.BigDecimal.TEN)
                .purchasePrice(java.math.BigDecimal.valueOf(5))
                .minThreshold(5)
                .maxThreshold(100)
                .quantityOnHand(qty)
                .archived(false)
                .build());
    }

    protected org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorized(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
            String token,
            String tenantSlug
    ) {
        return builder.header("Authorization", "Bearer " + token).header(TenantHeaders.TENANT_SLUG, tenantSlug);
    }
}
