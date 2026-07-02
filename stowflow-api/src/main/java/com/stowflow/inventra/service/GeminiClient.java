package com.stowflow.inventra.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stowflow.inventra.config.GeminiProperties;
import com.stowflow.inventra.service.ChatIntentResolver.ResolvedIntent;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client Google Gemini — classification d'intention + reformulation optionnelle des réponses.
 * Les faits viennent toujours de {@link ChatService} ; fallback sur le template Java si Gemini échoue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {

    private static final Pattern SKU_PATTERN =
            Pattern.compile("(?i)sku[-\\s]?(\\d{3,6})");

    private static final Pattern ARTICLE_NAME_IN_JSON =
            Pattern.compile("\"articleName\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern INTENT_IN_JSON =
            Pattern.compile("\"intent\"\\s*:\\s*\"([A-Z_]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARTICLE_NAME_QUERY = Pattern.compile(
            "(?i)(?:article|produit)\\s+(?:nomm[eé]|appel[eé]|s'?appelle)\\s+[\"']?([^\"'?.,!]+)|"
                    + "(?:nomm[eé]|appel[eé])\\s+[\"']?([^\"'?.,!]+)|"
                    + "(?:y a[- ]t[- ]il|existe[- ]t[- ]il|est[- ]ce qu['']il y a)\\s+(?:un\\s+)?(?:article|produit)\\s+(?:nomm[eé]|appel[eé])?\\s*[\"']?([^\"'?.,!]+)|"
                    + "combien\\s+(?:de|d[''])?\\s*([^\"'?.,!\\s]+?)\\s+(?:il\\s+)?reste|"
                    + "stock\\s+(?:du|de|d[''])?\\s*([^\"'?.,!\\s]+)");

    private static final int MAX_USER_MESSAGE_CHARS = 400;
    private static final int MAX_FACTS_CHARS = 900;

    /** Intents où le template Java suffit — pas de 2e appel (économie tokens). */
    private static final Set<ChatIntent> SKIP_REPLY_POLISH = EnumSet.of(
            ChatIntent.GREETING,
            ChatIntent.UNKNOWN,
            ChatIntent.FAQ_PROCUREMENT,
            ChatIntent.FAQ_HELP,
            ChatIntent.OPEN_INVENTORY,
            ChatIntent.OPEN_ORDERS,
            ChatIntent.OPEN_REPLENISHMENT,
            ChatIntent.OPEN_SUPPLIERS,
            ChatIntent.OPEN_DASHBOARD,
            ChatIntent.OPEN_PRODUCTS,
            ChatIntent.OPEN_HELP,
            ChatIntent.OPEN_POS,
            ChatIntent.OPEN_PLATFORM);

    /** Prompt court pour limiter les tokens d'entrée. */
    private static final String INTENT_SYSTEM_PROMPT = """
            Routeur Inventra (stock SaaS). JSON uniquement :
            {"intent":"...","sku":null,"quantity":null,"articleName":null}
            Intents: GREETING, LIST_ALERTS, DASHBOARD_SUMMARY, ORDERS_SUMMARY, REPLENISHMENT_SUMMARY,
            ARTICLE_STOCK, ARTICLE_SEARCH, FAQ_PROCUREMENT, FAQ_HELP, OPEN_INVENTORY, OPEN_ORDERS,
            OPEN_REPLENISHMENT, OPEN_SUPPLIERS, OPEN_DASHBOARD, OPEN_PRODUCTS, OPEN_HELP, OPEN_POS,
            OPEN_PLATFORM, SUGGEST_REPLENISH, SUGGEST_ORDER, CREATE_REPLENISH, UNKNOWN.
            SKU format SKU-XXXX. articleName si recherche par nom. UNKNOWN si incertain.
            """;

    private static final String REPLY_SYSTEM_PROMPT = """
            Assistant Inventra. Reformule les faits en français naturel (2-4 phrases max).
            N'invente aucun chiffre, SKU ou nom absent des faits. Pas de markdown ni listes à puces.
            """;

    private final GeminiProperties properties;
    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;

    public ResolvedIntent classify(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return unknownIntent();
        }
        String message = stripLeadingGreeting(rawMessage.trim());
        if (isConfigured()) {
            try {
                String content = generateWithRetry(
                        INTENT_SYSTEM_PROMPT,
                        truncate(message, MAX_USER_MESSAGE_CHARS),
                        true,
                        properties.getMaxOutputTokensIntent());
                if (content != null && !content.isBlank()) {
                    ResolvedIntent parsed = parseIntentJson(content);
                    if (parsed != null && parsed.intent() != ChatIntent.UNKNOWN) {
                        return parsed;
                    }
                    log.warn("Gemini JSON non exploitable, tentative extraction souple");
                    ResolvedIntent loose = parseIntentLoose(content);
                    if (loose.intent() != ChatIntent.UNKNOWN) {
                        return loose;
                    }
                }
            } catch (Exception ex) {
                log.warn("Gemini classify indisponible : {}", ex.getMessage());
            }
        }
        ResolvedIntent fallback = heuristicFallback(message);
        if (fallback.intent() != ChatIntent.UNKNOWN) {
            log.info("Fallback heuristique : {}", fallback.intent());
            return fallback;
        }
        return unknownIntent();
    }

    /**
     * Reformule la réponse template. Retourne empty → fallback template Java dans {@link ChatService}.
     */
    public Optional<String> polishReply(String userMessage, String factualReply, ChatIntent intent) {
        if (!properties.isGenerateReplies() || !isConfigured()) {
            return Optional.empty();
        }
        if (intent == null || SKIP_REPLY_POLISH.contains(intent)) {
            return Optional.empty();
        }
        if (factualReply == null || factualReply.isBlank() || shouldSkipPolish(factualReply)) {
            return Optional.empty();
        }
        try {
            String userPrompt = "Q: " + truncate(userMessage, MAX_USER_MESSAGE_CHARS)
                    + "\nFaits:\n" + truncate(factualReply, MAX_FACTS_CHARS);
            String polished = generateWithRetry(REPLY_SYSTEM_PROMPT, userPrompt, false, properties.getMaxOutputTokensReply());
            if (polished == null || polished.isBlank()) {
                log.debug("Gemini polish vide — fallback template ({})", intent);
                return Optional.empty();
            }
            return Optional.of(polished.trim());
        } catch (RestClientResponseException ex) {
            log.warn(
                    "Gemini polish HTTP {} — fallback template ({})",
                    ex.getStatusCode().value(),
                    intent);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Gemini polish indisponible — fallback template ({}): {}", intent, ex.getMessage());
            return Optional.empty();
        }
    }

    private boolean isConfigured() {
        if (!properties.isEnabled()) {
            log.warn("Gemini désactivé");
            return false;
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("Clé API Gemini manquante");
            return false;
        }
        return true;
    }

    private static boolean shouldSkipPolish(String factualReply) {
        String lower = factualReply.toLowerCase(Locale.ROOT);
        return lower.startsWith("accès refusé")
                || lower.startsWith("indiquez")
                || lower.startsWith("je n'ai pas compris");
    }

    private String generateWithRetry(String systemPrompt, String userText, boolean jsonMode, int maxOutputTokens) {
        try {
            return generate(systemPrompt, userText, jsonMode, maxOutputTokens);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) {
                log.warn("Gemini 429 — nouvelle tentative dans 2s");
                sleepBriefly();
                return generate(systemPrompt, userText, jsonMode, maxOutputTokens);
            }
            log.warn(
                    "Gemini HTTP {} : {}",
                    ex.getStatusCode().value(),
                    truncate(ex.getResponseBodyAsString(), 200));
            throw ex;
        }
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String generate(String systemPrompt, String userText, boolean jsonMode, int maxOutputTokens) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("thinkingConfig", Map.of("thinkingBudget", 0));
        if (jsonMode) {
            generationConfig.put("responseMimeType", "application/json");
        }

        GeminiGenerateResponse response = geminiRestClient
                .post()
                .uri("/models/{model}:generateContent?key={apiKey}", properties.getModel(), properties.getApiKey())
                .body(Map.of(
                        "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                        "contents", List.of(Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userText)))),
                        "generationConfig", generationConfig))
                .retrieve()
                .body(GeminiGenerateResponse.class);

        return extractText(response);
    }

    private static String extractText(GeminiGenerateResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        GeminiCandidate candidate = response.candidates().get(0);
        if (candidate.content() == null || candidate.content().parts() == null) {
            return null;
        }
        return candidate.content().parts().stream()
                .map(GeminiPart::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse(null);
    }

    private ResolvedIntent parseIntentJson(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            String intentRaw = textOrNull(root, "intent");
            ChatIntent intent = parseIntent(intentRaw);
            String sku = normalizeSku(textOrNull(root, "sku"));
            Integer quantity = intOrNull(root, "quantity");
            String articleName = textOrNull(root, "articleName");
            return finalizeIntent(intent, sku, quantity, articleName);
        } catch (Exception ex) {
            log.warn("JSON Gemini illisible : {}", truncate(content, 120));
            return null;
        }
    }

    private ResolvedIntent parseIntentLoose(String content) {
        Matcher intentMatcher = INTENT_IN_JSON.matcher(content);
        if (!intentMatcher.find()) {
            return unknownIntent();
        }
        ChatIntent intent = parseIntent(intentMatcher.group(1));
        String articleName = extractRegexGroup(ARTICLE_NAME_IN_JSON.matcher(content));
        String sku = normalizeSku(extractRegexGroup(Pattern.compile("\"sku\"\\s*:\\s*\"([^\"]+)\"").matcher(content)));
        return finalizeIntent(intent, sku, null, articleName);
    }

    private ResolvedIntent heuristicFallback(String message) {
        String msg = normalize(message);
        if (msg.isBlank()) {
            return unknownIntent();
        }
        if (msg.matches("^(bonjour|salut|hello|coucou|bonsoir|hey|ca va)[\\s!.?]*$")) {
            return new ResolvedIntent(ChatIntent.GREETING, null, null, null);
        }

        String sku = extractSku(msg);
        String articleName = extractArticleNameFromQuery(msg);

        if (articleName != null && !articleName.isBlank()) {
            return new ResolvedIntent(ChatIntent.ARTICLE_SEARCH, null, null, articleName.trim());
        }
        if (sku != null) {
            if (msg.contains("reappro") || msg.contains("replenish") || msg.contains("demande")) {
                return new ResolvedIntent(ChatIntent.CREATE_REPLENISH, sku, extractQuantity(msg), null);
            }
            return new ResolvedIntent(ChatIntent.ARTICLE_STOCK, sku, null, null);
        }
        if (msg.contains("alerte") || msg.contains("rupture") || msg.contains("critique") || msg.contains("stock bas")) {
            return new ResolvedIntent(ChatIntent.LIST_ALERTS, null, null, null);
        }
        if (msg.contains("dashboard") || msg.contains("tableau de bord") || msg.contains("synthese") || msg.contains("resume")) {
            return new ResolvedIntent(ChatIntent.DASHBOARD_SUMMARY, null, null, null);
        }
        if (msg.contains("commande") && (msg.contains("attente") || msg.contains("pending"))) {
            return new ResolvedIntent(ChatIntent.ORDERS_SUMMARY, null, null, null);
        }
        if (msg.contains("reappro") && msg.contains("attente")) {
            return new ResolvedIntent(ChatIntent.REPLENISHMENT_SUMMARY, null, null, null);
        }
        if (msg.contains("aide") || msg.contains("comment utiliser") || msg.contains("inventra") || msg.contains("StowFlow")) {
            return new ResolvedIntent(ChatIntent.FAQ_HELP, null, null, null);
        }
        return unknownIntent();
    }

    private ResolvedIntent finalizeIntent(ChatIntent intent, String sku, Integer quantity, String articleName) {
        if (intent == ChatIntent.UNKNOWN) {
            return unknownIntent();
        }
        if (intent == ChatIntent.GREETING && articleName != null && !articleName.isBlank()) {
            intent = ChatIntent.ARTICLE_SEARCH;
        }
        if (intent == ChatIntent.ARTICLE_SEARCH && (articleName == null || articleName.isBlank())) {
            return unknownIntent();
        }
        if (intent == ChatIntent.ARTICLE_STOCK && sku == null) {
            intent = articleName != null && !articleName.isBlank()
                    ? ChatIntent.ARTICLE_SEARCH
                    : ChatIntent.UNKNOWN;
        }
        if (intent == ChatIntent.UNKNOWN) {
            return unknownIntent();
        }
        return new ResolvedIntent(intent, sku, quantity, articleName);
    }

    private static String stripLeadingGreeting(String raw) {
        return raw.replaceFirst("(?i)^(bonjour|salut|hello|coucou|bonsoir|hey)[\\s,!.?]+", "").trim();
    }

    private static String extractArticleNameFromQuery(String msg) {
        Matcher m = ARTICLE_NAME_QUERY.matcher(msg);
        if (!m.find()) {
            return null;
        }
        for (int i = 1; i <= m.groupCount(); i++) {
            String group = m.group(i);
            if (group != null && !group.isBlank()) {
                return group.trim();
            }
        }
        return null;
    }

    private static String extractRegexGroup(Matcher matcher) {
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String extractSku(String msg) {
        Matcher m = SKU_PATTERN.matcher(msg);
        if (!m.find()) {
            return null;
        }
        return "SKU-" + m.group(1);
    }

    private static Integer extractQuantity(String msg) {
        Matcher m = Pattern.compile("(?i)(?:quantit[eé]|qty|qte|)\\s*(\\d{1,6})\\b").matcher(msg);
        Integer last = null;
        while (m.find()) {
            last = Integer.parseInt(m.group(1));
        }
        return last;
    }

    private static String normalize(String raw) {
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
                .replace('ù', 'u')
                .replace('ô', 'o')
                .replace('î', 'i')
                .replace('ç', 'c');
    }

    private static ResolvedIntent unknownIntent() {
        return new ResolvedIntent(ChatIntent.UNKNOWN, null, null, null);
    }

    private static ChatIntent parseIntent(String raw) {
        if (raw == null || raw.isBlank()) {
            return ChatIntent.UNKNOWN;
        }
        try {
            return ChatIntent.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ChatIntent.UNKNOWN;
        }
    }

    private static String textOrNull(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer intOrNull(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.intValue();
        }
        try {
            return Integer.parseInt(node.asText().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizeSku(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        Matcher m = SKU_PATTERN.matcher(raw.trim());
        if (m.find()) {
            return "SKU-" + m.group(1);
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        return upper.startsWith("SKU-") ? upper : null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "…";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiGenerateResponse(List<GeminiCandidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiCandidate(GeminiContent content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiContent(List<GeminiPart> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiPart(String text) {}
}
