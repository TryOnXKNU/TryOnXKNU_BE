package org.example.tryonx.comfy.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.fitting.domain.ProductFitting;
import org.example.tryonx.fitting.repository.ProductFittingRepository;
import org.example.tryonx.image.domain.MemberClothesImage;
import org.example.tryonx.image.repository.MemberClothesImageRepository;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Service
public class ComfyUiService {

    private static final Logger log = LoggerFactory.getLogger(ComfyUiService.class);

    private final RestTemplate restTemplate;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ProductFittingRepository productFittingRepository;
    private final AmazonS3 amazonS3;
    private final MemberClothesImageRepository memberClothesImageRepository;

    @Value("${ngrok.url}")
    private String baseUrl;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private enum BodySize {
        UNDERWEIGHT,  // 완전 마른
        SLIM,         // 마른
        NORMAL,       // 정상
        OVERWEIGHT,   // 통통한
        OBESE         // 뚱뚱한
    }

    // =============================
    //  1) 통합 진입 메서드
    // =============================
    public String executeFittingUnified(
            String email,
            Integer productId1, Integer productId2,
            String memberClothesId1, String memberClothesId2
    ) throws IOException, InterruptedException {

        Integer p1 = productId1;
        Integer p2 = productId2;

        Integer m1 = parseMemberClothesId(memberClothesId1);
        Integer m2 = parseMemberClothesId(memberClothesId2);

        if (p1 == null && p2 == null && m1 == null && m2 == null) {
            throw new IllegalArgumentException("최소 1개 이상의 의상 선택이 필요합니다.");
        }

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        FittingItem item1 = null;
        FittingItem item2 = null;

        if (p1 != null) item1 = FittingItem.product(p1);
        else if (m1 != null) item1 = FittingItem.clothes(m1);

        if (p2 != null) item2 = FittingItem.product(p2);
        else if (m2 != null) item2 = FittingItem.clothes(m2);

        if (item2 == null) {
            return executeSingleFitting(member, item1);
        } else {
            return executeDualFitting(member, item1, item2);
        }
    }


    // =============================
    //  2) 단일 피팅
    // =============================
    private String executeSingleFitting(Member member, FittingItem item)
            throws IOException, InterruptedException {

        BodyShape shape = member.getBodyShape();
        BodySize bodySize = classifyBodySize(member); // 🔹 키/몸무게로 마름/보통/뚱뚱 구분

        int categoryId;
        String fileNameOnly;

        if (item.type == FittingItem.Type.PRODUCT) {
            Product product = productRepository.findById(item.id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            categoryId = product.getCategory().getCategoryId();

            String fullUrl = productImageRepository.findByProductAndIsThumbnailTrue(product)
                    .orElseThrow(() -> new RuntimeException("Thumbnail not found"))
                    .getImageUrl();

            fileNameOnly = stripPrefix(fullUrl, getProductPrefix(product));

        } else {
            MemberClothesImage img = memberClothesImageRepository.findById(item.id)
                    .orElseThrow(() -> new RuntimeException("등록되지 않은 의상 이미지 입니다."));

            categoryId = img.getCategoryId();
            fileNameOnly = stripPrefix(img.getImageUrl(), getMyCLothesPrefix(member.getMemberId()));
        }

        String prompt = resolvePrompt(categoryId);
        String model = resolveModel(categoryId, shape, bodySize);


        // 🔹 bodySize 에 따라 워크플로우 & weight_body 결정
        String workflowFile;
        String weightBody = null;

        switch (bodySize) {
            case UNDERWEIGHT -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_to_17";
            }
            case SLIM -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_17_to_19";
            }
            case NORMAL -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_19_to_23";
            }
            case OVERWEIGHT -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_23_to_25";
            }
            case OBESE -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_25_to";
            }
            default -> throw new IllegalStateException("Unexpected value: " + bodySize);
        }

        String workflowJson = loadWorkflowFromResource(workflowFile)
                .replace("{{imageName}}", fileNameOnly)
                .replace("{{modelImage}}", model)
                .replace("{{prompt}}", prompt);

        // lora 템플릿에만 있는 변수이므로, NORMAL/CHUBBY 에서만 치환
        if (weightBody != null) {
            workflowJson = workflowJson.replace("{{weight_body}}", weightBody);
        }

        return runComfyUI(workflowJson);
    }



