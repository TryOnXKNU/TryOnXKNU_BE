package org.example.tryonx.admin.controller;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.service.ImageUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ImageUploadTestController {
    private final ImageUploadService imageUploadService;

    @PostMapping("/test/upload")
    public ResponseEntity<List<String>> uploadImage(@RequestParam("images") MultipartFile[] images) {
        List<String> responses = imageUploadService.uploadToS3AndComfy(images);
        return ResponseEntity.ok(responses);
    }
}
