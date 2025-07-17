package org.example.tryonx.review.service;

import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.domain.ReviewImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.image.repository.ReviewImageRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.dto.ProductCreateRequestDto;
import org.example.tryonx.review.domain.Review;
import org.example.tryonx.review.dto.CheckAuthForReviewDto;
import org.example.tryonx.review.dto.ReviewCreateRequestDto;
import org.example.tryonx.review.dto.ReviewResponseDto;
import org.example.tryonx.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductImageRepository productImageRepository;

    public ReviewService(ReviewRepository reviewRepository, OrderItemRepository orderItemRepository, MemberRepository memberRepository, ReviewImageRepository reviewImageRepository, ProductImageRepository productImageRepository) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.memberRepository = memberRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.productImageRepository = productImageRepository;
    }

    public boolean validateReviewPermission(String email, Integer orderItemId) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        if(reviewRepository.findByMemberAndProduct(member,item.getProductItem().getProduct()).isPresent())
            throw new RuntimeException("Review already exists");
        if(item.getMember().equals(member))
            return true;
        else
            return false;
    }

    public boolean create(ReviewCreateRequestDto reviewCreateRequestDto, List<MultipartFile> images){
        OrderItem orderItem = orderItemRepository.findById(reviewCreateRequestDto.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("Order item not found"));
        Member member = memberRepository.findById(orderItem.getMember().getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));
        Review review = Review.builder()
                .member(member)
                .product(orderItem.getProductItem().getProduct())
                .orderItem(orderItem)
                .content(reviewCreateRequestDto.getContent())
                .rating(reviewCreateRequestDto.getRating())
                .size(orderItem.getProductItem().getSize())
                .build();
        reviewRepository.save(review);

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


}
