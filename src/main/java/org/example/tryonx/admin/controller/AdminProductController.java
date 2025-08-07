package org.example.tryonx.admin.controller;

import org.example.tryonx.admin.dto.ProductDetailUpdateDto;
import org.example.tryonx.admin.dto.ProductImageUrl;
import org.example.tryonx.admin.dto.ProductListDto;
import org.example.tryonx.admin.dto.ProductStockAndStateUpdateDto;
import org.example.tryonx.admin.service.AdminProductService;
import org.example.tryonx.product.dto.ProductResponseDto;
import org.example.tryonx.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/product")
public class AdminProductController {
    private final AdminProductService adminProductService;
    private final ProductService productService;

    public AdminProductController(AdminProductService adminProductService, ProductService productService) {
        this.adminProductService = adminProductService;
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductListDto>> getProducts() {
        List<ProductListDto> allProducts = adminProductService.getAllProducts();
        return ResponseEntity.ok(allProducts);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> findProductById(@PathVariable Integer productId) {
        ProductResponseDto product = adminProductService.getProductDetail(productId);
        return ResponseEntity.ok(product);
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable("productId") Integer productId, @RequestBody ProductStockAndStateUpdateDto dto) {
        adminProductService.updateProductStockAndState(productId, dto);
        return ResponseEntity.ok().build();
    }


    @PatchMapping("/{productId}/detail")
    public ResponseEntity<?> updateProductDetail(@PathVariable("productId") Integer productId, @RequestBody ProductDetailUpdateDto detail) {
        adminProductService.updateProductDetail(productId, detail);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{productId}")
    public ResponseEntity<?> deleteProduct(@PathVariable("productId") Integer productId) {
        String message = adminProductService.deleteProduct(productId);
        return ResponseEntity.ok(message);
    }

    @DeleteMapping("/{productId}/image")
    public ResponseEntity<?> deleteProductImage(@PathVariable("productId") Integer productId, @RequestBody ProductImageUrl image) {
        adminProductService.deleteProductImage(image.imageUrl());
        return ResponseEntity.ok().build();
    }
}
