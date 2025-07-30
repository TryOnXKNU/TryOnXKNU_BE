package org.example.tryonx.product.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

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

    public void updateMeasurement(
            Double length, Double shoulder, Double chest,
            Double sleeveLength, Double waist, Double thigh,
            Double rise, Double hem, Double hip) {
        if (!Objects.equals(this.length, length)) this.length = length;
        if (!Objects.equals(this.shoulder, shoulder)) this.shoulder = shoulder;
        if (!Objects.equals(this.chest, chest)) this.chest = chest;
        if (!Objects.equals(this.sleeveLength, sleeveLength)) this.sleeveLength = sleeveLength;
        if (!Objects.equals(this.waist, waist)) this.waist = waist;
        if (!Objects.equals(this.thigh, thigh)) this.thigh = thigh;
        if (!Objects.equals(this.rise, rise)) this.rise = rise;
        if (!Objects.equals(this.hem, hem)) this.hem = hem;
        if (!Objects.equals(this.hip, hip)) this.hip = hip;
    }
}