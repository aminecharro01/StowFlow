package com.stowflow.inventra.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Résolution d'intention via Google Gemini uniquement.
 */
@Component
@RequiredArgsConstructor
public class ChatIntentResolver {

    private final GeminiClient geminiClient;

    public record ResolvedIntent(ChatIntent intent, String sku, Integer quantity, String searchName) {

        public ResolvedIntent(ChatIntent intent, String sku, Integer quantity) {
            this(intent, sku, quantity, null);
        }
    }

    public ResolvedIntent resolve(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return unknown();
        }
        return geminiClient.classify(rawMessage.trim());
    }

    private static ResolvedIntent unknown() {
        return new ResolvedIntent(ChatIntent.UNKNOWN, null, null, null);
    }
}
