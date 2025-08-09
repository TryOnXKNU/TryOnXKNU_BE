package org.example.tryonx.fitting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tryonx.product.domain.Product;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_fitting", uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "sequence"}))
@Getter
@Setter
public class ProductFitting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // 1 또는 2
    private int sequence;

    @Column(nullable = false)
    private String fittingImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "productId", nullable = false)
    private Product product;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
