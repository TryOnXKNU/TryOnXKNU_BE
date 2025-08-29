package org.example.tryonx.auth.sms.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.auth.sms.service.SmsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/sms")
@RequiredArgsConstructor
public class SmsAuthController {

    private final SmsService smsService;

    /**
     * 인증번호 전송
     * 수신자 번호 (010xxxxxxxx 형식)
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendCode(@RequestParam String phoneNumber) {
        try {
            if(smsService.sendAuthCode(phoneNumber))
                return ResponseEntity.ok("문자 인증번호가 전송되었습니다.");
            return ResponseEntity.badRequest().body("중복된 휴대폰 번호입니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("인증번호 전송 실패: " + e.getMessage());
        }
    }

    /**
     * 인증번호 검증
     * @param phoneNumber 수신자 번호
     * @param code 사용자 입력 코드
     */
    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String phoneNumber,
                                             @RequestParam String code) {
        boolean verified = smsService.verifyCode(phoneNumber, code);
        return verified
                ? ResponseEntity.ok("인증 성공")
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 실패: 코드가 일치하지 않습니다.");
    }
}
