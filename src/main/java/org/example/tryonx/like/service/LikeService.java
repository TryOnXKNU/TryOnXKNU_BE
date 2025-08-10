package org.example.tryonx.like.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.like.domain.Like;
import org.example.tryonx.like.dto.LikeResponse;
import org.example.tryonx.like.dto.ProductDto;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<ProductDto> getLikedProducts(String username) {
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        // 사용자가 좋아요한 항목들
        List<Like> likes = likeRepository.findByMember(member);


        return likes.stream()
                .map(Like::getProduct)
                .distinct()
                .map(p -> {
                    long likeCount = likeRepository.countByProduct(p);
                    return ProductDto.of(p, likeCount);
                })
                .collect(Collectors.toList());

    }

}
