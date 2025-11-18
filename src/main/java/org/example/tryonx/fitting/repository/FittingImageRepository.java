package org.example.tryonx.fitting.repository;

import org.example.tryonx.fitting.domain.FittingImage;
import org.example.tryonx.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FittingImageRepository extends JpaRepository<FittingImage, Long> {
    List<FittingImage> findByMemberOrderByCreatedAtDesc(Member member);
}
