package org.example.tryonx.like.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.like.dto.LikeResponse;
import org.example.tryonx.like.service.LikeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import retrofit2.Response;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;

    @PostMapping("/{productId}/like")
    public Response<LikeResponse> likeProduct(
            @PathVariable Integer productId,
            @AuthenticationPrincipal(expression = "username") String username) {

        LikeResponse likeResponse = likeService.goodProduct(productId, username);
        return Response.success(likeResponse);
    }
}
