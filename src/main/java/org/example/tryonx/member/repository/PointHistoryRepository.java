package org.example.tryonx.member.repository;

import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    List<PointHistory> findByMemberOrderByCreatedAtDesc(Member member);
}

