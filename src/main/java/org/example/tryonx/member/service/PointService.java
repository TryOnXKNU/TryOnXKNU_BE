package org.example.tryonx.member.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.dto.PointHistoryDto;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.member.repository.PointHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public List<PointHistoryDto> getPointHistory(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보가 없습니다."));

        return pointHistoryRepository.findByMemberOrderByCreatedAtDesc(member).stream()
                .map(PointHistoryDto::from)
                .toList();
    }
}

