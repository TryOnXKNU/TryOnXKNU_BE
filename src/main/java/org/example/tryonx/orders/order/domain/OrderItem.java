package org.example.tryonx.orders.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.ask.domain.Ask;
import org.example.tryonx.enums.AfterServiceStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.product.domain.ProductItem;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderItemId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_item_id")
    private ProductItem productItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column
    private Integer usedPoints = 0;

    @Enumerated(EnumType.STRING)
    private AfterServiceStatus afterServiceStatus = AfterServiceStatus.NONE;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Ask> asks;
}
