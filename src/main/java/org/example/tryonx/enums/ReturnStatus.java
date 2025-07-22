package org.example.tryonx.enums;

public enum ReturnStatus {
    REQUESTED,          // 사용자 요청 접수 (접수)
    ACCEPTED,           // 관리자 접수 완료 (승인)
    REJECTED,           // 관리자 반려
    COLLECTING,         // 상품 회수 중
    COMPLETED           // 반품 완료
}
