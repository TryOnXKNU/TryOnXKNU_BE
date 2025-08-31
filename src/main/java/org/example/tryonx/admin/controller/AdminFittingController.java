package org.example.tryonx.admin.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.comfy.service.ComfyUiDualService;
import org.example.tryonx.comfy.service.ComfyUiFittingService;
import org.example.tryonx.comfy.service.ComfyUiService;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductRepository;
import org.example.tryonx.storage.service.StorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/fitting")
@RequiredArgsConstructor
public class AdminFittingController {
//    private final ComfyUiService comfyUiService;
//    private final ProductRepository productRepository;
//    private final ComfyUiDualService comfyUiDualService;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final StorageService storageService;
    private final ComfyUiFittingService comfyUiFittingService;

//    public AdminFittingController(ComfyUiService comfyUiService, ProductRepository productRepository, ComfyUiDualService comfyUiDualService) {
//        this.comfyUiService = comfyUiService;
//        this.productRepository = productRepository;
//        this.comfyUiDualService = comfyUiDualService;
//    }

//    @PostMapping("/clothing")
//    public ResponseEntity<String> generateMyFittingWithClothingName(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam String clothingImageName,
//            @RequestParam Integer productId) throws Exception {
//
//        String email = userDetails.getUsername();
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new RuntimeException("Product not found"));
//
//        comfyUiService.executeFittingFlowWithClothingName(email, clothingImageName, product);
//
//        return ResponseEntity.ok("피팅 이미지 1장 생성 및 저장 완료");
//    }

    //기존 두개 생성
//    @PostMapping("/clothing/dual")
//    public ResponseEntity<String> generateMyFittingWithClothingNameDual(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam String clothingImageName,
//            @RequestParam Integer productId
//    ) throws Exception {
//
//        String email = userDetails.getUsername();
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new RuntimeException("Product not found"));
//
//        comfyUiDualService.executeFittingFlowWithClothingNameTwoImages(email, clothingImageName, product);
//
//        return ResponseEntity.ok("피팅 이미지 2장 생성 및 저장 완료");
//    }

    //이미지 등록용 (사용 안할듯)
//    @PostMapping(value = "/{productId}/images",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Transactional
//    public ResponseEntity<List<Map<String,Object>>> upload(
//            @PathVariable Integer productId,
//            @RequestPart("files") MultipartFile[] files
//    ) throws Exception {
//        if (files == null || files.length == 0) return ResponseEntity.badRequest().build();
//        if (files.length > 2) return ResponseEntity.status(413).build(); //최대 2장
//
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new EntityNotFoundException("상품 없음"));
//
//        long existing = productImageRepository.countByProduct(product);
//        if (existing + files.length > 2) throw new IllegalStateException("이미지 최대 2장");
//
//        String basePath = "product/" + productId + "/"; // 저장소 내부 경로
//        boolean makeThumb = (existing == 0);
//
//        List<Map<String,Object>> result = new ArrayList<>();
//        for (MultipartFile f : files) {
//            String url = storageService.store(f, basePath);
//            ProductImage img = ProductImage.builder()
//                    .product(product)
//                    .imageUrl(url)
//                    .isThumbnail(makeThumb) // 첫 이미지 썸네일
//                    .build();
//            productImageRepository.save(img);
//            result.add(Map.of(
//                    "imageId", img.getImageId(),
//                    "imageUrl", img.getImageUrl(),
//                    "isThumbnail", img.getIsThumbnail()
//            ));
//            makeThumb = false;
//        }
//        return ResponseEntity.ok(result);
//    }

    @PostMapping("/clothing/triple")
    public ResponseEntity<String> generateTriple(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam Integer productId
    ) throws Exception {
        String email = user.getUsername();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductImage thumbnail = productImageRepository.findFirstByProductAndIsThumbnailTrue(product)
                .orElseThrow(() -> new RuntimeException("Thumbnail image not found"));

        String clothingImageName = thumbnail.getImageUrl(); // ex) /upload/product/5/xxx.png

        comfyUiFittingService.executeFittingFlowWithClothingNameThreeImages(
                email, clothingImageName, product);

        return ResponseEntity.ok("피팅 이미지 3장 생성 및 저장 완료");
    }


}
