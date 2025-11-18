package org.example.tryonx.image.repository;

import org.example.tryonx.image.domain.MemberClothesImage;
import org.example.tryonx.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberClothesImageRepository extends JpaRepository<MemberClothesImage, Integer> {
    List<MemberClothesImage> findByMember_MemberId(Long memberId);
    Optional<MemberClothesImage> findByMemberAndMemberClothesId(Member member, String memberClothesId);

}
