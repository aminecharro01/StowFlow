package com.stowflow.inventra.security;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.dto.ChatDtos;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * RBAC assistant chat — aligné sur {@link InventraPolicies} (moindre privilège).
 */
public final class ChatRbac {

    private ChatRbac() {}

    public static ChatDtos.ChatResponse apply(AppRole role, ChatDtos.ChatResponse raw) {
        if (raw == null) {
            return ChatDtos.ChatResponse.text("Erreur assistant.");
        }
        return ChatDtos.ChatResponse.of(
                raw.reply(),
                filterSuggestions(role, raw.suggestions()),
                filterActions(role, raw.actions()),
                filterConfirmAction(role, raw.confirmAction()));
    }

    /** Suggestions initiales (puces rapides) selon le rôle. */
    public static List<String> quickSuggestions(AppRole role) {
        List<String> s = new ArrayList<>();
        if (canReadAlerts(role)) {
            s.add("Quels articles sont en alerte ?");
        }
        if (canReadDashboard(role)) {
            s.add("Résumé du dashboard");
        }
        if (role == AppRole.SUPER_ADMIN) {
            s.add("Ouvrir la plateforme");
        }
        if (canReadOrders(role)) {
            s.add("Commandes en attente");
        }
        if (canReadReplenishment(role) && !canCreateOrder(role)) {
            s.add("Demandes réappro en attente");
        }
        if (canCreateReplenishment(role)) {
            s.add("Créer réappro pour SKU-0088");
        }
        if (canCreateOrder(role)) {
            s.add("Stock du SKU-0021");
        }
        if (role == AppRole.SALES) {
            s.add("Stock du SKU-0021");
            s.add("Ouvrir le point de vente");
        }
        s.add("Différence commande et réappro");
        return s.stream().distinct().collect(Collectors.toList());
    }

    public static String greetingMessage(AppRole role) {
        String label = roleLabel(role);
        return switch (role) {
            case SUPER_ADMIN ->
                    "Bonjour ! Assistant StowFlow (Gemini) — connecté en tant que " + label
                            + ". Posez votre question en français naturel sur le pilotage, les alertes ou la plateforme.";
            case TENANT_ADMIN ->
                    "Bonjour ! Assistant StowFlow (Gemini) — " + label
                            + ". Posez une question sur le pilotage, les commandes ou les alertes.";
            case STOCK_MANAGER ->
                    "Bonjour ! Assistant StowFlow (Gemini) — " + label
                            + ". Interrogez-moi sur le stock, les alertes, commandes ou réapprovisionnement.";
            case MANAGER ->
                    "Bonjour ! Assistant StowFlow (Gemini) — " + label
                            + ". Posez une question sur le pilotage, les alertes ou les réapprovisionnements.";
            case SALES ->
                    "Bonjour ! Assistant StowFlow (Gemini) — " + label
                            + ". Demandez la disponibilité d'un article, les alertes ou le point de vente.";
        };
    }

