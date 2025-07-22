package org.example.tryonx.returns.repository;

import org.example.tryonx.member.domain.Member;
import org.example.tryonx.returns.domain.Returns;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnRepository extends JpaRepository<Returns, Integer> {
    List<Returns> findAllByMember(Member member);
}
