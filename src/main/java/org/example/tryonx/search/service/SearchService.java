package org.example.tryonx.search.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.product.domain.Product;
import org.example.tryonx.search.dto.ProductImageResponse;
import org.example.tryonx.search.dto.ProductResponse;
import org.example.tryonx.search.dto.SearchDto;
import org.example.tryonx.search.repository.SearchRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final SearchRepository searchRepository;

    public List<ProductResponse> searchProducts(SearchDto searchDto) {
        String keyword = searchDto.getKeyword();
        List<Product> products = searchRepository.findByProductNameContainingIgnoreCase(keyword);

        return products.stream()
                .map(product -> new ProductResponse(
                        product.getProductName(),
                        product.getPrice(),
                        product.getDiscountRate(),
                        product.getImages().stream()
                                .map(img -> new ProductImageResponse(img.getImageUrl()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
