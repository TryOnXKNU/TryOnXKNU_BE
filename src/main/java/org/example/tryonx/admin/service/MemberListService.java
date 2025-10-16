package org.example.tryonx.admin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.MemberInfoDto;
import org.example.tryonx.admin.dto.MemberListDto;
import org.example.tryonx.admin.dto.MemberOrderHistory;
import org.example.tryonx.admin.dto.MemberSearchRequest;
import org.example.tryonx.admin.specification.MemberSpecification;
import org.example.tryonx.cart.repository.CartItemRepository;
import org.example.tryonx.exchange.repository.ExchangeRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.returns.repository.ReturnRepository;
import org.example.tryonx.review.repository.ReviewRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberListService {
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;

    private final OrderItemRepository orderItemRepository;
    private final ReviewRepository reviewRepository;
    private final ExchangeRepository exchangeRepository;
    private final ReturnRepository returnRepository;

    /* 멤버 전체 */
    public List<MemberListDto> getUserList(){
        return memberRepository.findAll().stream()
                .map(member -> new MemberListDto(
                        member.getProfileUrl(),
                        member.getMemberId(),
//                        member.getName(),
                        member.getNickname(),
                        member.getMemberNum(),
                        member.getSocialId()
                ))
                .collect(Collectors.toList());
    }

    /* 멤버 상세정보 */
    public MemberInfoDto findById(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다."));

        return MemberInfoDto.builder()
                .profileUrl(member.getProfileUrl())
                .name(member.getName())
                .memberId(member.getMemberId())
                .nickname(member.getNickname())
                .phoneNumber(member.getPhoneNumber())
                .birthday(member.getBirthDate())
                .address(member.getAddress())
                .email(member.getEmail())
                .bodyShape(member.getBodyShape())
                .height(member.getHeight())
                .weight(member.getWeight())
                .memberNum(member.getMemberNum())
                .socialId(member.getSocialId())
                .build();
    }

    /* 신규 회원 조회 */
    public List<MemberListDto> getRecentUsers() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        return memberRepository.findByCreatedAtAfter(oneWeekAgo).stream()
                .map(member -> new MemberListDto(
                        member.getProfileUrl(),
                        member.getMemberId(),
//                        member.getName(),
                        member.getNickname(),
                        member.getMemberNum(),
                        member.getSocialId()
                ))
                .collect(Collectors.toList());
    }

    /* 멤버 삭제 */
    @Transactional
    public void deleteMemberWithDependencies(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("회원이 존재하지 않음"));
        cartItemRepository.deleteAllByMember(member);
        List<Order> orders = orderRepository.findByMember(member);

        for (Order order : orders) {
            List<OrderItem> orderItems = orderItemRepository.findByOrder(order);

            for (OrderItem orderItem : orderItems) {
                reviewRepository.deleteByOrderItem(orderItem);
                exchangeRepository.deleteByOrderItem(orderItem);
                returnRepository.deleteByOrderItem(orderItem);

                orderItemRepository.delete(orderItem);
            }

            orderRepository.delete(order);
        }

        memberRepository.delete(member);
    }


    /* 멤버 필터 검색 */
    public List<Member> searchMembers(MemberSearchRequest request) {
        Specification<Member> spec = MemberSpecification.search(request.getSearchKey(), request.getSearchValue());
        return memberRepository.findAll(spec);
    }

    public List<MemberOrderHistory> getOrderHistoryByMember(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버 없음"));

        List<Order> orders = orderRepository.findByMember(member);
        List<MemberOrderHistory> result = new ArrayList<>();

        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                ProductItem productItem = item.getProductItem();
                Product product = productItem.getProduct();

                BigDecimal unitPrice = nz(product.getPrice());       // 단가
                BigDecimal rate = nz(product.getDiscountRate());     // 할인율 (%)
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity());

                // 할인 적용 후 최종가 = 단가 * (1 - 할인율/100) * 수량
                BigDecimal discountedPrice = unitPrice
                        .multiply(BigDecimal.ONE.subtract(rate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)))
                        .multiply(qty)
                        .setScale(0, RoundingMode.DOWN);

                result.add(new MemberOrderHistory(
                        order.getOrderNum(),
                        member.getProfileUrl(),
//                        member.getName(),
                        member.getNickname(),
                        member.getMemberId(),
                        order.getOrderId(),
                        order.getOrderedAt(),
                        product.getProductId(),
                        product.getProductName(),
                        item.getQuantity(),
                        product.getPrice(),
                        order.getStatus(),
                        order.getDeliveryStatus(),
                        rate,
                        discountedPrice,
                        order.getFinalAmount(),
                        member.getMemberNum(),
                        member.getSocialId()
                ));
            }
        }

        return result;
    }
    // null-safe 유틸
    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    public long countNewMembers() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        return memberRepository.countByCreatedAtAfter(oneWeekAgo);
    }

    public long countTotalMembers() {
        return memberRepository.count();
    }
}
