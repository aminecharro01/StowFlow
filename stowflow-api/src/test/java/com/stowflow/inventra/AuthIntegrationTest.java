package com.stowflow.inventra;

import com.stowflow.inventra.dto.AuthDtos;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    void loginSuccess() throws Exception {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("stock@default.demo", PASSWORD);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.accessToken").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email").value("stock@default.demo"));
    }

    @Test
    void loginBadCredentialsReturns401() throws Exception {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("stock@default.demo", "wrong-password");
        mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void protectedRouteWithoutTokenReturns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/articles"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
