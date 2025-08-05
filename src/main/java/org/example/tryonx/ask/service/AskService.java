package org.example.tryonx.ask.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.ask.domain.Ask;
import org.example.tryonx.ask.domain.AskImage;
import org.example.tryonx.ask.dto.AskHistoryItem;
import org.example.tryonx.ask.dto.AskListItem;
import org.example.tryonx.ask.dto.AskRequestDto;
import org.example.tryonx.ask.dto.AskResponseDto;
import org.example.tryonx.ask.repository.AskRepository;
import org.example.tryonx.enums.AnswerStatus;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AskService {

    private final AskRepository askRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;

    // 문의 내역 조회
    public List<AskHistoryItem> getMyAsks(String email) {
        List<Ask> asks = askRepository.findByMemberEmail(email);
        return asks.stream()
                .map(ask -> new AskHistoryItem(
                        ask.getAskId(),
                        ask.getTitle(),
                        ask.getImages().stream()
                                .map(AskImage::getImageUrl)
                                .collect(Collectors.toList()),
                        ask.getAnswerStatus(),
                        ask.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // 문의 가능한 상품 목록 조회
    public List<AskListItem> getAskableItems(String email) {
        List<Order> orders = orderRepository.findByMemberEmail(email);

        return orders.stream()
                .flatMap(order -> orderItemRepository.findByOrder(order).stream())
                .map(orderItem -> {
                    ProductItem productItem = orderItem.getProductItem();
                    Product product = productItem.getProduct();

                    String imgUrl = product.getImages().stream()
                            .findFirst()
                            .map(ProductImage::getImageUrl)
                            .orElse(null);

                    return new AskListItem(
                            orderItem.getOrderItemId(),
                            product.getProductName(),
                            productItem.getSize(),
                            imgUrl
                    );
                })
                .collect(Collectors.toList());
    }

    // 문의 작성
    @Transactional
    public void createAsk(String email, AskRequestDto dto, List<MultipartFile> images) {
        // 1. 회원 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 회원"));

        // 2. 주문 상품 조회
        OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 주문 항목"));

        // 3. 이미지 개수 제한
        if (images != null && images.size() > 5) {
            throw new IllegalArgumentException("이미지는 최대 5장까지만 업로드할 수 있습니다.");
        }

        // 4. Ask 엔티티 생성
        Ask ask = Ask.builder()
                .member(member)
                .orderItem(orderItem)
                .title(dto.getTitle())
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .answerStatus(AnswerStatus.WAITING)
                .build();

        // 5. 이미지 저장 및 AskImage 연관
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
                Path savePath = Paths.get("upload/ask").resolve(filename);

                try {
                    Files.createDirectories(savePath.getParent());
                    image.transferTo(savePath);

                    AskImage askImage = AskImage.builder()
                            .ask(ask)
                            .imageUrl("/upload/ask/" + filename)  // 실제 접근 경로
                            .build();

                    ask.getImages().add(askImage); // 연관관계 설정
                } catch (IOException e) {
                    throw new RuntimeException("이미지 저장 실패: " + filename, e);
                }
            }
        }

        // 6. 저장
        askRepository.save(ask);
    }

    public AskResponseDto getAskDetail(String email, Long askId) {
        Ask ask = askRepository.findById(askId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문의글입니다."));

        // 본인 문의글인지 검증
        if (!ask.getMember().getEmail().equals(email)) {
            throw new RuntimeException("본인의 문의글만 조회할 수 있습니다.");
        }

        String productName = ask.getOrderItem().getProductItem().getProduct().getProductName();
        String size = ask.getOrderItem().getProductItem().getSize().name(); // enum -> String

        List<String> imageUrls = ask.getImages().stream()
                .map(image -> image.getImageUrl())
                .collect(Collectors.toList());

        return new AskResponseDto(
                ask.getAskId(),
                ask.getOrderItem().getOrderItemId(),
                ask.getTitle(),
                productName,
                size,
                ask.getContent(),
                imageUrls,
                ask.getCreatedAt(),
                ask.getAnswerStatus(),
                ask.getAnswer(),
                ask.getAnsweredAt()
        );
    }

    @Transactional
    public void deleteAsk(String email, Long askId) {
        Ask ask = askRepository.findById(askId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 문의글입니다."));

        // 본인 확인
        if (!ask.getMember().getEmail().equals(email)) {
            throw new RuntimeException("본인의 문의글만 삭제할 수 있습니다.");
        }

        askRepository.delete(ask);
    }

}
