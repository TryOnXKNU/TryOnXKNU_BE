package org.example.tryonx.cart.repository;

import org.example.tryonx.cart.domain.CartItem;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.product.domain.ProductItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByMember(Member member);
    Optional<CartItem> findByMemberAndProductItem(Member member, ProductItem productItem);
    void deleteAllByMember(Member member);
    void deleteByProductItemIn(List<ProductItem> productItems);
}
