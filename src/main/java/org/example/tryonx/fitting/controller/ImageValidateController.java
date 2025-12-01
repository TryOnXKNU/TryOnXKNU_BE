package org.example.tryonx.fitting.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fitting")
public class ImageValidateController {
    private static final Logger logger = LoggerFactory.getLogger(ImageValidateController.class);
    private static final double LOG_CONFIDENCE_THRESHOLD = 0.2;
//    @Value("${validate.decision.threshold:0.18}")
//    private double decisionThreshold;
//    @Value("${validate.skin.maxFraction:0.45}")
//    private double skinMaxFraction;
//    @Value("${validate.skin.penaltyWeight:0.8}")
//    private double skinPenaltyWeight;
//    @Value("${validate.weight.edge:0.6}")
//    private double weightEdge;
//    @Value("${validate.weight.nonwhite:0.4}")
//    private double weightNonwhite;
    @Value("${validate.service.url:}")
    private String validateServiceUrl; 

    private final WebClient webClient = WebClient.create();

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> validateImage(
            @RequestPart(value = "image", required = false) MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl
    ) {

        try {
            if ((file == null || file.isEmpty()) && (imageUrl == null || imageUrl.isBlank())) {
                return ResponseEntity.badRequest().body(Map.of("error", "no image provided"));
            }

            // --- 1) 이미지 바이트 로드 ---
            byte[] bytes;
            String filename = "upload";
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            if (file != null && !file.isEmpty()) {
                bytes = file.getBytes();
                if (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                    filename = file.getOriginalFilename();
                if (file.getContentType() != null)
                    contentType = file.getContentType();

            } else {
                // URL 로딩
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.net.URLConnection conn = url.openConnection();
                    conn.setRequestProperty("User-Agent", "TryonX-Validate/1.0");

                    bytes = conn.getInputStream().readAllBytes();
                    String path = url.getPath();
                    String name = path != null ? path.substring(path.lastIndexOf('/') + 1) : "";
                    if (!name.isBlank()) filename = name;

                    String ct = conn.getContentType();
                    if (ct != null) contentType = ct;

                } catch (Exception e) {
                    logger.warn("Failed to fetch image from URL: {}", imageUrl, e);
                    return ResponseEntity.badRequest().body(Map.of("error", "failed_fetch_image"));
                }
            }

            if (bytes == null || bytes.length == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "no_image_bytes"));
            }


            // --- 2) Python(CLIP) validate 서비스 호출 ---
            if (StringUtils.hasText(validateServiceUrl)) {

                final byte[] payload = bytes;
                final String fname = filename;
                final String ctype = contentType;

                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                ByteArrayResource bar = new ByteArrayResource(payload) {
                    @Override
                    public String getFilename() {
                        return fname;
                    }
                };
                builder.part("image", bar).header("Content-Type", ctype);

                var resp = webClient.post()
                        .uri(validateServiceUrl)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(builder.build()))
                        .retrieve()
                        .toEntity(Map.class)
                        .block();

                if (resp == null)
                    return ResponseEntity.status(500).body(Map.of("error", "validation_failed"));

                Map<String, Object> body = resp.getBody();

                boolean respIsClothing = false;
                if (body != null) {
                    if (body.get("isClothing") instanceof Boolean)
                        respIsClothing = (Boolean) body.get("isClothing");
                }

                if (!respIsClothing) {
                    logger.warn("[CLIP Validation] NOT-CLOTHING → file=({}, {} bytes) result={}",
                            filename, bytes.length, body);
                }

                return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
            }

            // validate.service.url 없으면 에러
            return ResponseEntity.status(500).body(Map.of("error", "validate_service_url_not_set"));

        } catch (Exception ex) {
            logger.error("Image validation failed", ex);
            return ResponseEntity.status(500).body(Map.of("error", "validation_failed", "message", ex.getMessage()));
        }
    }
