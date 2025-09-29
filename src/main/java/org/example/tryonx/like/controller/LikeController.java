package org.example.tryonx.like.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.like.dto.LikeResponse;
import org.example.tryonx.like.dto.ProductDto;
import org.example.tryonx.like.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "User Likes API", description = "회원 좋아요 API")
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/{productId}/like")
    @Operation(summary = "상품 좋아요/취소")
    public ResponseEntity<LikeResponse> likeProduct(
            @PathVariable Integer productId,
            @AuthenticationPrincipal(expression = "username") String username) {

        LikeResponse likeResponse = likeService.goodProduct(productId, username);
        return ResponseEntity.ok(likeResponse);
    }

    @GetMapping("/likes")
    @Operation(summary = "좋아요 목록 조회")
    public ResponseEntity<List<ProductDto>> getLikedProducts(
            @AuthenticationPrincipal(expression = "username") String username) {

        List<ProductDto> likedProducts = likeService.getLikedProducts(username);
        return ResponseEntity.ok(likedProducts);
    }
}
