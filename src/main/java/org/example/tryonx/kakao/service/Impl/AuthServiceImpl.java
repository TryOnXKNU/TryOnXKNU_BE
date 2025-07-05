package org.example.tryonx.kakao.service.Impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tryonx.kakao.dto.KakaoProfile;
import org.example.tryonx.kakao.dto.ResponseDto;
import org.example.tryonx.kakao.service.AuthService;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.Role;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.auth.local.token.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${kakao.client.id}")
    private String clientKey;
    @Value("${kakao.redirect.url}")
    private String redirectUrl;
    @Value("${kakao.accesstoken.url}")
    private String kakaoAccessTokenUrl;
    @Value("${kakao.userinfo.url}")
    private String kakaoUserInfoUrl;

    @Override
    public ResponseEntity<?> getKakaoUserInfo(String authorizeCode) {
        try {
            // 1. AccessToken 요청
            ObjectMapper objectMapper = new ObjectMapper();
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", clientKey);
            params.add("redirect_uri", redirectUrl);
            params.add("code", authorizeCode);

            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

            ResponseEntity<String> tokenResponse = restTemplate.exchange(
                    kakaoAccessTokenUrl,
                    HttpMethod.POST,
                    tokenRequest,
                    String.class
            );

            log.info("[kakao login] authorizecode issued successfully");
            Map<String, Object> tokenMap = objectMapper.readValue(tokenResponse.getBody(), new TypeReference<>() {});
            String accessToken = (String) tokenMap.get("access_token");

            // 2. 사용자 정보 요청
            ResponseDto dto = getInfo(accessToken);
            Long kakaoId = dto.getKakaoId();

            // 3. 회원 조회 또는 생성
            Member member = memberRepository.findByEmail(dto.getEmail())
                    .orElseGet(() -> {
                        Member newMember = Member.builder()
                                .email(dto.getEmail())
                                .name(dto.getName() != null ? dto.getName() : dto.getProfile_nickname())
                                .nickname(dto.getProfile_nickname() != null ? dto.getProfile_nickname() : "카카오사용자")
                                .profileUrl(dto.getProfile_image() != null ? dto.getProfile_image() : "")
                                .phoneNumber(dto.getPhone_number())
                                .gender(convertGender(dto.getGender()))
                                .birthDate(parseBirth(dto.getBirthyear(), dto.getBirthday()))
                                .address(dto.getShipping_address())
                                .socialType("KAKAO")
                                .socialId(kakaoId)
                                .password(null)
                                .role(Role.USER)
                                .build();
                        return memberRepository.save(newMember);
                    });

            String token = jwtTokenProvider.createtoken(member.getEmail(), member.getRole().toString());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token); // 클라이언트에서 저장할 수 있도록 포함시켜야 함

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("카카오 로그인 실패");
        }
    }

    private ResponseDto getInfo(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ObjectMapper objectMapper = new ObjectMapper();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);
            headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.postForEntity(kakaoUserInfoUrl, entity, String.class);

            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<>() {});
            Long kakaoId = ((Number) responseMap.get("id")).longValue();

            Map<String, Object> kakaoAccount = (Map<String, Object>) responseMap.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            return ResponseDto.builder()
                    .kakaoId(kakaoId)
                    .name((String) kakaoAccount.get("name"))
                    .email((String) kakaoAccount.get("email"))
                    .profile_image((String) profile.get("profile_image_url"))
                    .profile_nickname((String) profile.get("nickname"))
                    .gender((String) kakaoAccount.get("gender"))
                    .birthday((String) kakaoAccount.get("birthday"))
                    .birthyear((String) kakaoAccount.get("birthyear"))
                    .phone_number((String) kakaoAccount.get("phone_number"))
                    .shipping_address((String) kakaoAccount.get("shipping_address"))
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Integer convertGender(String gender) {
        if ("male".equalsIgnoreCase(gender)) return 1;
        if ("female".equalsIgnoreCase(gender)) return 2;
        return null;
    }

    private LocalDate parseBirth(String birthyear, String birthday) {
        try {
            return LocalDate.parse(birthyear + "-" + birthday.substring(0, 2) + "-" + birthday.substring(2));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public ResponseEntity<?> kakaoLoginWithSDK(String accessToken, KakaoProfile profile) {
        try {
            // 1. 카카오 액세스 토큰 유효성 검사
            if (!isValidKakaoToken(accessToken)) {
                return ResponseEntity.status(401).body("유효하지 않은 카카오 액세스토큰입니다.");
            }

            // 2. 사용자 조회 또는 생성
            Member member = memberRepository.findByEmail(profile.getEmail())
                    .orElseGet(() -> {
                        Member newMember = Member.builder()
                                .email(profile.getEmail())
                                .name(profile.getNickname() != null ? profile.getNickname() : "카카오사용자")
                                .nickname(profile.getNickname())
                                .profileUrl(profile.getProfile_image())
                                .phoneNumber(profile.getPhone_number())
                                .gender(convertGender(profile.getGender()))
                                .birthDate(parseBirth(profile.getBirthyear(), profile.getBirthday()))
                                .address(profile.getShipping_address())
                                .socialType("KAKAO")
                                .socialId(profile.getId())
                                .password(null)
                                .role(Role.USER)
                                .build();
                        return memberRepository.save(newMember);
                    });

            // 3. JWT 토큰 발급
            String token = jwtTokenProvider.createtoken(member.getEmail(), member.getRole().toString());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("nickname", member.getNickname());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("카카오 SDK 로그인 처리 중 오류 발생");
        }
    }

    private boolean isValidKakaoToken(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v1/user/access_token_info",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("카카오 토큰 유효성 검사 실패", e);
            return false;
        }
    }


}
