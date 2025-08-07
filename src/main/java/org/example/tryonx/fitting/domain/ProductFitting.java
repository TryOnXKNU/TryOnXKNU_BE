package org.example.tryonx.fitting.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.tryonx.product.domain.Product;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ProductFitting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int sequence;

    private String fittingImageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "productId")
    private Product product;

    private LocalDateTime createdAt = LocalDateTime.now();
}
