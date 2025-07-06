package org.example.tryonx.search.service;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.search.dto.SearchDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

//@Service
//@RequiredArgsConstructor
//public class SearchService {
//    private final SearchRepository searchRepository;
//
//    public List<ProductResponse> searchProducts(SearchDto searchDto) {
//        String keyword = searchDto.getKeyword();
//        List<Product> products = searchRepository.findByTitleContainingIgnoreCase(keyword);
//
//        return products.stream()
//                .map(e -> new ProductResponse(
//                        e.getId(),
//                        e.getTitle(),
//                        e.getContent(),
//                        e.getCreatedAt()
//                ))
//                .collect(Collectors.toList());
//    }
//}