//    private String executeSingleFitting(Member member, FittingItem item)
//            throws IOException, InterruptedException {
//
//        BodyShape shape = member.getBodyShape();
//
//        int categoryId;
//        String fileNameOnly;
//
//        if (item.type == FittingItem.Type.PRODUCT) {
//            Product product = productRepository.findById(item.id)
//                    .orElseThrow(() -> new RuntimeException("Product not found"));
//
//            categoryId = product.getCategory().getCategoryId();
//
//            String fullUrl = productImageRepository.findByProductAndIsThumbnailTrue(product)
//                    .orElseThrow(() -> new RuntimeException("Thumbnail not found"))
//                    .getImageUrl();
//
//            fileNameOnly = stripPrefix(fullUrl, getProductPrefix(product));
//
//        } else {
//            MemberClothesImage img = memberClothesImageRepository.findById(item.id)
//                    .orElseThrow(() -> new RuntimeException("등록되지 않은 의상 이미지 입니다."));
//
//            categoryId = img.getCategoryId();
//            fileNameOnly = stripPrefix(img.getImageUrl(), getMyCLothesPrefix(member.getMemberId()));
//        }
//
//        String prompt = resolvePrompt(categoryId);
//        String model  = resolveModel(categoryId, shape);
//
//        String workflowJson = loadWorkflowFromResource("v2_one_person_one_clothes.json")
//                .replace("{{imageName}}", fileNameOnly)
//                .replace("{{modelImage}}", model)
//                .replace("{{prompt}}", prompt);
//
//        return runComfyUI(workflowJson);
//    }


    // =============================
    //  3) 이중 피팅
    // =============================
    private String executeDualFitting(Member member, FittingItem a, FittingItem b)
            throws IOException, InterruptedException {

        BodySize bodySize = classifyBodySize(member); // 🔹 추가

        ClothingInfo infoA = loadClothingInfo(member, a);
        ClothingInfo infoB = loadClothingInfo(member, b);

        validateCategoryCombination(infoA.categoryId, infoB.categoryId);

        ClothingInfo prompt1Info;
        ClothingInfo prompt2Info;

        if (isBottom(infoA.categoryId)) {
            prompt1Info = infoA;
            prompt2Info = infoB;
        } else if (isBottom(infoB.categoryId)) {
            prompt1Info = infoB;
            prompt2Info = infoA;
        } else {
            prompt1Info = infoA;
            prompt2Info = infoB;
        }


        String model = resolveDualModel(
                prompt1Info.categoryId,
                prompt2Info.categoryId,
                member.getBodyShape(),
                bodySize
        );


        // 🔹 bodySize 에 따라 워크플로우 & weight_body 결정
        String workflowFile;
        String weightBody = null;

        switch (bodySize) {
            case UNDERWEIGHT -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_to_17";
            }
            case SLIM -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_17_to_19";
            }
            case NORMAL -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_19_to_23";
            }
            case OVERWEIGHT -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_23_to_25";
            }
            case OBESE -> {
                workflowFile = "lora_one_person_one_clothes.json";
                weightBody = "bmi_25_to";
            }
            default -> throw new IllegalStateException("Unexpected value: " + bodySize);
        }



        String workflowJson = loadWorkflowFromResource(workflowFile)
                .replace("{{modelImage}}", model)
                .replace("{{imageName1}}", prompt1Info.fileNameOnly)
                .replace("{{imageName2}}", prompt2Info.fileNameOnly)
                .replace("{{prompt1}}", resolvePrompt(prompt1Info.categoryId))
                .replace("{{prompt2}}", resolvePrompt(prompt2Info.categoryId));

        if (weightBody != null) {
            workflowJson = workflowJson.replace("{{weight_body}}", weightBody);
        }

        return runComfyUI(workflowJson);
    }


