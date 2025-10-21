package org.example.tryonx.product.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.category.Category;
import org.example.tryonx.category.CategoryRepository;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.fitting.domain.ProductFitting;
import org.example.tryonx.fitting.repository.ProductFittingRepository;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.like.dto.ProductDto;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.product.domain.Measurement;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.dto.ProductCreateRequestDto;
import org.example.tryonx.product.dto.ProductItemInfoDto;
import org.example.tryonx.product.dto.ProductListResponseDto;
import org.example.tryonx.product.dto.ProductResponseDto;
import org.example.tryonx.product.repository.MeasurementRepository;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.example.tryonx.review.dto.ProductReviewDto;
import org.example.tryonx.review.service.ReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final LikeRepository likeRepository;
    private final MeasurementRepository measurementRepository;
    private final ReviewService reviewService;
    private final MemberRepository memberRepository;
    private final ProductFittingRepository productFittingRepository;
    private final AmazonS3 amazonS3;

//    @Transactional
//    public Product createProduct(ProductCreateRequestDto dto, List<MultipartFile> images) {
//
//        if (productRepository.existsByProductName(dto.getName())) {
//            throw new IllegalArgumentException("이미 존재하는 상품명입니다: " + dto.getName());
//        }
//
//        String middleCode = getMiddleCode(dto);
//
//        // productCode 없이 먼저 저장
//        Product product = Product.builder()
//                .productName(dto.getName())
//                .description(dto.getDescription())
//                .category(categoryRepository.findById(dto.getCategoryId()).orElse(null))
//                .discountRate(dto.getDiscountRate())
//                .price(dto.getPrice())
//                .bodyShape(dto.getBodyShape())
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        productRepository.save(product); // 여기서 productId 생성됨
//
//        // 생성된 productId로 productCode 생성 후 다시 저장
//        String paddedId = String.format("%05d", product.getProductId());
//        String productCode = "ax" + middleCode + paddedId;
//        product.setProductCode(productCode);
//        productRepository.save(product);
//
//        // 아이템 저장
//        dto.getProductItemInfoDtos().forEach(itemDto -> {
//            createProductItem(product, itemDto);
//        });
//
//        // 이미지 저장
//        if (images != null && !images.isEmpty()) {
//            boolean isFirst = true;
//            for (MultipartFile image : images) {
//                String filename = image.getOriginalFilename();
//                Path savePath = Paths.get("upload/product").resolve(filename);
//
//                try {
//                    Files.createDirectories(savePath.getParent());
//                    image.transferTo(savePath);
//
//                    ProductImage productImage = ProductImage.builder()
//                            .product(product)
//                            .imageUrl("/upload/product/" + filename)
//                            .isThumbnail(isFirst)
//                            .build();
//
//                    productImageRepository.save(productImage);
//                    isFirst = false;
//                } catch (IOException e) {
//                    throw new RuntimeException("이미지 저장 실패", e);
//                }
//            }
//        }
//
//        return product;
//    }

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public Product createProduct(ProductCreateRequestDto dto, List<MultipartFile> images) {

        if (productRepository.existsByProductName(dto.getName())) {
            throw new IllegalArgumentException("이미 존재하는 상품명입니다: " + dto.getName());
        }

        String middleCode = getMiddleCode(dto);

        // productCode 없이 먼저 저장
        Product product = Product.builder()
                .productName(dto.getName())
                .description(dto.getDescription())
                .category(categoryRepository.findById(dto.getCategoryId()).orElse(null))
                .discountRate(dto.getDiscountRate())
                .price(dto.getPrice())
                .bodyShape(dto.getBodyShape())
                .createdAt(LocalDateTime.now())
                .build();

        productRepository.save(product); // 여기서 productId 생성됨

        // productCode 생성
        String paddedId = String.format("%05d", product.getProductId());
        String productCode = "ax" + middleCode + paddedId;
        product.setProductCode(productCode);
        productRepository.save(product);

        // 아이템 저장
        dto.getProductItemInfoDtos().forEach(itemDto -> createProductItem(product, itemDto));

        // 이미지 S3 업로드
        if (images != null && !images.isEmpty()) {
            boolean isFirst = true;
            for (MultipartFile image : images) {
                String originalFilename = image.getOriginalFilename();
                String s3Key = "product/" + productCode + "/" + originalFilename;

                try {
                    amazonS3.putObject(
                            new PutObjectRequest(bucketName, s3Key, image.getInputStream(), null)
                    );

                    String imageUrl = amazonS3.getUrl(bucketName, s3Key).toString();

                    ProductImage productImage = ProductImage.builder()
                            .product(product)
                            .imageUrl(imageUrl)  // S3 URL 저장
                            .isThumbnail(isFirst)
                            .build();

                    productImageRepository.save(productImage);
                    isFirst = false;

                } catch (IOException e) {
                    throw new RuntimeException("S3 업로드 실패: " + originalFilename, e);
                }
            }
        }

        return product;
    }


    private String getMiddleCode(ProductCreateRequestDto dto) {
        String middleCode;
        if(dto.getCategoryId() == 1)
            middleCode = "st";
        else if(dto.getCategoryId() == 2)
            middleCode = "lst";
        else if(dto.getCategoryId() == 3)
            middleCode = "lwt";
        else if(dto.getCategoryId() == 4)
            middleCode = "spa";
        else if(dto.getCategoryId() == 5)
            middleCode = "lspa";
        else if(dto.getCategoryId() == 6)
            middleCode = "lwpa";
        else if(dto.getCategoryId() == 7)
            middleCode = "souter";
        else if(dto.getCategoryId() == 8)
            middleCode = "louter";
        else if(dto.getCategoryId() == 9)
            middleCode = "ssdre";
        else if(dto.getCategoryId() == 10)
            middleCode = "sldre";
        else if(dto.getCategoryId() == 11)
            middleCode = "lsdre";
        else if(dto.getCategoryId() == 12)
            middleCode = "lldre";
        else if(dto.getCategoryId() == 13)
            middleCode = "sskt";
        else
            middleCode = "lskt";
        return middleCode;
    }


