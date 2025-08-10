package org.example.tryonx.orders.payment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.enums.PaymentStatus;
import org.example.tryonx.orders.order.domain.Order;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_merchant_uid", columnNames = "merchant_uid"),
                @UniqueConstraint(name = "uk_payments_imp_uid", columnNames = "imp_uid")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // BIGINT PK

    // 주문은 나중에 연결될 수 있으므로 nullable
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order; // FK: INT

    @Column(name = "merchant_uid", nullable = false, length = 100)
    private String merchantUid;

    @Column(name = "imp_uid", length = 100)
    private String impUid; // 결제 전에는 null 가능

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false)
    private Integer amount; // KRW 정수

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // DB가 DEFAULT/ON UPDATE로 관리하므로 JPA는 값을 넣지 않게 설정
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
