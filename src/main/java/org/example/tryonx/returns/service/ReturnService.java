package org.example.tryonx.returns.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.AfterServiceStatus;
import org.example.tryonx.enums.ReturnStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.returns.domain.Returns;
import org.example.tryonx.returns.dto.ReturnDetailDto;
import org.example.tryonx.returns.dto.ReturnListDto;
import org.example.tryonx.returns.dto.ReturnRequestDto;
import org.example.tryonx.returns.dto.ReturnResponseDto;
import org.example.tryonx.returns.repository.ReturnRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {
    private final ReturnRepository returnRepository;
    private final MemberRepository memberRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void requestReturn(String email, ReturnRequestDto dto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원 없음"));

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new EntityNotFoundException("주문 없음"));

        OrderItem orderItem = orderItemRepository.findById(dto.getOrderItemId())
                .orElseThrow(() -> new EntityNotFoundException("주문 아이템 없음"));

        // 상태 체크
        if (orderItem.getAfterServiceStatus() == AfterServiceStatus.EXCHANGE) {
            throw new IllegalStateException("이미 교환이 신청된 상품입니다. 반품 신청이 불가합니다.");
        }
        if (orderItem.getAfterServiceStatus() == AfterServiceStatus.RETURN) {
            throw new IllegalStateException("이미 반품이 신청된 상품입니다.");
        }

        // 상태 변경
        orderItem.setAfterServiceStatus(AfterServiceStatus.RETURN);
        orderItemRepository.save(orderItem);

        Product product = orderItem.getProductItem().getProduct();

        Returns returns = Returns.builder()
                .member(member)
                .order(order)
                .orderItem(orderItem)
                .product(product)
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .status(ReturnStatus.REQUESTED)
                .reason(dto.getReason())
                .returnRequestedAt(LocalDateTime.now())
                .build();

        returnRepository.save(returns);
    }

    public List<ReturnResponseDto> getMyReturns(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        return returnRepository.findAllByMember(member).stream()
                .map(returns -> {
                    Product product = returns.getProduct();
                    String productName = product != null ? product.getProductName() : null;
                    String imageUrl = (product != null && !product.getImages().isEmpty())
                            ? product.getImages().get(0).getImageUrl()
                            : null;

                    return new ReturnResponseDto(
                            returns.getReturnId(),
                            member.getMemberId(),
                            returns.getOrder().getOrderId(),
                            returns.getOrderItem().getOrderItemId(),
                            returns.getPrice(),
                            returns.getQuantity(),
                            returns.getReason(),
                            returns.getStatus().name(),
                            returns.getReturnRequestedAt(),
                            returns.getReturnApprovedAt(),
                            productName,
                            imageUrl
                    );
                })
                .toList();
    }

    public ReturnDetailDto findByReturnIdForAdmin(Integer returnId) {
        Returns returns = returnRepository.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("반품 내역을 찾을 수 없습니다."));

        Product product = returns.getProduct();
        String productName = product != null ? product.getProductName() : null;
        String imageUrl = (product != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getImageUrl()
                : null;

        String rejectReason = returns.getStatus() == ReturnStatus.REJECTED
                ? returns.getRejectReason()
                : null;
        BigDecimal discount = product.getPrice().multiply(product.getDiscountRate().divide(BigDecimal.valueOf(100)));

        return new ReturnDetailDto(
                returns.getReturnId(),
                returns.getMember().getMemberId(),
                returns.getOrder().getOrderId(),
                returns.getOrderItem().getOrderItemId(),
                returns.getOrder().getOrderNum(),
                returns.getPrice(),
                returns.getQuantity(),
                returns.getReason(),
                returns.getReturnRequestedAt(),
                returns.getReturnApprovedAt(),
                returns.getStatus().name(),
                productName,
                imageUrl,
                rejectReason,
                product.getDiscountRate(),
                product.getPrice().subtract(discount)
        );
    }

    /* 반품 전체 */
    public List<ReturnListDto> getReturnList() {
        return returnRepository.findAll().stream()
                .map(returns -> {
                    Product product = returns.getProduct();
                    String productName = product != null ? product.getProductName() : null;
                    String imageUrl = (product != null && !product.getImages().isEmpty())
                            ? product.getImages().get(0).getImageUrl()
                            : null;

                    return new ReturnListDto(
                            returns.getReturnId(),
                            returns.getMember().getMemberId(),
                            returns.getOrder().getOrderId(),
                            returns.getOrderItem().getOrderItemId(),
                            returns.getOrder().getOrderNum(),
                            returns.getReturnRequestedAt(),
                            returns.getStatus().name(),
                            returns.getPrice(),
                            returns.getQuantity(),
                            productName,
                            imageUrl
                    );
                }).collect(Collectors.toList());
    }

    /* 반품 상세정보 */
    public ReturnDetailDto findByReturnId(String email, Integer returnId) {
        Returns returns = returnRepository.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("반품 내역을 찾을 수 없습니다."));

        // 본인만 조회 가능
        if (!returns.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("본인의 반품 내역만 조회할 수 있습니다.");
        }

        Product product = returns.getProduct();
        String productName = product != null ? product.getProductName() : null;
        String imageUrl = (product != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getImageUrl()
                : null;

        // 상태가 REJECTED일 때만 반려 사유 세팅
        String rejectReason = returns.getStatus() == ReturnStatus.REJECTED
                ? returns.getRejectReason()
                : null;
        BigDecimal discount = product.getPrice().multiply(product.getDiscountRate().divide(BigDecimal.valueOf(100)));
        return new ReturnDetailDto(
                returns.getReturnId(),
                returns.getMember().getMemberId(),
                returns.getOrder().getOrderId(),
                returns.getOrderItem().getOrderItemId(),
                returns.getOrder().getOrderNum(),
                returns.getPrice(),
                returns.getQuantity(),
                returns.getReason(),
                returns.getReturnRequestedAt(),
                returns.getReturnApprovedAt(),
                returns.getStatus().name(),
                productName,
                imageUrl,
                rejectReason,
                product.getDiscountRate(),
                product.getPrice().subtract(discount)
        );
    }

    /* 반품 상태 변경 */
    @Transactional
    public void updateReturnStatus(Integer returnId, ReturnStatus status, String reason) {
        Returns returns = returnRepository.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("반품 내역이 존재하지 않습니다."));

        if (status == ReturnStatus.REJECTED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("REJECTED 상태일 때는 반려 사유가 필요합니다.");
        }

        returns.setStatus(status);

        if (status == ReturnStatus.ACCEPTED) {
            returns.setReturnApprovedAt(LocalDateTime.now());
            returns.setRejectReason(null); // 반려 사유 초기화
        } else if (status == ReturnStatus.REJECTED) {
            returns.setRejectReason(reason);
            returns.getOrderItem().setAfterServiceStatus(AfterServiceStatus.NONE);
            orderItemRepository.save(returns.getOrderItem());
            returns.setReturnApprovedAt(null); // 승인일시 제거
        } else {
            // 다른 상태일 경우 초기화
            returns.setRejectReason(null);
            returns.setReturnApprovedAt(null);
        }

        returnRepository.save(returns);
    }

    public List<ReturnListDto> getReturnsByStatus(ReturnStatus status) {
        return returnRepository.findAllByStatus(status).stream()
                .map(ret -> {
                    Product product = ret.getProduct();
                    String productName = product != null ? product.getProductName() : null;
                    String imageUrl = (product != null && !product.getImages().isEmpty())
                            ? product.getImages().get(0).getImageUrl()
                            : null;

                    return new ReturnListDto(
                            ret.getReturnId(),
                            ret.getMember().getMemberId(),
                            ret.getOrder().getOrderId(),
                            ret.getOrderItem().getOrderItemId(),
                            ret.getOrder().getOrderNum(),
                            ret.getReturnRequestedAt(),
                            ret.getStatus().name(),
                            ret.getPrice(),
                            ret.getQuantity(),
                            productName,
                            imageUrl
                    );
                })
                .toList();
    }

    public long countAllReturns() {
        return returnRepository.count();
    }

}
