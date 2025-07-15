package org.example.tryonx.member.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.tryonx.admin.membermanage.dto.MemberInfoDto;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.dto.MemberListResponseDto;
import org.example.tryonx.member.dto.MyInfoResponseDto;
import org.example.tryonx.member.dto.UpdateMemberRequestDto;
import org.example.tryonx.member.repository.MemberRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private static final long EXPIRE_TIME = 3 * 60;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, StringRedisTemplate redisTemplate) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
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
                .build();
    }

    public MyInfoResponseDto getMyInfo(String email){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        MyInfoResponseDto myInfoResponseDto = new MyInfoResponseDto(
                member.getNickname(),
                member.getPhoneNumber(),
                member.getBirthDate(),
                member.getAddress(),
                member.getEmail(),
                member.getBodyType(),
                member.getHeight(),
                member.getWeight()
        );
        return myInfoResponseDto;
    }

    public boolean isNicknameExist(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

    public boolean checkPassword(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(passwordEncoder.matches(password, member.getPassword())) {
            redisTemplate.opsForValue().set(email, "OK", EXPIRE_TIME, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    public void updateMember(String email, UpdateMemberRequestDto updateMemberRequestDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(updateMemberRequestDto.getNewPassword() != null){
            if("OK".equals(redisTemplate.opsForValue().get(email))){
                updateMemberRequestDto.setNewPassword(passwordEncoder.encode(updateMemberRequestDto.getNewPassword()));
            }else{
                throw new IllegalStateException("비밀번호 인증을 하십시오.");
            }
        }
        member.update(updateMemberRequestDto);
        memberRepository.save(member);
    }
}
