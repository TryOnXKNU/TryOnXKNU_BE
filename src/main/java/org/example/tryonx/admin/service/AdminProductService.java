package org.example.tryonx.admin.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.admin.dto.ProductDetailUpdateDto;
import org.example.tryonx.admin.dto.ProductItemDto;
import org.example.tryonx.admin.dto.ProductListDto;
import org.example.tryonx.admin.dto.ProductStockAndStateUpdateDto;
import org.example.tryonx.cart.repository.CartItemRepository;
import org.example.tryonx.category.Category;
import org.example.tryonx.category.CategoryRepository;
import org.example.tryonx.enums.ProductStatus;
import org.example.tryonx.image.domain.ProductImage;
import org.example.tryonx.image.repository.ProductImageRepository;
import org.example.tryonx.like.repository.LikeRepository;
import org.example.tryonx.orders.order.repository.OrderItemRepository;
import org.example.tryonx.product.domain.Measurement;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.product.domain.ProductItem;
import org.example.tryonx.product.dto.ProductItemInfoDto;
import org.example.tryonx.product.dto.ProductResponseDto;
import org.example.tryonx.product.repository.MeasurementRepository;
import org.example.tryonx.product.repository.ProductItemRepository;
import org.example.tryonx.product.repository.ProductRepository;
import org.example.tryonx.review.dto.ProductReviewDto;
import org.example.tryonx.review.service.ReviewService;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class AdminProductService {
    private final ProductRepository productRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final MeasurementRepository measurementRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final LikeRepository likeRepository;
    private final ReviewService reviewService;


    public List<ProductListDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return this.getProductList(products);
    }

    public ProductResponseDto getProductDetail(Integer productId) {
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

        // 리뷰 프리뷰
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

    @Transactional
    public void updateProductStockAndState(Integer productId, ProductStockAndStateUpdateDto productStockAndStateUpdateDto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        productStockAndStateUpdateDto.getItem().forEach(dto -> {
            ProductItem productItem = productItemRepository.findByProductAndSize(product, dto.getSize())
                    .orElseThrow(() -> new RuntimeException("Product item not found"));
            productItem.setStatus(dto.getStatus());
        });
    }

    @Transactional
    public void updateProductDetail(Integer productId, ProductDetailUpdateDto dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Category category = product.getCategory();
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
        }

        product.updateProduct(
                dto.getPrice() != null ? dto.getPrice() : product.getPrice(),
                dto.getDiscountRate() != null ? dto.getDiscountRate() : product.getDiscountRate(),
                dto.getBodyShape() != null ? dto.getBodyShape() : product.getBodyShape(),
                category
        );


        // --- ProductItem & Measurement 업데이트 ---
        dto.getProductItemInfoDtos().forEach(itemDto -> {
            ProductItem productItem = productItemRepository.findByProductAndSize(product, itemDto.getSize())
                    .orElseThrow(() -> new RuntimeException("Product item  (size :" + itemDto.getSize() +  ")not found"));

            if (itemDto.getStock() != null) {
                productItem.setStock(itemDto.getStock());
            }
            if (itemDto.getStatus() != null) {
                if(itemDto.getStock() == 0)
                    productItem.setStatus(ProductStatus.SOLDOUT);
                else
                    productItem.setStatus(itemDto.getStatus());
            }

            Measurement measurement = measurementRepository.findByProductItem(productItem)
                    .orElseGet(() -> Measurement.builder().productItem(productItem).build());

            // updateMeasurement에 null 전달하지 않도록 래핑
            measurement.updateMeasurement(
                    itemDto.getLength() != null ? itemDto.getLength() : measurement.getLength(),
                    itemDto.getShoulder() != null ? itemDto.getShoulder() : measurement.getShoulder(),
                    itemDto.getChest() != null ? itemDto.getChest() : measurement.getChest(),
                    itemDto.getSleeveLength() != null ? itemDto.getSleeveLength() : measurement.getSleeveLength(),
                    itemDto.getWaist() != null ? itemDto.getWaist() : measurement.getWaist(),
                    itemDto.getThigh() != null ? itemDto.getThigh() : measurement.getThigh(),
                    itemDto.getRise() != null ? itemDto.getRise() : measurement.getRise(),
                    itemDto.getHem() != null ? itemDto.getHem() : measurement.getHem(),
                    itemDto.getHip() != null ? itemDto.getHip() : measurement.getHip()
            );

            measurementRepository.save(measurement);
        });
    }

    @Transactional
    public void deleteProductImage(String imgUrl) {
        ProductImage byImageUrl = productImageRepository.findByImageUrl(imgUrl);

        if (byImageUrl == null) {
            throw new RuntimeException("해당 이미지가 없습니다.");
        }

        List<ProductImage> productImages = productImageRepository.findByProduct(byImageUrl.getProduct());

        if (byImageUrl.getIsThumbnail()) {
            // 삭제될 이미지를 제외한 목록
            List<ProductImage> remainingImages = productImages.stream()
                    .filter(img -> !img.getImageId().equals(byImageUrl.getImageId()))
                    .toList();

            if (!remainingImages.isEmpty()) {
                ProductImage newThumbnail = remainingImages.get(0);
                newThumbnail.setIsThumbnail(true);
                productImageRepository.save(newThumbnail);
            }
        }

        productImageRepository.delete(byImageUrl);
    }

    @Transactional
    public String deleteProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<ProductItem> productItems = product.getItems();

        boolean hasOrderedItem = productItems.stream()
                .anyMatch(orderItemRepository::existsByProductItem);

        if (hasOrderedItem) {
            return "주문 상품이 존재합니다. [상품 삭제 실패]";
        } else {
            if (!productItems.isEmpty()) {
                cartItemRepository.deleteByProductItemIn(productItems);
            }
            productRepository.delete(product);
            return "[상품 삭제 성공]";
        }
    }


    private List<ProductListDto> getProductList(List<Product> productList) {
        return productList.stream()
                .map(product -> {
                    ProductImage image = productImageRepository.findByProductAndIsThumbnailTrue(product)
                            .orElse(null);
                    String imageUrl;
                    if(image != null){
                        imageUrl = image.getImageUrl();
                    }else
                        imageUrl = null;
                    List<ProductItem> productItems = productItemRepository.findByProduct(product);
                    List<ProductItemDto> productItemDtos = productItems.stream().map(item->{
                        return new ProductItemDto(
                                item.getSize(),
                                item.getStock(),
                                item.getStatus()
                        );
                    }).toList();
                    return new ProductListDto(
                            product.getProductId(),
                            product.getProductName(),
                            product.getProductCode(),
                            product.getPrice(),
                            product.getDiscountRate(),
                            imageUrl,
                            productItemDtos
                    );
                }).toList();
    }
}
