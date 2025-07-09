package org.example.tryonx.product.repository;

import org.example.tryonx.enums.Size;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductItemRepository extends JpaRepository<ProductItem, Integer> {
    List<ProductItem> findByProduct(Product product);
    Optional<ProductItem> findByProductAndSize(Product product, Size size);
}
