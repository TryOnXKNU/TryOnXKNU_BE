package org.example.tryonx.apple.controller;

import org.example.tryonx.apple.dto.AppleDTO;
import org.example.tryonx.apple.dto.MsgEntity;
import org.example.tryonx.apple.service.AppleAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/apple")
public class AppleCallbackController {

    @Autowired
    private AppleAuthService appleAuthService;

    @PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<MsgEntity> callback(HttpServletRequest request) throws Exception {
        String code = request.getParameter("code");
        if (code == null) {
            return ResponseEntity.badRequest().body(new MsgEntity("Error", "code required"));
        }

        Map<String, Object> result = appleAuthService.exchangeCode(code);

        String sub = result.getOrDefault("sub", "").toString();
        String accessToken = result.getOrDefault("access_token", "").toString();
        String email = result.getOrDefault("email", "").toString();

        AppleDTO appleDTO = AppleDTO.builder()
                .id(sub)
                .token(accessToken)
                .email(email)
                .build();

        return ResponseEntity.ok(new MsgEntity("Success", appleDTO));
    }
}
