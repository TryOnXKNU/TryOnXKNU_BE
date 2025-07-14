package org.example.tryonx.product.service;

import org.example.tryonx.category.Category;
import org.example.tryonx.category.CategoryRepository;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.enums.Size;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.like.domain.Like;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final LikeRepository likeRepository;
    private final MeasurementRepository measurementRepository;

    public ProductService(ProductRepository productRepository, ProductItemRepository productItemRepository, ProductImageRepository productImageRepository, CategoryRepository categoryRepository, LikeRepository likeRepository, MeasurementRepository measurementRepository) {
        this.productRepository = productRepository;
        this.productItemRepository = productItemRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.likeRepository = likeRepository;
        this.measurementRepository = measurementRepository;
    }
    public Product createProduct(ProductCreateRequestDto dto, List<MultipartFile> images) {
        if (productRepository.findByProductCode(dto.getCode()).isPresent()) {
            throw new IllegalStateException("해당 코드의 상품 이미 존재");
        }

        Product product = Product.builder()
                .productCode(dto.getCode())
                .productName(dto.getName())
                .description(dto.getDescription())
                .category(categoryRepository.findById(dto.getCategoryId()).orElse(null))
                .discountRate(dto.getDiscountRate())
                .price(dto.getPrice())
                .bodyShape(dto.getBodyShape())
                .build();
        productRepository.save(product);
        dto.getProductItemInfoDtos().forEach(itemDto -> {
            createProductItem(product, itemDto);
        });

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
                            .isThumbnail(isFirst)  // 첫 번째 이미지 썸네일로
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

        return new ProductResponseDto(
                product.getProductId(),
                product.getProductName(),
                product.getPrice(),
                likeRepository.countByProduct(product),
                product.getCategory().getCategoryId(),
                product.getDescription(),
                imageUrls,
                itemDtos  // 사이즈 + 실측 포함된 목록
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
                .sleeveLength(dto.getSleeve_length())
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
}
