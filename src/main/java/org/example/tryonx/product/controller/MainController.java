package org.example.tryonx.product.controller;

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
@RequiredArgsConstructor
public class MainController {
    private final ProductService productService;

    @GetMapping("/popular-styles")
    public List<ProductDto> popularStyles() {
        return productService.getTopLikedProducts(6);
    }

    @GetMapping("/similar-styles")
    public List<ProductDto> similarStyles(@AuthenticationPrincipal(expression = "username") String email) {
        return productService.getSimilarByBodyShape(email, 6);
    }
}
