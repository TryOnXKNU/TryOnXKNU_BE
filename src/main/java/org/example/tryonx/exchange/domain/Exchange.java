package org.example.tryonx.exchange.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.enums.ExchangeStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exchange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer exchangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    private BigDecimal price;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private ExchangeStatus status; // REQUESTED, APPROVED, COMPLETED

    private String reason;

    private LocalDateTime exchange_requestedAt;

    private LocalDateTime exchange_processedAt;
}
