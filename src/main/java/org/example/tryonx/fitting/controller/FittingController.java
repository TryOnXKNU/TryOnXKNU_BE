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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/fitting")
@Tag(name = "User AI Fitting API", description = "회원 AI 피팅 API")
public class FittingController {
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
    @Operation(summary = "체형 조회")
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

    @PostMapping("/try-on/dual")
    @Operation(summary = "AI 피팅")
    public ResponseEntity<String> generateMyFitting(
            @AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId1, @RequestParam(required = false) Integer productId2) throws Exception {

        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email).orElseThrow();

        // 1) ComfyUI 실행 → 파일명 얻기
        String filename = comfyUiService.executeFittingTwoClothesFlow(email, productId1, productId2);

        // 2) URL 만들기
        String imageUrl = "https://tryonx.s3.ap-northeast-2.amazonaws.com/fitting/fittingRoom/" + filename;

        // 3) DB 저장
        fittingImageService.saveFittingImage(member, imageUrl, productId1, productId2);

        // 4) 응답
        return ResponseEntity.ok(imageUrl);
    }


    @PostMapping(value = "/try-on/custom/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "CUSTOM 피팅 의상 등록")
    public ResponseEntity<?> addMyClothes(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Integer categoryId1,
            @RequestParam(required = false) Integer categoryId2,
            @RequestParam("myClothesImage1") MultipartFile myClothesImage1,
            @RequestParam(value = "myClothesImage2", required = false) MultipartFile myClothesImage2
    )throws Exception{
        String email = userDetails.getUsername();
        List<MemberClothesImage> memberClothesImages = fittingService.addMemberClothesImage(email, categoryId1, categoryId2, myClothesImage1, myClothesImage2);
        return ResponseEntity.ok(memberClothesImages);
    }

    @PostMapping("/try-on/custom")
    @Operation(summary = "CUSTOM 피팅")
    public ResponseEntity<String> generateMyClothesFitting(@AuthenticationPrincipal UserDetails userDetails,@RequestParam Integer myClothesId1, @RequestParam(required = false)Integer myClothesId2) throws Exception {
        String email = userDetails.getUsername();
        String filename = comfyUiService.executeFittingMyClothesFlow(email, myClothesId1, myClothesId2);
        return ResponseEntity.ok("https://tryonx.s3.ap-northeast-2.amazonaws.com/fitting/fittingRoom/" + filename);
    }
}
