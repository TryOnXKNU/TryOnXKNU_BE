package org.example.tryonx.exchange.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.tryonx.enums.Size;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.AfterServiceStatus;
import org.example.tryonx.enums.ExchangeStatus;
import org.example.tryonx.exchange.domain.Exchange;
import org.example.tryonx.exchange.dto.ExchangeDetailDto;
import org.example.tryonx.exchange.dto.ExchangeListDto;
import org.example.tryonx.exchange.dto.ExchangeRequestDto;
import org.example.tryonx.exchange.dto.ExchangeResponseDto;
import org.example.tryonx.exchange.repository.ExchangeRepository;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeService {
    private final ExchangeRepository exchangeRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public void requestExchange(String email, ExchangeRequestDto dto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원 없음"));

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("주문 없음"));

        OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new EntityNotFoundException("주문 아이템 없음"));

        // 상태 체크
        if (orderItem.getAfterServiceStatus() == AfterServiceStatus.RETURN) {
            throw new IllegalStateException("이미 반품이 신청된 상품입니다. 교환 신청이 불가합니다.");
        }
        if (orderItem.getAfterServiceStatus() == AfterServiceStatus.EXCHANGE) {
            throw new IllegalStateException("이미 교환이 신청된 상품입니다.");
        }

        // 상태 변경
        orderItem.setAfterServiceStatus(AfterServiceStatus.EXCHANGE);
        orderItemRepository.save(orderItem);

        Product product = orderItem.getProductItem().getProduct();

        Exchange exchange = Exchange.builder()
                .member(member)
                .order(order)
                .orderItem(orderItem)
                .product(product)
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .status(ExchangeStatus.REQUESTED)
                .reason(dto.getReason())
                .exchange_requestedAt(LocalDateTime.now())
                .build();

        exchangeRepository.save(exchange);
    }

    public List<ExchangeResponseDto> getMyExchanges(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        return exchangeRepository.findAllByMember(member).stream()
                .map(e -> {
                    String productName = e.getProduct() != null ? e.getProduct().getProductName() : null;
                    String imageUrl = (e.getProduct() != null && !e.getProduct().getImages().isEmpty())
                            ? e.getProduct().getImages().get(0).getImageUrl()
                            : null;

                    OrderItem orderItem = e.getOrderItem();
                    Size purchasedSize = (orderItem != null && orderItem.getProductItem() != null)
                            ? orderItem.getProductItem().getSize()
                            : null;

                    return new ExchangeResponseDto(
                            e.getExchangeId(),
                            member.getMemberId(),
                            e.getOrder().getOrderId(),
                            e.getOrderItem().getOrderItemId(),
                            e.getReason(),
                            e.getExchange_requestedAt(),
                            e.getUpdatedAt(),
                            e.getStatus().name(),
                            e.getPrice(),
                            e.getQuantity(),
                            purchasedSize,
                            productName,
                            imageUrl
                    );
                })
                .toList();
    }

    public ExchangeResponseDto getExchangeDetail(String email, Integer exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 내역이 존재하지 않습니다."));

        if (!exchange.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("본인의 교환 내역만 조회할 수 있습니다.");
        }

        Product product = exchange.getProduct();
        OrderItem orderItem = exchange.getOrderItem();

        String productName = product != null ? product.getProductName() : null;
        String imageUrl = (product != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getImageUrl()
                : null;

        Size purchasedSize = (orderItem != null && orderItem.getProductItem() != null)
                ? orderItem.getProductItem().getSize()
                : null;

        return new ExchangeResponseDto(
                exchange.getExchangeId(),
                exchange.getMember().getMemberId(),
                exchange.getOrder().getOrderId(),
                exchange.getOrderItem().getOrderItemId(),
                exchange.getReason(),
                exchange.getExchange_requestedAt(),
                exchange.getUpdatedAt(),
                exchange.getStatus().name(),
                exchange.getPrice(),
                exchange.getQuantity(),
                purchasedSize,
                productName,
                imageUrl
        );
    }

    /* 교환 전체 */
    public List<ExchangeListDto> getExchangeList(){
        return exchangeRepository.findAll().stream()
                .map(exchange -> {
                    String productName = exchange.getProduct() != null ? exchange.getProduct().getProductName() : null;
                    String imageUrl = (exchange.getProduct() != null && !exchange.getProduct().getImages().isEmpty())
                            ? exchange.getProduct().getImages().get(0).getImageUrl()
                            : null;

                    return new ExchangeListDto(
                            exchange.getExchangeId(),
                            exchange.getMember().getMemberId(),
                            exchange.getOrder().getOrderId(),
                            exchange.getOrderItem().getOrderItemId(),
                            exchange.getOrder().getOrderNum(),
                            exchange.getExchange_requestedAt(),
                            exchange.getStatus().name(),
                            exchange.getPrice(),
                            exchange.getQuantity(),
                            productName,
                            imageUrl
                    );
                })
                .collect(Collectors.toList());
    }

    //교환 상세조회 (사용자 본인만)
    public ExchangeDetailDto findByExchangeId(String email, Integer exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 내역을 찾을 수 없습니다."));

        // 본인만 조회 가능
        if (!exchange.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("본인의 교환 내역만 조회할 수 있습니다.");
        }

        Product product = exchange.getProduct();
        OrderItem orderItem = exchange.getOrderItem();

        String productName = (product != null) ? product.getProductName() : null;
        String imageUrl = (product != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getImageUrl()
                : null;

        Size purchasedSize = (orderItem != null && orderItem.getProductItem() != null)
                ? orderItem.getProductItem().getSize()
                : null;

        BigDecimal discount = product.getPrice().multiply(product.getDiscountRate().divide(BigDecimal.valueOf(100)));
        // 상태가 REJECTED일 때만 반려 사유 세팅
        String rejectReason = exchange.getStatus() == ExchangeStatus.REJECTED
                ? exchange.getRejectReason()
                : null;

        return new ExchangeDetailDto(
                exchange.getExchangeId(),
                exchange.getMember().getMemberId(),
                exchange.getOrder().getOrderId(),
                exchange.getOrderItem().getOrderItemId(),
                exchange.getOrder().getOrderNum(),
                exchange.getPrice(),
                purchasedSize,
                exchange.getQuantity(),
                exchange.getReason(),
                exchange.getExchange_requestedAt(),
                exchange.getUpdatedAt(),
                exchange.getStatus().name(),
                productName,
                imageUrl,
                rejectReason,
                product.getDiscountRate(),
                product.getPrice().min(discount)
        );
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
    
    //교환 상세조회 (관리자용)
    public ExchangeDetailDto findByExchangeIdForAdmin(Integer exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 내역을 찾을 수 없습니다."));

        Product product = exchange.getProduct();
        OrderItem orderItem = exchange.getOrderItem();

        Size purchasedSize = (orderItem != null && orderItem.getProductItem() != null)
                ? orderItem.getProductItem().getSize()
                : null;

        String productName = (product != null) ? product.getProductName() : null;
        String imageUrl = (product != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getImageUrl()
                : null;
        BigDecimal discount = product.getPrice().multiply(product.getDiscountRate().divide(BigDecimal.valueOf(100)));
        String rejectReason = exchange.getStatus() == ExchangeStatus.REJECTED
                ? exchange.getRejectReason()
                : null;

        // 주문 시점 스냅샷 값 사용
        BigDecimal unitPrice = nz(orderItem.getPrice());       // 주문 당시 단가
        BigDecimal rate = nz(orderItem.getDiscountRate());     // 주문 당시 할인율(%)
        BigDecimal qty = BigDecimal.valueOf(exchange.getQuantity());

        // 최종 단가 = 단가 × (1 - rate/100)
        BigDecimal finalUnitPrice = unitPrice.multiply(
                BigDecimal.ONE.subtract(
                        rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                )
        ).setScale(0, RoundingMode.DOWN);

        // 교환 수량 기준 최종 금액
        BigDecimal discountedFinal = finalUnitPrice.multiply(qty).setScale(0, RoundingMode.DOWN);

        return new ExchangeDetailDto(
                exchange.getExchangeId(),
                exchange.getMember().getMemberId(),
                exchange.getOrder().getOrderId(),
                exchange.getOrderItem().getOrderItemId(),
                exchange.getOrder().getOrderNum(),
                unitPrice,
                purchasedSize,
                exchange.getQuantity(),
                exchange.getReason(),
                exchange.getExchange_requestedAt(),
                exchange.getUpdatedAt(),
                exchange.getStatus().name(),
                productName,
                imageUrl,
                rejectReason,
                rate,
                discountedFinal
        );
    }


    /* 교환 상태 변경 */
    @Transactional
    public void updateExchangeStatus(Integer exchangeId, ExchangeStatus status, String reason) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 내역이 존재하지 않습니다."));

        if (status == ExchangeStatus.REJECTED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("REJECTED 상태일 때는 반려 사유가 필요합니다.");
        }

        exchange.setStatus(status);
        exchange.setUpdatedAt(LocalDateTime.now());

        if (status == ExchangeStatus.REJECTED) {
            exchange.setRejectReason(reason);
            exchange.getOrderItem().setAfterServiceStatus(AfterServiceStatus.NONE);
            orderItemRepository.save(exchange.getOrderItem());
        }

        exchangeRepository.save(exchange);
    }

    public List<ExchangeListDto> getExchangesByStatus(ExchangeStatus status) {
        return exchangeRepository.findAllByStatus(status).stream()
                .map(exchange -> {
                    Product product = exchange.getProduct();

                    return new ExchangeListDto(
                            exchange.getExchangeId(),
                            exchange.getMember().getMemberId(),
                            exchange.getOrder().getOrderId(),
                            exchange.getOrderItem().getOrderItemId(),
                            exchange.getOrder().getOrderNum(),
                            exchange.getExchange_requestedAt(),
                            exchange.getStatus().name(),
                            exchange.getPrice(),
                            exchange.getQuantity(),
                            product.getProductName(),
                            product.getImages().stream()
                                    .findFirst()
                                    .map(ProductImage::getImageUrl)
                                    .orElse(null)
                    );
                })
                .collect(Collectors.toList());
    }

    public long countAllExchanges() {
        return exchangeRepository.count();
    }

}
