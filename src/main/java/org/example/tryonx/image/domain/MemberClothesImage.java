package org.example.tryonx.image.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.tryonx.member.domain.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_clothes_images")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberClothesImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @JsonIgnore
    private Member member;

    @Column(nullable = false)
    private Integer categoryId;

    @Column(nullable = false, length = 255)
    private String imageUrl;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
