package org.example.tryonx.apple.controller;

import org.example.tryonx.apple.service.AppleAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.domain.Role;
import org.example.tryonx.auth.local.token.JwtTokenProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AppleAuthController {

	@Autowired
	private AppleAuthService appleAuthService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@PostMapping("/apple")
	public ResponseEntity<?> appleIdentity(@RequestBody Map<String, Object> body) {
		// Expecting identityToken from client as `identityToken` field
		try {
			String idToken = (String) body.get("identityToken");
			if (idToken == null) {
				Map<String, Object> err = new HashMap<>();
				err.put("error", "identityToken required");
				return ResponseEntity.badRequest().body(err);
			}
			Map<String, Object> claims = appleAuthService.verifyIdToken(idToken);
			Map<String, Object> resp = new HashMap<>();
			resp.put("token", "server-jwt-for-" + claims.get("sub"));
			resp.put("refreshToken", "server-refresh-" + claims.get("sub"));
			resp.put("role", "USER");
			resp.put("isNewMember", false);
			resp.put("apple", claims);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			Map<String, Object> err = new HashMap<>();
			err.put("error", e.getMessage());
			return ResponseEntity.status(500).body(err);
		}
	}

	@PostMapping("/apple/code")
	public ResponseEntity<?> appleCode(@RequestBody Map<String, Object> body) {
		// Expecting authorizationCode from client as `code` field
		try {
			String code = (String) body.get("code");
			if (code == null) {
				Map<String, Object> err = new HashMap<>();
				err.put("error", "code required");
				return ResponseEntity.badRequest().body(err);
			}

			Map<String, Object> result = appleAuthService.exchangeCode(code);
			// extract sub/email
			String sub = result.getOrDefault("sub", "").toString();
			String email = result.getOrDefault("email", "").toString();
			boolean isNewMember = false;

			Member member = null;
			if (email != null && !email.isEmpty()) {
				member = memberRepository.findByEmail(email).orElse(null);
			}

			if (member != null) {
				// existing account with same email
				if (!"APPLE".equalsIgnoreCase(member.getSocialType())) {
					throw new IllegalStateException("이미 일반 또는 다른 소셜로 가입된 이메일입니다.");
				}
			} else {
				// create minimal member record using available info
				isNewMember = true;
				String name = email != null && email.contains("@") ? email.split("@")[0] : "AppleUser";
				String nickname = "apple_" + (sub.length() > 8 ? sub.substring(sub.length() - 8) : sub);
				String phoneNumber = "APPLE_" + System.currentTimeMillis() + "_" + Math.abs(sub.hashCode());
				Long socialId = (long) sub.hashCode();

				member = Member.builder()
					.email(email != null && !email.isEmpty() ? email : ("apple_" + socialId + "@apple.local"))
					.name(name)
					.nickname(nickname)
					.profileUrl("")
					.phoneNumber(phoneNumber)
					.birthDate(null)
					.address(null)
					.socialType("APPLE")
					.socialId(socialId)
					.password(null)
					.point(0)
					.role(Role.USER)
					.build();

				member = memberRepository.save(member);
			}

			String jwt = jwtTokenProvider.createtoken(member.getEmail(), member.getRole().toString());

			Map<String, Object> resp = new HashMap<>();
			resp.put("token", jwt);
			resp.put("refreshToken", result.getOrDefault("refresh_token", ""));
			resp.put("role", member.getRole().toString());
			resp.put("isNewMember", isNewMember);
			resp.put("apple", result);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			Map<String, Object> err = new HashMap<>();
			err.put("error", e.getMessage());
			return ResponseEntity.status(500).body(err);
		}
	}
}

