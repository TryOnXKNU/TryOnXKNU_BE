package org.example.tryonx.image.repository;

import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {
    List<ProductImage> findByProduct(Product product);
    Optional<ProductImage> findByProductAndIsThumbnailTrue(Product product);
    ProductImage findByImageUrl(String imageUrl);
    long countByProduct(Product product);
    Optional<ProductImage> findFirstByProductAndIsThumbnailTrue(Product product);
    Optional<ProductImage> findFirstByProductAndIsThumbnailFalse(Product product);
}
