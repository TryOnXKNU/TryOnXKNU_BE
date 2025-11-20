package org.example.tryonx.apple.controller;

import org.example.tryonx.apple.service.AppleAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppleWebController {

    @Autowired
    private AppleAuthService appleAuthService;

    @GetMapping("/login/getAppleAuthUrl")
    public String getAppleAuthUrl() {
        return appleAuthService.getAuthorizationUrl();
    }
}
