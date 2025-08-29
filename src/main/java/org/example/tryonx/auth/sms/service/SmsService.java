package org.example.tryonx.auth.sms.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.example.tryonx.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SmsService {

    private final StringRedisTemplate redisTemplate;
    private final MemberRepository memberRepository;

    private DefaultMessageService messageService;

    @Value("${coolsms.apiKey}")
    private String apiKey;

    @Value("${coolsms.apiSecret}")
    private String apiSecret;

    @Value("${coolsms.number}")
    private String number;

    private static final long EXPIRE_TIME = 3 * 60;

    @PostConstruct
    public void init() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.coolsms.co.kr");
    }

    public boolean sendAuthCode(String phoneNumber) {
        if(memberRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            return false;
        }else {
            String code = generateCode();
            saveCodeToRedis(phoneNumber, code);
            sendSms(phoneNumber, code);
            return true;
        }
    }

    private void sendSms(String to, String code) {
        Message message = new Message();
        message.setFrom(number);
        message.setTo(to);
        message.setText("[TryOnX] 인증번호는 " + code + "입니다.");

        SingleMessageSendingRequest request = new SingleMessageSendingRequest(message);
        messageService.sendOne(request);
    }

    private void saveCodeToRedis(String phoneNumber, String code) {
        redisTemplate.opsForValue().set("sms:" + phoneNumber, code, EXPIRE_TIME, TimeUnit.SECONDS);
    }

    private String generateCode() {
        return String.valueOf(new Random().nextInt(899999) + 100000);
    }

    public boolean verifyCode(String phoneNumber, String code) {
        String savedCode = redisTemplate.opsForValue().get("sms:" + phoneNumber);
        boolean success = code.equals(savedCode);

        if (success) {
            redisTemplate.opsForValue().set("verified:sms:" + phoneNumber, "true", 10, TimeUnit.MINUTES);
            redisTemplate.delete("sms:" + phoneNumber);
        }

        return success;
    }

    public boolean isVerified(String phoneNumber) {
        return "true".equals(redisTemplate.opsForValue().get("verified:sms:" + phoneNumber));
    }
}
