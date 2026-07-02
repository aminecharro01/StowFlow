package com.stowflow.inventra;

import com.stowflow.inventra.web.TenantHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

class TenantIsolationIntegrationTest extends IntegrationTestSupport {

    @Test
    void wrongTenantHeaderReturns403() throws Exception {
        saveArticle(defaultTenant, "SKU-001", 10);
        String token = loginToken("stock@default.demo");

        mockMvc.perform(authorized(MockMvcRequestBuilders.get("/api/articles"), token, "other"))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Ce compte n'a pas accès à ce tenant."));
    }

    @Test
    void correctTenantHeaderAllowsAccess() throws Exception {
        saveArticle(defaultTenant, "SKU-002", 10);
        String token = loginToken("stock@default.demo");

        mockMvc.perform(authorized(MockMvcRequestBuilders.get("/api/articles"), token, "default"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.content").isArray());
    }
}
