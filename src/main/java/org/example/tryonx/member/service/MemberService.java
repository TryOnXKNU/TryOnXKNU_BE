package org.example.tryonx.member.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.tryonx.admin.dto.MemberInfoDto;
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

    public MemberInfoDto findById(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원이 존재하지 않습니다."));

        return MemberInfoDto.builder()
                .profileUrl(member.getProfileUrl())
                .name(member.getName())
                .memberId(member.getMemberId())
                .nickName(member.getNickname())
                .phoneNumber(member.getPhoneNumber())
                .birthday(member.getBirthDate())
                .address(member.getAddress())
                .bodyType(member.getBodyType())
                .height(member.getHeight())
                .weight(member.getWeight())
                .gender(member.getGender())
                .build();
    }
}
