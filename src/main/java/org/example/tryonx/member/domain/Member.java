package org.example.tryonx.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.exchange.domain.Exchange;
import org.example.tryonx.image.domain.MemberClothesImage;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.member.dto.UpdateMemberRequestDto;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.returns.domain.Returns;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Table(name = "member")
@Entity
@Getter
@Setter
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

    @Enumerated(EnumType.STRING)
    private BodyShape bodyShape;

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
        if(request.getName() != null){
            this.name = request.getName();
        }
        if(request.getPhoneNumber() != null){
            this.phoneNumber = request.getPhoneNumber();
        }
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
        if(request.getBodyShape() != null){
            this.bodyShape = request.getBodyShape();
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
            throw new IllegalArgumentException("보유 적립금 부족");
        }
        this.point -= point;
    }
    public void savePoint(int point) {
        this.point += point;
    }

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
    private List<Order> orders;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
    private List<Exchange> exchanges;

    @OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
    private List<Returns> returns;

    @Column(nullable = true, unique = true, length = 20)
    private String memberNum;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberClothesImage> images;

}
