package org.example.tryonx.fitting.repository;

import org.example.tryonx.fitting.domain.ProductFitting;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductFittingRepository extends JpaRepository<ProductFitting, Long> {
    List<ProductFitting> findByProductOrderByUpdatedAtAsc(Product product);
    Optional<ProductFitting> findByProductAndSequence(Product product, int sequence);
    List<ProductFitting> findByProductOrderBySequenceAsc(Product product);
}
