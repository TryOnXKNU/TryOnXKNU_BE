package org.example.tryonx.like.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.like.domain.Like;
import org.example.tryonx.like.dto.LikeResponse;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public LikeResponse goodProduct(Integer productId, String username) {
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // 이미 좋아요한 경우: 취소
        return likeRepository.findByMemberAndProduct(member, product)
                .map(existingLike -> {
                    likeRepository.delete(existingLike);
                    Long count = likeRepository.countByProduct(product);
                    return new LikeResponse(false, count);
                })
                .orElseGet(() -> {
                    Like like = Like.builder()
                            .member(member)
                            .product(product)
                            .build();
                    likeRepository.save(like);
                    Long count = likeRepository.countByProduct(product);
                    return new LikeResponse(true, count);
                });
    }
}
