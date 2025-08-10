package org.example.tryonx.comfy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.fitting.domain.ProductFitting;
import org.example.tryonx.fitting.repository.ProductFittingRepository;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.product.domain.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
public class ComfyUiDualService {
    private final RestTemplate restTemplate;
    private final MemberRepository memberRepository;
    private final ProductFittingRepository productFittingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ngrok.url}")
    private String baseUrl;

    // 절대경로 권장: application.yml에 app.upload.dir: /var/www/tryonx/upload 처럼 지정
    @Value("${app.upload.dir:upload}")
    private String uploadRoot;

    public void executeFittingFlowWithClothingNameTwoImages(String email, String clothingImageName, Product product)
            throws IOException, InterruptedException {

        memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        String prefix = "/upload/product/";
        String fileNameOnly = clothingImageName.startsWith(prefix)
                ? clothingImageName.substring(prefix.length())
                : clothingImageName;

        String workflowJson = loadWorkflowFromResource("tryon_flow_dual.json")
                .replace("{{imageName}}", fileNameOnly);

        String promptId = sendWorkflow(workflowJson);
        waitUntilComplete(promptId); // 콘솔에 진행상황 실시간 출력됨

        Map<String, String> out = getGeneratedOutputImageFilenamesByPrefix(
                promptId, List.of("ComfyUI_A", "ComfyUI_B")
        );
        String originalA = out.get("ComfyUI_A");
        String originalB = out.get("ComfyUI_B");

        if (originalA == null || originalB == null) {
            throw new RuntimeException("두 출력 이미지 중 하나 이상을 찾지 못했습니다. out=" + out);
        }

        if (Objects.equals(originalA, originalB)) {
            System.out.println("[경고] 두 출력 파일명이 동일합니다: " + originalA +
                    " (워크플로 분기/SaveImage prefix/마스크 연결 확인 필요)");
        }

        String finalA = "A_" + originalA;
        String finalB = "B_" + originalB;
        System.out.println("다운로드 준비: " + finalA + " / " + finalB);

        downloadImageAs(originalA, finalA);
        downloadImageAs(originalB, finalB);

        String urlA = "/upload/fitting/" + finalA;
        String urlB = "/upload/fitting/" + finalB;
        System.out.println("저장 완료 URL: " + urlA + " / " + urlB);

        saveFixedSequences(product, urlA, urlB);
    }

    private String loadWorkflowFromResource(String filename) throws IOException {
        Resource resource = new ClassPathResource("templates/workflows/" + filename);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String sendWorkflow(String workflowJson) {
        String url = baseUrl + "/prompt";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // 1) 원본 JSON 파싱
            Map<String, Object> root = objectMapper.readValue(workflowJson, new TypeReference<>() {});
            // 2) 최상위에 prompt가 있으면 그걸 쓰고, 없으면 root 자체가 prompt라고 간주
            Object promptObj = root.getOrDefault("prompt", root);

            if (!(promptObj instanceof Map)) {
                throw new RuntimeException("prompt 구조가 올바르지 않습니다.");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> prompt = (Map<String, Object>) promptObj;

            // 3) 주석/메타 키(#로 시작) 제거 + 각 노드 class_type 검증
            Map<String, Object> sanitized = sanitizePrompt(prompt);

            // 4) 최종 payload 구성 (client_id는 선택이지만 넣어두면 좋음)
            Map<String, Object> payload = preparePayload(sanitized);

            String finalJson = objectMapper.writeValueAsString(payload);

            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(finalJson, headers), Map.class);

            Object promptId = response.getBody() != null ? response.getBody().get("prompt_id") : null;
            if (promptId == null) throw new RuntimeException("ComfyUI에서 prompt_id를 받지 못했습니다.");
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

            // '#...' 로 시작하는 키는 ComfyUI가 노드로 해석하므로 제거
            if (nodeId.startsWith("#")) {
                System.out.println("제거: 주석/메타 키 " + nodeId);
                continue;
            }
            if (!(nodeVal instanceof Map)) {
                throw new RuntimeException("노드가 객체가 아님: " + nodeId);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> node = (Map<String, Object>) nodeVal;

            // class_type 필수
            Object cls = node.get("class_type");
            if (cls == null || String.valueOf(cls).isBlank()) {
                throw new RuntimeException("class_type 누락: node " + nodeId);
            }
            out.put(nodeId, node);
        }
        if (out.isEmpty()) throw new RuntimeException("정상화 결과 노드가 비었습니다.");
        return out;
    }

    private Map<String, Object> preparePayload(Map<String, Object> prompt) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", prompt);
        payload.put("client_id", UUID.randomUUID().toString());
        return payload;
    }

    // 진행 상황 실시간 출력: 1초마다 status를 읽고 스피너/상태/대기열/현재 노드 등을 표시
    private void waitUntilComplete(String promptId) throws InterruptedException {
        String url = baseUrl + "/history/" + promptId;
        int retry = 0, max = 600;
        char[] spinner = new char[]{'|', '/', '-', '\\'};
        Instant start = Instant.now();

        while (retry < max) {
            try {
                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<?, ?> body = response.getBody();

                if (body == null || !body.containsKey(promptId)) {
                    printProgress(spinner, retry, start, "대기 중 (history 없음)");
                } else {
                    Map<?, ?> promptData = (Map<?, ?>) body.get(promptId);
                    Map<?, ?> status = (Map<?, ?>) promptData.get("status");
                    Boolean completed = status != null ? (Boolean) status.get("completed") : null;

                    String statusStr = status != null && status.get("status_str") != null
                            ? String.valueOf(status.get("status_str")) : "";
                    Object queueRem = status != null ? status.get("queue_remaining") : null;
                    String queueInfo = queueRem != null ? " | queue:" + queueRem : "";

                    // 현재 실행 중 노드 정보가 있으면 같이 출력 (있을 수도, 없을 수도 있음)
                    String currentNodeInfo = "";
                    Object currentNode = status != null ? status.get("current_node") : null;
                    if (currentNode != null) currentNodeInfo = " | node:" + currentNode;

                    if (Boolean.TRUE.equals(completed)) {
                        Duration d = Duration.between(start, Instant.now());
                        System.out.printf("\r완료! 소요 %.1fs%s%s%-40s%n",
                                d.toMillis() / 1000.0, queueInfo, currentNodeInfo, "");
                        return;
                    } else {
                        String msg = (statusStr == null || statusStr.isBlank()) ? "실행 중" : statusStr;
                        printProgress(spinner, retry, start, msg + queueInfo + currentNodeInfo);
                    }
                }
            } catch (Exception e) {
                printProgress(spinner, retry, start, "에러 재시도: " + e.getClass().getSimpleName());
            }

            Thread.sleep(1000);
            retry++;
        }
        throw new RuntimeException("ComfyUI 작업이 시간 내 완료되지 않았습니다. prompt_id=" + promptId);
    }

    private void printProgress(char[] spinner, int retry, Instant start, String extra) {
        char s = spinner[retry % spinner.length];
        Duration d = Duration.between(start, Instant.now());
        System.out.printf("\r[%c] %.1fs %s%-40s", s, d.toMillis() / 1000.0, extra == null ? "" : extra, "");
    }

    private Map<String, String> getGeneratedOutputImageFilenamesByPrefix(String promptId, List<String> prefixes) {
        String url = baseUrl + "/history/" + promptId;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<?, ?> body = response.getBody();
        Map<?, ?> promptData = (Map<?, ?>) (body != null ? body.get(promptId) : null);
        if (promptData == null || !promptData.containsKey("outputs")) {
            throw new RuntimeException("출력 결과가 없습니다. promptId=" + promptId);
        }

        Map<?, ?> outputs = (Map<?, ?>) promptData.get("outputs");
        Map<String, String> result = new HashMap<>();

        outputs.forEach((nodeId, nodeOut) -> {
            Map<?, ?> out = (Map<?, ?>) nodeOut;
            if (!out.containsKey("images")) return;
            List<?> images = (List<?>) out.get("images");
            for (Object imgObj : images) {
                Map<?, ?> image = (Map<?, ?>) imgObj;
                String type = String.valueOf(image.get("type"));
                String filename = String.valueOf(image.get("filename"));
                if (!"output".equalsIgnoreCase(type) || filename == null) continue;

                prefixes.forEach(pfx -> {
                    if (filename.startsWith(pfx + "_") && !result.containsKey(pfx)) {
                        result.put(pfx, filename);
                        System.out.println("탐지된 출력(" + pfx + "): " + filename + " [node=" + nodeId + "]");
                    }
                });
            }
        });

        for (String p : prefixes) {
            if (!result.containsKey(p)) {
                throw new RuntimeException("필요한 출력이 누락됨. prefix=" + p + ", found=" + result);
            }
        }
        if (new HashSet<>(result.values()).size() != result.size()) {
            throw new RuntimeException("두 출력 파일명이 동일합니다: " + result);
        }
        return result;
    }

    private Path ensureFittingDir() throws IOException {
        Path base = Paths.get(uploadRoot).toAbsolutePath();
        Path fitting = base.resolve("fitting");
        if (!Files.exists(fitting)) {
            Files.createDirectories(fitting);
        } else {
        }
        return fitting;
    }

    private void downloadImageAs(String originalFilename, String finalFilename) throws IOException, InterruptedException {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("원본 이미지 파일 이름이 비어 있습니다.");
        }
        String url = baseUrl + "/view?filename=" + originalFilename;
        int maxRetries = 15;

        Path uploadPath = ensureFittingDir();
        Path outputPath = uploadPath.resolve(finalFilename);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] imageData = restTemplate.getForObject(url, byte[].class);
                if (imageData != null && imageData.length > 0) {
                    Files.write(outputPath, imageData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    System.out.println("저장됨: " + outputPath);
                    return;
                }
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("이미지 준비중(" + attempt + "/" + maxRetries + "): " + originalFilename);
            }
            Thread.sleep(800);
        }
        throw new IOException("이미지 다운로드 실패: " + originalFilename + " → " + outputPath);
    }

    @Transactional
    public void saveFixedSequences(Product product, String urlA, String urlB) {
        ProductFitting seq1 = productFittingRepository.findByProductAndSequence(product, 1)
                .orElseGet(ProductFitting::new);
        seq1.setProduct(product);
        seq1.setSequence(1);
        seq1.setFittingImageUrl(urlA);
        productFittingRepository.save(seq1);

        ProductFitting seq2 = productFittingRepository.findByProductAndSequence(product, 2)
                .orElseGet(ProductFitting::new);
        seq2.setProduct(product);
        seq2.setSequence(2);
        seq2.setFittingImageUrl(urlB);
        productFittingRepository.save(seq2);
    }
}
