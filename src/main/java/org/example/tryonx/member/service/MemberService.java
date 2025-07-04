package org.example.tryonx.member.service;

import org.example.tryonx.auth.email.service.EmailService;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.dto.MemberListResponseDto;
import org.example.tryonx.member.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<MemberListResponseDto>  findAll() {
        List<Member> members = memberRepository.findAll();
        List<MemberListResponseDto> memberListResDtos = new ArrayList<>();
        for(Member member : members){
            MemberListResponseDto memberListResDto = new MemberListResponseDto();
            memberListResDto.setName(member.getName());
            memberListResDto.setEmail(member.getEmail());
            memberListResDto.setPhone(member.getPhoneNumber());
            memberListResDtos.add(memberListResDto);
        }
        return memberListResDtos;
    }
}
