package org.example.tryonx.product.controller;

import org.example.tryonx.product.dto.ProductListResponseDto;
import org.example.tryonx.product.dto.ProductResponseDto;
import org.example.tryonx.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductListResponseDto>> getAllProducts() {
        List<ProductListResponseDto> products = productService.getAllAvailableProducts();
        return ResponseEntity.ok(products);
    }
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductListResponseDto>> getProductListByCategory(@PathVariable("categoryId") Integer categoryId) {
        List<ProductListResponseDto> products = productService.getProductsByCategoryId(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable("productId") Integer productId) {
        ProductResponseDto product = productService.getProduct(productId);
        return ResponseEntity.ok(product);
    }

}
