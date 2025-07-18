package org.example.tryonx.exchange.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.ExchangeStatus;
import org.example.tryonx.exchange.domain.Exchange;
import org.example.tryonx.exchange.dto.ExchangeRequestDto;
import org.example.tryonx.exchange.dto.ExchangeResponseDto;
import org.example.tryonx.exchange.repository.ExchangeRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

        Exchange exchange = Exchange.builder()
                .memberId(member)
                .orderId(order)
                .orderItemId(orderItem)
                .status(ExchangeStatus.REQUESTED)
                .reason(dto.getReason())
                .exchange_requestedAt(LocalDateTime.now())
                .build();

        exchangeRepository.save(exchange);
    }

    public List<ExchangeResponseDto> getMyExchanges(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        return exchangeRepository.findAllByMemberId(member).stream()
                .map(e -> new ExchangeResponseDto(
                        e.getExchangeId(),
                        e.getStatus().name(),
                        e.getReason(),
                        e.getExchange_requestedAt(),
                        e.getExchange_processedAt(),
                        member.getMemberId(),
                        e.getOrderId().getOrderId(),
                        e.getOrderItemId().getOrderItemId()
                ))
                .toList();
    }

    public ExchangeResponseDto getExchangeDetail(String email, Integer exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 내역이 존재하지 않습니다."));

        if (!exchange.getMemberId().getEmail().equals(email)) {
            throw new AccessDeniedException("본인의 교환 내역만 조회할 수 있습니다.");
        }

        return new ExchangeResponseDto(
                exchange.getExchangeId(),
                exchange.getStatus().name(),
                exchange.getReason(),
                exchange.getExchange_requestedAt(),
                exchange.getExchange_processedAt(),
                exchange.getMemberId().getMemberId(),
                exchange.getOrderId().getOrderId(),
                exchange.getOrderItemId().getOrderItemId()
        );
    }
}