    public static List<String> filterSuggestions(AppRole role, List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }
        return suggestions.stream()
                .filter(s -> suggestionAllowed(role, s))
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<ChatDtos.ChatAction> filterActions(AppRole role, List<ChatDtos.ChatAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return List.of();
        }
        return actions.stream()
                .filter(a -> actionAllowed(role, a))
                .collect(Collectors.toList());
    }

    public static ChatDtos.ConfirmAction filterConfirmAction(AppRole role, ChatDtos.ConfirmAction action) {
        if (action == null) {
            return null;
        }
        if ("create_replenishment".equals(action.type()) && canCreateReplenishment(role)) {
            return action;
        }
        return null;
    }

    static boolean suggestionAllowed(AppRole role, String suggestion) {
        if (suggestion == null || suggestion.isBlank()) {
            return false;
        }
        String s = normalize(suggestion);

        if (s.contains("plateforme") || s.contains("tenant")) {
            return role == AppRole.SUPER_ADMIN;
        }
        if (s.contains("point de vente") || s.equals("ouvrir pos") || s.contains("ouvrir pos")) {
            return role == AppRole.SALES;
        }
        if (s.contains("dashboard") || s.contains("tableau de bord")) {
            return canReadDashboard(role);
        }
        if (s.contains("commande")) {
            if (s.contains("creer") || s.contains("commander") || s.contains("nouvelle")) {
                return canCreateOrder(role);
            }
            return canReadOrders(role);
        }
        if (s.contains("reappro")) {
            if (s.contains("creer") || s.contains("lancer")) {
                return canCreateReplenishment(role);
            }
            return canReadReplenishment(role);
        }
        if (s.contains("alerte") || s.contains("rupture") || s.contains("inventaire")) {
            return canReadAlerts(role);
        }
        if (s.contains("stock") || s.contains("sku")) {
            return canReadArticles(role);
        }
        if (s.contains("fournisseur")) {
            return canReadSuppliers(role);
        }
        if (s.contains("aide")) {
            return true;
        }
        if (s.contains("difference") || s.contains("plafond") || s.contains("faq")) {
            return true;
        }
        if (s.contains("article") || s.contains("catalogue") || s.contains("produit")) {
            return canReadArticles(role);
        }
        return false;
    }

    static boolean actionAllowed(AppRole role, ChatDtos.ChatAction action) {
        if (action == null || action.href() == null || action.href().isBlank()) {
            return false;
        }
        String href = action.href().split("\\?")[0];
        String full = action.href();

        if (href.startsWith("/platform")) {
            return role == AppRole.SUPER_ADMIN;
        }
        if (href.startsWith("/settings")) {
            return role == AppRole.SUPER_ADMIN || role == AppRole.TENANT_ADMIN;
        }
        if (href.startsWith("/pos/history")) {
            return role == AppRole.SALES
                    || role == AppRole.MANAGER
                    || role == AppRole.TENANT_ADMIN
                    || role == AppRole.SUPER_ADMIN;
        }
        if (href.startsWith("/pos")) {
            return role == AppRole.SALES;
        }
        if (href.startsWith("/dashboard")) {
            return canReadDashboard(role);
        }
        if (href.startsWith("/inventory")) {
            return canReadInventoryNav(role);
        }
        if (href.startsWith("/orders")) {
            if (full.contains("create=1")) {
                return canCreateOrder(role);
            }
            return canReadOrders(role);
        }
        if (href.startsWith("/replenishment")) {
            if (full.contains("create=1")) {
                return canCreateReplenishment(role);
            }
            return canReadReplenishment(role);
        }
        if (href.startsWith("/suppliers")) {
            return canReadSuppliers(role);
        }
        if (href.startsWith("/reports")) {
            return role == AppRole.SUPER_ADMIN || role == AppRole.TENANT_ADMIN || role == AppRole.MANAGER;
        }
        if (href.startsWith("/products")) {
            return canReadArticles(role);
        }
        if (href.startsWith("/help")) {
            return true;
        }
        return false;
    }

    public static boolean canReadAlerts(AppRole role) {
        return role == AppRole.SUPER_ADMIN
                || role == AppRole.TENANT_ADMIN
                || role == AppRole.STOCK_MANAGER
                || role == AppRole.MANAGER
                || role == AppRole.SALES;
    }

    public static boolean canReadDashboard(AppRole role) {
        return role == AppRole.SUPER_ADMIN
                || role == AppRole.TENANT_ADMIN
                || role == AppRole.STOCK_MANAGER
                || role == AppRole.MANAGER;
    }

    public static boolean canReadOrders(AppRole role) {
        return canReadDashboard(role);
    }

    public static boolean canReadReplenishment(AppRole role) {
        return canReadDashboard(role);
    }

    public static boolean canReadSuppliers(AppRole role) {
        return canReadDashboard(role);
    }

    public static boolean canReadInventoryNav(AppRole role) {
        return canReadDashboard(role);
    }

    public static boolean canReadArticles(AppRole role) {
        return true;
    }

    public static boolean canCreateReplenishment(AppRole role) {
        return role == AppRole.STOCK_MANAGER || role == AppRole.MANAGER;
    }

    public static boolean canCreateOrder(AppRole role) {
        return role == AppRole.STOCK_MANAGER;
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

    private static String roleLabel(AppRole role) {
        return switch (role) {
            case SUPER_ADMIN -> "Super administrateur";
            case TENANT_ADMIN -> "Administrateur";
            case STOCK_MANAGER -> "Gestionnaire de stock";
            case MANAGER -> "Manager";
            case SALES -> "Commercial";
        };
    }
}
