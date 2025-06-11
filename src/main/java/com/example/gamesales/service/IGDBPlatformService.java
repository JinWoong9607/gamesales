package com.example.gamesales.service;

import com.example.gamesales.config.IgdbProperties;
import com.example.gamesales.repository.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class IGDBPlatformService {

    private final WebClient igdbClient;
    private final GameRepository gameRepo;
    private final ObjectMapper objectMapper;

    // IgdbProperties 주입
    public IGDBPlatformService(IgdbProperties props, GameRepository gameRepo, ObjectMapper objectMapper) {
        this.igdbClient = WebClient.builder()
                .baseUrl("https://api.igdb.com/v4")
                .defaultHeader("Client-ID", props.getId())
                .defaultHeader("Authorization", "Bearer " + props.getToken())
                .build();
        this.gameRepo = gameRepo;
        this.objectMapper = objectMapper;
    }

    public void updatePlatformsForSteamGames(List<Integer> steamAppIds) {
        // 1. external_games로 IGDB game ID 조회
        String extGamesQuery = "fields game,uid; where category = 1 & uid = (" +
                steamAppIds.stream().map(Object::toString).collect(Collectors.joining(",")) +
                ");";
        List<JsonNode> extGames = igdbClient.post()
                .uri("/external_games")
                .bodyValue(extGamesQuery)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block()
                .findValues("data");

        Map<Integer, Integer> steamToIgdb = new HashMap<>();
        for (JsonNode node : extGames) {
            int igdbGameId = node.get("game").asInt();
            int steamId = node.get("uid").asInt();
            steamToIgdb.put(steamId, igdbGameId);
        }

        if (steamToIgdb.isEmpty()) return;

        // 2. games로 플랫폼 ID 리스트 조회
        String igdbIds = steamToIgdb.values().stream()
                .map(Object::toString).collect(Collectors.joining(","));
        String gamesQuery = "fields id,platforms; where id = (" + igdbIds + ");";
        List<JsonNode> games = igdbClient.post()
                .uri("/games")
                .bodyValue(gamesQuery)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block()
                .findValues("data");

        // Map<IGDB Game ID, List<Platform ID>>
        Map<Integer, List<Integer>> gameToPlatformIds = new HashMap<>();
        for (JsonNode node : games) {
            int id = node.get("id").asInt();
            List<Integer> platforms = new ArrayList<>();
            node.withArray("platforms").forEach(p -> platforms.add(p.asInt()));
            gameToPlatformIds.put(id, platforms);
        }

        // 3. platforms로 플랫폼 이름 조회
        Set<Integer> allPlatformIds = gameToPlatformIds.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet());
        if (!allPlatformIds.isEmpty()) {
            String platIds = allPlatformIds.stream()
                    .map(Object::toString).collect(Collectors.joining(","));
            String platformsQuery = "fields id,name; where id = (" + platIds + ");";
            List<JsonNode> plats = igdbClient.post()
                    .uri("/platforms")
                    .bodyValue(platformsQuery)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block()
                    .findValues("data");

            Map<Integer, String> platformNames = plats.stream()
                    .collect(Collectors.toMap(
                            p -> p.get("id").asInt(),
                            p -> p.get("name").asText()
                    ));

            // 4. DB 업데이트 또는 출력
            for (Map.Entry<Integer, Integer> entry : steamToIgdb.entrySet()) {
                int steamId = entry.getKey();
                int igdbId = entry.getValue();
                List<String> names = gameToPlatformIds.getOrDefault(igdbId, Collections.emptyList())
                        .stream().map(platformNames::get).toList();

                gameRepo.findByAppId(steamId).ifPresent(game -> {
                    game.setSupportedPlatforms(names);
                    gameRepo.save(game);
                    System.out.printf("✅ %s (SteamID=%d) supports: %s%n",
                            game.getEnglishName(), steamId, String.join(", ", names));
                });
            }
        }
    }
}
