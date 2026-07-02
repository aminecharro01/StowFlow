package com.stowflow.inventra.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "articles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "sku"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(length = 128)
    private String category;

    @Column(length = 32)
    @Builder.Default
    private String unitOfMeasure = "unit";

    @Column(precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal purchasePrice = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer minThreshold = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxThreshold = Integer.MAX_VALUE;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantityOnHand = 0;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;
}
