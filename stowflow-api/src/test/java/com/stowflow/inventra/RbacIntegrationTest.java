package com.stowflow.inventra;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

class RbacIntegrationTest extends IntegrationTestSupport {

    @Test
    void salesCannotCreateArticles() throws Exception {
        String token = loginToken("sales@default.demo");
        String body = """
                {
                  "name": "Blocked Product",
                  "sku": "SKU-BLOCK",
                  "category": "Test",
                  "salePrice": 10,
                  "minThreshold": 1,
                  "maxThreshold": 50
                }
                """;

        mockMvc.perform(authorized(MockMvcRequestBuilders.post("/api/articles"), token, "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void stockManagerCanCreateArticles() throws Exception {
        String token = loginToken("stock@default.demo");
        String body = """
                {
                  "name": "Allowed Product",
                  "sku": "SKU-ALLOW",
                  "category": "Test",
                  "salePrice": 10,
                  "minThreshold": 1,
                  "maxThreshold": 50
                }
                """;

        mockMvc.perform(authorized(MockMvcRequestBuilders.post("/api/articles"), token, "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.sku").value("SKU-ALLOW"));
    }
}
