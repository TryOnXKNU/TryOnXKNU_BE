package org.example.tryonx.auth.email.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.tryonx.member.repository.MemberRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final MemberRepository memberRepository;
    private static final long EXPIRE_TIME = 3 * 60; // 3분

    public EmailService(JavaMailSender mailSender, StringRedisTemplate redisTemplate, MemberRepository memberRepository) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
        this.memberRepository = memberRepository;
    }

    public void sendAuthCode(String email) {
        String code = generateCode();
        saveCodeToRedis(email, code);
        sendEmail(email, code);
    }

    public void sendAuthCodeForUpdatePassword(String email) {
        if(!memberRepository.existsByEmail(email)){
            throw new EntityNotFoundException("가입되지 않은 이메일입니다: " + email);
        }
        String code = generateCode();
        saveCodeToRedis(email, code);
        sendEmail(email, code);
    }

    private void saveCodeToRedis(String email, String code) {
        redisTemplate.opsForValue().set(email, code, EXPIRE_TIME, TimeUnit.SECONDS);
    }

    private String generateCode() {
        return String.valueOf(new Random().nextInt(899999) + 100000); // 6자리 숫자
    }

    private void sendEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("TryonX - 인증 코드입니다.");
        message.setText("인증 코드: " + code);
        mailSender.send(message);
    }

    public boolean verifyCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get(email);
        boolean success = code.equals(savedCode);

        if (success) {
            redisTemplate.opsForValue().set("verified:" + email, "true", 10, TimeUnit.MINUTES);
            redisTemplate.delete(email);
        }

        return success;
    }
    public boolean isVerified(String email) {
        return "true".equals(redisTemplate.opsForValue().get("verified:" + email));
    }
}
