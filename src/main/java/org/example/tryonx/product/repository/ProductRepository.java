package org.example.tryonx.product.repository;

import org.example.tryonx.category.Category;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    Optional<Product> findByProductCode(String productCode);
    List<Product> findByCategory(Category category);
}
