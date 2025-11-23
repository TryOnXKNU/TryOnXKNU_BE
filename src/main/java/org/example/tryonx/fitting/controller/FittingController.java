package org.example.tryonx.fitting.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.comfy.service.ComfyUiService;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.fitting.dto.BodyShapeRequest;
import org.example.tryonx.fitting.dto.FittingResponse;
import org.example.tryonx.fitting.repository.FittingImageRepository;
import org.example.tryonx.fitting.service.FittingImageService;
import org.example.tryonx.fitting.service.FittingService;
import org.example.tryonx.image.domain.MemberClothesImage;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fitting")
@Tag(name = "User AI Fitting API", description = "회원 AI 피팅 API")
public class FittingController {
    private static final Logger logger = LoggerFactory.getLogger(FittingController.class);
    private static final double LOG_CONFIDENCE_THRESHOLD = 0.2;
    private final MemberService memberService;
    private final FittingService fittingService;
    private final ComfyUiService comfyUiService;
    private final MemberRepository memberRepository;
    private final FittingImageService fittingImageService;

//    public FittingController(MemberService memberService, FittingService fittingService, ComfyUiService comfyUiService) {
//        this.memberService = memberService;
//        this.fittingService = fittingService;
//        this.comfyUiService = comfyUiService;
//    }

    @GetMapping
    @Operation(summary = "피팅 페이지 조회")
    public ResponseEntity<?> fittingPageInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        FittingResponse pageData = fittingService.getFittingPageData(email);
        return ResponseEntity.ok(pageData);
    }


    @PostMapping("/body-shape")
    @Operation(summary = "체형 변경")
    public ResponseEntity<?> selectBodyShape(@AuthenticationPrincipal UserDetails userDetails, @RequestBody BodyShapeRequest request) {
        String email = userDetails.getUsername();
        BodyShape response = fittingService.updateBodyShape(email, request);
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/try-on/single")
//    public ResponseEntity<String> generateMyFitting(@AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId) throws Exception {
//        String email = userDetails.getUsername();
//        String filename = comfyUiService.executeFittingFlow(email, productId);
//        return ResponseEntity.ok("/upload/fitting/downloaded_"+ filename);
//    }

//    @PostMapping("/try-on/dual")
//    @Operation(summary = "AI 피팅")
//    public ResponseEntity<String> generateMyFitting(@AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId1, @RequestParam(required = false) Integer productId2) throws Exception {
//        String email = userDetails.getUsername();
//        String filename = comfyUiService.executeFittingTwoClothesFlow(email, productId1, productId2);
//        return ResponseEntity.ok("https://tryonx.s3.ap-northeast-2.amazonaws.com/fitting/fittingRoom/" + filename);
//    }

//    @PostMapping("/try-on/dual")
//    @Operation(summary = "AI 피팅")
//    public ResponseEntity<String> generateMyFitting(
//            @AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId1, @RequestParam(required = false) Integer productId2) throws Exception {
//
//        String email = userDetails.getUsername();
//        Member member = memberRepository.findByEmail(email).orElseThrow();
//
//        // 1) ComfyUI 실행 → 파일명 얻기
//        String filename = comfyUiService.executeFittingTwoClothesFlow(email, productId1, productId2);
//
//        // 2) URL 만들기
//        String imageUrl = "https://tryonx.s3.ap-northeast-2.amazonaws.com/fitting/fittingRoom/" + filename;
//
//        // 3) DB 저장
//        fittingImageService.saveFittingImage(member, imageUrl, productId1, productId2);
//
//        // 4) 응답
//        return ResponseEntity.ok(imageUrl);
//    }

    @PostMapping("/try-on/dual")
    @Operation(summary = "AI 피팅")
    public ResponseEntity<String> generateMyFitting(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Integer productId1,
            @RequestParam(required = false) Integer productId2,
            @RequestParam(required = false) String memberClothesId1,
            @RequestParam(required = false) String memberClothesId2
    ) throws Exception {
        try {
            if (productId1 == null &&
                    productId2 == null &&
                    (memberClothesId1 == null || memberClothesId1.isBlank()) &&
                    (memberClothesId2 == null || memberClothesId2.isBlank())) {

                return ResponseEntity.badRequest().body("productId1, productId2, memberClothesId1, memberClothesId2 중 최소 1개는 필요합니다.");
            }

            String email = userDetails.getUsername();
            Member member = memberRepository.findByEmail(email).orElseThrow();

            // 1) ComfyUI 실행 → 파일명 얻기
            String filename = comfyUiService.executeFittingUnified(
                    email,
                    productId1,
                    productId2,
                    memberClothesId1,
                    memberClothesId2
            );

            // 2) URL 만들기
            String imageUrl = "https://tryonx.s3.ap-northeast-2.amazonaws.com/fitting/fittingRoom/" + filename;

            // 3) DB 저장
            fittingImageService.saveFittingImage(member, imageUrl, productId1, productId2);

            // 4) 응답
            return ResponseEntity.ok(imageUrl);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            // 로그 출력 후 500으로 응답
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }


    @PostMapping(value = "/custom/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "CUSTOM 의상 등록")
    public ResponseEntity<?> addMyClothes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String name,
            @RequestParam Integer categoryId,
            @RequestParam("myClothesImage1") MultipartFile myClothesImage
    ) {
        String email = userDetails.getUsername();
        try {
            // 서버 측 유효성 검사: 이미지를 분석하고 의류가 아닌 경우 거부
            byte[] bytes = myClothesImage.getBytes();
            Map<String, Object> analysis = org.example.tryonx.fitting.service.ImageValidationUtil.analyze(bytes);
            if (analysis.containsKey("error")) {
                logger.info("Image validation error for user={}, file={} size={} -> {}", email, myClothesImage.getOriginalFilename(), myClothesImage.getSize(), analysis);
                return ResponseEntity.badRequest().body(analysis);
            }
            boolean isClothing = Boolean.TRUE.equals(analysis.get("isClothing"));
            double confidence = 0.0;
            try {
                Object cf = analysis.get("confidence");
                if (cf instanceof Number) confidence = ((Number) cf).doubleValue();
                else if (cf instanceof String) confidence = Double.parseDouble((String) cf);
            } catch (Exception ignored) {}

            // 의류가 아니거나 신뢰도가 임계값 미만인 경우 WARN 로그를, 그렇지 않은 경우 INFO 로그를 남깁니다.
            if (!isClothing || confidence < LOG_CONFIDENCE_THRESHOLD) {
                logger.warn("Image validation for user={}, file={} size={} -> isClothing={} confidence={} analysis={}", email, myClothesImage.getOriginalFilename(), myClothesImage.getSize(), isClothing, confidence, analysis);
            } else {
                logger.info("Image validation for user={}, file={} size={} -> isClothing={} confidence={} analysis={}", email, myClothesImage.getOriginalFilename(), myClothesImage.getSize(), isClothing, confidence, analysis);
            }

            if (!isClothing) {
                return ResponseEntity.status(400).body(analysis);
            }

            MemberClothesImage memberClothesImage = fittingService.addMemberClothesImage(email, name, categoryId, myClothesImage);
            return ResponseEntity.ok(memberClothesImage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("서버 오류: " + e.getMessage());
        }
    }

    @DeleteMapping("/custom/delete")
    @Operation(summary = "CUSTOM 의상 삭제")
    public ResponseEntity<String> deleteMyClothes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String memberClothesId
    ) {
        String email = userDetails.getUsername();
        fittingService.deleteMemberClothesImage(email, memberClothesId);

        return ResponseEntity.ok("삭제가 완료되었습니다.");
    }
}
