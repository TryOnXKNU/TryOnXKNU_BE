package org.example.tryonx.comfy.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.fitting.domain.ProductFitting;
import org.example.tryonx.fitting.repository.ProductFittingRepository;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.ProductImageRepository;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ComfyUiFittingService {
    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final ProductFittingRepository productFittingRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductImageRepository productImageRepository;
    private final AmazonS3 amazonS3;

    @Value("${ngrok.url}")
    private String baseUrl;

    @Value("${app.upload.dir:upload}")
    private String uploadRoot;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

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

    /**
     * 피팅 생성 후 S3 업로드 저장 버전
     */

    public void executeFittingFlowWithClothingNameThreeImages(
            String email, String clothingImageName, Product product
    ) throws IOException, InterruptedException {

        memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        refreshGoogleDrive();

        // '/upload/product/' 접두어 제거
        String prefix = "/upload/product/";
        String fileNameOnly = clothingImageName.startsWith(prefix)
                ? clothingImageName.substring(prefix.length())
                : clothingImageName;

        // 카테고리명과 체형 불러오기
        String categoryName = product.getCategory().getName();   // ex) STOP, SBOTTOM
        String bodyShape = product.getBodyShape().name();        // STRAIGHT, NATURAL, WAVE

        // 체형 → 번호 매핑
        int bodyShapeNum = switch (bodyShape) {
            case "STRAIGHT" -> 1;
            case "NATURAL"  -> 2;
            case "WAVE"     -> 3;
            default -> throw new RuntimeException("지원하지 않는 체형: " + bodyShape);
        };

        // 카테고리별 프롬프트 매핑
        Map<String, String> promptMap = Map.ofEntries(
                Map.entry("STOP", "short t-shirts"),
                Map.entry("LSTOP", "long sleeve"),
                Map.entry("LWTOP", "long t-shirts"),
                Map.entry("SBOTTOM", "short pants"),
                Map.entry("LSBOTTOM", "long pants"),
                Map.entry("LWBOTTOM", "long wide pants"),
                Map.entry("SOUTERWEAR", "cardigan"),
                Map.entry("LOUTERWEAR", "cardigan"),
                Map.entry("SSDRESS", "short sleeve short dress"),
                Map.entry("SLDRESS", "short sleeve long dress"),
                Map.entry("LSDRESS", "long sleeve shore dress"),
                Map.entry("LLDRESS", "long sleeve long dress")
        );

        String clothingPrompt = promptMap.get(categoryName);
        if (clothingPrompt == null) {
            throw new RuntimeException("지원하지 않는 카테고리: " + categoryName);
        }

        // 상품 대표 이미지
        ProductImage thumbnail = productImageRepository
                .findFirstByProductAndIsThumbnailTrue(product)
                .orElseThrow(() -> new RuntimeException("대표 이미지가 없습니다."));

        // 상품 보조 이미지
        ProductImage subImage = productImageRepository
                .findFirstByProductAndIsThumbnailFalse(product)
                .orElse(thumbnail); // 없으면 대표 이미지로 대체

        // 파일명만 추출 (prefix 제거)
        String clothFront = Paths.get(thumbnail.getImageUrl()).getFileName().toString();
        String clothBack  = Paths.get(subImage.getImageUrl()).getFileName().toString();

        // 모델 이미지명 생성
        String model1 = bodyShapeNum + categoryName + "a.png";
        String model2 = bodyShapeNum + categoryName + "b.png";
        String model3 = bodyShapeNum + categoryName + "c.png";

        // 워크플로 JSON 치환
        String workflowJson = loadWorkflowFromResource("templates/workflows/v2_admin_fitting.json")
                .replace("{{clothingPrompt}}", clothingPrompt)
                .replace("{{model1}}", model1)
                .replace("{{model2}}", model2)
                .replace("{{model3}}", model3)
                .replace("{{clothFront}}", clothFront)   // a, c 매핑용 (파일명만)
                .replace("{{clothBack}}", clothBack);    // b 매핑용 (파일명만)


        String promptId = sendWorkflow(workflowJson);
        waitUntilComplete(promptId);

        // 출력 prefix 3개
        List<String> prefixes = List.of("TRYONX_A", "TRYONX_B", "TRYONX_C");
        Map<String, List<String>> imagesByPrefix = getGeneratedOutputImageFilenamesByPrefix(promptId, prefixes);

        // 각 프리픽스에서 1장씩 꺼내기
        List<String> originals = new ArrayList<>(3);
        for (String pfx : prefixes) {
            List<String> list = imagesByPrefix.getOrDefault(pfx, Collections.emptyList());
//            if (list.isEmpty()) {
//                throw new RuntimeException("SaveImage 출력이 없습니다. prefix=" + pfx + " | 전체=" + imagesByPrefix);
//            }
            if (list.isEmpty()) throw new RuntimeException("출력 없음: " + pfx);
            originals.add(list.get(0));
            //System.out.println("탐지된 출력(" + pfx + "): " + list.get(0));
        }

        // 중복 방지 체크
        if (new HashSet<>(originals).size() != originals.size()) {
            throw new RuntimeException("세 출력 이미지 중 중복 발생: " + originals);
        }

        // 저장 파일명
        List<String> finals = List.of(
                "A_" + originals.get(0),
                "B_" + originals.get(1),
                "C_" + originals.get(2)
        );

//        for (int i = 0; i < 3; i++) {
//            downloadImageAs(originals.get(i), finals.get(i));
//        }

//        // DB 저장
//        saveFixedSequences3(product,
//                "/upload/fitting/" + finals.get(0),
//                "/upload/fitting/" + finals.get(1),
//                "/upload/fitting/" + finals.get(2));
//
//        System.out.println("저장 완료: " + finals);

        // S3 업로드
        List<String> fittingUrls = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String s3Url = downloadImageAndUploadToS3(originals.get(i), finals.get(i), product.getProductCode());
            fittingUrls.add(s3Url);
        }

        // DB 저장 시 S3 URL 저장
        saveFixedSequences3(product, fittingUrls.get(0), fittingUrls.get(1), fittingUrls.get(2));

        System.out.println("S3 저장 완료: " + fittingUrls);
    }


    private String loadWorkflowFromResource(String path) throws IOException {
        Resource resource = new ClassPathResource(path);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String sendWorkflow(String workflowJson) {
        String url = baseUrl + "/prompt";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> root = objectMapper.readValue(workflowJson, new TypeReference<>() {});
            Object promptObj = root.getOrDefault("prompt", root);
            if (!(promptObj instanceof Map)) throw new RuntimeException("prompt 구조 오류");

            @SuppressWarnings("unchecked")
            Map<String, Object> prompt = (Map<String, Object>) promptObj;

            Map<String, Object> payload = Map.of(
                    "prompt", sanitizePrompt(prompt),
                    "client_id", UUID.randomUUID().toString()
            );
            String finalJson = objectMapper.writeValueAsString(payload);

            ResponseEntity<Map> res =
                    restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(finalJson, headers), Map.class);

            Object promptId = res.getBody() != null ? res.getBody().get("prompt_id") : null;
            if (promptId == null) throw new RuntimeException("prompt_id 수신 실패");

            System.out.println("prompt_id: " + promptId);
            return promptId.toString();
        } catch (Exception e) {
            throw new RuntimeException("워크플로 전송 실패: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> sanitizePrompt(Map<String, Object> prompt) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<String, Object> e : prompt.entrySet()) {
            String nodeId = e.getKey();
            Object nodeVal = e.getValue();
            if (nodeId.startsWith("#")) continue;
            if (!(nodeVal instanceof Map)) throw new RuntimeException("노드가 객체가 아님: " + nodeId);

            Map<String, Object> node = (Map<String, Object>) nodeVal;
            Object cls = node.get("class_type");
            if (cls == null || String.valueOf(cls).isBlank())
                throw new RuntimeException("class_type 누락: " + nodeId);

            out.put(nodeId, node);
        }
        if (out.isEmpty()) throw new RuntimeException("정상화 결과 노드가 비었습니다.");
        return out;
    }

    private void waitUntilComplete(String promptId) throws InterruptedException {
        String url = baseUrl + "/history/" + promptId;
        int retry = 0, max = 600;
        Instant start = Instant.now();

        while (retry++ < max) {
            try {
                ResponseEntity<Map> res = restTemplate.getForEntity(url, Map.class);
                Map<?, ?> body = res.getBody();
                if (body != null && body.containsKey(promptId)) {
                    Map<?, ?> promptData = (Map<?, ?>) body.get(promptId);
                    Map<?, ?> status = (Map<?, ?>) promptData.get("status");
                    if (status != null && Boolean.TRUE.equals(status.get("completed"))) {
                        Duration d = Duration.between(start, Instant.now());
                        System.out.println("완료! 소요 " + d.toSeconds() + "s");
                        return;
                    }
                }
            } catch (Exception ignored) {}
            Thread.sleep(1000);
        }
        throw new RuntimeException("ComfyUI 작업 타임아웃: " + promptId);
    }

    private Map<String, List<String>> getGeneratedOutputImageFilenamesByPrefix(String promptId, List<String> prefixes) {
        String url = baseUrl + "/history/" + promptId;
        ResponseEntity<Map> res = restTemplate.getForEntity(url, Map.class);
        Map<?, ?> body = res.getBody();
        Map<?, ?> promptData = (Map<?, ?>) (body != null ? body.get(promptId) : null);
        if (promptData == null || !promptData.containsKey("outputs"))
            throw new RuntimeException("출력 결과가 없습니다. promptId=" + promptId);

        Map<String, List<String>> result = new HashMap<>();
        Map<?, ?> outputs = (Map<?, ?>) promptData.get("outputs");

        outputs.forEach((nodeId, nodeOut) -> {
            Map<?, ?> out = (Map<?, ?>) nodeOut;
            if (!out.containsKey("images")) return;

            List<?> images = (List<?>) out.get("images");
            for (Object imgObj : images) {
                Map<?, ?> image = (Map<?, ?>) imgObj;
                String type = String.valueOf(image.get("type"));
                String filename = String.valueOf(image.get("filename"));
                if ("output".equalsIgnoreCase(type) && filename != null && !filename.isBlank()) {
                    for (String pfx : prefixes) {
                        if (filename.startsWith(pfx)) {
                            result.computeIfAbsent(pfx, k -> new ArrayList<>()).add(filename);
                        }
                    }
                }
            }
        });

        return result;
    }

    private Path ensureFittingDir() throws IOException {
        Path base = Paths.get(uploadRoot).toAbsolutePath();
        Path fitting = base.resolve("fitting");
        if (!Files.exists(fitting)) Files.createDirectories(fitting);
        return fitting;
    }

