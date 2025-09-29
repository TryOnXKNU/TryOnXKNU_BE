package org.example.tryonx.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.tryonx.product.dto.ProductListResponseDto;
import org.example.tryonx.product.dto.ProductResponseDto;
import org.example.tryonx.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Users Products API", description = "회원 상품 API")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "전체 상품 조회")
    public ResponseEntity<List<ProductListResponseDto>> getAllProducts() {
        List<ProductListResponseDto> products = productService.getAllAvailableProducts();
        return ResponseEntity.ok(products);
    }
    @GetMapping("/category/{categoryId}")
    @Operation(summary = "카테고리별 상품 조회")
    public ResponseEntity<List<ProductListResponseDto>> getProductListByCategory(@PathVariable("categoryId") Integer categoryId) {
        List<ProductListResponseDto> products = productService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable("productId") Integer productId) {
        ProductResponseDto product = productService.getProduct(productId);
        return ResponseEntity.ok(product);
    }

}
