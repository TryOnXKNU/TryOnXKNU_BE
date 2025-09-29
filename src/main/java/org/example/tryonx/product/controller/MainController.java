package org.example.tryonx.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.like.dto.ProductDto;
import org.example.tryonx.product.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/main")
@Tag(name = "User Main Home API", description = "회원 메인 홈 API")
@RequiredArgsConstructor
public class MainController {
    private final ProductService productService;

    @GetMapping("/popular-styles")
    @Operation(summary = "인기 많은 스타일")
    public List<ProductDto> popularStyles() {
        return productService.getTopLikedProducts(6);
    }

    @GetMapping("/similar-styles")
    @Operation(summary = "비슷한 스타일 추천")
    public List<ProductDto> similarStyles(@AuthenticationPrincipal(expression = "username") String email) {
        return productService.getSimilarByBodyShape(email, 6);
    }
}
