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

// 정빈 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

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

    @Value("${comfy.input-dir:/tmp/comfy_input}")
    private String comfyInputDir;

    @Value("${comfy.drive-refresh-script:}") // 빈이면 실행하지 않음
    private String driveRefreshScript;

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

    @PostMapping("/drive/refresh")
    public ResponseEntity<?> refreshDriveMount(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Integer productId
    ) {
        try {
            // 1) productId가 주어지면 서버에 저장된 썸네일 파일을 comfy input 폴더로 복사 시도
            if (productId != null) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                ProductImage thumbnail = productImageRepository
                        .findFirstByProductAndIsThumbnailTrue(product)
                        .orElseThrow(() -> new RuntimeException("Thumbnail image not found"));

                String imageUrl = thumbnail.getImageUrl(); // 예: /upload/product/5/xxx.png 또는 저장된 상대경로/파일명

                try {
                    Path src;
                    if (imageUrl.startsWith("/")) {
                        // 예: /upload/fitting/xxx.png -> 프로젝트 루트 기준으로 resolve
                        src = Paths.get(System.getProperty("user.dir")).resolve(imageUrl.substring(1));
                    } else {
                        // imageUrl이 상대경로 또는 파일명일 경우 업로드 루트에서 찾음
                        src = Paths.get(System.getProperty("user.dir")).resolve("upload").resolve("fitting").resolve(imageUrl);
                    }

                    Path destDir = Paths.get(comfyInputDir);
                    Files.createDirectories(destDir);
                    Path dest = destDir.resolve(src.getFileName().toString());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copied thumbnail to comfy input: " + dest.toString());
                } catch (Exception e) {
                    // 복사 실패해도 아래 스크립트 실행은 시도하도록 계속 진행
                    System.err.println("Unable to copy thumbnail to comfy input: " + e.getMessage());
                }
            }

            // 2) 외부 스크립트로 드라이브 재마운트/새로고침을 수행하도록 구성되어 있다면 실행
            if (StringUtils.hasText(driveRefreshScript)) {
                ProcessBuilder pb = new ProcessBuilder(driveRefreshScript);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                int exitCode = p.waitFor(); // 동기적으로 대기
                System.out.println("drive refresh script exitCode=" + exitCode);
                if (exitCode != 0) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("ok", false, "message", "drive refresh script failed", "exitCode", exitCode));
                }
            }

            // 3) 성공 응답
            Map<String, Object> resp = new HashMap<>();
            resp.put("ok", true);
            resp.put("message", "drive refresh triggered (copied files and/or ran script)");
            return ResponseEntity.ok(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

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
