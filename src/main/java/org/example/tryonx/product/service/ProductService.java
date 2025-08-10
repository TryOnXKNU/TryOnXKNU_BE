package org.example.tryonx.product.service;

import org.example.tryonx.category.Category;
import org.example.tryonx.category.CategoryRepository;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.like.dto.ProductDto;
import org.example.tryonx.like.repository.LikeRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final LikeRepository likeRepository;
    private final MeasurementRepository measurementRepository;
    private final ReviewService reviewService;

    public ProductService(ProductRepository productRepository, ProductItemRepository productItemRepository, ProductImageRepository productImageRepository, CategoryRepository categoryRepository, LikeRepository likeRepository, MeasurementRepository measurementRepository, ReviewService reviewService) {
        this.productRepository = productRepository;
        this.productItemRepository = productItemRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.likeRepository = likeRepository;
        this.measurementRepository = measurementRepository;
        this.reviewService = reviewService;
    }
    public Product createProduct(ProductCreateRequestDto dto, List<MultipartFile> images) {
        String middleCode;
        if(dto.getCategoryId() == 1)
            middleCode = "top";
        else if(dto.getCategoryId() == 2)
            middleCode = "bot";
        else if(dto.getCategoryId() == 3)
            middleCode = "dre";
        else if(dto.getCategoryId() == 4)
            middleCode = "out";
        else
            middleCode = "acc";

        // productCode 없이 먼저 저장
        Product product = Product.builder()
                .productName(dto.getName())
                .description(dto.getDescription())
                .category(categoryRepository.findById(dto.getCategoryId()).orElse(null))
                .discountRate(dto.getDiscountRate())
                .price(dto.getPrice())
                .bodyShape(dto.getBodyShape())
                .build();

        productRepository.save(product); // 여기서 productId 생성됨

        // 생성된 productId로 productCode 생성 후 다시 저장
        String paddedId = String.format("%05d", product.getProductId());
        String productCode = "ax" + middleCode + paddedId;
        product.setProductCode(productCode);
        productRepository.save(product);

        // 아이템 저장
        dto.getProductItemInfoDtos().forEach(itemDto -> {
            createProductItem(product, itemDto);
        });

        // 이미지 저장
        if (images != null && !images.isEmpty()) {
            boolean isFirst = true;
            for (MultipartFile image : images) {
                String filename = UUID.randomUUID() + "_" + image.getOriginalFilename();
                Path savePath = Paths.get("upload/product").resolve(filename);

                try {
                    Files.createDirectories(savePath.getParent());
                    image.transferTo(savePath);

                    ProductImage productImage = ProductImage.builder()
                            .product(product)
                            .imageUrl("/upload/product/" + filename)
                            .isThumbnail(isFirst)
                            .build();

                    productImageRepository.save(productImage);
                    isFirst = false;
                } catch (IOException e) {
                    throw new RuntimeException("이미지 저장 실패", e);
                }
            }
        }

        return product;
    }


    public List<ProductListResponseDto> getAllProducts() {
        List<Product> productList = productRepository.findAll();
        return this.getProductListResponseDto(productList);
    }

    public List<ProductListResponseDto> getProductsByCategoryId(Integer categoryId){
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalStateException("해당 카테고리가 존재하지 않습니다."));
        List<Product> byCategory = productRepository.findByCategory(category);
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

        List<ProductReviewDto> reviewsPreview = reviewService.getProductReviewsPreview(productId);
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
                reviewsPreview
        );
    }

    private void createProductItem(Product product, ProductItemInfoDto dto) {
        if (dto.getStock() == null || dto.getStock() <= 0) return;

        ProductItem item = ProductItem.builder()
                .product(product)
                .size(dto.getSize())
                .stock(dto.getStock())
                .status(ProductStatus.AVAILABLE)
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

    private List<ProductListResponseDto> getProductListResponseDto(List<Product> products) {
        return products.stream()
                .map(product -> {
                    ProductImage image = productImageRepository.findByProductAndIsThumbnailTrue(product)
                            .orElseThrow(() -> new IllegalStateException("썸네일 이미지 혹은 상품이 없습니다."));
                    return new ProductListResponseDto(
                            product.getProductId(),
                            product.getProductName(),
                            product.getPrice(),
                            likeRepository.countByProduct(product),
                            product.getCategory().getCategoryId(),
                            image.getImageUrl()
                    );
                }).toList();
    }
    @Transactional(readOnly = true)
    public List<ProductDto> getTopLikedProducts(int size) {
        return productRepository.findAll().stream()
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
}
