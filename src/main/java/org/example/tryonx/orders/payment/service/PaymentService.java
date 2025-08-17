package org.example.tryonx.orders.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.example.tryonx.enums.PaymentStatus;
import org.example.tryonx.enums.Size;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.order.dto.OrderPreviewRequestDto;
import org.example.tryonx.orders.payment.dto.PaymentCompleteReqDto;
import org.example.tryonx.orders.payment.dto.PaymentCompleteResDto;
import org.example.tryonx.orders.payment.dto.PrecheckRequstDto;
import org.example.tryonx.orders.payment.repository.PaymentRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IamportClient iamportClient;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;// 로그인 유효성 체크용(필요 없으면 제거)
    private final ProductItemRepository productItemRepo;
    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    public PrecheckResp precheck(PrecheckRequstDto req) {
        List<PrecheckResp.Fail> fails = new ArrayList<>();
        for (var it : req.getItems()) {
            Product pd = productRepo.findById(it.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            var pi = productItemRepo.findByProductAndSize(pd, it.getSize()).orElse(null);
            if (pi == null) {
                fails.add(new PrecheckResp.Fail(it.getProductId(), it.getSize(), "NOT_AVAILABLE"));
                continue;
            }
            int need = Optional.ofNullable(it.getQuantity()).orElse(0);
            int have = productItemRepo.findStock(pi.getProductItemId()).orElse(0);
            if (need <= 0 || have < need) {
                fails.add(new PrecheckResp.Fail(it.getProductId(), it.getSize(),
                        need <= 0 ? "INVALID_QTY" : "INSUFFICIENT_STOCK"));
            }
        }
        if (!fails.isEmpty()) return PrecheckResp.fail(fails);
        return PrecheckResp.ok();
    }


    @Transactional
    public PaymentCompleteResDto verifyPaymentComplete(String email, PaymentCompleteReqDto req) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("회원 정보를 찾을 수 없습니다."));

        if (isBlank(req.getImp_uid()) || isBlank(req.getMerchant_uid())) {
            throw new IllegalArgumentException("imp_uid 또는 merchant_uid가 없습니다");
        }

        try {
            IamportResponse<Payment> resp = iamportClient.paymentByImpUid(req.getImp_uid());
            Payment pgPay = resp.getResponse();
// 유효성 검사(status, merchant_uid) 끝난 뒤

            LocalDateTime paidAt = null;
            if (pgPay.getPaidAt() != null) {
                paidAt = pgPay.getPaidAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            Integer amount = pgPay.getAmount().intValue();

// upsert 로직 그대로 유지
            Optional<org.example.tryonx.orders.payment.domain.Payment> maybe =
                    paymentRepository.findByMerchantUid(req.getMerchant_uid());

            org.example.tryonx.orders.payment.domain.Payment payment = maybe.orElseGet(() ->
                    org.example.tryonx.orders.payment.domain.Payment.builder()
                            .merchantUid(req.getMerchant_uid())
                            .status(PaymentStatus.READY)
                            .amount(amount)
                            .build()
            );

// 결제 메타 채우기
            payment.setImpUid(req.getImp_uid());
            payment.setStatus(PaymentStatus.PAID);
            payment.setAmount(amount);
            payment.setPaidAt(paidAt);

// 결제수단·PG
            payment.setPayMethod(pgPay.getPayMethod()); // 예: "card", "kakaopay", "trans" ...
            payment.setPgProvider(pgPay.getPgProvider()); // 예: "kcp", "inicis", "tosspayments"

// 카드 정보 (카드 결제인 경우만 값이 있음)
            payment.setCardName(pgPay.getCardName());
            payment.setCardCode(pgPay.getCardCode());
            payment.setCardNumberMasked(pgPay.getCardNumber()); // SDK는 마스킹된 번호를 반환
            payment.setCardQuota(pgPay.getCardQuota());
            payment.setCardType(pgPay.getCardType());

// 은행 정보 (계좌이체/가상계좌 등인 경우)
            payment.setBankName(pgPay.getBankName());
            payment.setBankCode(pgPay.getBankCode());

// 영수증 URL
            payment.setReceiptUrl(pgPay.getReceiptUrl());

// 간편결제 제공사 (pay_method가 간편결제일 때 구분용)
            payment.setEasyPayProvider(
                    "card".equalsIgnoreCase(pgPay.getPayMethod()) ? null : pgPay.getPayMethod()
            );

            try {
                paymentRepository.save(payment);
            } catch (DataIntegrityViolationException dup) {
                payment = paymentRepository.findByMerchantUid(req.getMerchant_uid())
                        .orElseThrow(() -> dup);
// 동시성 재갱신 시에도 동일하게 세팅
                payment.setImpUid(req.getImp_uid());
                payment.setStatus(PaymentStatus.PAID);
                payment.setAmount(amount);
                payment.setPaidAt(paidAt);
                payment.setPayMethod(pgPay.getPayMethod());
                payment.setPgProvider(pgPay.getPgProvider());
                payment.setCardName(pgPay.getCardName());
                payment.setCardCode(pgPay.getCardCode());
                payment.setCardNumberMasked(pgPay.getCardNumber());
                payment.setCardQuota(pgPay.getCardQuota());
                payment.setCardType(pgPay.getCardType());
                payment.setBankName(pgPay.getBankName());
                payment.setBankCode(pgPay.getBankCode());
                payment.setReceiptUrl(pgPay.getReceiptUrl());
                payment.setEasyPayProvider("card".equalsIgnoreCase(pgPay.getPayMethod()) ? null : pgPay.getPayMethod());
                paymentRepository.save(payment);
            }

            return PaymentCompleteResDto.builder()
                    .imp_uid(req.getImp_uid())
                    .merchant_uid(req.getMerchant_uid())
                    .status(pgPay.getStatus())
                    .amount(amount)
                    .paidAt(paidAt)
                    .verified(true)
                    .build();

        } catch (IamportResponseException | IOException e) {
            throw new RuntimeException("포트원 결제 조회 실패", e);
        }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    // 응답 DTO
    @Getter
    @AllArgsConstructor
    public static class PrecheckResp {
        private boolean ok;
        private List<Fail> fails;
        public static PrecheckResp ok() { return new PrecheckResp(true, List.of()); }
        public static PrecheckResp fail(List<Fail> f) { return new PrecheckResp(false, f); }
        @Getter @AllArgsConstructor
        public static class Fail {
            private Integer productId;
            private Size size;
            private String reason;
        }
    }
}
