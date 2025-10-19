package org.example.tryonx.orders.payment.service;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.OrderStatus;
import org.example.tryonx.enums.PaymentStatus;
import org.example.tryonx.orders.order.domain.Order;
import org.example.tryonx.orders.order.repository.OrderRepository;
import org.example.tryonx.orders.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentRefundService {

    private final IamportClient iamportClient;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    @Transactional
    public void refundPayment(Integer orderId, String reason) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found : " + orderId));
        org.example.tryonx.orders.payment.domain.Payment payment =
                paymentRepository.findByOrder_OrderId(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("실제 결제 내역 없음(ex.전액 포인트 사용)"));

        // 전체 환불: 금액 지정 X
        CancelData cancelData = new CancelData(payment.getImpUid(), true);
        cancelData.setReason(reason);

        // 환불 API 호출
        IamportResponse<Payment> response = iamportClient.cancelPaymentByImpUid(cancelData);

        if (response.getResponse() == null) {
            throw new RuntimeException("환불 실패: " + response.getMessage());
        }

        // DB 상태 업데이트
        order.setStatus(OrderStatus.CANCELLED);
        payment.setStatus(PaymentStatus.CANCELLED);
        orderRepository.save(order);
        paymentRepository.save(payment);
    }
    @Transactional
    public void refundPaymentPartial(Integer orderId, BigDecimal refundAmount, String reason) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new Exception("Order not found : " + orderId));
        org.example.tryonx.orders.payment.domain.Payment payment =
                paymentRepository.findByOrder_OrderId(orderId)
                        .orElseThrow(() -> new IllegalArgumentException("실제 결제 내역 없음"));

        // 부분환불: 금액 지정
        CancelData cancelData = new CancelData(payment.getImpUid(), true, refundAmount);
        cancelData.setReason(reason);

        IamportResponse<Payment> response = iamportClient.cancelPaymentByImpUid(cancelData);
        if (response.getResponse() == null) {
            throw new RuntimeException("부분 환불 실패: " + response.getMessage());
        }

        // 결제 상태는 전액 환불이 아닌 경우, 부분 환불용 상태 유지
        order.setStatus(OrderStatus.PARTIAL_REFUNDED);
        payment.setStatus(PaymentStatus.PARTIAL_REFUNDED);
        paymentRepository.save(payment);
    }

}
