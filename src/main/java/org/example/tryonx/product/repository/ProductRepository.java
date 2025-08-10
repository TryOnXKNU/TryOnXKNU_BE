package org.example.tryonx.product.repository;

import org.example.tryonx.category.Category;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory(Category category);
    List<Product> findByBodyShapeOrderByCreatedAtDesc(BodyShape bodyShape, Pageable pageable);
}
