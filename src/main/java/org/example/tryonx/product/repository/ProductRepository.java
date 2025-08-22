package org.example.tryonx.product.repository;

import org.example.tryonx.category.Category;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategory(Category category);
    List<Product> findByBodyShapeOrderByCreatedAtDesc(BodyShape bodyShape, Pageable pageable);

    @Query("""
select distinct p
from Product p
where exists (
  select 1 from ProductItem i
  where i.product = p
    and i.status = org.example.tryonx.enums.ProductStatus.AVAILABLE
)
""")
    List<Product> findAllWithAnyAvailableItem();

    @Query("""
select distinct p
from Product p
where p.category = :category
  and exists (
    select 1 from ProductItem i
    where i.product = p
      and i.status = org.example.tryonx.enums.ProductStatus.AVAILABLE
)
""")
    List<Product> findByCategoryWithAnyAvailableItem(@Param("category") Category category);

    @Query("""
select p
from Product p
where p.bodyShape = :shape
  and exists (
    select 1 from ProductItem i
    where i.product = p
      and i.status = org.example.tryonx.enums.ProductStatus.AVAILABLE
  )
order by p.createdAt desc
""")
    List<Product> findByBodyShapeWithAnyAvailableItem(@Param("shape") BodyShape shape, Pageable pageable);

    @Query("""
select p
from Product p
where exists (
  select 1 from ProductItem i
  where i.product = p
    and i.status = org.example.tryonx.enums.ProductStatus.AVAILABLE
)
order by p.createdAt desc
""")
    List<Product> findAllWithAnyAvailableItem(Pageable pageable);

    @Query("""
        select p
        from Product p
        where p.bodyShape = :shape
          and exists (
            select 1 from ProductItem i
            where i.product = p
              and i.status = org.example.tryonx.enums.ProductStatus.AVAILABLE
          )
        """)
    List<Product> findByBodyShapeWithAnyAvailableItem(@Param("shape") BodyShape shape);
}
