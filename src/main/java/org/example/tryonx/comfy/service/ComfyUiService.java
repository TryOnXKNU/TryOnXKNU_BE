package org.example.tryonx.comfy.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.fitting.domain.ProductFitting;
import org.example.tryonx.fitting.repository.ProductFittingRepository;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ComfyUiService {

    private final RestTemplate restTemplate;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ProductFittingRepository productFittingRepository;

    @Value("${ngrok.url}")
    private String baseUrl;

    private void refreshGoogleDrive() {
        String url = baseUrl + "/pysssss/drive/sync";
        System.out.println("구글드라이브 새로고침중");
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
            System.out.println("googledrive새로고침.");
        } catch (RestClientException e) {
            // 실패하더라도 워크플로우는 계속 진행하도록 오류만 로그에 남깁니다.
            System.err.println("구글드라이브 새로고침 실패 :  " + e.getMessage());
        }
    }

    public String executeInternalWorkflow() throws IOException, InterruptedException {
        String workflowJson = loadWorkflowFromResource("tryon_flow.json");
        // Google Drive 새로고침
        refreshGoogleDrive();

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

        BodyShape memberBodyShape = member.getBodyShape();

        String model = switch (memberBodyShape) {
            case STRAIGHT -> "straight.png";
            case WAVE -> "wave.png";
            case NATURAL -> "natural.png";
        };

        Product product = productRepository.findById(productid)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String prompt = switch (product.getCategory().getCategoryId()){
            case 1 -> "black tshirt";
            case 2 -> "pants";
            case 3 -> "dress";
            case 4 -> "black ";
            default -> "clothes";
        };

        String imgName = productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl();
        String prefix = "/upload/product/";
        String fileNameOnly = imgName.startsWith(prefix)
                ? imgName.substring(prefix.length())
                : imgName;


        String workflowJson = loadWorkflowFromResource("v2_one_person_one_clothes.json")
                .replace("{{imageName}}", fileNameOnly)
                .replace("{{modelImage}}", model)
                .replace("{{prompt}}", prompt);

        // Google Drive 새로고침
        refreshGoogleDrive();

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

    public String executeFittingTwoClothesFlow(String email, Integer productId1, Integer productId2) throws IOException, InterruptedException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        BodyShape memberBodyShape = member.getBodyShape();
        String model = switch (memberBodyShape) {
            case STRAIGHT -> "straight.png";
            case WAVE -> "wave.png";
            case NATURAL -> "natural.png";
        };

        Product product1 = productRepository.findById(productId1)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        Product product2 = productRepository.findById(productId2)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        String imgName1 = productImageRepository.findByProductAndIsThumbnailTrue(product1)
                .orElseThrow(() -> new RuntimeException("Thumbnail not found for product1"))
                .getImageUrl();
        String imgName2 = productImageRepository.findByProductAndIsThumbnailTrue(product2)
                .orElseThrow(() -> new RuntimeException("Thumbnail not found for product2"))
                .getImageUrl();

        String fileNameOnly1 = stripPrefix(imgName1, "/upload/product/");
        String fileNameOnly2 = stripPrefix(imgName2, "/upload/product/");

        String imageName1 = null;
        String imageName2 = null;


        if (product1.getCategory().getCategoryId() == 1) {
            imageName1 = fileNameOnly1;
        } else if (product1.getCategory().getCategoryId() == 2) {
            imageName2 = fileNameOnly1;
        }

        if (product2.getCategory().getCategoryId() == 1) {
            imageName1 = fileNameOnly2;
        } else if (product2.getCategory().getCategoryId() == 2) {
            imageName2 = fileNameOnly2;
        }

        // 워크플로우 JSON 생성
        String workflowJson = loadWorkflowFromResource("v2_one_person_two_clothes.json")
                .replace("{{modelImage}}", model)
                .replace("{{imageName1}}", imageName1 != null ? imageName1 : "")
                .replace("{{imageName2}}", imageName2 != null ? imageName2 : "");

        // Google Drive 새로고침
        refreshGoogleDrive();

        // 1. 워크플로우 실행
        String promptId = sendWorkflow(workflowJson);

        // 2. 완료 대기
        waitUntilComplete(promptId);

        // 3. 이미지명 추출
        List<String> generatedOutputImageFilenameList = getGeneratedOutputImageFilenameList(promptId);

        // 4. 이미지 다운로드
        String fileName = generatedOutputImageFilenameList.get(0);
        downloadImage(fileName);

        return fileName;
    }


    private String stripPrefix(String fileName, String prefix) {
        return fileName.startsWith(prefix) ? fileName.substring(prefix.length()) : fileName;
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

    private List<String> getGeneratedOutputImageFilenameList(String promptId) {
        String url = baseUrl + "/history/" + promptId;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<?, ?> promptData = (Map<?, ?>) response.getBody().get(promptId);

        if (promptData == null || !promptData.containsKey("outputs")) {
            throw new RuntimeException("prompt_id에 대한 출력 결과가 존재하지 않습니다.");
        }

        Map<?, ?> outputs = (Map<?, ?>) promptData.get("outputs");
        List<String> filenames = new ArrayList<>();

        for (Object outputNode : outputs.values()) {
            Map<?, ?> output = (Map<?, ?>) outputNode;
            if (!output.containsKey("images")) continue;

            List<?> images = (List<?>) output.get("images");
            for (Object imgObj : images) {
                Map<?, ?> image = (Map<?, ?>) imgObj;
                String type = image.get("type").toString();
                String filename = image.get("filename").toString();

                if ("output".equalsIgnoreCase(type) && filename != null && !filename.isBlank()) {
                    filenames.add(filename);
                }
            }
        }
        return filenames;
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

    // 상품사진파일명으로 한장 생성
//    public String executeFittingFlowWithClothingName(String email, String clothingImageName) throws IOException, InterruptedException {
//        // 1. 로그인된 사용자 정보 확인
//        Member member = memberRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("Member not found"));
//
//        // 2. 이미지 이름이 경로 접두어를 포함하는 경우 제거
//        String prefix = "/upload/product/";
//        String fileNameOnly = clothingImageName.startsWith(prefix)
//                ? clothingImageName.substring(prefix.length())
//                : clothingImageName;
//
//        // 3. 워크플로우 JSON 불러와서 옷 이미지 파일명 치환
//        String workflowJson = loadWorkflowFromResource("tryon_flow.json")
//                .replace("{{imageName}}", fileNameOnly);
//
//        // 4. ComfyUI에 워크플로우 전송 및 실행 대기
//        String promptId = sendWorkflow(workflowJson);
//        waitUntilComplete(promptId);
//
//        // 5. 결과 이미지 추출 및 저장
//        String filename = getGeneratedOutputImageFilename(promptId);
//        downloadImage(filename);
//
//        return filename;
//    }

    public void executeFittingFlowWithClothingName(String email, String clothingImageName, Product product) throws IOException, InterruptedException {
        // 1. 사용자 확인
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // 2. 파일명 전처리
        String prefix = "/upload/product/";
        String fileNameOnly = clothingImageName.startsWith(prefix)
                ? clothingImageName.substring(prefix.length())
                : clothingImageName;

        // 3. 워크플로우 JSON 설정
        String workflowJson = loadWorkflowFromResource("tryon_flow.json")
                .replace("{{imageName}}", fileNameOnly);

        // Google Drive 새로고침
        refreshGoogleDrive();

        // 4. ComfyUI 실행
        String promptId = sendWorkflow(workflowJson);
        waitUntilComplete(promptId);

        // 5. 결과 이미지 원래 이름 → 다운로드용 이름
        String originalFilename = getGeneratedOutputImageFilename(promptId);
        String finalFilename = "downloaded_" + originalFilename;

//        // 6. 이미지 저장
//        downloadImageAs(originalFilename, finalFilename);
//
//        // 7. DB 저장
//        ProductFitting fitting = new ProductFitting();
//        fitting.setProduct(product);
//        fitting.setSequence(1);
//        fitting.setFittingImageUrl("/upload/fitting/" + finalFilename);  // 저장된 경로 반영
//        productFittingRepository.save(fitting);

        // 6. 이미지 저장
        downloadImageAs(originalFilename, finalFilename);
        String publicUrl = "/upload/fitting/" + finalFilename;

        // 7. DB 저장 (한 장일 때)
        saveOrRotateFittings(product, List.of(publicUrl));
//        saveOrRotateFittings(product, List.of(publicUrl1, publicUrl2));

    }
    private void downloadImageAs(String originalFilename, String finalFilename) throws IOException, InterruptedException {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("원본 이미지 파일 이름이 null이거나 비어 있습니다.");
        }

        String url = baseUrl + "/view?filename=" + originalFilename;
        int maxRetries = 10;

        Path uploadPath = Paths.get("upload/fitting");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] imageData = restTemplate.getForObject(url, byte[].class);
                if (imageData != null && imageData.length > 0) {
                    Path outputPath = uploadPath.resolve(finalFilename);
                    Files.write(outputPath, imageData);
                    System.out.println("✅ 이미지 저장 완료: " + outputPath);
                    return;
                }
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println(" [대기 중] 이미지가 아직 준비되지 않음. 재시도 " + attempt);
            }

            Thread.sleep(1000);
        }

        throw new IOException("이미지 다운로드 실패: " + originalFilename);
    }

    @Transactional
    public void saveOrRotateFittings(Product product, List<String> urls) {
        if (urls == null || urls.isEmpty()) return;

        // 현재 슬롯 상황
        List<ProductFitting> current = productFittingRepository.findByProductOrderByUpdatedAtAsc(product);
        boolean hasSeq1 = current.stream().anyMatch(p -> p.getSequence() == 1);
        boolean hasSeq2 = current.stream().anyMatch(p -> p.getSequence() == 2);

        for (String url : urls) {
            if (current.size() < 2) {
                // 빈 슬롯에 채우기
                int targetSeq = !hasSeq1 ? 1 : (!hasSeq2 ? 2 : 1); // 이 경우 1,2 중 비어있는 쪽
                ProductFitting pf = productFittingRepository.findByProductAndSequence(product, targetSeq)
                        .orElseGet(ProductFitting::new);
                pf.setProduct(product);
                pf.setSequence(targetSeq);
                pf.setFittingImageUrl(url);
                productFittingRepository.save(pf);

                if (targetSeq == 1) hasSeq1 = true; else hasSeq2 = true;
                // 리스트 최신화
                if (current.stream().noneMatch(p -> p.getSequence() == targetSeq)) {
                    current.add(pf);
                }
            } else {
                ProductFitting oldest = current.get(0);
                oldest.setFittingImageUrl(url); // @PreUpdate로 updatedAt 갱신
                productFittingRepository.save(oldest);

                current = productFittingRepository.findByProductOrderByUpdatedAtAsc(product);
            }
        }
    }

}
