package org.example.tryonx.returns.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.tryonx.enums.ReturnStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;

import java.time.LocalDateTime;

@Entity
@Table(name = "returns")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Returns {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer returnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItemId;

    @Enumerated(EnumType.STRING)
    private ReturnStatus status;

    private String reason;

    @Column(name = "return_requested_at")
    private LocalDateTime returnRequestedAt;

    @Column(name = "return_approved_at")
    private LocalDateTime returnApprovedAt;

}
