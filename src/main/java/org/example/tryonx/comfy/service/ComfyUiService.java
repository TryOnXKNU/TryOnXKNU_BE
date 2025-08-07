package org.example.tryonx.comfy.service;

import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Service
public class ComfyUiService {

    private final RestTemplate restTemplate;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    public ComfyUiService(RestTemplateBuilder builder, ProductImageRepository productImageRepository, ProductRepository productRepository, MemberRepository memberRepository) {
        this.restTemplate = builder.build();
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }

    @Value("${ngrok.url}")
    private String baseUrl;

    public String executeInternalWorkflow() throws IOException, InterruptedException {
        String workflowJson = loadWorkflowFromResource("tryon_flow.json");

        // 1. 워크플로우 실행
        String promptId = sendWorkflow(workflowJson);

        // 2. 완료 대기
        waitUntilComplete(promptId);

        // 3. 이미지명 추출
        String filename = getGeneratedOutputImageFilename(promptId);

        // 4. 이미지 다운로드
        downloadImage(filename);

        return filename;
    }

    public String executeFittingFlow(String email, Integer productid) throws IOException, InterruptedException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        Product product = productRepository.findById(productid)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        String imgName = productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl();
        String prefix = "/upload/product/";
        String fileNameOnly = imgName.startsWith(prefix)
                ? imgName.substring(prefix.length())
                : imgName;

//        BodyShape bodyShape;
//        if(member.getBodyType() == 0)
//            bodyShape = BodyShape.STRAIGHT;
//        else if(member.getBodyType() == 1)
//            bodyShape = BodyShape.NATURAL;
//        else if(member.getBodyType() == 2)
//            bodyShape = BodyShape.WAVE;
//        else
//            throw new RuntimeException();


        String workflowJson = loadWorkflowFromResource("tryon_flow.json")
                .replace("{{imageName}}", fileNameOnly);

        // 1. 워크플로우 실행
        String promptId = sendWorkflow(workflowJson);

        // 2. 완료 대기
        waitUntilComplete(promptId);

        // 3. 이미지명 추출
        String filename = getGeneratedOutputImageFilename(promptId);

        // 4. 이미지 다운로드
        downloadImage(filename);

        return filename;
    }

    private String loadWorkflowFromResource(String filename) throws IOException {
        Resource resource = new ClassPathResource("templates/workflows/" + filename);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String sendWorkflow(String workflowJson) {
        String url = baseUrl + "/prompt";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(workflowJson, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Object promptId = response.getBody().get("prompt_id");
        if (promptId == null) {
            throw new RuntimeException(" ComfyUI에서 prompt_id를 받지 못했습니다.");
        }

        return promptId.toString();
    }

    private void waitUntilComplete(String promptId) throws InterruptedException {
        String url = baseUrl + "/history/" + promptId;
        int retryCount = 0, maxRetries = 600;

        while (retryCount < maxRetries) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<?, ?> body = response.getBody();

                if (body == null || !body.containsKey(promptId)) {
                    System.out.println(" [대기 중] ComfyUI 이미지 생성 중..[요청 -" + (retryCount + 1) + "]");
                    Thread.sleep(1000);
                    retryCount++;
                    continue;
                }

                Map<?, ?> promptData = (Map<?, ?>) body.get(promptId);
                Map<?, ?> status = (Map<?, ?>) promptData.get("status");
                Boolean completed = (Boolean) status.get("completed");

                if (Boolean.TRUE.equals(completed)) {
                    System.out.println("✅ ComfyUI 이미지 생성 완료!");
                    return;
                }

            } catch (Exception e) {
                System.out.println(" [에러 발생] " + e.getMessage() + " → 재시도 " + (retryCount + 1));
            }

            Thread.sleep(1000);
            retryCount++;
        }

        throw new RuntimeException(" ComfyUI 작업이 완료되지 않거나 prompt_id가 유효하지 않습니다.");
    }

    private String getGeneratedOutputImageFilename(String promptId) {
        String url = baseUrl + "/history/" + promptId;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<?, ?> promptData = (Map<?, ?>) response.getBody().get(promptId);

        if (promptData == null || !promptData.containsKey("outputs")) {
            throw new RuntimeException(" prompt_id에 대한 출력 결과가 존재하지 않습니다.");
        }

        Map<?, ?> outputs = (Map<?, ?>) promptData.get("outputs");

        for (Object outputNode : outputs.values()) {
            Map<?, ?> output = (Map<?, ?>) outputNode;
            if (!output.containsKey("images")) continue;

            List<?> images = (List<?>) output.get("images");
            for (Object imgObj : images) {
                Map<?, ?> image = (Map<?, ?>) imgObj;
                String type = image.get("type").toString();
                String filename = image.get("filename").toString();

                if ("output".equalsIgnoreCase(type) && filename != null && !filename.isBlank()) {
                    return filename;
                }
            }
        }

        throw new RuntimeException(" 출력용 이미지(type: output)가 없습니다.");
    }

    private void downloadImage(String filename) throws IOException, InterruptedException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(" 이미지 파일 이름이 null이거나 비어 있습니다.");
        }

        String url = baseUrl + "/view?filename=" + filename;
        int maxRetries = 10;

        // uploads 디렉토리 경로
        Path uploadPath = Paths.get("upload/fitting");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath); // 디렉토리 없으면 생성
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] imageData = restTemplate.getForObject(url, byte[].class);
                if (imageData != null && imageData.length > 0) {
                    Path outputPath = uploadPath.resolve("downloaded_" + filename);
                    Files.write(outputPath, imageData);
                    System.out.println("✅ 이미지 저장 완료: " + outputPath);
                    return;
                }
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println(" [대기 중] 이미지가 아직 준비되지 않음. 재시도 " + attempt);
            }

            Thread.sleep(1000);
        }

        throw new IOException(" 이미지 다운로드 실패: " + filename);
    }

    public String executeFittingFlowWithClothingName(String email, String clothingImageName) throws IOException, InterruptedException {
        // 1. 로그인된 사용자 정보 확인
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 2. 이미지 이름이 경로 접두어를 포함하는 경우 제거
        String prefix = "/upload/product/";
        String fileNameOnly = clothingImageName.startsWith(prefix)
                ? clothingImageName.substring(prefix.length())
                : clothingImageName;

        // 3. 워크플로우 JSON 불러와서 옷 이미지 파일명 치환
        String workflowJson = loadWorkflowFromResource("tryon_flow.json")
                .replace("{{imageName}}", fileNameOnly);

        // 4. ComfyUI에 워크플로우 전송 및 실행 대기
        String promptId = sendWorkflow(workflowJson);
        waitUntilComplete(promptId);

        // 5. 결과 이미지 추출 및 저장
        String filename = getGeneratedOutputImageFilename(promptId);
        downloadImage(filename);

        return filename;
    }


}
