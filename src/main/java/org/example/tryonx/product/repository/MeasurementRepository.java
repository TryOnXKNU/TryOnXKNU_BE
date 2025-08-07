package org.example.tryonx.product.repository;

import org.example.tryonx.product.domain.Measurement;
import org.example.tryonx.product.domain.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {
    Optional<Measurement> findByProductItem(ProductItem productItem);
}
