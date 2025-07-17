package org.example.tryonx.member.controller;

import org.example.tryonx.member.dto.CheckPasswordRequest;
import org.example.tryonx.member.dto.MemberListResponseDto;
import org.example.tryonx.member.dto.MyInfoResponseDto;
import org.example.tryonx.member.dto.UpdateMemberRequestDto;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class MemberController {
    private final MemberService memberService;
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/list")
    //걍 테스트용 하나 만들어봤수
    public ResponseEntity<?> memberList() {
        List<MemberListResponseDto> dtos = memberService.findAll();
        return new ResponseEntity<>(dtos, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<MyInfoResponseDto> myInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        MyInfoResponseDto myInfo = memberService.getMyInfo(email);
        return new ResponseEntity<>(myInfo, HttpStatus.OK);
    }

    @PostMapping("/check-password")
    public ResponseEntity<?> checkPassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody CheckPasswordRequest request) {
        String email = userDetails.getUsername();
        return new ResponseEntity<>(memberService.checkPassword(email, request.password()), HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<?> updateMember(@AuthenticationPrincipal UserDetails userDetails,@RequestBody UpdateMemberRequestDto dto) {
        String email = userDetails.getUsername();
        memberService.updateMember(email, dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }
    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("profileImage") MultipartFile profileImage) {

        String email = userDetails.getUsername();
        try {
            memberService.updateProfileImage(email, profileImage);
            return ResponseEntity.ok("프로필 이미지가 성공적으로 업데이트되었습니다.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("프로필 이미지 업데이트 실패: " + e.getMessage());
        }
    }

    @GetMapping("/profile-image")
    public ResponseEntity<String> getProfileImage(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        String profileImage = memberService.getProfileImage(email);
        return ResponseEntity.ok(profileImage);
    }
}