//    private String executeDualFitting(Member member, FittingItem a, FittingItem b)
//            throws IOException, InterruptedException {
//
//        ClothingInfo infoA = loadClothingInfo(member, a);
//        ClothingInfo infoB = loadClothingInfo(member, b);
//
//        validateCategoryCombination(infoA.categoryId, infoB.categoryId);
//
//        ClothingInfo prompt1Info;
//        ClothingInfo prompt2Info;
//
//        if (isBottom(infoA.categoryId)) {
//            prompt1Info = infoA;
//            prompt2Info = infoB;
//        } else if (isBottom(infoB.categoryId)) {
//            prompt1Info = infoB;
//            prompt2Info = infoA;
//        } else {
//            prompt1Info = infoA;
//            prompt2Info = infoB;
//        }
//
//        String model = resolveDualModel(
//                prompt1Info.categoryId,
//                prompt2Info.categoryId,
//                member.getBodyShape()
//        );
//
//        String workflowJson = loadWorkflowFromResource("v2_one_person_two_clothes.json")
//                .replace("{{modelImage}}", model)
//                .replace("{{imageName1}}", prompt1Info.fileNameOnly)
//                .replace("{{imageName2}}", prompt2Info.fileNameOnly)
//                .replace("{{prompt1}}", resolvePrompt(prompt1Info.categoryId))
//                .replace("{{prompt2}}", resolvePrompt(prompt2Info.categoryId));
//
//        return runComfyUI(workflowJson);
//    }


    // =============================
    //  4) 상품/내옷 로딩 공통화
    // =============================
    private ClothingInfo loadClothingInfo(Member member, FittingItem item) {

        ClothingInfo info = new ClothingInfo();

        if (item.type == FittingItem.Type.PRODUCT) {

            Product product = productRepository.findById(item.id)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            info.categoryId = product.getCategory().getCategoryId();

            String fullUrl = productImageRepository.findByProductAndIsThumbnailTrue(product)
                    .orElseThrow(() -> new RuntimeException("Thumbnail not found"))
                    .getImageUrl();

            info.fileNameOnly = stripPrefix(fullUrl, getProductPrefix(product));

        } else {

            MemberClothesImage img = memberClothesImageRepository.findById(item.id)
                    .orElseThrow(() -> new RuntimeException("등록되지 않은 의상 이미지입니다."));

            info.categoryId = img.getCategoryId();
            info.fileNameOnly = stripPrefix(img.getImageUrl(), getMyCLothesPrefix(member.getMemberId()));
        }

        return info;
    }


    // =============================
    //  5) 카테고리 규칙
    // =============================
    private void validateCategoryCombination(Integer c1, Integer c2) {

        if (c1.equals(c2))
            throw new RuntimeException("같은 카테고리는 중복 착용 불가 (" + c1 + ")");

        Set<Integer> dress = Set.of(9, 10, 11, 12);
        if (dress.contains(c1) || dress.contains(c2))
            throw new RuntimeException("드레스는 단일 착용만 가능합니다.");

        Set<Integer> top = Set.of(1, 2, 3, 7, 8);
        Set<Integer> bottom = Set.of(4, 5, 6, 13, 14);

        if (top.contains(c1) && top.contains(c2))
            throw new RuntimeException("상의끼리 중복 착용 불가.");

        if (bottom.contains(c1) && bottom.contains(c2))
            throw new RuntimeException("하의끼리 중복 착용 불가.");
    }

    private boolean isBottom(int categoryId) {
        return Set.of(4, 5, 6, 13, 14).contains(categoryId);
    }


    // =============================
    //  6) prompt / model 단일
    // =============================
    private String resolvePrompt(int categoryId) {
        return switch (categoryId) {
            case 1 -> "short t-shirts";
            case 2 -> "long t-shirts";
            case 3 -> "gray t-shirts";
            case 4 -> "short pants";
            case 5 -> "long pants";
            case 6 -> "long wide pants";
            case 7, 8 -> "cardigan";
            case 9 -> "short sleeve short dress";
            case 10 -> "short sleeve long dress";
            case 11 -> "long sleeve short dress";
            case 12 -> "long sleeve long dress";
            case 13 -> "short skirt";
            case 14 -> "long skirt";
            default -> "clothes";
        };
    }

    //    private String resolveModel(int categoryId, BodyShape shape) {
