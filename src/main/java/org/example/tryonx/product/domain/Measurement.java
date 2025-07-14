package org.example.tryonx.product.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "measurements",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "product_item_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Measurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "measurement_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) // 한 product_item 당 하나의 measurement
    @JoinColumn(name = "product_item_id", nullable = false)
    private ProductItem productItem;

    @Column
    private Double length;

    @Column
    private Double shoulder;

    @Column
    private Double chest;

    @Column
    private Double sleeveLength;

    @Column
    private Double waist;

    @Column
    private Double thigh;

    @Column
    private Double rise;

    @Column
    private Double hem;

    @Column
    private Double hip;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}