package com.stowflow.inventra.service;

import com.stowflow.inventra.domain.AppRole;
import com.stowflow.inventra.domain.Article;
import com.stowflow.inventra.domain.Supplier;
import com.stowflow.inventra.domain.Tenant;
import com.stowflow.inventra.dto.AlertDtos;
import com.stowflow.inventra.dto.ChatDtos;
import com.stowflow.inventra.dto.DashboardDtos;
import com.stowflow.inventra.dto.OrderDtos;
import com.stowflow.inventra.dto.ReplenishmentDtos;
import com.stowflow.inventra.exception.BusinessException;
import com.stowflow.inventra.repo.ArticleRepository;
import com.stowflow.inventra.repo.SupplierRepository;
import com.stowflow.inventra.security.ChatRbac;
import com.stowflow.inventra.security.InventraUserDetails;
import com.stowflow.inventra.service.ChatIntentResolver.ResolvedIntent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatIntentResolver intentResolver;
    private final GeminiClient geminiClient;
    private final AlertService alertService;
    private final DashboardService dashboardService;
    private final PurchaseOrderService purchaseOrderService;
    private final ReplenishmentRequestService replenishmentRequestService;
    private final ArticleRepository articleRepository;
    private final SupplierRepository supplierRepository;
    private final ProcurementPolicy procurementPolicy;

    @Transactional(readOnly = true)
    public ChatDtos.ChatBootstrapResponse bootstrap(InventraUserDetails user) {
        AppRole role = user.getRole();
        return new ChatDtos.ChatBootstrapResponse(
                ChatRbac.greetingMessage(role),
                ChatRbac.quickSuggestions(role));
    }

    @Transactional(readOnly = true)
    public ChatDtos.ChatResponse handle(Tenant tenant, InventraUserDetails user, String message) {
        ResolvedIntent resolved = intentResolver.resolve(message);
        AppRole role = user.getRole();

        ChatDtos.ChatResponse raw = switch (resolved.intent()) {
            case GREETING -> greeting(role);
            case FAQ_PROCUREMENT -> faqProcurement(role);
            case FAQ_HELP -> faqHelp(role);
            case LIST_ALERTS -> listAlerts(tenant, role);
            case DASHBOARD_SUMMARY -> dashboardSummary(tenant, role);
            case ORDERS_SUMMARY -> ordersSummary(tenant, role);
            case REPLENISHMENT_SUMMARY -> replenishmentSummary(tenant, role);
            case ARTICLE_STOCK -> articleStock(tenant, role, resolved.sku());
            case ARTICLE_SEARCH -> articleSearch(tenant, role, resolved.searchName());
            case OPEN_INVENTORY -> navigate("/inventory", "Stocks & alertes", role, ChatRbac.canReadInventoryNav(role));
            case OPEN_ORDERS -> navigate("/orders", "Commandes fournisseurs", role, ChatRbac.canReadOrders(role));
            case OPEN_REPLENISHMENT -> navigate("/replenishment", "Demandes réappro.", role, ChatRbac.canReadReplenishment(role));
            case OPEN_SUPPLIERS -> navigate("/suppliers", "Fournisseurs", role, ChatRbac.canReadSuppliers(role));
            case OPEN_DASHBOARD -> navigate("/dashboard", "Tableau de bord", role, ChatRbac.canReadDashboard(role));
            case OPEN_PRODUCTS -> navigate("/products", "Articles", role, ChatRbac.canReadArticles(role));
            case OPEN_HELP -> navigate("/help", "Aide & support", role, true);
            case OPEN_POS -> navigate("/pos", "Point de vente", role, role == AppRole.SALES);
            case OPEN_PLATFORM -> navigate("/platform/tenants", "Plateforme SaaS", role, role == AppRole.SUPER_ADMIN);
            case SUGGEST_REPLENISH -> suggestReplenish(tenant, role, resolved.sku());
            case SUGGEST_ORDER -> suggestOrder(tenant, role, resolved.sku());
            case CREATE_REPLENISH -> createReplenishProposal(tenant, role, resolved.sku(), resolved.quantity());
            case UNKNOWN -> unknown(role);
        };

        return ChatRbac.apply(role, withPolishedReply(message, resolved.intent(), raw));
    }

    /** Template Java d'abord ; Gemini reformule si activé, sinon fallback silencieux. */
    private ChatDtos.ChatResponse withPolishedReply(String userMessage, ChatIntent intent, ChatDtos.ChatResponse template) {
        return geminiClient
                .polishReply(userMessage, template.reply(), intent)
                .map(text -> ChatDtos.ChatResponse.of(
                        text,
                        template.suggestions(),
                        template.actions(),
                        template.confirmAction()))
                .orElse(template);
    }

    private ChatDtos.ChatResponse greeting(AppRole role) {
        return ChatDtos.ChatResponse.of(
                ChatRbac.greetingMessage(role),
                ChatRbac.quickSuggestions(role),
                List.of(),
                null);
    }

    private ChatDtos.ChatResponse unknown(AppRole role) {
        return ChatDtos.ChatResponse.of(
                "Je n'ai pas compris cette demande. Reformulez en français naturel "
                        + "(ex. « quels articles sont en alerte ? », « stock du SKU-0021 », "
                        + "« existe-t-il un article keyboard ? »).",
                ChatRbac.quickSuggestions(role),
                List.of(navigateAction("Aide", "/help")),
                null);
    }

    private ChatDtos.ChatResponse faqProcurement(AppRole role) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Articles en alerte");
        if (ChatRbac.canReadReplenishment(role)) {
            suggestions.add("Ouvrir réapprovisionnement");
        }
        if (ChatRbac.canReadOrders(role)) {
            suggestions.add("Ouvrir commandes");
        }

        List<ChatDtos.ChatAction> actions = new ArrayList<>();
        if (ChatRbac.canReadOrders(role)) {
            actions.add(navigateAction("Commandes", "/orders"));
        }
        if (ChatRbac.canReadReplenishment(role)) {
            actions.add(navigateAction("Réapprovisionnement", "/replenishment"));
        }

        return ChatDtos.ChatResponse.of(
                "Deux flux distincts :\n"
                        + "• Commande fournisseur — nouveau produit ou stock sain (gestionnaire de stock).\n"
                        + "• Demande de réappro — produit déjà approvisionné et en alerte (gestionnaire ou manager).\n"
                        + "Règle plafond : stock actuel + quantité ≤ maxThreshold.",
                suggestions,
                actions,
                null);
    }

    private ChatDtos.ChatResponse faqHelp(AppRole role) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Ouvrir aide");
        if (ChatRbac.canReadDashboard(role)) {
            suggestions.add("Résumé dashboard");
        }
        return ChatDtos.ChatResponse.of(
                "Utilisez le menu latéral selon votre rôle. Les actions sensibles (annulation, archivage) "
                        + "demandent une confirmation. Support 24/7 sur la page Aide.",
                suggestions,
                List.of(navigateAction("Page d'aide", "/help")),
                null);
    }

    private ChatDtos.ChatResponse listAlerts(Tenant tenant, AppRole role) {
        if (!ChatRbac.canReadAlerts(role)) {
            return denied("consulter les alertes stock");
        }
        AlertDtos.AlertSummary s = alertService.summary(tenant);
        List<AlertDtos.AlertRow> rows = alertService.list(tenant, "All");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                Locale.FRENCH,
                "Alertes : %d rupture(s), %d critique(s), %d stock bas, %d sain(s).%n",
                s.outOfStock(),
                s.critical(),
                s.low(),
                s.healthy()));
        if (rows.isEmpty()) {
            sb.append("Aucun article en alerte pour le moment.");
        } else {
            int limit = Math.min(5, rows.size());
            sb.append("Top ").append(limit).append(" :");
            for (int i = 0; i < limit; i++) {
                AlertDtos.AlertRow r = rows.get(i);
                sb.append(String.format(
                        Locale.FRENCH,
                        "%n• %s (%s) — stock %d, statut %s",
                        r.name(),
                        r.sku(),
                        r.stock(),
                        r.status()));
            }
            if (rows.size() > limit) {
                sb.append(String.format(Locale.FRENCH, "%n… et %d autre(s).", rows.size() - limit));
            }
        }

        List<String> suggestions = new ArrayList<>();
        if (ChatRbac.canReadInventoryNav(role)) {
            suggestions.add("Ouvrir inventaire");
        }
        if (ChatRbac.canCreateReplenishment(role)) {
            suggestions.add("Créer réappro");
        }

        List<ChatDtos.ChatAction> actions = new ArrayList<>();
        if (ChatRbac.canReadInventoryNav(role)) {
            actions.add(navigateAction("Voir inventaire", "/inventory"));
        }

        return ChatDtos.ChatResponse.of(sb.toString(), suggestions, actions, null);
    }

    private ChatDtos.ChatResponse dashboardSummary(Tenant tenant, AppRole role) {
        if (!ChatRbac.canReadDashboard(role)) {
            return denied("consulter le tableau de bord");
        }
        DashboardDtos.DashboardPayload d = dashboardService.build(tenant);
        String kpis = d.kpis().stream()
                .map(k -> k.label() + " : " + k.value())
                .reduce((a, b) -> a + " | " + b)
                .orElse("—");

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Articles en alerte");
        if (ChatRbac.canReadOrders(role)) {
            suggestions.add("Commandes en attente");
        }

        return ChatDtos.ChatResponse.of(
                "Tableau de bord — " + kpis,
                suggestions,
                List.of(navigateAction("Ouvrir dashboard", "/dashboard")),
                null);
    }

    private ChatDtos.ChatResponse ordersSummary(Tenant tenant, AppRole role) {
        if (!ChatRbac.canReadOrders(role)) {
            return denied("consulter les commandes");
        }
        OrderDtos.OrderSummary s = purchaseOrderService.summary(tenant);
        return ChatDtos.ChatResponse.of(
                String.format(
                        Locale.FRENCH,
                        "Commandes : %d total — %d en attente, %d expédiées, %d reçues, %d annulées.",
                        s.totalOrders(),
                        s.pending(),
                        s.shipped(),
                        s.received(),
                        s.cancelled()),
                List.of("Ouvrir commandes"),
                List.of(navigateAction("Voir commandes", "/orders")),
                null);
    }

    private ChatDtos.ChatResponse replenishmentSummary(Tenant tenant, AppRole role) {
        if (!ChatRbac.canReadReplenishment(role)) {
            return denied("consulter les demandes de réapprovisionnement");
        }
        ReplenishmentDtos.ReplenishmentSummary s = replenishmentRequestService.summary(tenant);
        return ChatDtos.ChatResponse.of(
                String.format(
                        Locale.FRENCH,
                        "Réapprovisionnement : %d en attente, %d en cours, %d reçues.",
                        s.pending(),
                        s.inProgress(),
                        s.received()),
                List.of("Ouvrir réappro"),
                List.of(navigateAction("Demandes réappro.", "/replenishment")),
                null);
    }

    private ChatDtos.ChatResponse articleStock(Tenant tenant, AppRole role, String sku) {
        if (sku == null || sku.isBlank()) {
            return ChatDtos.ChatResponse.text("Indiquez une référence SKU, par ex. SKU-0021.");
        }
        Article article = articleRepository
                .findActiveByTenantIdAndSku(tenant.getId(), sku.toUpperCase(Locale.ROOT))
                .orElse(null);
        if (article == null) {
            return ChatDtos.ChatResponse.text("Aucun article actif trouvé pour « " + sku + " ».");
        }
        int maxOrder = procurementPolicy.maxOrderableQuantity(article);
        String status = ArticleService.catalogStatus(article);
        String reply = String.format(
                Locale.FRENCH,
                "%s (%s) — stock : %d, min : %d, max : %d, statut : %s. Maximum commandable : %d.",
                article.getName(),
                article.getSku(),
                article.getQuantityOnHand(),
                article.getMinThreshold(),
                article.getMaxThreshold(),
                status,
                maxOrder);

        List<ChatDtos.ChatAction> actions = new ArrayList<>();
        if (ChatRbac.canCreateReplenishment(role) && procurementPolicy.eligibleForReplenishment(article)) {
            actions.add(navigateAction(
                    "Créer demande réappro",
                    "/replenishment?create=1&articleId=" + article.getId()));
        }
        if (ChatRbac.canCreateOrder(role) && procurementPolicy.eligibleForPurchaseOrder(article)) {
            actions.add(navigateAction(
                    "Créer commande",
                    "/orders?create=1&articleId=" + article.getId()));
        }

        List<String> suggestions = new ArrayList<>();
        if (ChatRbac.canReadAlerts(role)) {
            suggestions.add("Articles en alerte");
        }

        return ChatDtos.ChatResponse.of(reply, suggestions, actions, null);
    }

    private ChatDtos.ChatResponse articleSearch(Tenant tenant, AppRole role, String searchName) {
        if (searchName == null || searchName.isBlank()) {
            return ChatDtos.ChatResponse.text("Indiquez le nom d'un article à rechercher.");
        }
        List<Article> matches = searchArticlesByName(tenant.getId(), searchName.trim());
        if (matches.isEmpty()) {
            return ChatDtos.ChatResponse.of(
                    "Aucun article actif trouvé pour « " + searchName + " ».",
                    List.of("Articles en alerte", "Ouvrir les articles"),
                    List.of(navigateAction("Catalogue articles", "/products")),
                    null);
        }
        if (matches.size() == 1) {
            return articleStock(tenant, role, matches.get(0).getSku());
        }
        StringBuilder sb = new StringBuilder("Plusieurs articles correspondent à « ")
                .append(searchName)
                .append(" » :\n");
        for (Article article : matches.stream().limit(8).toList()) {
            sb.append("• ")
                    .append(article.getName())
                    .append(" (")
                    .append(article.getSku())
                    .append(") — stock ")
                    .append(article.getQuantityOnHand())
                    .append('\n');
        }
        return ChatDtos.ChatResponse.of(
                sb.toString().trim(),
                List.of("Articles en alerte"),
                List.of(navigateAction("Catalogue articles", "/products")),
                null);
    }

    private ChatDtos.ChatResponse suggestReplenish(Tenant tenant, AppRole role, String sku) {
        if (!ChatRbac.canCreateReplenishment(role)) {
            return denied("créer une demande de réapprovisionnement");
        }
        if (sku == null) {
            List<ChatDtos.ChatAction> actions = new ArrayList<>();
            if (ChatRbac.canReadInventoryNav(role)) {
                actions.add(navigateAction("Voir alertes", "/inventory"));
            }
            return ChatDtos.ChatResponse.of(
                    "Indiquez un SKU pour le réapprovisionnement, ex. « réappro SKU-0021 ».",
                    List.of("Articles en alerte"),
                    actions,
                    null);
        }
        return buildReplenishNavigation(tenant, role, sku, null);
    }

    private ChatDtos.ChatResponse suggestOrder(Tenant tenant, AppRole role, String sku) {
        if (!ChatRbac.canCreateOrder(role)) {
            return denied("créer une commande fournisseur");
        }
        if (sku == null) {
            return ChatDtos.ChatResponse.of(
                    "Indiquez un SKU pour la commande, ex. « commander SKU-0021 ».",
                    List.of("Ouvrir commandes"),
                    List.of(navigateAction("Commandes", "/orders")),
                    null);
        }
        Article article = loadArticle(tenant, sku);
        if (!procurementPolicy.eligibleForPurchaseOrder(article)) {
            List<String> suggestions = new ArrayList<>();
            List<ChatDtos.ChatAction> actions = new ArrayList<>();
            if (ChatRbac.canCreateReplenishment(role)) {
                suggestions.add("Créer réappro");
                actions.add(navigateAction(
                        "Réapprovisionner",
                        "/replenishment?create=1&articleId=" + article.getId()));
            }
            return ChatDtos.ChatResponse.of(
                    "« " + sku + " » n'est pas éligible à une commande fournisseur (article en alerte). "
                            + "Utilisez une demande de réapprovisionnement.",
                    suggestions,
                    actions,
                    null);
        }
        return ChatDtos.ChatResponse.of(
                "« " + article.getSku() + " » est éligible à une commande fournisseur.",
                List.of(),
                List.of(navigateAction(
                        "Créer commande",
                        "/orders?create=1&articleId=" + article.getId())),
                null);
    }

    private ChatDtos.ChatResponse createReplenishProposal(
            Tenant tenant,
            AppRole role,
            String sku,
            Integer requestedQty
    ) {
        if (!ChatRbac.canCreateReplenishment(role)) {
            return denied("créer une demande de réapprovisionnement");
        }
        if (sku == null) {
            return suggestReplenish(tenant, role, null);
        }
        Article article = loadArticle(tenant, sku);
        String reject = procurementPolicy.replenishmentRejectionReason(article);
        if (reject != null) {
            return ChatDtos.ChatResponse.text(reject);
        }
        int suggested = procurementPolicy.suggestedReplenishmentQuantity(article);
        int qty = requestedQty != null && requestedQty > 0 ? requestedQty : suggested;
        int max = procurementPolicy.maxOrderableQuantity(article);
        if (qty > max) {
            return ChatDtos.ChatResponse.of(
                    String.format(
                            Locale.FRENCH,
                            "La quantité %d dépasse le plafond max (%d commandable). Proposition : %d unités.",
                            qty,
                            max,
                            max),
                    List.of(),
                    List.of(navigateAction(
                            "Ouvrir formulaire",
                            "/replenishment?create=1&articleId=" + article.getId() + "&qty=" + max)),
                    buildConfirmReplenishment(tenant, article, max));
        }
        Supplier supplier = defaultSupplier(tenant);

        return ChatDtos.ChatResponse.of(
                String.format(
                        Locale.FRENCH,
                        "Créer une demande de réappro de %d unité(s) pour « %s » (%s) chez %s ?",
                        qty,
                        article.getName(),
                        article.getSku(),
                        supplier.getName()),
                List.of(),
                List.of(navigateAction(
                        "Modifier avant envoi",
                        "/replenishment?create=1&articleId=" + article.getId() + "&qty=" + qty)),
                new ChatDtos.ConfirmAction("create_replenishment", "Confirmer la création", confirmPayload(article, supplier, qty)));
    }

    private ChatDtos.ChatResponse buildReplenishNavigation(Tenant tenant, AppRole role, String sku, Integer qty) {
        Article article = loadArticle(tenant, sku);
        String reject = procurementPolicy.replenishmentRejectionReason(article);
        if (reject != null) {
            if (procurementPolicy.eligibleForPurchaseOrder(article) && ChatRbac.canCreateOrder(role)) {
                return ChatDtos.ChatResponse.of(
                        reject + " Vous pouvez créer une commande fournisseur.",
                        List.of(),
                        List.of(navigateAction(
                                "Créer commande",
                                "/orders?create=1&articleId=" + article.getId())),
                        null);
            }
            return ChatDtos.ChatResponse.text(reject);
        }
        int suggested = qty != null && qty > 0 ? qty : procurementPolicy.suggestedReplenishmentQuantity(article);
        String href = "/replenishment?create=1&articleId=" + article.getId() + "&qty=" + suggested;
        return ChatDtos.ChatResponse.of(
                String.format(
                        Locale.FRENCH,
                        "« %s » (%s) — stock %d. Quantité suggérée : %d.",
                        article.getName(),
                        article.getSku(),
                        article.getQuantityOnHand(),
                        suggested),
                List.of("Créer la demande maintenant"),
                List.of(navigateAction("Ouvrir réappro", href)),
                null);
    }

    private ChatDtos.ConfirmAction buildConfirmReplenishment(Tenant tenant, Article article, int qty) {
        Supplier supplier = defaultSupplier(tenant);
        return new ChatDtos.ConfirmAction(
                "create_replenishment",
                "Confirmer avec " + qty + " unités",
                confirmPayload(article, supplier, qty));
    }

    private static Map<String, Object> confirmPayload(Article article, Supplier supplier, int qty) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("articleId", article.getId());
        payload.put("supplierId", supplier.getId());
        payload.put("quantity", qty);
        payload.put("note", "Créée via l'assistant StowFlow");
        return payload;
    }

    private ChatDtos.ChatResponse navigate(String href, String label, AppRole role, boolean allowed) {
        if (!allowed) {
            return denied("accéder à « " + label + " »");
        }
        return ChatDtos.ChatResponse.of(
                "Ouverture de « " + label + " ».",
                List.of(),
                List.of(navigateAction("Aller à " + label, href)),
                null);
    }

    private ChatDtos.ChatResponse denied(String action) {
        return ChatDtos.ChatResponse.text(
                "Accès refusé : votre rôle ne permet pas de " + action + ".");
    }

    private Article loadArticle(Tenant tenant, String sku) {
        return articleRepository
                .findActiveByTenantIdAndSku(tenant.getId(), sku.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException("Article « " + sku + " » introuvable."));
    }

    private Supplier defaultSupplier(Tenant tenant) {
        List<Supplier> suppliers = supplierRepository.findActiveByTenantId(tenant.getId());
        if (suppliers.isEmpty()) {
            throw new BusinessException("Aucun fournisseur actif — créez un fournisseur d'abord.");
        }
        return suppliers.get(0);
    }

    private List<Article> searchArticlesByName(Long tenantId, String term) {
        List<Article> matches = articleRepository.searchActiveByTenantIdAndName(tenantId, term);
        if (!matches.isEmpty()) {
            return matches;
        }
        if (term.length() > 3 && term.endsWith("s")) {
            return articleRepository.searchActiveByTenantIdAndName(tenantId, term.substring(0, term.length() - 1));
        }
        return List.of();
    }

    private static ChatDtos.ChatAction navigateAction(String label, String href) {
        return new ChatDtos.ChatAction("navigate", label, href);
    }
}
