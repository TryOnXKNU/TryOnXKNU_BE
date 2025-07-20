package org.example.tryonx.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.member.dto.UpdateMemberRequestDto;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Setter
    private String profileUrl;

    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String phoneNumber;

    private Integer height;

    private Integer weight;

    private Integer bodyType;

    private LocalDate birthDate;

    private String address;

    private String socialType;

    private Long socialId;

    @Column(name = "points")
    private Integer point;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void update(UpdateMemberRequestDto request){
        if(request.getNickname() != null){
            this.nickname = request.getNickname();
        }
        if(request.getAddress() != null){
            this.address = request.getAddress();
        }
        if(request.getNewPassword() != null){
            this.updatePassword(request.getNewPassword());
        }
        if(request.getHeight() != null){
            this.height = request.getHeight();
        }
        if(request.getWeight() != null){
            this.weight = request.getWeight();
        }
        if(request.getBodyType() != null){
            this.bodyType = request.getBodyType();
        }
    }

    public void updatePassword(String encodedPassword) {
        if (encodedPassword == null || encodedPassword.length() < 10) {
            throw new IllegalArgumentException("비밀번호 형식이 잘못되었습니다.");
        }
        this.password = encodedPassword;
    }

    public void usePoint(int point) {
        if (this.point < point) {
            throw new IllegalArgumentException("보유 포인트 부족");
        }
        this.point -= point;
    }
    public void savePoint(int point) {
        this.point += point;
    }
}
