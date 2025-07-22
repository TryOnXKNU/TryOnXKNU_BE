package org.example.tryonx.returns.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.ReturnStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.returns.domain.Returns;
import org.example.tryonx.returns.dto.ReturnRequestDto;
import org.example.tryonx.returns.dto.ReturnResponseDto;
import org.example.tryonx.returns.repository.ReturnRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

        Returns returns = Returns.builder()
                .member(member)
                .order(order)
                .orderItem(orderItem)
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
                .map(returns -> new ReturnResponseDto(
                        returns.getReturnId(),
                        member.getMemberId(),
                        returns.getOrder().getOrderId(),
                        returns.getOrderItem().getOrderItemId(),
                        returns.getPrice(),
                        returns.getQuantity(),
                        returns.getReason(),
                        returns.getStatus().name(),
                        returns.getReturnRequestedAt(),
                        returns.getReturnApprovedAt()
                ))
                .toList();
    }

    public ReturnResponseDto getReturnDetail(String email, Integer returnId) {
        Returns returns = returnRepository.findById(returnId)
                .orElseThrow(() -> new EntityNotFoundException("반품 내역이 존재하지 않습니다."));

        if (!returns.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("본인의 반품 내역만 조회할 수 있습니다.");
        }

        return new ReturnResponseDto(
                returns.getReturnId(),
                returns.getMember().getMemberId(),
                returns.getOrder().getOrderId(),
                returns.getOrderItem().getOrderItemId(),
                returns.getPrice(),
                returns.getQuantity(),
                returns.getReason(),
                returns.getStatus().name(),
                returns.getReturnRequestedAt(),
                returns.getReturnApprovedAt()
        );
    }
}
