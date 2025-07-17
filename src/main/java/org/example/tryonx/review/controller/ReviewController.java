package org.example.tryonx.review.controller;

import okhttp3.Response;
import org.example.tryonx.review.dto.CheckAuthForReviewDto;
import org.example.tryonx.review.dto.ReviewCreateRequestDto;
import org.example.tryonx.review.dto.ReviewDeleteRequest;
import org.example.tryonx.review.dto.ReviewResponseDto;
import org.example.tryonx.review.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<String> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart(name = "dto")ReviewCreateRequestDto reviewCreateRequestDto,
            @RequestPart(name = "images", required = false)List<MultipartFile> images){
        String email = userDetails.getUsername();
        if(reviewService.validateReviewPermission(email,reviewCreateRequestDto.getOrderItemId())) {
            boolean b = reviewService.create(reviewCreateRequestDto, images);
            return ResponseEntity.status(HttpStatus.CREATED).body(b ? "리뷰가 성공적으로 생성되었습니다." : "리뷰 작성 도중 문제가 생겼습니다.");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리뷰 작성 권한이 없습니다.");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteReview(@AuthenticationPrincipal UserDetails userDetails, @RequestBody ReviewDeleteRequest reviewDeleteRequest){
        String email = userDetails.getUsername();
        if(reviewService.delete(email, reviewDeleteRequest.reviewId()))
            return ResponseEntity.ok("리뷰가 삭제되었습니다.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리뷰 삭제 실패");
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<ReviewResponseDto> reviews = reviewService.getMyReviews(email);
        return ResponseEntity.ok(reviews);
    }


}
