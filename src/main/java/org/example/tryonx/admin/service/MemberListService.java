package org.example.tryonx.admin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.MemberInfoDto;
import org.example.tryonx.admin.dto.MemberListDto;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberListService {
    private final MemberRepository memberRepository;

    /* 멤버 전체 */
    public List<MemberListDto> getUserList(){
        return memberRepository.findAll().stream()
                .map(member -> new MemberListDto(
                        member.getProfileUrl(),
                        member.getMemberId(),
                        member.getName()
                ))
                .collect(Collectors.toList());
    }

    /* 멤버 상세정보 */
    public MemberInfoDto findById(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다."));

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

    /* 신규 회원 조회 */
    public List<MemberListDto> getRecentUsers() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        return memberRepository.findByCreatedAtAfter(oneWeekAgo).stream()
                .map(member -> new MemberListDto(
                        member.getProfileUrl(),
                        member.getMemberId(),
                        member.getName()
                ))
                .collect(Collectors.toList());
    }

    /* 멤버 삭제 */
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다."));
        memberRepository.delete(member);
    }

}
