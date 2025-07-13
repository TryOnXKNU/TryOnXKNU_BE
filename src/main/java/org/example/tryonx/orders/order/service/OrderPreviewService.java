package org.example.tryonx.orders.order.service;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.dto.MemberInfoDto;
import org.example.tryonx.orders.order.dto.OrderPreviewRequestDto;
import org.example.tryonx.orders.order.dto.OrderPreviewResponseDto;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPreviewService {
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final MemberRepository memberRepository;
    private final ProductImageRepository productImageRepository;

    public OrderPreviewResponseDto calculatePreview(String email, OrderPreviewRequestDto dto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보 없음"));

        List<OrderPreviewResponseDto.Item> itemList = dto.getItems().stream()
                .map(reqItem -> {
                    Product product = productRepository.findById(reqItem.getProductId()).orElseThrow();
                    ProductItem productItem = productItemRepository
                            .findByProductAndSize(product, reqItem.getSize())
                            .orElseThrow(() -> new EntityNotFoundException("상품 또는 사이즈 정보 오류"));

                    return new OrderPreviewResponseDto.Item(
                            productItem.getProduct().getProductName(),
                            product.getPrice(),
                            reqItem.getQuantity(),
                            productItem.getSize(),
                            product.getDiscountRate().toString() + "%",
                            productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl()
                    );
                })
                .toList();

        BigDecimal totalAmount = itemList.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = dto.getItems().stream()
                .map(reqItem -> {
                    Product product = productRepository.findById(reqItem.getProductId()).orElseThrow();
                    ProductItem productItem = productItemRepository
                            .findByProductAndSize(product, reqItem.getSize())
                            .orElseThrow();

                    BigDecimal discountRate = product.getDiscountRate(); // 예: 10%
                    BigDecimal itemDiscount = product.getPrice()
                            .multiply(discountRate.divide(BigDecimal.valueOf(100)))
                            .multiply(BigDecimal.valueOf(reqItem.getQuantity()));

                    return itemDiscount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        Integer expectedPoint = finalAmount.multiply(BigDecimal.valueOf(0.01))
                .setScale(0, BigDecimal.ROUND_DOWN)
                .intValue();

        return new OrderPreviewResponseDto(
                new MemberInfoDto(member.getName(),member.getPhoneNumber(),member.getAddress(), member.getPoint()),
                totalAmount,
                discountAmount,
                finalAmount,
                expectedPoint,
                itemList
        );
    }
}

