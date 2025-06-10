package com.example.gamesales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AppDetailsResponse(
        @JsonProperty("success")
        boolean success,
        @JsonProperty("data")
        Data data
) {
    public record Data(
            @JsonProperty("name")
            String name,
            @JsonProperty("price_overview")
            PriceOverview priceOverview,
            @JsonProperty("is_free")
            Boolean isFree
    ) {
    }

    public record PriceOverview(
            @JsonProperty("initial")
            int initial,
            @JsonProperty("final")
            int finalPrice,
            @JsonProperty("discount_percent")
            int discountPercent
    ) {}
}
