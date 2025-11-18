package org.example.tryonx.fitting.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.fitting.domain.FittingImage;
import org.example.tryonx.fitting.repository.FittingImageRepository;
import org.example.tryonx.member.domain.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FittingImageService {
   private final FittingImageRepository fittingImageRepository;

    @Transactional
    public String saveFittingImage(Member member, String imageUrl, Integer productId1, Integer productId2) {

        FittingImage fittingImage = FittingImage.builder()
                .member(member)
                .imageUrl(imageUrl)
                .productId1(productId1)
                .productId2(productId2)
                .createdAt(LocalDateTime.now())
                .build();

        fittingImageRepository.save(fittingImage);

        return imageUrl;
    }
}
