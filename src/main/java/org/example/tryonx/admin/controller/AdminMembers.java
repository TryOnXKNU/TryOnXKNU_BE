package org.example.tryonx.admin.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.MemberInfoDto;
import org.example.tryonx.admin.dto.MemberListDto;
import org.example.tryonx.admin.service.MemberListService;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@CrossOrigin(origins = {"http://localhost:3000"})
public class AdminMembers {
    private final MemberListService memberListService;
    private final MemberService memberService;

    // 전체 멤버 목록 조회
    @GetMapping("/members")
    public ResponseEntity<List<MemberListDto>> getAllUsers() {
        List<MemberListDto> userList = memberListService.getUserList();
        return ResponseEntity.ok(userList);
    }

    //멤버 상세정보
    @GetMapping("/admin/member/{memberId}")
    public ResponseEntity<MemberInfoDto> showMember(@PathVariable Long memberId) {
        MemberInfoDto dto = memberService.findById(memberId);
        return ResponseEntity.ok(dto);
    }


}
