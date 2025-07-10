package org.example.tryonx.like.repository;

import org.example.tryonx.like.domain.Like;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByMemberAndProduct(Member member, Product product);
    Long countByProduct(Product product);
    void deleteByMemberAndProduct(Member member, Product product);
    List<Like> findByMember(Member member);

}