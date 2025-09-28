package org.example.tryonx.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.MemberInfoDto;
import org.example.tryonx.admin.dto.MemberListDto;
import org.example.tryonx.admin.dto.MemberOrderHistory;
import org.example.tryonx.admin.dto.MemberSearchRequest;
import org.example.tryonx.admin.service.MemberListService;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Admin Members API", description = "관리자 회원 관리 API")
public class AdminMembers {
    private final MemberListService memberListService;
    private final MemberService memberService;

    // 전체 회원 목록 조회
    @GetMapping("/members")
    @Operation(summary = "전체 회원 목록 조회")
    public ResponseEntity<List<MemberListDto>> getAllUsers() {
        List<MemberListDto> userList = memberListService.getUserList();
        return ResponseEntity.ok(userList);
    }

    //회원 상세정보
    @GetMapping("/members/{memberId}")
    @Operation(summary = "회원 상세 조회")
    public ResponseEntity<MemberInfoDto> showMember(@PathVariable Long memberId) {
        MemberInfoDto dto = memberService.findById(memberId);
        return ResponseEntity.ok(dto);
    }

    //신규 회원 목록 조회
    @GetMapping("/members/recent")
    @Operation(summary = "신규 회원 목록 조회")
    public ResponseEntity<List<MemberListDto>> getRecentUsers() {
        List<MemberListDto> recentUsers = memberListService.getRecentUsers();
        return ResponseEntity.ok(recentUsers);
    }

    //회원 삭제
    @DeleteMapping("/member/{memberId}")
    @Operation(summary = "회원 삭제")
    public ResponseEntity<Void> deleteMember(@PathVariable Long memberId) {
        memberListService.deleteMemberWithDependencies(memberId);
        return ResponseEntity.noContent().build();
    }

    //멤버 필터 검색
//    @GetMapping("/members/search")
//    public ResponseEntity<List<Member>> searchMembers(@RequestParam String searchKey,
//                                                      @RequestParam String searchValue) {
//        List<Member> result = memberListService.searchMembers(
//                new MemberSearchRequest(searchKey, searchValue));
//        return ResponseEntity.ok(result);
//    }

    //회원별 주문 내역 조회
    @GetMapping("/members/{memberId}/orders")
    @Operation(summary = "회원별 주문 내역 조회")
    public ResponseEntity<List<MemberOrderHistory>> getOrderHistoryByMember(@PathVariable Long memberId) {
        List<MemberOrderHistory> history = memberListService.getOrderHistoryByMember(memberId);
        return ResponseEntity.ok(history);
    }

}
