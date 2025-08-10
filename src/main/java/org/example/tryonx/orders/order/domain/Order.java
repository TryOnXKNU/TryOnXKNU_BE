package org.example.tryonx.orders.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.orders.payment.domain.PaymentMethod;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Builder.Default
    @Column(nullable = false)
    private Integer usedPoints = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    @Setter
    private OrderStatus status = OrderStatus.PENDING;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime orderedAt = LocalDateTime.now();

    // 결제수단(프론트의 method를 그대로 저장해두면 분석/정산 편함)
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentMethod paymentMethod;

    // 배송 요청사항(프론트 selectedRequest)
    @Column(length = 255)
    private String deliveryRequest;

    @Setter
    @Column(name = "order_num", unique = true, length = 20)
    private String orderNum;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter
    private List<OrderItem> orderItems;

    // 멱등/동시성 방어
    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (orderedAt == null) orderedAt = LocalDateTime.now();
        if (orderNum == null) {
            // 길이 20 맞춤: ORD + yyyyMMddHHmmssSSS(3 + 17 = 20)
            orderNum = "ORD" + DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
        }
    }
}
