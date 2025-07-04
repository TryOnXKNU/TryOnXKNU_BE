package org.example.tryonx.member.controller;

import org.example.tryonx.member.dto.MemberListResponseDto;
import org.example.tryonx.member.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
