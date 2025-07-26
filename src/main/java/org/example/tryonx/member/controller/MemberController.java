package org.example.tryonx.member.controller;

import org.example.tryonx.member.domain.Role;
import org.example.tryonx.member.dto.*;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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


    @PatchMapping("/password")
    public ResponseEntity<?> updatePassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UpdatePasswordReq updatePasswordReq){
        String email = userDetails.getUsername();
        memberService.updatePassword(email,updatePasswordReq.newPassword());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/nickname")
    public ResponseEntity<?> updateNickname(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String nickName){
        String email = userDetails.getUsername();
        memberService.updateNickname(email, nickName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/address")
    public ResponseEntity<?> updateAddress(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String address){
        String email = userDetails.getUsername();
        memberService.updateAddress(email, address);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/bodyInfo")
    public ResponseEntity<?> updateBodyInfo(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UpdateBodyInfoDto dto) {
        String email = userDetails.getUsername();
        memberService.updateBodyInformation(email, dto);
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

    @PostMapping("/check-password")
    public ResponseEntity<?> checkPassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody CheckPasswordRequest request) {
        String email = userDetails.getUsername();
        return new ResponseEntity<>(memberService.checkPassword(email, request.password()), HttpStatus.OK);
    }

    @GetMapping("/profile-image")
    public ResponseEntity<String> getProfileImage(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        String profileImage = memberService.getProfileImage(email);
        return ResponseEntity.ok(profileImage);
    }

    /* 권한 변경 */
    @PatchMapping("/members/{memberId}/role/{role}")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable Long memberId,
            @PathVariable Role role
    ) {
        memberService.updateRole(memberId, role);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMember(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        memberService.deleteMember(email);
        return ResponseEntity.ok().build();
    }

}
