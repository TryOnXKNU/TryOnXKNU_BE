package org.example.tryonx.product.controller;

import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.dto.ProductCreateRequestDto;
import org.example.tryonx.product.dto.ProductListResponseDto;
import org.example.tryonx.product.dto.ProductResponseDto;
import org.example.tryonx.product.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> createProduct(
            @RequestPart(value="dto") ProductCreateRequestDto dto,
            @RequestPart(value="images", required = false) List<MultipartFile> images) {
        Product product = productService.createProduct(dto, images);
        return ResponseEntity.ok(product);
    }
    @GetMapping
    public ResponseEntity<List<ProductListResponseDto>> getAllProducts() {
        List<ProductListResponseDto> products = productService.getAllProducts();
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
