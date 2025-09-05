package org.example.tryonx.member.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.tryonx.admin.dto.MemberInfoDto;
import org.example.tryonx.ask.domain.Ask;
import org.example.tryonx.ask.repository.AskRepository;
import org.example.tryonx.cart.repository.CartItemRepository;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.exchange.repository.ExchangeRepository;
import org.example.tryonx.fitting.dto.BodyShapeRequest;
import org.example.tryonx.image.repository.ReviewImageRepository;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.Role;
import org.example.tryonx.member.dto.MemberListResponseDto;
import org.example.tryonx.member.dto.MyInfoResponseDto;
import org.example.tryonx.member.dto.UpdateBodyInfoDto;
import org.example.tryonx.member.dto.UpdateMemberRequestDto;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.returns.repository.ReturnRepository;
import org.example.tryonx.review.domain.Review;
import org.example.tryonx.review.repository.ReviewRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final CartItemRepository cartItemRepository;
    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final AskRepository askRepository;
    private final ExchangeRepository exchangeRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRepository returnRepository;
    private final OrderRepository orderRepository;
    private static final long EXPIRE_TIME = 3 * 60;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, StringRedisTemplate redisTemplate, CartItemRepository cartItemRepository, LikeRepository likeRepository, ReviewRepository reviewRepository, ReviewImageRepository reviewImageRepository, AskRepository askRepository, ExchangeRepository exchangeRepository, OrderItemRepository orderItemRepository, ReturnRepository returnRepository, OrderRepository orderRepository) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.cartItemRepository = cartItemRepository;
        this.likeRepository = likeRepository;
        this.reviewRepository = reviewRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.askRepository = askRepository;
        this.exchangeRepository = exchangeRepository;
        this.orderItemRepository = orderItemRepository;
        this.returnRepository = returnRepository;
        this.orderRepository = orderRepository;
    }

    public Member getMember(String email){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Member not found"));
        return member;
    }

    public List<MemberListResponseDto>  findAll() {
        List<Member> members = memberRepository.findAll();
        List<MemberListResponseDto> memberListResDtos = new ArrayList<>();
        for(Member member : members){
            MemberListResponseDto memberListResDto = new MemberListResponseDto();
            memberListResDto.setName(member.getName());
            memberListResDto.setEmail(member.getEmail());
            memberListResDto.setPhone(member.getPhoneNumber());
            memberListResDtos.add(memberListResDto);
        }
        return memberListResDtos;
    }

    public MemberInfoDto findById(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원이 존재하지 않습니다."));

        return MemberInfoDto.builder()
                .profileUrl(member.getProfileUrl())
                .name(member.getName())
                .memberId(member.getMemberId())
                .nickName(member.getNickname())
                .phoneNumber(member.getPhoneNumber())
                .birthday(member.getBirthDate())
                .address(member.getAddress())
                .email(member.getEmail())
                .bodyShape(member.getBodyShape())
                .height(member.getHeight())
                .weight(member.getWeight())
                .memberNum(member.getMemberNum())
                .build();
    }

    public MyInfoResponseDto getMyInfo(String email){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        MyInfoResponseDto myInfoResponseDto = new MyInfoResponseDto(
                member.getName(),
                member.getNickname(),
                member.getProfileUrl(),
                member.getPhoneNumber(),
                member.getBirthDate(),
                member.getAddress(),
                member.getEmail(),
                member.getBodyShape(),
                member.getHeight(),
                member.getWeight(),
                member.getPoint()
        );
        return myInfoResponseDto;
    }

    public boolean isNicknameExist(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

    public boolean checkPassword(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(passwordEncoder.matches(password, member.getPassword())) {
            redisTemplate.opsForValue().set(email, "OK", EXPIRE_TIME, TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    public void updateNickname(String email, String nickname) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("중복된 닉네임입니다.");
        }else {
            member.setNickname(nickname);
            memberRepository.save(member);
        }
    }

    @Transactional
    public void updateAddress(String email, String address) {
        if (!StringUtils.hasText(address)) {
            throw new IllegalArgumentException("주소는 비어 있을 수 없습니다.");
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));

        member.setAddress(address);
        memberRepository.save(member);
    }

    @Transactional
    public void updateBodyInformation(String email, UpdateBodyInfoDto updateBodyInfoDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        BodyShape bodyShape = updateBodyInfoDto.getBodyShape();
        Integer height = updateBodyInfoDto.getHeight();
        Integer weight = updateBodyInfoDto.getWeight();
        if(bodyShape != null)
            member.setBodyShape(bodyShape);
        if(height != null)
            member.setHeight(height);
        if (weight != null)
            member.setWeight(weight);
        if (bodyShape == null && height == null && weight == null) {
            return;
        }
        memberRepository.save(member);
    }


    @Transactional
    public void updatePassword(String email, String password){
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(password != null && !password.isEmpty()) {
            if ("OK".equals(redisTemplate.opsForValue().get(email))) {
                member.updatePassword(passwordEncoder.encode(password));
                memberRepository.save(member);
            }else
                throw new IllegalStateException("비밀번호 인증을 하십시오.");
        }
    }
    public void updateMember(String email, UpdateMemberRequestDto updateMemberRequestDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(updateMemberRequestDto.getNewPassword() != null){
            if("OK".equals(redisTemplate.opsForValue().get(email))){
                updateMemberRequestDto.setNewPassword(passwordEncoder.encode(updateMemberRequestDto.getNewPassword()));
            }else{
                throw new IllegalStateException("비밀번호 인증을 하십시오.");
            }
        }
        member.update(updateMemberRequestDto);
        memberRepository.save(member);
    }

    public void updateProfileImage(String email, MultipartFile profileImage) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(profileImage != null){
            String fileName = UUID.randomUUID() + "_" + profileImage.getOriginalFilename();
            Path filePath = Paths.get("upload/profile").resolve(fileName);

            try{
                Files.createDirectories(filePath.getParent());
                profileImage.transferTo(filePath);
                member.setProfileUrl("/upload/profile/" + fileName);
                memberRepository.save(member);
            }catch (Exception e){
                throw new RuntimeException("이미지 저장 실패", e);
            }
        }
    }

    public String getProfileImage(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        if(member.getProfileUrl() == null){
            return null;
        }
        return member.getProfileUrl();
    }

    /* 권한 변경 */
    @Transactional
    public void updateRole(Long memberId, Role role) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));
        member.setRole(role);
    }

    @Transactional
    public void deleteMember(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않습니다."));

        // 1. 장바구니 항목 삭제
        cartItemRepository.deleteAllByMember(member);

        // 2. 좋아요 삭제
        likeRepository.deleteAllByMember(member);

        // 3. 리뷰 이미지 + 리뷰 삭제
        List<Review> reviews = reviewRepository.findByMember(member);
        reviewRepository.deleteAll(reviews);

        // 4. 문의 이미지 + 문의 삭제
        List<Ask> asks = askRepository.findAllByMember(member);
        askRepository.deleteAll(asks);

        // 5. 반품 삭제
        returnRepository.deleteAllByMember(member);

        // 6. 교환 삭제
        exchangeRepository.deleteAllByMember(member);

        // 7. 주문 아이템 삭제 (order는 cascade 설정이므로 별도 처리 X)
        orderItemRepository.deleteAllByMember(member);

        orderRepository.deleteAllByMember(member);

        // 8. 회원 삭제
        memberRepository.delete(member);
    }

}
