package org.example.tryonx.orders.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tryonx.orders.payment.dto.PaymentCompleteReqDto;
import org.example.tryonx.orders.payment.dto.PaymentCompleteResDto;
import org.example.tryonx.orders.payment.dto.PrecheckRequstDto;
import org.example.tryonx.orders.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@Tag(name = "Users Payments API", description = "회원 결제 API")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/precheck")
    @Operation(summary = "결제 확인")
    public ResponseEntity<?> precheck(@RequestBody @Valid PrecheckRequstDto req) {
        var res = paymentService.precheck(req);
        if (!res.isOk()) return ResponseEntity.status(HttpStatus.CONFLICT).body(false);
        return ResponseEntity.ok(true);
    }

    @PostMapping("/complete")
    @Operation(summary = "결제 완료")
    public ResponseEntity<PaymentCompleteResDto> complete(
            @RequestBody PaymentCompleteReqDto req, @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest httpReq
            ) {
        System.out.println("[HIT] /api/v1/payment/complete"
                + " ip=" + (httpReq.getHeader("X-Forwarded-For") != null
                ? httpReq.getHeader("X-Forwarded-For")
                : httpReq.getRemoteAddr())
                + " imp_uid=" + req.getImp_uid()
                + " merchant_uid=" + req.getMerchant_uid());
        String email = userDetails.getUsername();
        PaymentCompleteResDto res = paymentService.verifyPaymentComplete(email, req);
        System.out.println("[DONE] /payment/complete verified=" + res.isVerified());
        return ResponseEntity.ok(res);
    }

//    @PostMapping("/{orderId}/refund")
//    public ResponseEntity<String> refundPayment(
//            @PathVariable Integer orderId,
//            @RequestParam(defaultValue = "사용자 요청 환불") String reason) throws Exception {
//
//        refundService.refundPayment(orderId, reason);
//        return ResponseEntity.ok("환불이 완료되었습니다.");
//    }
}
