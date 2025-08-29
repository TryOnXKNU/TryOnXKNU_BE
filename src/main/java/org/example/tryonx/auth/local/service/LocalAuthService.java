package org.example.tryonx.auth.local.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.tryonx.auth.email.service.EmailService;
import org.example.tryonx.auth.local.dto.LoginRequestDto;
import org.example.tryonx.auth.local.dto.SignupRequestDto;
import org.example.tryonx.auth.sms.service.SmsService;
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
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    public LocalAuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, EmailService emailService, SmsService smsService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    public Member create(SignupRequestDto dto) {
        if(memberRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }
        if (!emailService.isVerified(dto.getEmail())) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        if(!smsService.isVerified(dto.getPhoneNumber())){
            throw new IllegalStateException("휴대폰 인증이 완료되지 않았습니다.");
        }
        if(!isValidPassword(dto.getPassword())) {
            throw new IllegalStateException("비밀번호 형식이 옳바르지 않습니다.");
        }
        Member member = Member.builder()
                .email(dto.getEmail())
                .name(dto.getName())
                .nickname(dto.getNickname())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phoneNumber(dto.getPhoneNumber())
                .height(dto.getHeight())
                .weight(dto.getWeight())
                .bodyShape(dto.getBodyShape())
                .birthDate(dto.getBirthDate())
                .point(0)
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

    public boolean isNicknameExist(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }
    public String findIdByPhoneNumber(String phoneNumber) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(()->new EntityNotFoundException("해당 휴대폰 번호 사용자가 없습니다."));
        if(!smsService.isVerified(phoneNumber)){
            throw new IllegalStateException("휴대폰 인증이 완료되지 않았습니다.");
        }
        return member.getEmail();
    }

    public void resetPassword(String email, String newPassword) {
        if(!emailService.isVerified(email)){
            throw new IllegalStateException("Invalid email");
        }
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(!isValidPassword(newPassword)){
            throw new IllegalStateException("비밀번호 형식이 옳바르지 않습니다.");
        }
        member.updatePassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    private boolean isValidPassword(String password) {
        // 영문, 숫자, 특수문자 각 1개 이상 포함, 최소 8자
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
        return password.matches(regex);
    }
    public boolean IsExistedEmail(String email) {
        if(memberRepository.findByEmail(email).isPresent()) {
            return true;
        }else
            return false;
    }
}
