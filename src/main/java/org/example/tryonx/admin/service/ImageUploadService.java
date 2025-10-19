package org.example.tryonx.admin.service;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

//    private final RestTemplate restTemplate;
//
//    private final String NGROK_URL = "https://tryonxcomfy.ngrok.app/upload/image";
//
//    public List<String> uploadToComfy(MultipartFile[] images) {
//        List<String> results = new ArrayList<>();
//
//        for (MultipartFile image : images) {
//            try {
//                ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
//                    @Override
//                    public String getFilename() {
//                        return image.getOriginalFilename();
//                    }
//                };
//
//                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//                body.add("image", fileResource);
//
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//
//                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//
//                ResponseEntity<String> response =
//                        restTemplate.postForEntity(NGROK_URL, requestEntity, String.class);
//
//                results.add(response.getBody());
//
//            } catch (IOException e) {
//                results.add("XXXXX -> " + image.getOriginalFilename() + " 업로드 실패: " + e.getMessage());
//            }
//        }
//
//        return results;
//    }

    private final AmazonS3 amazonS3;
    private final RestTemplate restTemplate;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${ngrok.url}")
    private String baseUrl;

    public List<String> uploadToS3AndComfy(MultipartFile[] images) {
        String COMFY_URL = baseUrl + "/upload/image";
        List<String> results = new ArrayList<>();

        for (MultipartFile image : images) {
            try {
                //S3 업로드
                String fileName = "test/" + UUID.randomUUID() + "_" + image.getOriginalFilename();

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(image.getContentType());
                metadata.setContentLength(image.getSize());

                try (InputStream inputStream = image.getInputStream()) {
                    amazonS3.putObject(bucket, fileName, inputStream, metadata);
                }

                String s3Url = amazonS3.getUrl(bucket, fileName).toString();

                //컴피 서버로 업로드
                ByteArrayResource fileResource = new ByteArrayResource(image.getBytes()) {
                    @Override
                    public String getFilename() {
                        return image.getOriginalFilename();
                    }
                };

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("image", fileResource);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                ResponseEntity<String> response =
                        restTemplate.postForEntity(COMFY_URL, requestEntity, String.class);

                results.add(" !!!!!업로드 성공!!!!!: " + s3Url + " | ngrok 응답: " + response.getBody());

            } catch (IOException e) {
                results.add("XXXXXXXXXXX " + image.getOriginalFilename() + " 업로드 실패: " + e.getMessage()+ " XXXXXXXXXXX");
            }
        }
        return results;
    }
}
