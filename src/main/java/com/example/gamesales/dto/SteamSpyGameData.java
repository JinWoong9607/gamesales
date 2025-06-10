package com.example.gamesales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// SteamSpy 데이터 모델 클래스 (record로 정의)
public record SteamSpyGameData(
        int appid,
        @JsonProperty("owners") String ownersRange
) {
    public long ownersEstimate() {
        try {
            String[] parts = ownersRange.replace(" ", "").split("\\.\\.");
            long min = Long.parseLong(parts[0]);
            long max = Long.parseLong(parts[1]);
            return (min + max) / 2;
        } catch (Exception e) {
            return 0;
        }
    }
}
