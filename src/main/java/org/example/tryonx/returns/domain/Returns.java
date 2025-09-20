package org.example.tryonx.returns.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.enums.ReturnStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.product.domain.Product;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private BigDecimal price;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReturnStatus status;

    private String reason;

    @Column(name = "return_requested_at", nullable = false, updatable = false)
    private LocalDateTime returnRequestedAt;

    private LocalDateTime updatedAt;

    @Column(name = "reject_reason")
    private String rejectReason;


}
