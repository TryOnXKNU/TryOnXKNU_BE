package org.example.tryonx.product.repository;

import org.example.tryonx.product.domain.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductItemRepository extends JpaRepository<ProductItem, Integer> {

}
