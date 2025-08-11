package org.example.tryonx.product.repository;

import org.example.tryonx.enums.Size;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductItemRepository extends JpaRepository<ProductItem, Integer> {
    List<ProductItem> findByProduct(Product product);
    Optional<ProductItem> findByProductAndSize(Product product, Size size);

    @Query("""
    select p
      from ProductItem p
     where p.product.productId = :productId
       and p.size = :size
       and p.status = org.example.tryonx.enums.ProductStatus.AVAILABLE
  """)
    Optional<ProductItem> findByProductAndSize(@Param("productId") Integer productId,
                                               @Param("size") Size size);

    @Modifying
    @Query("""
    update ProductItem p
       set p.stock = p.stock - :qty
     where p.productItemId = :id
       and p.status = org.example.tryonx.enums.ProductStatus.AVAILABLE
       and p.stock >= :qty
  """)
    int tryCommit(@Param("id") Integer productItemId, @Param("qty") int qty);

    @Query("select p.stock from ProductItem p where p.productItemId = :id")
    Optional<Integer> findStock(@Param("id") Integer productItemId);
}
