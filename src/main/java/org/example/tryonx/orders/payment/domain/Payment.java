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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order;

    @Column(name = "merchant_uid", nullable = false, length = 100)
    private String merchantUid;

    @Column(name = "imp_uid", length = 100)
    private String impUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ====== 추가 메타: 결제수단/PG/카드/은행/영수증 ======
    @Column(name = "pay_method", length = 30)         // 예: card, trans, vbank, kakaopay 등
    private String payMethod;

    @Column(name = "pg_provider", length = 50)        // 예: kcp, inicis, tosspayments 등
    private String pgProvider;

    @Column(name = "card_name", length = 50)          // 예: KB국민카드
    private String cardName;

    @Column(name = "card_code", length = 20)          // 카드사 코드
    private String cardCode;

    @Column(name = "card_number_masked", length = 64) // 마스킹된 카드번호
    private String cardNumberMasked;

    @Column(name = "card_quota")                      // 할부개월(0=일시불)
    private Integer cardQuota;

    @Column(name = "card_type")                       // 0=신용, 1=체크 (SDK 기준)
    private Integer cardType;

    @Column(name = "bank_name", length = 50)          // 계좌이체/가상계좌 은행명
    private String bankName;

    @Column(name = "bank_code", length = 20)          // 은행 코드
    private String bankCode;

    @Column(name = "receipt_url", length = 255)       // 영수증 URL
    private String receiptUrl;

    // 간편결제(예: kakaopay, naverpay 등) 식별이 필요한 경우
    @Column(name = "easy_pay_provider", length = 50)  // 예: kakaopay
    private String easyPayProvider;

    // DB가 DEFAULT/ON UPDATE로 관리
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

}
