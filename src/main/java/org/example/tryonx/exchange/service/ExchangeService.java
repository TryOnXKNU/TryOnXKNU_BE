package org.example.tryonx.exchange.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.ExchangeStatus;
import org.example.tryonx.exchange.domain.Exchange;
import org.example.tryonx.exchange.dto.ExchangeDetailDto;
import org.example.tryonx.exchange.dto.ExchangeListDto;
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

        Exchange exchange = Exchange.builder()
                .member(member)
                .order(order)
                .orderItem(orderItem)
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
                .map(e -> new ExchangeResponseDto(
                        e.getExchangeId(),
                        member.getMemberId(),
                        e.getOrder().getOrderId(),
                        e.getOrderItem().getOrderItemId(),
                        e.getReason(),
                        e.getExchange_requestedAt(),
                        e.getExchange_processedAt(),
                        e.getStatus().name(),
                        e.getPrice(),
                        e.getQuantity()
                ))
                .toList();
    }

    public ExchangeResponseDto getExchangeDetail(String email, Integer exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 내역이 존재하지 않습니다."));

        if (!exchange.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("본인의 교환 내역만 조회할 수 있습니다.");
        }

        return new ExchangeResponseDto(
                exchange.getExchangeId(),
                exchange.getMember().getMemberId(),
                exchange.getOrder().getOrderId(),
                exchange.getOrderItem().getOrderItemId(),
                exchange.getReason(),
                exchange.getExchange_requestedAt(),
                exchange.getExchange_processedAt(),
                exchange.getStatus().name(),
                exchange.getPrice(),
                exchange.getQuantity()
        );
    }

    /* 교환 전체 */
    public List<ExchangeListDto> getExchangeList(){
        return exchangeRepository.findAll().stream()
                .map(exchange -> new ExchangeListDto(
                        exchange.getExchangeId(),
                        exchange.getMember().getMemberId(),
                        exchange.getOrder().getOrderId(),
                        exchange.getOrderItem().getOrderItemId(),
                        exchange.getExchange_requestedAt(),
                        exchange.getStatus().name(),
                        exchange.getPrice(),
                        exchange.getQuantity()
                )).collect(Collectors.toList());
    }

    /* 교환 상세정보 */
    public ExchangeDetailDto findByExchangeId(Integer exchangeId) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new EntityNotFoundException("교환 내역을 찾을 수 없습니다."));

        return new ExchangeDetailDto(
                exchange.getExchangeId(),
                exchange.getMember().getMemberId(),
                exchange.getOrder().getOrderId(),
                exchange.getOrderItem().getOrderItemId(),
                exchange.getPrice(),
                exchange.getQuantity(),
                exchange.getReason(),
                exchange.getExchange_requestedAt(),
                exchange.getExchange_processedAt(),
                exchange.getStatus().name()
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

        if (status == ExchangeStatus.ACCEPTED) {
            exchange.setExchange_processedAt(LocalDateTime.now());
        }

        if (status == ExchangeStatus.REJECTED) {
            exchange.setRejectReason(reason);
        }

        exchangeRepository.save(exchange);
    }

}
