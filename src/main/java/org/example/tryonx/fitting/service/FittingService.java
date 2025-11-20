package org.example.tryonx.fitting.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.category.CategoryRepository;
import org.example.tryonx.enums.BodyShape;
import org.example.tryonx.fitting.dto.*;
import org.example.tryonx.image.domain.MemberClothesImage;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.MemberClothesImageRepository;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.like.domain.Like;
import org.example.tryonx.like.dto.ProductDto;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.repository.ProductRepository;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FittingService {
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final MemberClothesImageRepository memberClothesImageRepository;
    private final LikeRepository likeRepository;
    private final AmazonS3 amazonS3;
    @Autowired
    private RestTemplate restTemplate;


    @Value("${ngrok.url}")
    private String baseUrl;
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public FittingResponse getFittingPageData(String email) {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));

        // === memberInfo ===
        FittingMemberInfo memberInfo = getFittingMemberInfo(member);

        // === 찜한 상품 productInfos ===
        List<FittingProductInfo> productInfos = likeRepository.findByMember(member)
                .stream()
                .map(Like::getProduct)
                .distinct()
                .map(p -> {
                    String imgUrl = productImageRepository.findByProductAndIsThumbnailTrue(p)
                            .orElseThrow(() -> new RuntimeException("썸네일 이미지 없습니다."))
                            .getImageUrl();

                    boolean best = (member.getBodyShape() == p.getBodyShape());

                    return new FittingProductInfo(
                            p.getProductId(),
                            p.getCategory().getCategoryId(),
                            p.getProductName(),
                            imgUrl,
                            best
                    );
                })
                .toList();

        // === 커스텀 의상 customClothesInfos ===
        List<CustomClothesInfo> customClothesInfos =
                memberClothesImageRepository.findByMember_MemberId(member.getMemberId())
                        .stream()
                        .map(img -> new CustomClothesInfo(
                                img.getMemberClothesId(),  // clothesId (x15 같은 값)
                                img.getCategoryId(),
                                img.getName(),
                                img.getImageUrl()
                        ))
                        .toList();

        // === 최종 응답 ===
        return new FittingResponse(
                memberInfo,
                productInfos,
                customClothesInfos
        );
    }


    @NotNull
    private static FittingMemberInfo getFittingMemberInfo(Member member) {
        BodyShape bodyShape = member.getBodyShape();
        String modelImage = null;

        if (bodyShape != null) {
            if (bodyShape.equals(BodyShape.STRAIGHT))
                modelImage = "upload/model/straight.png";
            else if (bodyShape.equals(BodyShape.WAVE))
                modelImage = "upload/model/wave.png";
            else if (bodyShape.equals(BodyShape.NATURAL))
                modelImage = "upload/model/natural.png";
        }

        FittingMemberInfo memberInfo = new FittingMemberInfo(
                member.getMemberId(),
                member.getWeight(),
                member.getHeight(),
                member.getBodyShape(),
                modelImage
        );
        return memberInfo;
    }

    @Transactional
    public BodyShape updateBodyShape(String email, BodyShapeRequest bodyShapeRequest) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 이메일의 사용자가 없습니다."));
        BodyShape bodyShape = bodyShapeRequest.bodyShape();
        if (bodyShape != null && !Objects.equals(member.getBodyShape(), bodyShape)) {
            member.setBodyShape(bodyShape);
            memberRepository.save(member);
        }
        return bodyShape;
    }

    @Transactional
    public void updateMemberBodyShape(Long memberId, String bodyTypeRaw) {
        if (memberId == null || bodyTypeRaw == null) return;

        BodyShape shape = mapToEnum(bodyTypeRaw);
        if (shape == null) return;

        Member m = memberRepository.findById(memberId)
                .orElse(null);
        if (m == null) return;

        m.setBodyShape(shape); // JPA dirty checking으로 update
        // 별도 save() 불필요 (영속 상태라면)
    }



    @Transactional
    public MemberClothesImage addMemberClothesImage(
            String email,
            String name,
            Integer categoryId,
            MultipartFile myClothesImage
    ) {

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Member not found."));

        String originalFilename = myClothesImage.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID() + "_" + originalFilename;

        String s3Key = "member/" + member.getMemberId() + "/" + uniqueFilename;

        try {
            byte[] bytes = myClothesImage.getBytes();

            // 1) S3 업로드
            uploadToS3(bytes, myClothesImage.getContentType(), s3Key);

            // 2) S3 URL
            String s3Url = amazonS3.getUrl(bucketName, s3Key).toString();

            // 3) Comfy 전송
            sendToComfyServer(bytes, uniqueFilename);

            MemberClothesImage image = MemberClothesImage.builder()
                    .member(member)
                    .name(name)
                    .categoryId(categoryId)
                    .imageUrl(s3Url)
                    // memberClothesId 아직 없음
                    .build();

            memberClothesImageRepository.save(image);


            String memberClothesId = "x" + image.getImageId();
            image.setMemberClothesId(memberClothesId);

            // 업데이트 저장
            memberClothesImageRepository.save(image);

            return image;

        } catch (IOException e) {
            throw new RuntimeException("이미지 처리 실패: " + originalFilename, e);
        }
    }


    /**
     * S3 업로드 로직 분리
     */
    private void uploadToS3(byte[] fileBytes, String contentType, String s3Key) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(fileBytes.length);

        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            amazonS3.putObject(new PutObjectRequest(bucketName, s3Key, inputStream, metadata));
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패: " + s3Key, e);
        }
    }

    @Transactional
    public void deleteMemberClothesImage(String email, String memberClothesId) {

        // 1) Member 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("해당 회원이 존재하지 않습니다."));

        // 2) 이미지 조회 + 본인 소유인지 검증
        MemberClothesImage image = memberClothesImageRepository
                .findByMemberAndMemberClothesId(member, memberClothesId)
                .orElseThrow(() -> new IllegalStateException("해당 옷 이미지가 존재하지 않거나 권한이 없습니다."));

        // 3) S3 삭제
        String imageUrl = image.getImageUrl();
        String s3Key = extractS3KeyFromUrl(imageUrl);
        amazonS3.deleteObject(bucketName, s3Key);

        // 4) DB 삭제
        memberClothesImageRepository.delete(image);
    }
    private String extractS3KeyFromUrl(String imageUrl) {
        // URL 예: https://tryonx.s3.ap-northeast-2.amazonaws.com/member/2/abcd.png
        return imageUrl.substring(imageUrl.indexOf("member/"));
    }

    /**
     * Comfy 서버로 이미지 전달
     */
    private void sendToComfyServer(byte[] imageBytes, String filename) {
        String COMFY_URL = baseUrl + "/upload/image";

        try {
            ByteArrayResource fileResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", fileResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(COMFY_URL, requestEntity, String.class);

            System.out.println("Comfy 전송 성공: " + response.getBody());

        } catch (Exception e) {
            System.err.println("Comfy 전송 실패 (" + filename + "): " + e.getMessage());
        }
    }


    /**
     * 업로드 대상(파일 + 카테고리) 묶기 위한 record
     */
    private record UploadTarget(MultipartFile file, Integer categoryId) {}

    private BodyShape mapToEnum(String s) {
        String k = s.trim().toUpperCase();
        switch (k) {
            case "STRAIGHT": return BodyShape.STRAIGHT;
            case "WAVE":     return BodyShape.WAVE;
            case "NATURAL":  return BodyShape.NATURAL;
            default:         return null; // UNKNOWN 등은 저장하지 않음
        }
    }


}
