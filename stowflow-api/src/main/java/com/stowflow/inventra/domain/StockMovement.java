package com.stowflow.inventra.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MovementType type;

    /** Quantité positive ; le sens est donné par {@link #type}. */
    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 2000)
    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 255)
    private String createdBy;

    /** Lien traçabilité : sortie issue d’une ligne de vente POS (sinon null). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_line_id")
    private SaleLine saleLine;

    /** Lien traçabilité : entrée issue d’une ligne de commande fournisseur réceptionnée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_line_id")
    private PurchaseOrderLine purchaseOrderLine;

    /** Lien traçabilité : entrée issue d’une demande de réapprovisionnement réceptionnée. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replenishment_request_id")
    private ReplenishmentRequest replenishmentRequest;
}
