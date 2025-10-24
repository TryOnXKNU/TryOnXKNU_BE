package org.example.tryonx.comfy.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
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

    @Value("${ngrok.url}")
    private String baseUrl;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

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

        String model = null;
        String prompt = null;

        Product product = productRepository.findById(productid)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int categoryId = product.getCategory().getCategoryId();

        switch (categoryId) {
            case 1:
                prompt = "black tshirt";
                model = "STOPA.png";
                break;
            case 2:
                prompt = "black tshirt";
                model = "LSTOPA.png";
                break;
            case 3:
                prompt = "black tshirt";
                model = "LWTOPA.png";
                break;
            case 4:
                prompt = "pants";
                model = "LSTOPC.png";
                break;
            case 5:
                prompt = "pants";
                model = "LSTOPA.png";
                break;
            case 6:
                prompt = "pants";
                model = "LSTOPB.png";
                break;
            case 7:
                prompt = "black cardigan";
                model = "SOUTERWEARB.png";
                break;
            case 8:
                prompt = "black cardigan";
                model = "LOUTERWEARB.png";
                break;
            case 9:
                prompt = "dress";
                model = "SSDRESS.png";
                break;
            case 10:
                prompt = "dress";
                model = "SLDRESS.png";
                break;
            case 11:
                prompt = "dress";
                model = "LSDRESS.png";
                break;
            case 12:
                prompt = "dress";
                model = "LLDRESS.png";
                break;
            default:
                throw new IllegalArgumentException("Unknown categoryId: " + categoryId);
        }

        if (memberBodyShape == BodyShape.STRAIGHT) {
            model = "1" + model;
        } else if (memberBodyShape == BodyShape.NATURAL) {
            model = "2" + model;
        } else if (memberBodyShape == BodyShape.WAVE) {
            model = "3" + model;
        }


        String imgName = productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl();
        String productCode = product.getProductCode();
        String prefix = "https://tryonx.s3.ap-northeast-2.amazonaws.com/product/" + productCode + "/";
        //https://tryonx.s3.ap-northeast-2.amazonaws.com/product/axlsdre00008/970e533f-3fef-43ea-a590-179035d863a1_06968472-AA0C-4369-AFEF-263682E826E2.png
        String fileNameOnly = imgName.startsWith(prefix)
                ? imgName.substring(prefix.length())
                : imgName;

        System.out.println(fileNameOnly+"*************");


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
        String model;

        String prompt = null;
        String prompt1 = null;
        String prompt2 = null;

        String imageName1 = null;
        String imageName2 = null;

        String defaultPrompt = null;
        String requestPrompt = null;

        String defaultImageName = null;
        String requestImageName = null;

        String fileNameOnly1 = null;
        String fileNameOnly2 = null;

        String workflowJson = null;

        if (productId1 == null && productId2 == null){
            throw new IllegalArgumentException("최소 1개 이상의 상품을 선택해야 합니다.");
        }else if(productId1 != null && productId2 == null){
            Product product = productRepository.findById(productId1)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            int categoryId = product.getCategory().getCategoryId();

            switch (categoryId) {
                case 1:
                    prompt = "black tshirt";
                    model = "STOPA.png";
                    break;
                case 2:
                    prompt = "black tshirt";
                    model = "LSTOPA.png";
                    break;
                case 3:
                    prompt = "black tshirt";
                    model = "LWTOPA.png";
                    break;
                case 4:
                    prompt = "pants";
                    model = "LSTOPC.png";
                    break;
                case 5:
                    prompt = "pants";
                    model = "LSTOPA.png";
                    break;
                case 6:
                    prompt = "pants";
                    model = "LSTOPB.png";
                    break;
                case 7:
                    prompt = "black cardigan";
                    model = "SOUTERWEARB.png";
                    break;
                case 8:
                    prompt = "black cardigan";
                    model = "LOUTERWEARB.png";
                    break;
                case 9:
                    prompt = "dress";
                    model = "SSDRESS.png";
                    break;
                case 10:
                    prompt = "dress";
                    model = "SLDRESS.png";
                    break;
                case 11:
                    prompt = "dress";
                    model = "LSDRESS.png";
                    break;
                case 12:
                    prompt = "dress";
                    model = "LLDRESS.png";
                    break;
                case 13:
                    prompt = "skirt";
                    model = "SSKIRTB.png";
                    break;
                case 14:
                    prompt = "skirt";
                    model = "LSKIRTB.png";
                    break;
                default:
                    throw new IllegalArgumentException("Unknown categoryId: " + categoryId);
            }

            if (memberBodyShape == BodyShape.STRAIGHT) {
                model = "1" + model;
            } else if (memberBodyShape == BodyShape.NATURAL) {
                model = "2" + model;
            } else if (memberBodyShape == BodyShape.WAVE) {
                model = "3" + model;
            }


            String imgName = productImageRepository.findByProductAndIsThumbnailTrue(product).get().getImageUrl();
            String productCode = product.getProductCode();
            String prefix = "https://tryonx.s3.ap-northeast-2.amazonaws.com/product/" + productCode + "/";
            //https://tryonx.s3.ap-northeast-2.amazonaws.com/product/axlsdre00008/970e533f-3fef-43ea-a590-179035d863a1_06968472-AA0C-4369-AFEF-263682E826E2.png
            String fileNameOnly = imgName.startsWith(prefix)
                    ? imgName.substring(prefix.length())
                    : imgName;


            workflowJson = loadWorkflowFromResource("v2_one_person_one_clothes.json")
                    .replace("{{imageName}}", fileNameOnly)
                    .replace("{{modelImage}}", model)
                    .replace("{{prompt}}", prompt);
        } else if (productId1 != null && productId2 != null) {
            Product product1 = productRepository.findById(productId1)
                    .orElseThrow(() -> new RuntimeException("Product not found - id : " + productId1));
            Product product2 = productRepository.findById(productId2)
                    .orElseThrow(() -> new RuntimeException("Product not found - id : " + productId2));

            Integer categoryId1 = product1.getCategory().getCategoryId();
            Integer categoryId2 = product2.getCategory().getCategoryId();


            if (categoryId1.equals(categoryId2)) {
                throw new RuntimeException("같은 카테고리는 선택할 수 없습니다. ("
                        + categoryId1 + " & " + categoryId2 + ")");
            }
// 드레스 (9~12)
            Set<Integer> dress = Set.of(9, 10, 11, 12);
            if (dress.contains(categoryId1) || dress.contains(categoryId2)) {
                throw new RuntimeException("드레스는 단일착용만 가능합니다. ("
                        + categoryId1 + " & " + categoryId2 + ")");
            }
// 그룹1: 1,2,3,7,8
            Set<Integer> group1 = Set.of(1, 2, 3, 7, 8);
// 그룹2: 4,5,6
            Set<Integer> group2 = Set.of(4, 5, 6, 13, 14);
// 서로 같은 그룹 안에서 두 개가 동시에 오면 불가
            if (group1.contains(categoryId1) && group1.contains(categoryId2)) {
                throw new RuntimeException("같은 그룹(상의 그룹)에서 두 개를 선택할 수 없습니다. ("
                        + categoryId1 + " & " + categoryId2 + ")");
            }

            if (group2.contains(categoryId1) && group2.contains(categoryId2)) {
                throw new RuntimeException("같은 그룹(하의 그룹)에서 두 개를 선택할 수 없습니다. ("
                        + categoryId1 + " & " + categoryId2 + ")");
            }


// prompt1 설정
            prompt1 = switch (product1.getCategory().getCategoryId()) {
                case 1, 2, 3 -> "black tshirt";
                case 7, 8-> "black cardigan";
                case 4, 5, 6 -> "pants";
                case 9, 10, 11, 12-> "dress";
                case 13, 14-> "skirt";
                default -> "clothes";
            };

// prompt2 설정
            prompt2 = switch (product2.getCategory().getCategoryId()) {
                case 1, 2, 3 -> "black tshirt";
                case 7, 8-> "black cardigan";
                case 4, 5, 6 -> "pants";
                case 9, 10, 11, 12-> "dress";
                case 13, 14-> "skirt";
                default -> "clothes";
            };

// model 설정
            int cat1 = product1.getCategory().getCategoryId();
            int cat2 = product2.getCategory().getCategoryId();
            model = null;

// 순서 상관 없도록 작은 값, 큰 값으로 정렬
            int first = Math.min(cat1, cat2);
            int second = Math.max(cat1, cat2);

// 조합별 모델 매핑
            if (first == 1 && second == 4) model = "STOPC.png";
            else if (first == 1 && second == 5) model = "STOPA.png";
            else if (first == 1 && second == 6) model = "STOPB.png";
            else if (first == 1 && second == 13) model = "SSKIRTA.png";
            else if (first == 1 && second == 14) model = "LSKIRTA.png";

            else if (first == 2 && second == 4) model = "LSTOPC.png";
            else if (first == 2 && second == 5) model = "LSTOPA.png";
            else if (first == 2 && second == 6) model = "LSTOPB.png";
            else if (first == 2 && second == 13) model = "SSKIRTB.png";
            else if (first == 2 && second == 14) model = "LSKIRTB.png";

            else if (first == 3 && second == 4) model = "LWTOPC.png";
            else if (first == 3 && second == 5) model = "LWTOPA.png";
            else if (first == 3 && second == 6) model = "LWTOPB.png";
            else if (first == 3 && second == 13) model = "SSKIRTC.png";
            else if (first == 3 && second == 14) model = "LSKIRTC.png";

            else if (first == 4 && (second == 7)) model = "SOUTERWEARA.png";
            else if (first == 5 && (second == 7)) model = "SOUTERWEARB.png";
            else if (first == 6 && (second == 7)) model = "SOUTERWEARC.png";

            else if (first == 4 && (second == 8)) model = "LOUTERWEARA.png";
            else if (first == 5 && (second == 8)) model = "LOUTERWEARB.png";
            else if (first == 6 && (second == 8)) model = "LOUTERWEARC.png";

            else if (first == 4 && (second == 13)) model = "SOUTERWEARE.png";
            else if (first == 5 && (second == 13)) model = "SOUTERWEARE.png";
            else if (first == 6 && (second == 13)) model = "SOUTERWEARE.png";

            else if (first == 4 && (second == 14)) model = "SOUTERWEARD.png";
            else if (first == 5 && (second == 14)) model = "SOUTERWEARD.png";
            else if (first == 6 && (second == 14)) model = "SOUTERWEARD.png";

            if (model != null) {
                if (memberBodyShape == BodyShape.STRAIGHT) {
                    model = "1" + model;
                } else if (memberBodyShape == BodyShape.NATURAL) {
                    model = "2" + model;
                } else if (memberBodyShape == BodyShape.WAVE) {
                    model = "3" + model;
                }
            }


            imageName1 = productImageRepository.findByProductAndIsThumbnailTrue(product1)
                    .orElseThrow(() -> new RuntimeException("Thumbnail not found for product1"))
                    .getImageUrl();
            imageName2 = productImageRepository.findByProductAndIsThumbnailTrue(product2)
                    .orElseThrow(() -> new RuntimeException("Thumbnail not found for product2"))
                    .getImageUrl();



            fileNameOnly1 = stripPrefix(imageName1, getProductPrefix(product1));
            fileNameOnly2 = stripPrefix(imageName2, getProductPrefix(product2));

            // 워크플로우 JSON 생성
            workflowJson = loadWorkflowFromResource("v2_one_person_two_clothes.json")
                    .replace("{{modelImage}}", model)
                    .replace("{{imageName1}}", fileNameOnly1 != null ? fileNameOnly1 : "")
                    .replace("{{imageName2}}", fileNameOnly2 != null ? fileNameOnly2 : "")
                    .replace("{{prompt1}}", prompt1)
                    .replace("{{prompt2}}", prompt2);

        }else{
            throw new IllegalArgumentException("상품 선택 옵션이 잘못되었습니다.");
        }

        // Google Drive 새로고침
        refreshGoogleDrive();

        // 1. 워크플로우 실행
        String promptId = sendWorkflow(workflowJson);

        // 2. 완료 대기
        waitUntilComplete(promptId);

        List<String> generatedOutputImageFilenameList = getGeneratedOutputImageFilenameList(promptId);

        String fileName = generatedOutputImageFilenameList.stream()
                .max(Comparator.comparingInt(name -> {
                    // "ComfyUI_숫자_.png" 에서 숫자만 추출
                    int start = name.indexOf('_') + 1;
                    int end = name.lastIndexOf('_');
                    return Integer.parseInt(name.substring(start, end));
                }))
                .orElseThrow(() -> new IllegalArgumentException("파일명이 없습니다."));

        downloadImageToS3(fileName);
        System.out.println("--------------------------------------------");
//        System.out.println("prompt1: " + prompt1);
//        System.out.println("prompt2: " + prompt2);
//        System.out.println("model: " + model);
        System.out.println(fileName);
//        System.out.println("filename2: " + fileNameOnly2);
//        System.out.println("fittingimage: " + generatedOutputImageFilenameList);
        System.out.println("--------------------------------------------");
        return fileName;
    }

    private String getProductPrefix(Product product) {
        return "https://tryonx.s3.ap-northeast-2.amazonaws.com/product/" + product.getProductCode() + "/";
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
                    System.out.println("✅ 이미지 업로드 완료: " + imageUrl);
                    return;
                }
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("⏳ [대기 중] 이미지가 아직 준비되지 않음. 재시도 " + attempt);
            }

            Thread.sleep(1000);
        }

        throw new IOException("❌ 이미지 다운로드 실패: " + filename);
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
