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

@RequiredArgsConstructor
@Service
public class ComfyUiService {

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
        String model  = resolveModel(categoryId, shape);

        String workflowJson = loadWorkflowFromResource("v2_one_person_one_clothes.json")
                .replace("{{imageName}}", fileNameOnly)
                .replace("{{modelImage}}", model)
                .replace("{{prompt}}", prompt);

        return runComfyUI(workflowJson);
    }


    // =============================
    //  3) 이중 피팅
    // =============================
    private String executeDualFitting(Member member, FittingItem a, FittingItem b)
            throws IOException, InterruptedException {

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
                member.getBodyShape()
        );

        String workflowJson = loadWorkflowFromResource("v2_one_person_two_clothes.json")
                .replace("{{modelImage}}", model)
                .replace("{{imageName1}}", prompt1Info.fileNameOnly)
                .replace("{{imageName2}}", prompt2Info.fileNameOnly)
                .replace("{{prompt1}}", resolvePrompt(prompt1Info.categoryId))
                .replace("{{prompt2}}", resolvePrompt(prompt2Info.categoryId));

        return runComfyUI(workflowJson);
    }


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

    private String resolveModel(int categoryId, BodyShape shape) {

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

        return shape == BodyShape.STRAIGHT ? "1" + base :
                shape == BodyShape.NATURAL  ? "2" + base :
                        "3" + base;
    }

    private String resolveDualModel(int c1, int c2, BodyShape shape) {

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

        return shape == BodyShape.STRAIGHT ? "1" + model :
                shape == BodyShape.NATURAL  ? "2" + model :
                        "3" + model;
    }




    private Integer parseMemberClothesId(String raw) {
        if (raw == null || raw.isBlank()) return null;

        raw = raw.trim();
        if (raw.startsWith("x") || raw.startsWith("X"))
            raw = raw.substring(1);

        return Integer.parseInt(raw);
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

//    public String executeFittingTwoClothesFlow(String email, Integer productId1, Integer productId2) throws IOException, InterruptedException {
//        Member member = memberRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("Member not found"));
//        BodyShape memberBodyShape = member.getBodyShape();
//        String model = null;
//
//        String prompt = null;
//        String prompt1 = null;
//        String prompt2 = null;
//
//        String imageName1 = null;
//        String imageName2 = null;
//
//        String defaultPrompt = null;
//        String requestPrompt = null;
//
//        String defaultImageName = null;
//        String requestImageName = null;
//
//        String fileNameOnly1 = null;
//        String fileNameOnly2 = null;
//
//        String workflowJson = null;
//
//        if (productId1 == null && productId2 == null)
//        {
//            throw new IllegalArgumentException("최소 1개 이상의 상품을 선택해야 합니다.");
//        }
//        else if (productId1 != null && productId2 == null)
//        {
//            Product product = productRepository.findById(productId1).orElseThrow(() -> new RuntimeException("Product not found"));
//
//            int categoryId = product.getCategory().getCategoryId();
//
//            switch (categoryId) {
//                case 1:
//                    prompt = "short t-shirts";
//                    model = "STOPA.png";
//                    break;
//                case 2:
//                    prompt = "long t-shirts";
//                    model = "LSTOPA.png";
//                    break;
//                case 3:
//                    prompt = "gray t-shirts";
//                    model = "LWTOPA.png";
//                    break;
//                case 4:
//                    prompt = "short pants";
//                    model = "SPANTSA.png";
//                    break;
//                case 5:
//                    prompt = "long pants";
//                    model = "LSPANTSB.png";
//                    break;
//                case 6:
//                    prompt = "long wide pants";
//                    model = "LWPANTSC.png";
//                    break;
//                case 7:
//                    prompt = "cardigan";
//                    model = "SOUTERWEARB.png";
//                    break;
//                case 8:
//                    prompt = "cardigan";
//                    model = "LOUTERWEARB.png";
//                    break;
//                case 9:
//                    prompt = "short sleeve short dress";
//                    model = "SSDRESS.png";
//                    break;
//                case 10:
//                    prompt = "short sleeve long dress";
//                    model = "SLDRESS.png";
//                    break;
//                case 11:
//                    prompt = "long sleeve shore dress";
//                    model = "LSDRESS.png";
//                    break;
//                case 12:
//                    prompt = "long sleeve long dress";
//                    model = "LLDRESS.png";
//                    break;
//                case 13:
//                    prompt = "short skirt";
//                    model = "SSKIRTB.png";
//                    break;
//                case 14:
//                    prompt = "long skirt";
//                    model = "LSKIRTB.png";
//                    break;
//                default:
//                    throw new IllegalArgumentException("Unknown categoryId: " + categoryId);
//            }
//
//            if (memberBodyShape == BodyShape.STRAIGHT) {
//                model = "1" + model;
//            } else if (memberBodyShape == BodyShape.NATURAL) {
//                model = "2" + model;
//            } else if (memberBodyShape == BodyShape.WAVE) {
//                model = "3" + model;
//            }
//
//
//            String imgName = productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl();
//            String productCode = product.getProductCode();
//            String prefix = "https://tryonx.s3.ap-northeast-2.amazonaws.com/product/" + productCode + "/";
//            //https://tryonx.s3.ap-northeast-2.amazonaws.com/product/axlsdre00008/970e533f-3fef-43ea-a590-179035d863a1_06968472-AA0C-4369-AFEF-263682E826E2.png
//            String fileNameOnly = imgName.startsWith(prefix)
//                    ? imgName.substring(prefix.length())
//                    : imgName;
//
//
//            workflowJson = loadWorkflowFromResource("v2_one_person_one_clothes.json")
//                    .replace("{{imageName}}", fileNameOnly)
//                    .replace("{{modelImage}}", model)
//                    .replace("{{prompt}}", prompt);
//        }
//        else if (productId1 != null && productId2 != null)
//        {
//            Product product1 = productRepository.findById(productId1)
//                    .orElseThrow(() -> new RuntimeException("Product not found - id : " + productId1));
//            Product product2 = productRepository.findById(productId2)
//                    .orElseThrow(() -> new RuntimeException("Product not found - id : " + productId2));
//
//            Integer categoryId1 = product1.getCategory().getCategoryId();
//            Integer categoryId2 = product2.getCategory().getCategoryId();
//
//            // 동일 카테고리 / 드레스 / 그룹 중복 예외처리
//            if (categoryId1.equals(categoryId2)) {
//                throw new RuntimeException("같은 카테고리는 선택할 수 없습니다. ("
//                        + categoryId1 + " & " + categoryId2 + ")");
//            }
//
//            Set<Integer> dress = Set.of(9, 10, 11, 12);
//            if (dress.contains(categoryId1) || dress.contains(categoryId2)) {
//                throw new RuntimeException("드레스는 단일착용만 가능합니다. ("
//                        + categoryId1 + " & " + categoryId2 + ")");
//            }
//
//            Set<Integer> group1 = Set.of(1, 2, 3, 7, 8);
//            Set<Integer> group2 = Set.of(4, 5, 6, 13, 14);
//            if (group1.contains(categoryId1) && group1.contains(categoryId2)) {
//                throw new RuntimeException("같은 그룹(상의 그룹)에서 두 개를 선택할 수 없습니다. ("
//                        + categoryId1 + " & " + categoryId2 + ")");
//            }
//            if (group2.contains(categoryId1) && group2.contains(categoryId2)) {
//                throw new RuntimeException("같은 그룹(하의 그룹)에서 두 개를 선택할 수 없습니다. ("
//                        + categoryId1 + " & " + categoryId2 + ")");
//            }
//
//            // 하의(4,5,6,13.14) → prompt1 / 나머지 → prompt2 로 지정
//            Set<Integer> bottomCategories = Set.of(4, 5, 6, 13, 14);
//            Product prompt1Product;
//            Product prompt2Product;
//
//            if (bottomCategories.contains(categoryId1)) {
//                prompt1Product = product1;
//                prompt2Product = product2;
//            } else if (bottomCategories.contains(categoryId2)) {
//                prompt1Product = product2;
//                prompt2Product = product1;
//            } else {
//                // 둘 다 하의가 아닌 경우 기존 순서 유지
//                prompt1Product = product1;
//                prompt2Product = product2;
//            }
//
//            // prompt1 설정
//            prompt1 = switch (prompt1Product.getCategory().getCategoryId()) {
//                case 1 -> "short t-shirts";
//                case 2 -> "long t-shirts";
//                case 3 -> "gray t-shirts";
//                case 4 -> "short pants";
//                case 5 -> "long pants";
//                case 6 -> "long wide pants";
//                case 7, 8 -> "cardigan";
//                case 9 -> "short sleeve short dress";
//                case 10 -> "short sleeve long dress";
//                case 11 -> "long sleeve shore dress";
//                case 12 -> "long sleeve long dress";
//                case 13 -> "short skirt";
//                case 14 -> "long skirt";
//                default -> "clothes";
//            };
//
//            // prompt2 설정
//            prompt2 = switch (prompt2Product.getCategory().getCategoryId()) {
//                case 1 -> "short t-shirts";
//                case 2 -> "long t-shirts";
//                case 3 -> "gray t-shirts";
//                case 4 -> "short pants";
//                case 5 -> "long pants";
//                case 6 -> "long wide pants";
//                case 7, 8 -> "cardigan";
//                case 9 -> "short sleeve short dress";
//                case 10 -> "short sleeve long dress";
//                case 11 -> "long sleeve shore dress";
//                case 12 -> "long sleeve long dress";
//                case 13 -> "short skirt";
//                case 14 -> "long skirt";
//                default -> "clothes";
//            };
//
//            // model 설정
//            int first = Math.min(categoryId1, categoryId2);
//            int second = Math.max(categoryId1, categoryId2);
//
//            if (first == 1 && second == 4) model = "STOPC.png";
//            else if (first == 1 && second == 5) model = "STOPA.png";
//            else if (first == 1 && second == 6) model = "STOPB.png";
//            else if (first == 1 && second == 13) model = "SSKIRTA.png";
//            else if (first == 1 && second == 14) model = "LSKIRTA.png";
//
//            else if (first == 2 && second == 4) model = "LSTOPC.png";
//            else if (first == 2 && second == 5) model = "LSTOPA.png";
//            else if (first == 2 && second == 6) model = "LSTOPB.png";
//            else if (first == 2 && second == 13) model = "SSKIRTB.png";
//            else if (first == 2 && second == 14) model = "LSKIRTB.png";
//
//            else if (first == 3 && second == 4) model = "LWTOPC.png";
//            else if (first == 3 && second == 5) model = "LWTOPA.png";
//            else if (first == 3 && second == 6) model = "LWTOPB.png";
//            else if (first == 3 && second == 13) model = "SSKIRTC.png";
//            else if (first == 3 && second == 14) model = "LSKIRTC.png";
//
//            else if (first == 4 && (second == 7)) model = "SOUTERWEARA.png";
//            else if (first == 5 && (second == 7)) model = "SOUTERWEARB.png";
//            else if (first == 6 && (second == 7)) model = "SOUTERWEARC.png";
//
//            else if (first == 4 && (second == 8)) model = "LOUTERWEARA.png";
//            else if (first == 5 && (second == 8)) model = "LOUTERWEARB.png";
//            else if (first == 6 && (second == 8)) model = "LOUTERWEARC.png";
//
//            else if (first == 7 && (second == 13)) model = "SOUTERWEARE.png";
//            else if (first == 8 && (second == 13)) model = "LOUTERWEARE.png";
//
//            else if (first == 7 && (second == 14)) model = "SOUTERWEARD.png";
//            else if (first == 8 && (second == 14)) model = "LOUTERWEARD.png";
//
//            if (model != null) {
//                if (memberBodyShape == BodyShape.STRAIGHT) {
//                    model = "1" + model;
//                } else if (memberBodyShape == BodyShape.NATURAL) {
//                    model = "2" + model;
//                } else if (memberBodyShape == BodyShape.WAVE) {
//                    model = "3" + model;
//                }
//            }
//
//            // prompt1, prompt2 순서에 맞게 이미지 매칭
//            String imageNamePrompt1 = productImageRepository.findByProductAndIsThumbnailTrue(prompt1Product)
//                    .orElseThrow(() -> new RuntimeException("Thumbnail not found for prompt1 product"))
//                    .getImageUrl();
//            String imageNamePrompt2 = productImageRepository.findByProductAndIsThumbnailTrue(prompt2Product)
//                    .orElseThrow(() -> new RuntimeException("Thumbnail not found for prompt2 product"))
//                    .getImageUrl();
//
//            fileNameOnly1 = stripPrefix(imageNamePrompt1, getProductPrefix(prompt1Product));
//            fileNameOnly2 = stripPrefix(imageNamePrompt2, getProductPrefix(prompt2Product));
//
//            // 워크플로우 JSON 치환
//            workflowJson = loadWorkflowFromResource("v2_one_person_two_clothes.json")
//                    .replace("{{modelImage}}", model)
//                    .replace("{{imageName1}}", fileNameOnly1)
//                    .replace("{{imageName2}}", fileNameOnly2)
//                    .replace("{{prompt1}}", prompt1)
//                    .replace("{{prompt2}}", prompt2);
//        }
//        else
//        {
//            throw new IllegalArgumentException("상품 선택 옵션이 잘못되었습니다.");
//        }
//
//        // Google Drive 새로고침
////        refreshGoogleDrive();
//
//        // 1. 워크플로우 실행
//        String promptId = sendWorkflow(workflowJson);
//
//        // 2. 완료 대기
//        waitUntilComplete(promptId);
//
//        List<String> generatedOutputImageFilenameList = getGeneratedOutputImageFilenameList(promptId);
//
//        String fileName = generatedOutputImageFilenameList.stream()
//                .max(Comparator.comparingInt(name -> {
//                    // "ComfyUI_숫자_.png" 에서 숫자만 추출
//                    int start = name.indexOf('_') + 1;
//                    int end = name.lastIndexOf('_');
//                    return Integer.parseInt(name.substring(start, end));
//                }))
//                .orElseThrow(() -> new IllegalArgumentException("파일명이 없습니다."));
//
//        downloadImageToS3(fileName);
//        return fileName;
//    }
}
