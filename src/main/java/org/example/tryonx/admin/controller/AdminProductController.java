package org.example.tryonx.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.tryonx.admin.dto.ProductDetailUpdateDto;
import org.example.tryonx.admin.dto.ProductImageUrl;
import org.example.tryonx.admin.dto.ProductListDto;
import org.example.tryonx.admin.dto.ProductStockAndStateUpdateDto;
import org.example.tryonx.admin.service.AdminProductService;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.dto.ProductCreateRequestDto;
import org.example.tryonx.product.dto.ProductResponseDto;
import org.example.tryonx.product.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/product")
@Tag(name = "Admin Products API", description = "관리자 상품 관리 API")
public class AdminProductController {
    private final AdminProductService adminProductService;
    private final ProductService productService;

    public AdminProductController(AdminProductService adminProductService, ProductService productService) {
        this.adminProductService = adminProductService;
        this.productService = productService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "상품 등록")
    public ResponseEntity<Product> createProduct(
            @RequestPart(value="dto") ProductCreateRequestDto dto,
            @RequestPart(value="images", required = false) List<MultipartFile> images) {
        Product product = productService.createProduct(dto, images);
        return ResponseEntity.ok(product);
    }

    @GetMapping
    @Operation(summary = "상품 조회")
    public ResponseEntity<List<ProductListDto>> getProducts() {
        List<ProductListDto> allProducts = adminProductService.getAllProducts();
        return ResponseEntity.ok(allProducts);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "상품 상세 조회")
    public ResponseEntity<?> findProductById(@PathVariable Integer productId) {
        ProductResponseDto product = adminProductService.getProductDetail(productId);
        return ResponseEntity.ok(product);
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "상품 상태 수정")
    public ResponseEntity<?> updateProduct(@PathVariable("productId") Integer productId, @RequestBody ProductStockAndStateUpdateDto dto) {
        adminProductService.updateProductStockAndState(productId, dto);
        return ResponseEntity.ok().build();
    }


    @PatchMapping("/{productId}/detail")
    @Operation(summary = "상품 정보 수정")
    public ResponseEntity<?> updateProductDetail(@PathVariable("productId") Integer productId, @RequestBody ProductDetailUpdateDto detail) {
        adminProductService.updateProductDetail(productId, detail);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{productId}")
    @Operation(summary = "상품 삭제")
    public ResponseEntity<?> deleteProduct(@PathVariable("productId") Integer productId) {
        String message = adminProductService.deleteProduct(productId);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{productId}/image")
    @Operation(summary = "상품 이미지 삭제")
    public ResponseEntity<?> deleteProductImage(@PathVariable("productId") Integer productId, @RequestBody ProductImageUrl image) {
        adminProductService.deleteProductImage(image.imageUrl());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/name")
    @Operation(summary = "상품명 중복 확인")
    public ResponseEntity<?> checkAvailableProductName(@RequestParam String productName) {
        boolean value = adminProductService.checkProductNameDuplicate(productName);
        return ResponseEntity.ok(value);
    }
}
