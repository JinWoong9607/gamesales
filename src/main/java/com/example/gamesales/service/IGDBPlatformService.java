package com.example.gamesales.service;

import com.example.gamesales.config.IgdbProperties;
import com.example.gamesales.repository.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class IGDBPlatformService {

    private final WebClient igdbClient;
    private final GameRepository gameRepo;
    private final ObjectMapper objectMapper;

    @Autowired
    public IGDBPlatformService(IgdbProperties props,
                               GameRepository gameRepo,
                               ObjectMapper objectMapper) {
        this.gameRepo = gameRepo;
        this.objectMapper = objectMapper;

        // 1) Twitch로부터 App Access Token 발급받기
        WebClient authClient = WebClient.create();
        String tokenResponse = authClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("id.twitch.tv")
                        .path("/oauth2/token")
                        .queryParam("client_id", props.getId())
                        .queryParam("client_secret", props.getSecret())
                        .queryParam("grant_type", "client_credentials")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block()
                .get("access_token").asText();

        // 2) IGDB API 호출용 WebClient 구성
        this.igdbClient = WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Client-ID", props.getId())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenResponse)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * IGDB API POST 요청을 처리하고 JSON 응답을 반환하는 공통 헬퍼
     */
    private JsonNode preparePost(String endpoint, String query) {
        try {
            return igdbClient.post()
                    .uri(endpoint)
                    .bodyValue(query)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.err.printf("⚠️ IGDB %s 실패 (%d): %s%n",
                    endpoint, e.getRawStatusCode(), e.getMessage());
            return null;
        }
    }


    public JsonNode searchGameByName(String title) {
        String q = String.format(
                "fields id,name;\n" +
                        "search \"%s\";\n" +
                        "limit 1;", title);
        return preparePost("/games", q);
    }

    /**
     * 게임 이름으로 지원 플랫폼 조회 (특수문자 제거 및 재시도 포함)
     */
    public List<String> getSupportedPlatformsByGameName(String title) {
        // 1) 원본 제목
        List<String> platforms = fetchPlatforms(title);
        if (!platforms.isEmpty()) return platforms;

        // 2) ™·® 제거
        String cleaned = title.replaceAll("[™®]", "").trim();
        if (!cleaned.equals(title)) {
            platforms = fetchPlatforms(cleaned);
            if (!platforms.isEmpty()) return platforms;
        }

        // 3) 제목 뒤에 ™ 추가
        platforms = fetchPlatforms(cleaned + "™");
        return platforms;
    }

    /**
     * 실제 API 호출 및 JSON 파싱 로직
     */
    private List<String> fetchPlatforms(String title) {
        if (title == null || title.isBlank()) return Collections.emptyList();

        // 1) 게임 검색
        String gamesQ = String.format(
                "fields id,platforms;\n" +
                        "search \"%s\";\n" +
                        "limit 1;", title);
        JsonNode gamesResp = preparePost("/games", gamesQ);
        if (gamesResp == null) return Collections.emptyList();

        // IGDB v4는 결과가 최상위 배열로 오므로, 배열 또는 data 속성 중 선택
        List<JsonNode> gameArray;
        if (gamesResp.isArray()) {
            gameArray = StreamSupport.stream(gamesResp.spliterator(), false)
                    .collect(Collectors.toList());
        } else {
            JsonNode dataNode = gamesResp.get("data");
            if (dataNode == null || !dataNode.isArray()) return Collections.emptyList();
            gameArray = StreamSupport.stream(dataNode.spliterator(), false)
                    .collect(Collectors.toList());
        }
        if (gameArray.isEmpty()) return Collections.emptyList();

        // 2) 플랫폼 ID 추출
        JsonNode platformsNode = gameArray.get(0).get("platforms");
        if (platformsNode == null || !platformsNode.isArray()) return Collections.emptyList();
        List<Integer> platformIds = StreamSupport.stream(platformsNode.spliterator(), false)
                .map(JsonNode::asInt)
                .collect(Collectors.toList());
        if (platformIds.isEmpty()) return Collections.emptyList();

        // 3) 플랫폼 이름 조회
        String platsQ = String.format(
                "fields name; where id = (%s);",
                platformIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        JsonNode platsResp = preparePost("/platforms", platsQ);
        if (platsResp == null) return Collections.emptyList();

        List<JsonNode> platArray;
        if (platsResp.isArray()) {
            platArray = StreamSupport.stream(platsResp.spliterator(), false)
                    .collect(Collectors.toList());
        } else {
            JsonNode dataNode = platsResp.get("data");
            if (dataNode == null || !dataNode.isArray()) return Collections.emptyList();
            platArray = StreamSupport.stream(dataNode.spliterator(), false)
                    .collect(Collectors.toList());
        }
        if (platArray.isEmpty()) return Collections.emptyList();

        // 4) 이름 리스트 반환
        return platArray.stream()
                .map(n -> n.get("name").asText())
                .collect(Collectors.toList());
    }

    public void updatePlatformsForSteamGames(List<Integer> steamAppIds) {
        if (steamAppIds.isEmpty()) return;

        // 1) external_games 조회
        String extQ = String.format(
                "fields game,uid; where category = 1 & uid = (%s);",
                steamAppIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        JsonNode extResp = preparePost("/external_games", extQ);
        List<JsonNode> extData = Optional.ofNullable(extResp)
                .map(r -> r.findValues("data")).orElse(Collections.emptyList());
        Map<Integer,Integer> steamToIgdb = new HashMap<>();
        extData.forEach(arr -> arr.forEach(node ->
                steamToIgdb.put(node.get("uid").asInt(), node.get("game").asInt())
        ));
        if (steamToIgdb.isEmpty()) return;

        // 2) games → platforms ID
        String igdbIds = steamToIgdb.values().stream()
                .map(String::valueOf).collect(Collectors.joining(","));
        String gamesQ = "fields id,platforms; where id = (" + igdbIds + ");";
        JsonNode gamesResp = preparePost("/games", gamesQ);
        List<JsonNode> gamesData = Optional.ofNullable(gamesResp)
                .map(r -> r.findValues("data")).orElse(Collections.emptyList());
        Map<Integer,List<Integer>> gameToPlats = new HashMap<>();
        gamesData.forEach(arr -> arr.forEach(node -> {
            int id = node.get("id").asInt();
            List<Integer> plats = new ArrayList<>();
            node.withArray("platforms").forEach(p -> plats.add(p.asInt()));
            gameToPlats.put(id, plats);
        }));

        // 3) platforms 조회
        Set<Integer> allPlatIds = gameToPlats.values().stream()
                .flatMap(Collection::stream).collect(Collectors.toSet());
        if (allPlatIds.isEmpty()) return;
        String platIds = allPlatIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String platsQ = "fields id,name; where id = (" + platIds + ");";
        JsonNode platsResp = preparePost("/platforms", platsQ);
        List<JsonNode> platsData = Optional.ofNullable(platsResp)
                .map(r -> r.findValues("data")).orElse(Collections.emptyList());
        Map<Integer,String> platNames = platsData.stream()
                .collect(Collectors.toMap(
                        p -> p.get("id").asInt(),
                        p -> p.get("name").asText()
                ));

        // 4) DB 업데이트
        steamToIgdb.forEach((steamId, igdbId) -> {
            List<String> names = gameToPlats.getOrDefault(igdbId, List.of())
                    .stream().map(platNames::get).toList();
            gameRepo.findByAppId(steamId).ifPresent(game -> {
                game.setSupportedPlatforms(names);
                gameRepo.save(game);
                System.out.printf("✅ %s (SteamID=%d) supports: %s%n",
                        game.getEnglishName(), steamId, String.join(", ", names));
            });
        });
    }
}
