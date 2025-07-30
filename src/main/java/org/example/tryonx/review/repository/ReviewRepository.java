package org.example.tryonx.review.repository;

import org.example.tryonx.enums.Size;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMember(Member member);
    List<Review> findByProduct(Product product);
    Optional<Review> findByMemberAndProduct(Member member, Product product);
    Integer countByMember(Member member);
    Integer countByProduct(Product product);
    List<Review> findByProductOrderByCreatedAtDesc(Product product);
    void deleteByOrderItem(OrderItem orderItem);

    @Query("""
    SELECT r FROM Review r
    WHERE r.product = :product
    AND (:size IS NULL OR r.size = :size)
    AND (:minHeight IS NULL OR r.member.height >= :minHeight)
    AND (:maxHeight IS NULL OR r.member.height <= :maxHeight)
    AND (:minWeight IS NULL OR r.member.weight >= :minWeight)
    AND (:maxWeight IS NULL OR r.member.weight <= :maxWeight)
    ORDER BY r.createdAt DESC
""")
    List<Review> findFilteredReviews(
            @Param("product") Product product,
            @Param("size") Size size,
            @Param("minHeight") Integer minHeight,
            @Param("maxHeight") Integer maxHeight,
            @Param("minWeight") Integer minWeight,
            @Param("maxWeight") Integer maxWeight
    );

    Optional<Object> findByOrderItem(OrderItem orderItem);
}
