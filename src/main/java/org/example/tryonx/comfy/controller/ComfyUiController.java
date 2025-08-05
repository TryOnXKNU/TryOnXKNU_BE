package org.example.tryonx.comfy.controller;

import org.example.tryonx.comfy.service.ComfyUiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fitting")
public class ComfyUiController {

    private final ComfyUiService comfyUiService;

    public ComfyUiController(ComfyUiService comfyUiService) {
        this.comfyUiService = comfyUiService;
    }

//    @GetMapping("/generate")
//    public ResponseEntity<String> generate() throws Exception {
//        String filename = comfyUiService.executeInternalWorkflow();
//        return ResponseEntity.ok(" 생성된 이미지 파일: " + filename);
//    }

    @PostMapping("/save")
    public ResponseEntity<String> generateMyFitting(@AuthenticationPrincipal UserDetails userDetails, @RequestParam Integer productId) throws Exception {
        String email = userDetails.getUsername();
        String filename = comfyUiService.executeFittingFlow(email, productId);
        return ResponseEntity.ok(" 생성된 이미지 파일: " + filename);
    }
}
