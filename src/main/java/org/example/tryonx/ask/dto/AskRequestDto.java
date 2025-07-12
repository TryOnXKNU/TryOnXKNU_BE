package org.example.tryonx.ask.dto;

import lombok.Data;

import java.util.List;

@Data
public class AskRequestDto {
    private Integer orderItemId;
    private String title;
    private String content;
    private List<String> imageUrls;}
