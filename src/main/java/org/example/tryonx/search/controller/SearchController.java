package org.example.tryonx.search.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.search.dto.SearchDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/v1/search")
//public class SearchController {
//    private final SearchService searchService;
//
//    @GetMapping
//    public List<ProductResponse> search(@ModelAttribute SearchDto searchDto) {
//        return searchService.searchProducts(searchDto);
//    }
//}
