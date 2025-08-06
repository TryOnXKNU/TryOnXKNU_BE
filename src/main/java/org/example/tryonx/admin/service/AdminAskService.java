package org.example.tryonx.admin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.AskAnswerRequestDto;
import org.example.tryonx.admin.dto.AskListDto;
import org.example.tryonx.admin.dto.CompletedAskDetailsDto;
import org.example.tryonx.ask.domain.Ask;
import org.example.tryonx.ask.domain.AskImage;
import org.example.tryonx.ask.repository.AskRepository;
import org.example.tryonx.enums.AnswerStatus;
import org.example.tryonx.enums.Size;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAskService {
    private final AskRepository askRepository;

    public List<AskListDto> getNewAsks() {
        return askRepository.findByAnswerStatus(AnswerStatus.WAITING).stream()
                .map(ask -> {
                    var productItem = ask.getOrderItem().getProductItem();
                    var product = productItem.getProduct();
                    List<String> askImageUrls = ask.getImages().stream()
                            .map(AskImage::getImageUrl)
                            .toList();

                    return new AskListDto(
                            ask.getAskId(),
                            ask.getTitle(),
                            ask.getContent(),
                            product.getProductName(),
                            productItem.getSize(),
                            product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl(),
                            askImageUrls
                    );
                })
                .collect(Collectors.toList());
    }


    public List<AskListDto> getCompletedAsks() {
        return askRepository.findByAnswerStatus(AnswerStatus.COMPLETED).stream()
                .map(ask -> {
                    var productItem = ask.getOrderItem().getProductItem();
                    var product = productItem.getProduct();
                    List<String> askImageUrls = ask.getImages().stream()
                            .map(AskImage::getImageUrl)
                            .toList();

                    return new AskListDto(
                            ask.getAskId(),
                            ask.getTitle(),
                            ask.getContent(),
                            product.getProductName(),
                            productItem.getSize(),
                            product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl(),
                            askImageUrls
                    );
                })
                .collect(Collectors.toList());
    }


    public void answerAsk(AskAnswerRequestDto dto) {
        Ask ask = askRepository.findById(dto.getAskId())
                .orElseThrow(() -> new EntityNotFoundException("해당 문의를 찾을 수 없습니다."));

        ask.setAnswer(dto.getAnswer());
        ask.setAnsweredAt(LocalDateTime.now());
        ask.setAnswerStatus(AnswerStatus.COMPLETED);

        askRepository.save(ask);
    }

    public CompletedAskDetailsDto getCompletedAskDetail(Long askId) {
        Ask ask = askRepository.findById(askId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문의글입니다."));

        // 답변 완료된 상태인지 확인
        if (ask.getAnswerStatus() != AnswerStatus.COMPLETED) {
            throw new RuntimeException("답변이 완료된 문의만 조회할 수 있습니다.");
        }

        // 상품명과 사이즈 추출
        String productName = ask.getOrderItem().getProductItem().getProduct().getProductName();
        Size size = ask.getOrderItem().getProductItem().getSize();

        // 이미지 URL 리스트 추출
        List<String> imageUrls = ask.getImages().stream()
                .map(image -> image.getImageUrl())
                .collect(Collectors.toList());

        return new CompletedAskDetailsDto(
                ask.getAskId(),
                ask.getMember().getNickname(),
                productName,
                size,
                ask.getTitle(),
                ask.getContent(),
                ask.getAnswer(),
                ask.getCreatedAt(),
                ask.getAnsweredAt(),
                imageUrls
        );
    }



}