//
//        String base = switch (categoryId) {
//            case 1 -> "STOPA.png";
//            case 2 -> "LSTOPA.png";
//            case 3 -> "LWTOPA.png";
//            case 4 -> "SPANTSA.png";
//            case 5 -> "LSPANTSB.png";
//            case 6 -> "LWPANTSC.png";
//            case 7 -> "SOUTERWEARB.png";
//            case 8 -> "LOUTERWEARB.png";
//            case 9 -> "SSDRESS.png";
//            case 10 -> "SLDRESS.png";
//            case 11 -> "LSDRESS.png";
//            case 12 -> "LLDRESS.png";
//            case 13 -> "SSKIRTB.png";
//            case 14 -> "LSKIRTB.png";
//            default -> throw new IllegalArgumentException("Unknown categoryId: " + categoryId);
//        };
//
//        return shape == BodyShape.STRAIGHT ? "1" + base :
//                shape == BodyShape.NATURAL  ? "2" + base :
//                        "3" + base;
//    }
    private String resolveModel(int categoryId, BodyShape shape, BodySize size) {

        String base = switch (categoryId) {
            case 1 -> "STOPA.png";
            case 2 -> "LSTOPA.png";
            case 3 -> "LWTOPA.png";
            case 4 -> "SPANTSA.png";
            case 5 -> "LSPANTSB.png";
            case 6 -> "LWPANTSC.png";
            case 7 -> "SOUTERWEARB.png";
            case 8 -> "LOUTERWEARB.png";
            case 9 -> "SSDRESS.png";
            case 10 -> "SLDRESS.png";
            case 11 -> "LSDRESS.png";
            case 12 -> "LLDRESS.png";
            case 13 -> "SSKIRTB.png";
            case 14 -> "LSKIRTB.png";
            default -> throw new IllegalArgumentException("Unknown categoryId: " + categoryId);
        };

        // BodyShape prefix (기존)
        String shapePrefix =
                (shape == BodyShape.STRAIGHT) ? "1" :
                        (shape == BodyShape.NATURAL)  ? "2" : "3";

        // BodySize prefix (새로 추가)
        String sizePrefix = switch (size) {
            case UNDERWEIGHT -> "T";
            case SLIM        -> "";
            case NORMAL      -> "A";
            case OVERWEIGHT  -> "F";
            case OBESE       -> "O";
        };

        return shapePrefix + sizePrefix +  base;
    }