//    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<?> validateImage(@RequestPart(value = "image", required = false) MultipartFile file,
//                                           @RequestParam(value = "imageUrl", required = false) String imageUrl) {
//        try {
//            if ((file == null || file.isEmpty()) && (imageUrl == null || imageUrl.isBlank())) {
//                return ResponseEntity.badRequest().body(Map.of("error", "no image provided"));
//            }
//
//
//            byte[] bytes;
//            String filename = "upload";
//            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
//
//            if (file != null && !file.isEmpty()) {
//                bytes = file.getBytes();
//                if (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) filename = file.getOriginalFilename();
//                if (file.getContentType() != null) contentType = file.getContentType();
//            } else {
//                // imageUrl에서 가져오기
//                try {
//                    java.net.URL url = new java.net.URL(imageUrl);
//                    java.net.URLConnection conn = url.openConnection();
//                    conn.setRequestProperty("User-Agent", "TryonX-Validate/1.0");
//                    try (java.io.InputStream is = conn.getInputStream()) {
//                        bytes = is.readAllBytes();
//                    }
//                    String path = url.getPath();
//                    String name = path != null ? path.substring(path.lastIndexOf('/') + 1) : "";
//                    if (name != null && !name.isBlank()) filename = name;
//                    String ct = conn.getContentType();
//                    if (ct != null && !ct.isBlank()) contentType = ct;
//                } catch (Exception e) {
//                    logger.warn("Failed to fetch image from URL: {}", imageUrl, e);
//                    return ResponseEntity.badRequest().body(Map.of("error", "failed_fetch_image"));
//                }
//            }
//
//            if (bytes == null || bytes.length == 0) {
//                return ResponseEntity.badRequest().body(Map.of("error", "no_image_bytes"));
//            }
//
//            if (StringUtils.hasText(validateServiceUrl)) {
//                final byte[] payload = bytes;
//                final String fname = filename;
//                final String ctype = contentType;
//
//                MultipartBodyBuilder builder = new MultipartBodyBuilder();
//                ByteArrayResource bar = new ByteArrayResource(payload) {
//                    @Override
//                    public String getFilename() {
//                        return fname;
//                    }
//                };
//                builder.part("image", bar).header("Content-Type", ctype);
//
//                var resp = webClient.post()
//                        .uri(validateServiceUrl)
//                        .contentType(MediaType.MULTIPART_FORM_DATA)
//                        .body(BodyInserters.fromMultipartData(builder.build()))
//                        .retrieve()
//                        .toEntity(Map.class)
//                        .block();
//
//                if (resp == null) return ResponseEntity.status(500).body(Map.of("error", "validation_failed"));
//
//                @SuppressWarnings("unchecked")
//                Map<String, Object> body = resp.getBody();
//
//                boolean respIsClothing = false;
//                double respConfidence = 0.0;
//                if (body != null) {
//                    Object ic = body.get("isClothing");
//                    if (ic instanceof Boolean) respIsClothing = (Boolean) ic;
//                    else if (ic instanceof String) respIsClothing = Boolean.parseBoolean((String) ic);
//                    Object cf = body.get("confidence");
//                    if (cf instanceof Number) respConfidence = ((Number) cf).doubleValue();
//                    else if (cf instanceof String) {
//                        try { respConfidence = Double.parseDouble((String) cf); } catch (Exception ignored) {}
//                    }
//                }
//
//                if (!respIsClothing || respConfidence < LOG_CONFIDENCE_THRESHOLD) {
//                    try {
//                        logger.warn("Validation LOW CONFIDENCE or NOT-CLOTHING - file=({}, {} bytes) -> response={}", fname, payload.length, body);
//                    } catch (Exception logEx) {
//                        logger.warn("Validation LOW CONFIDENCE - but failed to log details", logEx);
//                    }
//                }
//
//                return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
//            }
//
//
//            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
//            if (img == null) {
//                return ResponseEntity.badRequest().body(Map.of("error", "invalid_image"));
//            }
//
//            double nonWhiteFraction = computeNonWhiteFraction(img);
//            double edgeDensity = computeEdgeDensity(img);
//            double skinFraction = computeSkinFraction(img);
//
//            double base = weightEdge * edgeDensity + weightNonwhite * nonWhiteFraction;
//             // 피부 영역이 큰 이미지에 페널티 부과 (예: 얼굴/발)
//            double penalty = 0.0;
//            if (skinMaxFraction > 0) {
//                double ratio = Math.min(1.0, skinFraction / skinMaxFraction);
//                penalty = ratio * skinPenaltyWeight;
//            }
//            double confidence = base * (1.0 - penalty);
//            confidence = Math.max(0.0, Math.min(1.0, confidence));
//            boolean isClothing = confidence >= decisionThreshold;
//
//            Map<String, Object> result = new HashMap<>();
//            result.put("isClothing", isClothing);
//            result.put("confidence", confidence);
//            Map<String, Object> extras = new HashMap<>();
//            extras.put("edge_density", edgeDensity);
//            extras.put("nonwhite_fraction", nonWhiteFraction);
//            extras.put("skin_fraction", skinFraction);
//            result.put("extras", extras);
//
//            if (!isClothing || confidence < LOG_CONFIDENCE_THRESHOLD) {
//                try {
//                    logger.warn("Java Validation LOW CONFIDENCE or NOT-CLOTHING - file=({}, {} bytes) -> confidence={} skin_fraction={} extras={}", filename, bytes.length, confidence, skinFraction, extras);
//                } catch (Exception e) {
//                    logger.warn("Java validation low-confidence but failed to log details", e);
//                }
//            }
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception ex) {
//            logger.error("Image validation failed", ex);
//            return ResponseEntity.status(500).body(Map.of("error", "validation_failed", "message", ex.getMessage()));
//        }
//    }
//
//    private double computeNonWhiteFraction(BufferedImage img) {
//        int w = img.getWidth();
//        int h = img.getHeight();
//        long count = 0; long total = 0;
//        for (int y = 0; y < h; y++) {
//            for (int x = 0; x < w; x++) {
//                int rgb = img.getRGB(x, y);
//                int r = (rgb >> 16) & 0xff;
//                int g = (rgb >> 8) & 0xff;
//                int b = rgb & 0xff;
//                double lum = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;
//                if (lum < 0.95) count++;
//                total++;
//            }
//        }
//        return total > 0 ? (double) count / (double) total : 0.0;
//    }
//
//    private double computeEdgeDensity(BufferedImage img) {
//        int w = img.getWidth();
//        int h = img.getHeight();
//        // 그레이스케일 배열로 변환
//        double[][] gray = new double[h][w];
//        for (int y = 0; y < h; y++) {
//            for (int x = 0; x < w; x++) {
//                int rgb = img.getRGB(x, y);
//                int r = (rgb >> 16) & 0xff;
//                int g = (rgb >> 8) & 0xff;
//                int b = rgb & 0xff;
//                gray[y][x] = (0.2126 * r + 0.7152 * g + 0.0722 * b);
//            }
//        }
//
//        // Sobel 커널
//        int[][] kx = {{-1,0,1},{-2,0,2},{-1,0,1}};
//        int[][] ky = {{-1,-2,-1},{0,0,0},{1,2,1}};
//        int count = 0;
//        double maxMag = 1e-9;
//        double[][] mag = new double[h][w];
//        for (int y = 1; y < h-1; y++) {
//            for (int x = 1; x < w-1; x++) {
//                double gx = 0.0, gy = 0.0;
//                for (int j = -1; j <= 1; j++) {
//                    for (int i = -1; i <= 1; i++) {
//                        gx += kx[j+1][i+1] * gray[y+j][x+i];
//                        gy += ky[j+1][i+1] * gray[y+j][x+i];
//                    }
//                }
//                double g = Math.hypot(gx, gy);
//                mag[y][x] = g;
//                if (g > maxMag) maxMag = g;
//                count++;
//            }
//        }
//        if (count == 0) return 0.0;
//        // normalize and compute fraction above threshold
//        int strong = 0;
//        for (int y = 1; y < h-1; y++) {
//            for (int x = 1; x < w-1; x++) {
//                double v = mag[y][x] / maxMag;
//                if (v > 0.12) strong++;
//            }
//        }
//        return (double) strong / (double) count;
//    }
//
//   /**
//     * YCbCr 임계값을 사용하여 피부색일 가능성이 있는 픽셀의 비율을 추정합니다.
//     * 일반적인 Cb/Cr 범위를 사용하여 피부를 감지합니다. 완벽하지는 않지만 얼굴/발을 필터링하는 데 유용합니다.
//     */
//    private double computeSkinFraction(BufferedImage img) {
//        int w = img.getWidth();
//        int h = img.getHeight();
//        long skinCount = 0; long total = 0;
//        for (int y = 0; y < h; y++) {
//            for (int x = 0; x < w; x++) {
//                int rgb = img.getRGB(x, y);
//                int r = (rgb >> 16) & 0xff;
//                int g = (rgb >> 8) & 0xff;
//                int b = rgb & 0xff;
//                // RGB를 YCbCr로 변환
//                double Y  =  0.299 * r + 0.587 * g + 0.114 * b;
//                double Cb = 128.0 - 0.168736 * r - 0.331264 * g + 0.5 * b;
//                double Cr = 128.0 + 0.5 * r - 0.418688 * g - 0.081312 * b;
//                // Cb/Cr의 일반적인 피부색 범위
//                if (Cb >= 77 && Cb <= 127 && Cr >= 133 && Cr <= 173 && Y > 20) {
//                    skinCount++;
//                }
//                total++;
//            }
//        }
//        return total > 0 ? (double) skinCount / (double) total : 0.0;
//    }


}

