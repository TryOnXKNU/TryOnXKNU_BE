package org.example.tryonx.orders.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.example.tryonx.enums.PaymentStatus;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.orders.payment.dto.PaymentCompleteReqDto;
import org.example.tryonx.orders.payment.dto.PaymentCompleteResDto;
import org.example.tryonx.orders.payment.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final IamportClient iamportClient;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;// 로그인 유효성 체크용(필요 없으면 제거)

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
            if (pgPay == null) throw new IllegalStateException("결제 내역을 찾을 수 없습니다");

            // 1) 검증
            boolean statusOk = "paid".equalsIgnoreCase(pgPay.getStatus());
            boolean merchantOk = req.getMerchant_uid().equals(pgPay.getMerchantUid());
            if (!statusOk || !merchantOk) {
                String reason = (!statusOk ? "상태 불일치 " : "") + (!merchantOk ? "merchant_uid 불일치" : "");
                throw new IllegalStateException("결제 검증 실패: " + reason.trim());
            }

            // 2) DB 반영(없으면 생성, 있으면 갱신)
            LocalDateTime paidAt = null;
            if (pgPay.getPaidAt() != null) {
                paidAt = pgPay.getPaidAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }

            Integer amount = pgPay.getAmount().intValue();

            Optional<org.example.tryonx.orders.payment.domain.Payment> maybe = paymentRepository.findByMerchantUid(req.getMerchant_uid());
            org.example.tryonx.orders.payment.domain.Payment payment = maybe.orElseGet(() ->
                    org.example.tryonx.orders.payment.domain.Payment.builder()
                            .merchantUid(req.getMerchant_uid())
                            .status(PaymentStatus.READY)
                            .amount(amount)
                            .build()
            );

            payment.setImpUid(req.getImp_uid());
            payment.setStatus(PaymentStatus.PAID);
            payment.setAmount(amount);
            payment.setPaidAt(paidAt);
            // order 연결은 나중에 handleOrder에서 setOrder(...)로

            try {
                paymentRepository.save(payment);
            } catch (DataIntegrityViolationException dup) {
                // 동시성으로 UNIQUE(merchant_uid/imp_uid) 충돌 시 재조회 후 갱신
                payment = paymentRepository.findByMerchantUid(req.getMerchant_uid())
                        .orElseThrow(() -> dup);
                payment.setImpUid(req.getImp_uid());
                payment.setStatus(PaymentStatus.PAID);
                payment.setAmount(amount);
                payment.setPaidAt(paidAt);
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
}
