package org.example.tryonx.member.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_history")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Integer amount;  // +적립, -사용

    @Column(nullable = false, length = 100)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static PointHistory earn(Member member, int amount, String description) {
        return PointHistory.builder()
                .member(member)
                .amount(amount)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static PointHistory use(Member member, int amount, String description) {
        return PointHistory.builder()
                .member(member)
                .amount(-amount)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
