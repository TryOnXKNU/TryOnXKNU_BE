package org.example.tryonx.image.repository;

import org.example.tryonx.image.domain.MemberClothesImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberClothesImageRepository extends JpaRepository<MemberClothesImage, Integer> {
    List<MemberClothesImage> findByMember_MemberId(Long memberId);
}
