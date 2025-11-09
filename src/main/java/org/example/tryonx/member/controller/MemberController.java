package org.example.tryonx.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.Role;
import org.example.tryonx.member.dto.*;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users API", description = "회원 API")
public class MemberController {
    private final MemberService memberService;
    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "마이페이지")
    public ResponseEntity<MyInfoResponseDto> myInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        MyInfoResponseDto myInfo = memberService.getMyInfo(email);
        return new ResponseEntity<>(myInfo, HttpStatus.OK);
    }

    @PatchMapping("/password")
    @Operation(summary = "비밀번호 변경")
    public ResponseEntity<?> updatePassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UpdatePasswordReq updatePasswordReq){
        String email = userDetails.getUsername();
        memberService.updatePassword(email,updatePasswordReq.newPassword());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/nickname")
    @Operation(summary = "닉네임 변경")
    public ResponseEntity<?> updateNickname(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String nickName){
        String email = userDetails.getUsername();
        memberService.updateNickname(email, nickName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/address")
    @Operation(summary = "주소 변경")
    public ResponseEntity<?> updateAddress(@AuthenticationPrincipal UserDetails userDetails, @RequestParam String address){
        String email = userDetails.getUsername();
        memberService.updateAddress(email, address);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/bodyInfo")
    @Operation(summary = "체형 변경")
    public ResponseEntity<?> updateBodyInfo(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UpdateBodyInfoDto dto) {
        String email = userDetails.getUsername();
        memberService.updateBodyInformation(email, dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/phonenumber")
    @Operation(summary = "전화번호 변경")
    public ResponseEntity<String> updatePhoneNumber(@AuthenticationPrincipal UserDetails userDetails, @RequestBody UpdatePhoneNumberDto updatePhoneNumberDto) {

        String email = userDetails.getUsername();
        String phoneNumber = updatePhoneNumberDto.getPhoneNumber();

        memberService.updatePhoneNumber(email, phoneNumber);
        return ResponseEntity.ok("전화번호가 수정되었습니다.");
    }


    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 변경")
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
    @Operation(summary = "비밀번호 확인")
    public ResponseEntity<?> checkPassword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody CheckPasswordRequest request) {
        String email = userDetails.getUsername();
        return new ResponseEntity<>(memberService.checkPassword(email, request.password()), HttpStatus.OK);
    }

    @GetMapping("/profile-image")
    @Operation(summary = "프로필 이미지 조회")
    public ResponseEntity<String> getProfileImage(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        String profileImage = memberService.getProfileImage(email);
        return ResponseEntity.ok(profileImage);
    }

    /* 권한 변경 */
    @PatchMapping("/members/{memberId}/role/{role}")
    @Operation(summary = "권한 변경")
    public ResponseEntity<Void> updateMemberRole(
            @PathVariable Long memberId,
            @PathVariable Role role
    ) {
        memberService.updateRole(memberId, role);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    @Operation(summary = "회원 탈퇴")
    public ResponseEntity<Void> deleteMember(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        memberService.deleteMember(email);
        return ResponseEntity.ok().build();
    }

}
