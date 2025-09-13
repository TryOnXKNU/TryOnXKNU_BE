package org.example.tryonx.auth.email.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.auth.email.service.EmailService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email")
public class EmailAuthController {

    private final EmailService mailService;

    public EmailAuthController(EmailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendCode(@RequestParam String email) {
        mailService.sendAuthCode(email);
        return ResponseEntity.ok("인증 코드가 전송되었습니다.");
    }

    @PostMapping("/send/password")
    public ResponseEntity<String> sendCodeForResetPassword(@RequestParam String email) {
        mailService.sendAuthCodeForUpdatePassword(email);
        return ResponseEntity.ok("인증 코드가 전송되었습니다.");
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String email,
                                             @RequestParam String code) {
        boolean verified = mailService.verifyCode(email, code);
        return verified ?
                ResponseEntity.ok("인증 성공") :
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 실패");
    }
}