//    public List<ProductListResponseDto> getAllProducts() {
//        List<Product> productList = productRepository.findAll();
//        return this.getProductListResponseDto(productList);
//    }

    public List<ProductListResponseDto> getAllAvailableProducts() {
        List<Product> productList = productRepository.findAllWithAnyAvailableItem();
        return this.getProductListResponseAvailabeDto(productList);
    }

    public List<ProductListResponseDto> getProductsByCategoryId(Integer categoryId){
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalStateException("해당 카테고리가 존재하지 않습니다."));
        List<Product> byCategory = productRepository.findByCategoryWithAnyAvailableItem(category);
        return this.getProductListResponseDto(byCategory);
    }

    public ProductResponseDto getProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalStateException("해당 상품이 없습니다."));

        List<ProductItem> productItems = productItemRepository.findByProduct(product);

        List<ProductItemInfoDto> itemDtos = productItems.stream().map(item -> {
            Measurement m = measurementRepository.findByProductItem(item)
                    .orElseThrow(() -> new IllegalStateException("해당 실측 내역이 없습니다."));
            return new ProductItemInfoDto(
                    item.getSize(),
                    item.getStock(),
                    item.getStatus(),
                    m != null ? m.getLength() : null,
                    m != null ? m.getShoulder() : null,
                    m != null ? m.getChest() : null,
                    m != null ? m.getSleeveLength() : null,
                    m != null ? m.getWaist() : null,
                    m != null ? m.getThigh() : null,
                    m != null ? m.getRise() : null,
                    m != null ? m.getHem() : null,
                    m != null ? m.getHip() : null
            );
        }).toList();

        List<String> imageUrls = productImageRepository.findByProduct(product)
                .stream().map(ProductImage::getImageUrl).toList();

        Double avg = reviewService.getAverageRatingByProductId(productId);
        Integer cnt = reviewService.getReviewCountByProductId(productId);

        List<ProductReviewDto> reviewsPreview = reviewService.getProductReviewsPreview(productId);

        BigDecimal discountRate = product.getDiscountRate();
        BigDecimal price = product.getPrice();
        BigDecimal discountedPrice = price.multiply(discountRate.divide(BigDecimal.valueOf(100)));

        Integer pointForOrder = price
                .subtract(discountedPrice)
                .multiply(BigDecimal.valueOf(0.01))
                .intValue();
        Integer pointForReview = price
                .subtract(discountedPrice)
                .multiply(BigDecimal.valueOf(0.05))
                .intValue();

        // 피팅 이미지 여러 장 가져오기
        List<String> fittingImageUrls = productFittingRepository.findByProductOrderBySequenceAsc(product)
                .stream()
                .map(ProductFitting::getFittingImageUrl)
                .toList();

        return new ProductResponseDto(
                product.getProductId(),
                product.getProductName(),
                product.getPrice(),
                product.getDiscountRate(),
                likeRepository.countByProduct(product),
                product.getCategory().getCategoryId(),
                product.getDescription(),
                product.getBodyShape(),
                imageUrls,
                itemDtos,
                reviewsPreview,
                avg,
                cnt,
                pointForOrder,
                pointForReview,
                fittingImageUrls
        );
    }

    private void createProductItem(Product product, ProductItemInfoDto dto) {
        if (dto.getStock() == null) return;
        ProductStatus productStatus = null;

        if(dto.getStock() > 0) {
            productStatus = ProductStatus.AVAILABLE;
        }else if(dto.getStock() == 0){
            productStatus = ProductStatus.HIDDEN;
        }

        ProductItem item = ProductItem.builder()
                .product(product)
                .size(dto.getSize())
                .stock(dto.getStock())
                .status(productStatus)
                .build();

        productItemRepository.save(item);

        Measurement measurement = Measurement.builder()
                .productItem(item)
                .length(dto.getLength())
                .shoulder(dto.getShoulder())
                .chest(dto.getChest())
                .sleeveLength(dto.getSleeveLength())
                .waist(dto.getWaist())
                .thigh(dto.getThigh())
                .rise(dto.getRise())
                .hem(dto.getHem())
                .hip((dto.getHip()))
                .build();

        measurementRepository.save(measurement);
    }
    private Double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal calcDiscountPrice(BigDecimal price, BigDecimal discountRate) {
        if (price == null) return null;
        if (discountRate == null) return price;

        // DB에 10(%)로 저장된 경우 0.10으로 변환
        BigDecimal rate = discountRate.compareTo(BigDecimal.ONE) > 0
                ? discountRate.divide(BigDecimal.valueOf(100))
                : discountRate;

        return price.multiply(BigDecimal.ONE.subtract(rate))
                .setScale(0, RoundingMode.HALF_UP); // 원 단위 반올림
    }

    private List<ProductListResponseDto> getProductListResponseDto(List<Product> products) {
        return products.stream()
                .map(product -> {
                    ProductImage image = productImageRepository.findByProductAndIsThumbnailTrue(product)
                            .orElseThrow(() -> new IllegalStateException("썸네일 이미지 혹은 상품이 없습니다."));

                    BigDecimal price = product.getPrice();
                    BigDecimal discountRate = product.getDiscountRate();
                    BigDecimal discountPrice = calcDiscountPrice(price, discountRate);

                    Double avg = reviewService.getAverageRatingByProductId(product.getProductId());
                    Integer cnt = reviewService.getReviewCountByProductId(product.getProductId());

                    return new ProductListResponseDto(
                            product.getProductId(),
                            product.getProductName(),
                            product.getPrice(),
                            likeRepository.countByProduct(product),
                            product.getCategory().getCategoryId(),
                            image.getImageUrl(),
                            discountRate,
                            discountPrice,
                            avg,
                            cnt,
                            product.getCreatedAt()
                    );
                }).toList();
    }
    private List<ProductListResponseDto> getProductListResponseAvailabeDto(List<Product> products) {
        return products.stream()
                .map(product -> {
                    ProductImage image = productImageRepository.findByProductAndIsThumbnailTrue(product)
                            .orElseThrow(() -> new IllegalStateException("썸네일 이미지 혹은 상품이 없습니다."));
                    BigDecimal price = product.getPrice();
                    BigDecimal discountRate = product.getDiscountRate();
                    BigDecimal discountPrice = calcDiscountPrice(price, discountRate);

                    Double avg = reviewService.getAverageRatingByProductId(product.getProductId());
                    Integer cnt = reviewService.getReviewCountByProductId(product.getProductId());

                    return new ProductListResponseDto(
                            product.getProductId(),
                            product.getProductName(),
                            product.getPrice(),
                            likeRepository.countByProduct(product),
                            product.getCategory().getCategoryId(),
                            image.getImageUrl(),
                            discountRate,
                            discountPrice,
                            avg,
                            cnt,
                            product.getCreatedAt()
                    );
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getTopLikedProducts(int size) {
        return productRepository.findAllWithAnyAvailableItem().stream()
                .map(p -> new Ranked(p, likeRepository.countByProduct(p))) // 기존 카운터 그대로 사용
                .sorted(
                        Comparator.<Ranked, Long>comparing(r -> r.likeCount)
                                .reversed()
                                .thenComparing(r -> r.product.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .limit(size)
                .map(r -> ProductDto.of(r.product, r.likeCount)) // 네가 쓰던 of(dto) 그대로
                .toList();
    }

    private record Ranked(Product product, long likeCount) {}

    @Transactional(readOnly = true)
    public List<ProductDto> getSimilarByBodyShape(String email, int size) {
        var member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음"));

        var shape = member.getBodyShape();

        // 전체 후보 불러오기 (Pageable X)
        List<Product> products = (shape != null)
                ? productRepository.findByBodyShapeWithAnyAvailableItem(shape)
                : productRepository.findAllWithAnyAvailableItem();

        // 랜덤 섞고 size개만 추출
        Collections.shuffle(products);

        return products.stream()
                .limit(size)
                .map(p -> ProductDto.of(p, likeRepository.countByProduct(p)))
                .toList();
    }
}
