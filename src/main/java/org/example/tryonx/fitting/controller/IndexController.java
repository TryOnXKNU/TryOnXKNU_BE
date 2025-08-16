package org.example.tryonx.fitting.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.member.service.MemberService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/api/v1/fitting/camera")
@RequiredArgsConstructor
public class IndexController {
    private final MemberRepository memberRepository;

    @GetMapping("/{memberId}")
    public String index(Model model, @PathVariable Integer memberId
                        ) {
        model.addAttribute("memberId", memberId);

        return "index"; // resources/templates/index.html
    }
}