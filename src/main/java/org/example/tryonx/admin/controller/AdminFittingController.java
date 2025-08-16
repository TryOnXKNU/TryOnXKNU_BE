package org.example.tryonx.admin.controller;

import org.example.tryonx.comfy.service.ComfyUiDualService;
import org.example.tryonx.comfy.service.ComfyUiService;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/fitting")
public class AdminFittingController {
    private final ComfyUiService comfyUiService;
    private final ProductRepository productRepository;
    private final ComfyUiDualService comfyUiDualService;

    public AdminFittingController(ComfyUiService comfyUiService, ProductRepository productRepository, ComfyUiDualService comfyUiDualService) {
        this.comfyUiService = comfyUiService;
        this.productRepository = productRepository;
        this.comfyUiDualService = comfyUiDualService;
    }

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

    @PostMapping("/clothing/dual")
    public ResponseEntity<String> generateMyFittingWithClothingNameDual(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String clothingImageName,
            @RequestParam Integer productId
    ) throws Exception {

        String email = userDetails.getUsername();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        comfyUiDualService.executeFittingFlowWithClothingNameTwoImages(email, clothingImageName, product);

        return ResponseEntity.ok("피팅 이미지 2장 생성 및 저장 완료");
    }
}
