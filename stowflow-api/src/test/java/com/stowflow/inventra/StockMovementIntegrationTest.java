package com.stowflow.inventra;

import com.stowflow.inventra.domain.MovementType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

class StockMovementIntegrationTest extends IntegrationTestSupport {

    @Test
    void outMovementFailsWhenInsufficientStock() throws Exception {
        var article = saveArticle(defaultTenant, "SKU-OUT", 2);
        String token = loginToken("stock@default.demo");

        String body = """
                {"articleId":%d,"type":"OUT","quantity":5,"note":"test"}
                """.formatted(article.getId());

        mockMvc.perform(authorized(MockMvcRequestBuilders.post("/api/movements"), token, "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Stock insuffisant pour cette sortie (RG10)."));
    }

    @Test
    void outMovementSucceedsWithSufficientStock() throws Exception {
        var article = saveArticle(defaultTenant, "SKU-OK", 10);
        String token = loginToken("stock@default.demo");

        String body = """
                {"articleId":%d,"type":"%s","quantity":3,"note":"test"}
                """.formatted(article.getId(), MovementType.OUT.name());

        mockMvc.perform(authorized(MockMvcRequestBuilders.post("/api/movements"), token, "default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.quantity").value(3));
    }
}
