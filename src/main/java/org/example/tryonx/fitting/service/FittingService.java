package org.example.tryonx.fitting.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.category.CategoryRepository;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.fitting.dto.BodyShapeRequest;
import org.example.tryonx.fitting.dto.FittingMemberInfo;
import org.example.tryonx.fitting.dto.FittingProductInfo;
import org.example.tryonx.fitting.dto.FittingResponse;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.like.domain.Like;
import org.example.tryonx.like.dto.ProductDto;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FittingService {
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final LikeRepository likeRepository;

//    public FittingResponse getFittingPageData(String email){
//        Member member = memberRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
//        FittingMemberInfo memberInfo = new FittingMemberInfo(
//                member.getWeight(),
//                member.getHeight(),
//                member.getBodyShape()
//        );
//        List<FittingProductInfo> productInfos = likeRepository.findByMember(member)
//                .stream()
//                .map(Like::getProduct)
//                .distinct()
//                .map(p->{
//                    String imgUrl = productImageRepository.findByProductAndIsThumbnailTrue(p)
//                            .orElseThrow(() -> new RuntimeException("썸네일 이미지 없습니다."))
//                            .getImageUrl();
//                    Boolean best = (member.getBodyShape() == p.getBodyShape());
//                    return new FittingProductInfo(
//                            p.getProductId(),
//                            p.getCategory().getCategoryId(),
//                            p.getProductName(),
//                            imgUrl,
//                            best
//                    );
//                }).toList();
//
//        return new FittingResponse(
//                memberInfo,
//                productInfos
//        );
//    }

    public FittingResponse getFittingPageData(String email, Integer categoryId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));

        FittingMemberInfo memberInfo = new FittingMemberInfo(
                member.getWeight(),
                member.getHeight(),
                member.getBodyShape()
        );

        List<FittingProductInfo> productInfos = likeRepository.findByMember(member)
                .stream()
                .map(Like::getProduct)
                .filter(p -> categoryId == null ||
                        (p.getCategory() != null && p.getCategory().getCategoryId().equals(categoryId))) // 카테고리 필터링
                .distinct()
                .map(p -> {
                    String imgUrl = productImageRepository.findByProductAndIsThumbnailTrue(p)
                            .orElseThrow(() -> new RuntimeException("썸네일 이미지 없습니다."))
                            .getImageUrl();
                    Boolean best = (member.getBodyShape() == p.getBodyShape());
                    return new FittingProductInfo(
                            p.getProductId(),
                            p.getCategory().getCategoryId(),
                            p.getProductName(),
                            imgUrl,
                            best
                    );
                })
                .toList();

        return new FittingResponse(
                memberInfo,
                productInfos
        );
    }

    @Transactional
    public BodyShape updateBodyShape(String email, BodyShapeRequest bodyShapeRequest) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        BodyShape bodyShape = bodyShapeRequest.bodyShape();
        if(bodyShape != null && !member.getBodyShape().equals(bodyShape)){
            member.setBodyShape(bodyShape);
            memberRepository.save(member);
        }
        return bodyShape;
    }

}
