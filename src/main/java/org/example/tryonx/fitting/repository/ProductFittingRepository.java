package org.example.tryonx.fitting.repository;

import org.example.tryonx.fitting.domain.ProductFitting;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductFittingRepository extends JpaRepository<ProductFitting, Long> {
    List<ProductFitting> findByProductOrderBySequenceAsc(Product product);
}
