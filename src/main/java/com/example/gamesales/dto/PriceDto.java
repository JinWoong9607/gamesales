package com.example.gamesales.dto;

public record PriceDto(
        int appId,
        String englishName,
        String koreanName,
        int initialPrice,
        int finalPrice,
        int discountPercent,
        int ranking
) {}