//    private String resolveDualModel(int c1, int c2, BodyShape shape) {
//
//        int first = Math.min(c1, c2);
//        int second = Math.max(c1, c2);
//
//        String model = switch (first + "-" + second) {
//            case "1-4" -> "STOPC.png";
//            case "1-5" -> "STOPA.png";
//            case "1-6" -> "STOPB.png";
//            case "1-13" -> "SSKIRTA.png";
//            case "1-14" -> "LSKIRTA.png";
//            case "2-4" -> "LSTOPC.png";
//            case "2-5" -> "LSTOPA.png";
//            case "2-6" -> "LSTOPB.png";
//            case "2-13" -> "SSKIRTB.png";
//            case "2-14" -> "LSKIRTB.png";
//            case "3-4" -> "LWTOPC.png";
//            case "3-5" -> "LWTOPA.png";
//            case "3-6" -> "LWTOPB.png";
//            case "3-13" -> "SSKIRTC.png";
//            case "3-14" -> "LSKIRTC.png";
//            case "4-7" -> "SOUTERWEARA.png";
//            case "5-7" -> "SOUTERWEARB.png";
//            case "6-7" -> "SOUTERWEARC.png";
//            case "4-8" -> "LOUTERWEARA.png";
//            case "5-8" -> "LOUTERWEARB.png";
//            case "6-8" -> "LOUTERWEARC.png";
//            case "7-13" -> "SOUTERWEARE.png";
//            case "8-13" -> "LOUTERWEARE.png";
//            case "7-14" -> "SOUTERWEARD.png";
//            case "8-14" -> "LOUTERWEARD.png";
//            default -> throw new RuntimeException("모델 매핑 불가능 조합: " + c1 + ", " + c2);
//        };
//
//        return shape == BodyShape.STRAIGHT ? "1" + model :
//                shape == BodyShape.NATURAL  ? "2" + model :
//                        "3" + model;
//    }

    private String resolveDualModel(int c1, int c2, BodyShape shape, BodySize size) {

        int first = Math.min(c1, c2);
        int second = Math.max(c1, c2);

        String model = switch (first + "-" + second) {
            case "1-4" -> "STOPC.png";
            case "1-5" -> "STOPA.png";
            case "1-6" -> "STOPB.png";
            case "1-13" -> "SSKIRTA.png";
            case "1-14" -> "LSKIRTA.png";
            case "2-4" -> "LSTOPC.png";
            case "2-5" -> "LSTOPA.png";
            case "2-6" -> "LSTOPB.png";
            case "2-13" -> "SSKIRTB.png";
            case "2-14" -> "LSKIRTB.png";
            case "3-4" -> "LWTOPC.png";
            case "3-5" -> "LWTOPA.png";
            case "3-6" -> "LWTOPB.png";
            case "3-13" -> "SSKIRTC.png";
            case "3-14" -> "LSKIRTC.png";
            case "4-7" -> "SOUTERWEARA.png";
            case "5-7" -> "SOUTERWEARB.png";
            case "6-7" -> "SOUTERWEARC.png";
            case "4-8" -> "LOUTERWEARA.png";
            case "5-8" -> "LOUTERWEARB.png";
            case "6-8" -> "LOUTERWEARC.png";
            case "7-13" -> "SOUTERWEARE.png";
            case "8-13" -> "LOUTERWEARE.png";
            case "7-14" -> "SOUTERWEARD.png";
            case "8-14" -> "LOUTERWEARD.png";
            default -> throw new RuntimeException("모델 매핑 불가능 조합: " + c1 + ", " + c2);
        };

        // 기존 BodyShape prefix
        String shapePrefix =
                (shape == BodyShape.STRAIGHT) ? "1" :
                        (shape == BodyShape.NATURAL)  ? "2" : "3";

        // 새 BodySize prefix
        String sizePrefix = switch (size) {
            case UNDERWEIGHT -> "T";
            case SLIM        -> "";
            case NORMAL      -> "A";
            case OVERWEIGHT  -> "F";
            case OBESE       -> "O";
        };

        return shapePrefix + sizePrefix + model;
    }


    private Integer parseMemberClothesId(String raw) {
        if (raw == null || raw.isBlank()) return null;

        raw = raw.trim();
        if (raw.startsWith("x") || raw.startsWith("X")) raw = raw.substring(1);

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            log.warn("parseMemberClothesId: invalid memberClothesId='{}' -> returning null", raw);
            return null;
        }
    }


    private String runComfyUI(String workflowJson)
            throws IOException, InterruptedException {

        String promptId = sendWorkflow(workflowJson);
        waitUntilComplete(promptId);

        List<String> list = getGeneratedOutputImageFilenameList(promptId);

        String max = list.stream()
                .max(Comparator.comparingInt(name -> {
                    int s = name.indexOf('_') + 1;
                    int e = name.lastIndexOf('_');
                    return Integer.parseInt(name.substring(s, e));
                }))
                .orElseThrow();

        downloadImageToS3(max);
        return max;
    }


    private static class ClothingInfo {
        int categoryId;
        String fileNameOnly;
    }

    private static class FittingItem {

        enum Type { PRODUCT, MY_CLOTHES }

        final Type type;
        final Integer id;

        private FittingItem(Type t, Integer id) {
            this.type = t;
            this.id = id;
        }

        public static FittingItem product(Integer id) {
            return new FittingItem(Type.PRODUCT, id);
        }

        public static FittingItem clothes(Integer id) {
            return new FittingItem(Type.MY_CLOTHES, id);
        }
    }

    private String getProductPrefix(Product product) {
        return "https://tryonx.s3.ap-northeast-2.amazonaws.com/product/" + product.getProductCode() + "/";
    }

    private String getMyCLothesPrefix(Long memberId) {
        return "https://tryonx.s3.ap-northeast-2.amazonaws.com/member/" + memberId + "/";
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
                    System.out.println("## ComfyUI 이미지 생성 완료! ##");
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



    private void downloadImageToS3(String filename) throws IOException, InterruptedException {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("❌ 이미지 파일 이름이 null이거나 비어 있습니다.");
        }

        String url = baseUrl + "/view?filename=" + filename;
        int maxRetries = 10;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                byte[] imageData = restTemplate.getForObject(url, byte[].class);

                if (imageData != null && imageData.length > 0) {
                    // 🔹 새 파일명 (S3 경로 포함)
                    String s3FileName = "fitting/fittingRoom/" + filename;

                    // 🔹 메타데이터 설정
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentType("image/jpeg"); // 필요 시 content-type 감지 로직 추가
                    metadata.setContentLength(imageData.length);

                    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
                        amazonS3.putObject(bucket, s3FileName, inputStream, metadata);
                    }

                    // 🔹 S3 URL 반환 또는 출력
                    String imageUrl = amazonS3.getUrl(bucket, s3FileName).toString();
                    System.out.println("## 이미지 업로드 완료: " + imageUrl + " ##");
                    return;
                }
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("⏳ [대기 중] 이미지가 아직 준비되지 않음. 재시도 " + attempt);
            }

            Thread.sleep(1000);
        }

        throw new IOException("❌ 이미지 다운로드 실패: " + filename);
    }

    /**
     * 키/몸무게로 "마름/보통/뚱뚱" 구분
     * - 마름 : BMI < 18.5
     * - 보통 : 18.5 <= BMI < 23.0
     * - 뚱뚱 : BMI >= 23.0
     * height 또는 weight 가 없으면 보통으로 처리
     */
    /**
     * BMI 기준 5단계 분류:
     * UNDERWEIGHT : BMI < 18.5
     * SLIM        : 18.5 ≤ BMI < 21.0
     * NORMAL      : 21.0 ≤ BMI < 23.0
     * OVERWEIGHT  : 23.0 ≤ BMI < 27.0
     * OBESE       : BMI ≥ 27.0
     */
    private BodySize classifyBodySize(Member member) {
        Integer h = member.getHeight();  // cm
        Integer w = member.getWeight();  // kg

        if (h == null || w == null || h == 0) {
            log.warn("height/weight null → NORMAL 처리 (memberId={})", member.getMemberId());
            return BodySize.NORMAL;
        }

        double heightM = h / 100.0;
        double bmi = w / (heightM * heightM);

        BodySize result;
        if (bmi < 18.5) {
            result = BodySize.UNDERWEIGHT;
        } else if (bmi < 21.0) {
            result = BodySize.SLIM;
        } else if (bmi < 23.0) {
            result = BodySize.NORMAL;
        } else if (bmi < 27.0) {
            result = BodySize.OVERWEIGHT;
        } else {
            result = BodySize.OBESE;
        }

        log.info("memberId={} height={} weight={} bmi={} -> bodySize={}",
                member.getMemberId(), h, w, String.format("%.2f", bmi), result);

        return result;
    }

}
