package org.example.tryonx.returns.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.enums.ReturnStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Returns {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer returnId;

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
    @Column(length = 20)
    private ReturnStatus status;

    private String reason;

    @Column(name = "return_requested_at")
    private LocalDateTime returnRequestedAt;

    @Column(name = "return_approved_at")
    private LocalDateTime returnApprovedAt;

}