//    private void downloadImageAs(String originalFilename, String finalFilename)
//            throws IOException, InterruptedException {
//        if (originalFilename == null || originalFilename.isBlank())
//            throw new IllegalArgumentException("원본 이미지 파일 이름이 비어 있습니다.");
//
//        String url = baseUrl + "/view?filename=" + originalFilename;
//        int maxRetries = 15;
//        Path outputPath = ensureFittingDir().resolve(finalFilename);
//
//        for (int attempt = 1; attempt <= maxRetries; attempt++) {
//            try {
//                byte[] imageData = restTemplate.getForObject(url, byte[].class);
//                if (imageData != null && imageData.length > 0) {
//                    Files.write(outputPath, imageData,
//                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
//                    System.out.println("저장됨: " + outputPath);
//                    return;
//                }
//            } catch (HttpClientErrorException.NotFound e) {
//                System.out.println("이미지 준비중(" + attempt + "/" + maxRetries + "): " + originalFilename);
//            }
//            Thread.sleep(800);
//        }
//        throw new IOException("이미지 다운로드 실패: " + originalFilename + " → " + outputPath);
//    }

    /**
     * S3 업로드 버전으로 변경된 downloadImageAs()
     */
    private String downloadImageAndUploadToS3(String originalFilename, String finalFilename, String productCode)
            throws IOException, InterruptedException {

        String url = baseUrl + "/view?filename=" + originalFilename;
        int maxRetries = 15;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] imageData = restTemplate.getForObject(url, byte[].class);
                if (imageData != null && imageData.length > 0) {

                    // S3 경로 지정: fitting/{productCode}/{finalFilename}
                    String key = "fitting/" + productCode + "/" + finalFilename;

                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(imageData.length);
                    metadata.setContentType("image/png");

                    amazonS3.putObject(bucketName, key, new ByteArrayInputStream(imageData), metadata);
                    String imageUrl = amazonS3.getUrl(bucketName, key).toString();

                    System.out.println("S3 업로드 완료: " + imageUrl);
                    return imageUrl;
                }
            } catch (Exception e) {
                System.out.println("이미지 준비중(" + attempt + "/" + maxRetries + "): " + originalFilename);
            }
            Thread.sleep(800);
        }

        throw new IOException("이미지 다운로드 실패: " + originalFilename);
    }

    @Transactional
    public void saveFixedSequences3(Product product, String urlA, String urlB, String urlC) {
        upsert(product, 1, urlA);
        upsert(product, 2, urlB);
        upsert(product, 3, urlC);
    }

    private void upsert(Product product, int seq, String url) {
        ProductFitting pf = productFittingRepository.findByProductAndSequence(product, seq)
                .orElseGet(ProductFitting::new);
        pf.setProduct(product);
        pf.setSequence(seq);
        pf.setFittingImageUrl(url);
        productFittingRepository.save(pf);
    }

    public List<String> getFittingImageUrls(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("해당 상품이 없습니다 : " + productId));
        List<String> fittingImageUrls = productFittingRepository.findByProductOrderBySequenceAsc(product)
                .stream()
                .map(ProductFitting::getFittingImageUrl)
                .toList();
        return fittingImageUrls;
    }
}
