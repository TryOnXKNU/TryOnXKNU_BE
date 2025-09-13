package org.example.tryonx.member.repository;

import org.example.tryonx.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByPhoneNumber(String phoneNumber);
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
    Optional<Member> findByMemberId(Long memberId);
    List<Member> findByCreatedAtAfter(LocalDateTime dateTime);
    long countByCreatedAtAfter(LocalDateTime dateTime);
    long count();
}
