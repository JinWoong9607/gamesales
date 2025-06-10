package com.example.gamesales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SteamChartsResponse(
        @JsonProperty("response")
        Response response
) {
    public record Response(
            @JsonProperty("ranks")
            List<Rank> ranks,

            @JsonProperty("total_unique_players")
            int totalUniquePlayers
    ) {}

    public record Rank(
            @JsonProperty("appid")
            int appid,

            @JsonProperty("players")
            int players
    ) {}
}
