package org.example.tryonx.auth.local.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.tryonx.auth.local.dto.LoginRequestDto;
import org.example.tryonx.auth.local.dto.SignupRequestDto;
import org.example.tryonx.auth.local.token.JwtTokenProvider;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.dto.MemberListResponseDto;
import org.example.tryonx.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocalAuthService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public LocalAuthService(MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    public Member create(SignupRequestDto dto) {
        if(memberRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }
        Member member = Member.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .nickname(dto.getNickname())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(dto.getPhoneNumber())
                .gender(dto.getGender())
                .height(dto.getHeight())
                .weight(dto.getWeight())
                .bodyType(dto.getBodyType())
                .birthDate(dto.getBirthDate())
                .build();
        return memberRepository.save(member);
    }

    public Member login(LoginRequestDto loginRequestDto) {
        Member member = memberRepository.findByEmail(loginRequestDto.getEmail()).orElseThrow(()->new EntityNotFoundException("존재하지 않는 ID입니다."));
        if(!passwordEncoder.matches(loginRequestDto.getPassword(), member.getPassword())) {
            throw new IllegalStateException("비밀번호가 일치하지 않습니다.");
        }
        return member;
    }

    public List<MemberListResponseDto> findAll(){
        List<Member> members = memberRepository.findAll();
        List<MemberListResponseDto> memberListResponseDtos = new ArrayList<>();
        for(Member member : members) {
            MemberListResponseDto memberListResponseDto = new MemberListResponseDto();
            memberListResponseDto.setName(member.getName());
            memberListResponseDto.setPhone(member.getPhoneNumber());
            memberListResponseDto.setEmail(member.getEmail());
            memberListResponseDtos.add(memberListResponseDto);
        }
        return memberListResponseDtos;
    }

}
