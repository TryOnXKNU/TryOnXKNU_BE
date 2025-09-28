package org.example.tryonx.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.tryonx.search.dto.ProductResponse;
import org.example.tryonx.search.dto.SearchDto;
import org.example.tryonx.search.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
@Tag(name = "Search API", description = "검색 API")
public class SearchController {
    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "검색")
    public List<ProductResponse> search(@ModelAttribute SearchDto searchDto) {
        return searchService.searchProducts(searchDto);
    }
}
