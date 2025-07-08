package org.example.tryonx.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.enums.Size;

@Entity
@Table(name = "product_item", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "size"})
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Size size;

    @Column(nullable = false)
    private Integer stock;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.OUT_OF_STOCK;
}
