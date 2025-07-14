package org.example.tryonx.review.repository;

import org.example.tryonx.member.domain.Member;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.review.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMember(Member member);
    List<Review> findByProduct(Product product);
}
