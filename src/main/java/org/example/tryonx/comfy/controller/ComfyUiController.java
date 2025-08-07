package org.example.tryonx.comfy.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.comfy.service.ComfyUiService;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/fitting")
public class ComfyUiController {

    private final ComfyUiService comfyUiService;
    private final ProductRepository productRepository;


//    public ComfyUiController(ComfyUiService comfyUiService) {
//        this.comfyUiService = comfyUiService;
//    }

//    @GetMapping("/generate")
//    public ResponseEntity<String> generate() throws Exception {
//        String filename = comfyUiService.executeInternalWorkflow();
//        return ResponseEntity.ok(" 생성된 이미지 파일: " + filename);
//    }

    @PostMapping("/save")
    public ResponseEntity<String> generateMyFitting(@AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId) throws Exception {
        String email = userDetails.getUsername();
        String filename = comfyUiService.executeFittingFlow(email, productId);
        return ResponseEntity.ok(" 생성된 이미지 파일: " + filename);
    }

    //상품파일명으로 한장 생성
//    @PostMapping("/clothing")
//    public ResponseEntity<String> generateMyFittingWithClothingName(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @RequestParam String clothingImageName) throws Exception {
//
//        String email = userDetails.getUsername();
//        String filename = comfyUiService.executeFittingFlowWithClothingName(email, clothingImageName);
//        return ResponseEntity.ok(" 생성된 이미지 파일: " + filename);
//    }

    @PostMapping("/clothing")
    public ResponseEntity<String> generateMyFittingWithClothingName(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String clothingImageName,
            @RequestParam Integer productId) throws Exception {

        String email = userDetails.getUsername();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        comfyUiService.executeFittingFlowWithClothingName(email, clothingImageName, product);

        return ResponseEntity.ok("피팅 이미지 1장 생성 및 저장 완료");
    }
}
