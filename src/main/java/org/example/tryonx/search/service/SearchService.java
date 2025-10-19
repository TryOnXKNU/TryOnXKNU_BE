package org.example.tryonx.search.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.review.service.ReviewService;
import org.example.tryonx.search.dto.ProductImageResponse;
import org.example.tryonx.search.dto.ProductResponse;
import org.example.tryonx.search.dto.SearchDto;
import org.example.tryonx.search.repository.SearchRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final SearchRepository searchRepository;
    private final LikeRepository likeRepository;
    private final ReviewService reviewService;

    private static BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) return BigDecimal.ZERO;
        // 1보다 크면 %로 판단(예: 10) → 0.10으로 변환
        return rate.compareTo(BigDecimal.ONE) > 0
                ? rate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : rate;
    }
    private static BigDecimal calcDiscountPrice(BigDecimal price, BigDecimal rate) {
        if (price == null) return null;
        BigDecimal r = normalizeRate(rate);
        if (r.compareTo(BigDecimal.ZERO) <= 0) return price; // 할인 없음 → 원가
        return price.multiply(BigDecimal.ONE.subtract(r))
                .setScale(0, RoundingMode.HALF_UP);       // 원 단위 반올림
    }

    public List<ProductResponse> searchProducts(SearchDto searchDto) {
        String keyword = searchDto.getKeyword();
        List<ProductStatus> excluded = List.of(ProductStatus.HIDDEN);

        List<Product> products = searchRepository
                .findDistinctByItems_StatusNotInAndProductNameContainingIgnoreCase(excluded, keyword);

        return products.stream()
                .map(product -> {
                    BigDecimal price = product.getPrice();
                    BigDecimal discountRate = product.getDiscountRate();
                    BigDecimal discountPrice = calcDiscountPrice(price, discountRate);

                    long likeCount = likeRepository.countByProduct(product);
                    Double avgRating = reviewService.getAverageRatingByProductId(product.getProductId());
                    if (avgRating == null) avgRating = 0.0;

                    return ProductResponse.builder()
                            .productId(product.getProductId())
                            .productName(product.getProductName())
                            .price(price)
                            .discountRate(discountRate)
                            .images(product.getImages().stream()
                                    .map(img -> new ProductImageResponse(img.getImageUrl()))
                                    .collect(Collectors.toList()))
                            .discountPrice(discountPrice)
                            .likeCount(likeCount)
                            .averageRating(avgRating)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
