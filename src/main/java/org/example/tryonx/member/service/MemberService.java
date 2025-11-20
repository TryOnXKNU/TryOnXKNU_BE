package org.example.tryonx.member.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.MemberInfoDto;
import org.example.tryonx.ask.domain.Ask;
import org.example.tryonx.ask.repository.AskRepository;
import org.example.tryonx.cart.repository.CartItemRepository;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.exchange.repository.ExchangeRepository;
import org.example.tryonx.fitting.repository.FittingImageRepository;
import org.example.tryonx.image.repository.ReviewImageRepository;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.Role;
import org.example.tryonx.member.dto.MemberListResponseDto;
import org.example.tryonx.member.dto.MyInfoResponseDto;
import org.example.tryonx.member.dto.UpdateBodyInfoDto;
import org.example.tryonx.member.dto.UpdateMemberRequestDto;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.member.repository.PointHistoryRepository;
import org.example.tryonx.notice.repository.NotificationRepository;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.orders.payment.repository.PaymentRepository;
import org.example.tryonx.returns.repository.ReturnRepository;
import org.example.tryonx.review.domain.Review;
import org.example.tryonx.review.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
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
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final FittingImageRepository fittingImageRepository;
    private static final long EXPIRE_TIME = 3 * 60;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

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
                .nickname(member.getNickname())
                .phoneNumber(member.getPhoneNumber())
                .address(member.getAddress())
                .email(member.getEmail())
                .bodyShape(member.getBodyShape())
                .height(member.getHeight())
                .weight(member.getWeight())
                .memberNum(member.getMemberNum())
                .socialId(member.getSocialId())
                .role(member.getRole())
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
    public void updatePhoneNumber(String email, String phoneNumber) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));

        if (!StringUtils.hasText(phoneNumber)) {
            return; // 비어 있으면 수정하지 않음
        }

        member.setPhoneNumber(phoneNumber);
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

//    public void updateProfileImage(String email, MultipartFile profileImage) {
//        Member member = memberRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
//        if(profileImage != null){
//            String fileName = UUID.randomUUID() + "_" + profileImage.getOriginalFilename();
//            Path filePath = Paths.get("upload/profile").resolve(fileName);
//
//            try{
//                Files.createDirectories(filePath.getParent());
//                profileImage.transferTo(filePath);
//                member.setProfileUrl("/upload/profile/" + fileName);
//                memberRepository.save(member);
//            }catch (Exception e){
//                throw new RuntimeException("이미지 저장 실패", e);
//            }
//        }
//    }
//
//    public String getProfileImage(String email) {
//        Member member = memberRepository.findByEmail(email)
//                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
//        if(member.getProfileUrl() == null){
//            return null;
//        }
//        return member.getProfileUrl();
//    }

    @Transactional
    public void updateProfileImage(String email, MultipartFile profileImage) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));

        if (profileImage != null && !profileImage.isEmpty()) {

            // 기존 프로필 이미지가 존재할 때만 S3에서 삭제
            String oldUrl = member.getProfileUrl();
            if (oldUrl != null && oldUrl.startsWith("https://")) {
                try {
                    String oldKey = extractKeyFromUrl(oldUrl);
                    amazonS3.deleteObject(bucket, oldKey);
                } catch (Exception e) {
                    System.err.println("기존 프로필 이미지 삭제 실패: " + e.getMessage());
                }
            }

            // 새 파일명 지정 (폴더명 + UUID)
            String fileName = "profile/" + UUID.randomUUID() + "_" + profileImage.getOriginalFilename();

            // 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(profileImage.getContentType());
            metadata.setContentLength(profileImage.getSize());

            try (InputStream inputStream = profileImage.getInputStream()) {
                amazonS3.putObject(bucket, fileName, inputStream, metadata);
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 S3 업로드 실패", e);
            }

            // 이미지 URL 생성 및 저장
            String imageUrl = amazonS3.getUrl(bucket, fileName).toString();
            member.setProfileUrl(imageUrl);
            memberRepository.save(member);
        }
    }

    public String getProfileImage(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        return member.getProfileUrl(); // null이면 프론트에서 기본 이미지로 처리
    }

    private String extractKeyFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl이 비어 있습니다.");
        }

        int index = imageUrl.indexOf(".amazonaws.com/");
        if (index == -1) {
            throw new IllegalArgumentException("유효하지 않은 S3 URL: " + imageUrl);
        }

        return imageUrl.substring(index + ".amazonaws.com/".length());
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

        // 7. 결제 정보 삭제 (orders 삭제 전에 필수)
        List<org.example.tryonx.orders.order.domain.Order> orders = orderRepository.findByMember(member);
        for (org.example.tryonx.orders.order.domain.Order order : orders) {
            paymentRepository.findByOrder_OrderId(order.getOrderId())
                    .ifPresent(paymentRepository::delete);
        }

        // 8. 주문 아이템 삭제
        orderItemRepository.deleteAllByMember(member);

        // 9. 주문 삭제
        orderRepository.deleteAllByMember(member);

        // 10. 알림 삭제
        List<org.example.tryonx.notice.domain.Notification> notifications = notificationRepository.findByMember(member);
        notificationRepository.deleteAll(notifications);

        // 11. 포인트 히스토리 삭제
        List<org.example.tryonx.member.domain.PointHistory> pointHistories = pointHistoryRepository.findByMemberOrderByCreatedAtDesc(member);
        pointHistoryRepository.deleteAll(pointHistories);

        fittingImageRepository.deleteAllByMember(member);
        // 12. 회원 삭제
        memberRepository.delete(member);
    }

}
