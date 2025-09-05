package org.example.tryonx.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.category.Category;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.fitting.domain.ProductFitting;
import org.example.tryonx.image.domain.ProductImage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productId;

    @Setter
    @Column(unique = true, length = 50)
    private String productCode;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private BodyShape bodyShape;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductItem> items;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductFitting> fittings;

    public void updateProduct(
            BigDecimal price,
            BigDecimal discountRate,
            BodyShape bodyShape,
            Category category
    ) {
        Optional.ofNullable(price)
                .filter(newPrice -> !Objects.equals(this.price, newPrice))
                .ifPresent(newPrice -> this.price = newPrice);

        Optional.ofNullable(discountRate)
                .filter(newDiscount -> !Objects.equals(this.discountRate, newDiscount))
                .ifPresent(newDiscount -> this.discountRate = newDiscount);

        Optional.ofNullable(bodyShape)
                .filter(newShape -> !Objects.equals(this.bodyShape, newShape))
                .ifPresent(newShape -> this.bodyShape = newShape);

        Optional.ofNullable(category)
                .filter(newCategory -> !Objects.equals(this.category, newCategory))
                .ifPresent(newCategory -> this.category = newCategory);
    }

}
