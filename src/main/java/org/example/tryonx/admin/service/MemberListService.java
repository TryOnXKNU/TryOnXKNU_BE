package org.example.tryonx.admin.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.MemberInfoDto;
import org.example.tryonx.admin.dto.MemberListDto;
import org.example.tryonx.admin.dto.MemberOrderHistory;
import org.example.tryonx.admin.dto.MemberSearchRequest;
import org.example.tryonx.admin.specification.MemberSpecification;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.domain.OrderItem;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberListService {
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    /* 멤버 전체 */
    public List<MemberListDto> getUserList(){
        return memberRepository.findAll().stream()
                .map(member -> new MemberListDto(
                        member.getProfileUrl(),
                        member.getMemberId(),
                        member.getName()
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
                .nickName(member.getNickname())
                .phoneNumber(member.getPhoneNumber())
                .birthday(member.getBirthDate())
                .address(member.getAddress())
                .bodyType(member.getBodyType())
                .height(member.getHeight())
                .weight(member.getWeight())
                .build();
    }

    /* 신규 회원 조회 */
    public List<MemberListDto> getRecentUsers() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        return memberRepository.findByCreatedAtAfter(oneWeekAgo).stream()
                .map(member -> new MemberListDto(
                        member.getProfileUrl(),
                        member.getMemberId(),
                        member.getName()
                ))
                .collect(Collectors.toList());
    }

    /* 멤버 삭제 */
    public void deleteMember(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다."));
        memberRepository.delete(member);
    }

    /* 멤버 필터 검색 */
    public List<Member> searchMembers(MemberSearchRequest request) {
        Specification<Member> spec = MemberSpecification.search(request.getSearchKey(), request.getSearchValue());
        return memberRepository.findAll(spec);
    }

    /* 멤버별 문의내역 조회 */
    public List<MemberOrderHistory> getOrderHistoryByMember(Long memberId) {
        Member member = memberRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버 없음"));

        List<Order> orders = orderRepository.findByMember(member);
        List<MemberOrderHistory> result = new ArrayList<>();

        for (Order order : orders) {
            for (OrderItem item : order.getOrderItems()) {
                ProductItem productItem = item.getProductItem();
                Product product = productItem.getProduct(); // 여기 주의
                result.add(new MemberOrderHistory(
                        member.getProfileUrl(),
                        member.getName(),
                        member.getMemberId(),
                        order.getOrderId(),
                        order.getOrderedAt(),
                        product.getProductId(),
                        product.getProductName(),
                        product.getPrice(),
                        order.getStatus()
                ));
            }
        }

        return result;
    }

}
