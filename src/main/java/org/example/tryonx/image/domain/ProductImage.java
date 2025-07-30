package org.example.tryonx.image.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.product.domain.Product;

import java.time.LocalDateTime;


@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 255)
    private String imageUrl;

    @Column(nullable = false)
    @Setter
    private Boolean isThumbnail = false;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public ProductImage(Product product, String imageUrl) {
        this.product = product;
        this.imageUrl = imageUrl;
    }
}
