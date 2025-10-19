package org.example.tryonx.search.repository;

import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SearchRepository extends JpaRepository<Product, Long> {
    List<Product> findByProductNameContainingIgnoreCase(String keyword);
    List<Product> findDistinctByItems_StatusNotInAndProductNameContainingIgnoreCase(
            List<ProductStatus> excludedStatuses,
            String keyword
    );
}
