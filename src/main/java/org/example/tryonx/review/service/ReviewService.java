package org.example.tryonx.review.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.Size;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.domain.ReviewImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.image.repository.ReviewImageRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.PointHistory;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.member.repository.PointHistoryRepository;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.example.tryonx.review.domain.Review;
import org.example.tryonx.review.dto.*;
import org.example.tryonx.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public boolean validateReviewPermission(String email, Integer orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (reviewRepository.existsByOrderItem(item))
            throw new RuntimeException("Review already exists");

        return item.getMember().equals(member);
    }

    @Transactional
    public boolean create(ReviewCreateRequestDto reviewCreateRequestDto, List<MultipartFile> images){
        OrderItem orderItem = orderItemRepository.findById(reviewCreateRequestDto.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Order item not found"));
        Member member = memberRepository.findById(orderItem.getMember().getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        String productName = orderItem.getProductItem().getProduct().getProductName();

        Review review = Review.builder()
                .member(member)
                .product(orderItem.getProductItem().getProduct())
                .orderItem(orderItem)
                .content(reviewCreateRequestDto.getContent())
                .rating(reviewCreateRequestDto.getRating())
                .size(orderItem.getProductItem().getSize())
                .build();
        reviewRepository.save(review);

        BigDecimal discountRate = orderItem.getDiscountRate()
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        int savePoint = orderItem.getPrice()
                .multiply(BigDecimal.ONE.subtract(discountRate))
                .multiply(BigDecimal.valueOf(0.05))
                .setScale(0, RoundingMode.DOWN)
                .intValue();

        System.out.println("Save point " + savePoint);
        System.out.println("origin_point " + orderItem.getPrice().multiply(BigDecimal.ONE.subtract(orderItem.getDiscountRate())));
        member.savePoint(savePoint);
        memberRepository.save(member);

        pointHistoryRepository.save(PointHistory.earn(member, savePoint, "[" + productName + "] 리뷰 작성 포인트 " + savePoint + "지급"));

        if (images != null && !images.isEmpty()){
            for (MultipartFile image : images) {
                String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
                Path savePath = Paths.get("upload/review").resolve(filename);

                try {
                    Files.createDirectories(savePath.getParent());
                    image.transferTo(savePath);

                    ReviewImage reviewImage = ReviewImage.builder()
                            .review(review)
                            .imageUrl("/upload/review/" + filename)
                            .build();
                    reviewImageRepository.save(reviewImage);
                } catch (IOException e) {
                    throw new RuntimeException("이미지 저장 실패", e);
                }
            }
        }
        return true;
    }

    public boolean delete(String email, Long reviewId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        if(review.getMember().equals(member)) {
            reviewRepository.delete(review);
            return true;
        }
        return false;
    }

    public List<ReviewResponseDto> getMyReviews(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        List<Review> reviews = reviewRepository.findByMember(member);

        return reviews.stream().map(review -> {
            String productImage = productImageRepository.findByProductAndIsThumbnailTrue(review.getProduct())
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
            List<String> reviewImages = reviewImageRepository.findByReviewId(review.getId()).stream()
                    .map(image -> image.getImageUrl())
                    .toList();

            return ReviewResponseDto.builder()
                    .reviewId(review.getId())
                    .productId(review.getProduct().getProductId())
                    .productName(review.getProduct().getProductName())
                    .description(review.getContent())
                    .rating(review.getRating())
                    .createdAt(review.getCreatedAt())
                    .productImage(productImage)
                    .reviewImages(reviewImages)
                    .size(review.getSize())
                    .build();
        }).toList();
    }

    public List<ProductReviewDto> getProductReviewsPreview(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<Size> availableSizes = productItemRepository.findByProduct(product).stream()
                .map(ProductItem::getSize)
                .toList();
        List<Review> reviews = reviewRepository.findByProductOrderByCreatedAtDesc(product)  // 정렬된 리스트
                .stream()
                .limit(2) // 최대 2개만
                .toList();

        return reviews.stream().map(review -> {
            List<String> reviewImages = reviewImageRepository.findByReviewId(review.getId()).stream()
                    .map(ReviewImage::getImageUrl)
                    .toList();

            return getProductReviewDto(review, reviewImages, availableSizes);
        }).toList();
    }

    public List<ProductReviewDto> getProductReviews(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<Size> availableSizes = productItemRepository.findByProduct(product).stream()
                .map(ProductItem::getSize)
                .toList();
        List<Review> reviews = reviewRepository.findByProduct(product);
        return reviews.stream().map(review -> {
            List<String> reviewImages = reviewImageRepository.findByReviewId(review.getId()).stream()
                    .map(image -> image.getImageUrl())
                    .toList();
            return getProductReviewDto(review, reviewImages, availableSizes);
        }).toList();
    }

    public List<ProductReviewDto> getProductReviewsFiltered(FilteringRequestDto requestDto) {
        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        List<Size> availableSizes = productItemRepository.findByProduct(product).stream()
                .map(ProductItem::getSize)
                .toList();

        List<Review> reviews = reviewRepository.findFilteredReviews(
                product,
                requestDto.getSize(),
                requestDto.getMinHeight(),
                requestDto.getMaxHeight(),
                requestDto.getMinWeight(),
                requestDto.getMaxWeight()
        );

        return reviews.stream()
                .map(review -> {
                    List<String> reviewImages = reviewImageRepository.findByReviewId(review.getId())
                            .stream().map(image -> image.getImageUrl()).toList();
                    return getProductReviewDto(review, reviewImages, availableSizes);
                })
                .toList();
    }
    public Integer reviewCountByProductId(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return reviewRepository.countByProduct(product);
    }

    private ProductReviewDto getProductReviewDto(Review review, List<String> reviewImages, List<Size> availableSizes) {
        return ProductReviewDto.builder()
                .memberNickname(review.getMember().getNickname())
                .height(review.getMember().getHeight())
                .weight(review.getMember().getWeight())
                .profileImageUrl(review.getMember().getProfileUrl())
                .rating(review.getRating())
                .productId(review.getProduct().getProductId())
                .productName(review.getProduct().getProductName())
                .size(review.getSize())
                .description(review.getContent())
                .createdAt(review.getCreatedAt())
                .reviewImages(reviewImages)
                .availableOrderSizes(availableSizes)
                .build();
    }

    public Integer countMyReviews(String email){
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Member not found"));
        return reviewRepository.countByMember(member);
    }

    // 평균/카운트 요약만 필요할 때
    public double getAverageRatingByProductId(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        Double avg = reviewRepository.findAverageRatingByProduct(product);
        // 소수 1자리 반올림
        return BigDecimal.valueOf(avg == null ? 0.0 : avg)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public int getReviewCountByProductId(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return reviewRepository.countByProduct(product);
    }
}

