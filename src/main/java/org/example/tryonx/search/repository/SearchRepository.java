package org.example.tryonx.search.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SearchRepository extends JpaRepository<Product, Long> {
    List<Product> findByProductNameContainingIgnoreCase(String keyword);
    @Query("SELECT p FROM Product p " +
            "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "AND p.status <> org.example.tryonx.enums.ProductStatus.HIDDEN")
    List<Product> searchVisibleProducts(@Param("keyword") String keyword);
}
