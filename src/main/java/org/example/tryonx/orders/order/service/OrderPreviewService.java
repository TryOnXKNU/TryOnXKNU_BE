package org.example.tryonx.orders.order.service;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.cart.domain.CartItem;
import org.example.tryonx.cart.repository.CartItemRepository;
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
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPreviewService {
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final MemberRepository memberRepository;
    private final ProductImageRepository productImageRepository;
    private final CartItemRepository cartItemRepository;

    public OrderPreviewResponseDto calculatePreview(String email, OrderPreviewRequestDto dto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보 없음"));

        List<OrderPreviewResponseDto.Item> itemList = dto.getItems().stream()
                .map(reqItem -> {
                    Product product = productRepository.findById(reqItem.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException("상품 없음"));

                    ProductItem productItem = productItemRepository
                            .findByProductAndSize(product, reqItem.getSize())
                            .orElseThrow(() -> new EntityNotFoundException("사이즈 정보 없음"));

                    // 장바구니 기반일 경우 유효성 검증
                    if (reqItem.getCartItemId() != null) {
                        CartItem cartItem = cartItemRepository.findById(reqItem.getCartItemId())
                                .orElseThrow(() -> new EntityNotFoundException("장바구니 항목 없음"));

                        if (!cartItem.getMember().equals(member)) {
                            throw new IllegalArgumentException("해당 장바구니 항목은 본인의 것이 아닙니다.");
                        }

                        if (!cartItem.getProductItem().equals(productItem)) {
                            throw new IllegalArgumentException("장바구니 항목의 상품/사이즈 정보가 일치하지 않습니다.");
                        }

                        if (!cartItem.getQuantity().equals(reqItem.getQuantity())) {
                            throw new IllegalArgumentException("장바구니 항목의 수량과 요청 수량이 다릅니다.");
                        }
                    }

                    String imageUrl = productImageRepository
                            .findByProductAndIsThumbnailTrue(product)
                            .map(img -> img.getImageUrl())
                            .orElse(null);

                    return new OrderPreviewResponseDto.Item(
                            product.getProductName(),
                            product.getPrice(),
                            reqItem.getQuantity(),
                            productItem.getSize(),
                            product.getDiscountRate().toPlainString() + "%",
                            imageUrl,
                            reqItem.getCartItemId()
                    );
                })

                .toList();

        BigDecimal totalAmount = itemList.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = dto.getItems().stream()
                .map(reqItem -> {
                    Product product = productRepository.findById(reqItem.getProductId())
                            .orElseThrow(() -> new EntityNotFoundException("상품 없음"));

                    BigDecimal discountRate = product.getDiscountRate();

                    return product.getPrice()
                            .multiply(discountRate.divide(BigDecimal.valueOf(100)))
                            .multiply(BigDecimal.valueOf(reqItem.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        Integer expectedPoint = finalAmount.multiply(BigDecimal.valueOf(0.01))
                .setScale(0, BigDecimal.ROUND_DOWN)
                .intValue();

        return new OrderPreviewResponseDto(
                new MemberInfoDto(member.getName(), member.getPhoneNumber(), member.getAddress(), member.getPoint()),
                totalAmount,
                totalAmount.compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : discountAmount.multiply(BigDecimal.valueOf(100)).divide(totalAmount, 2, RoundingMode.HALF_UP),
                discountAmount,
                finalAmount,
                expectedPoint,
                itemList
        );
    }

}

