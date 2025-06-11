package com.example.gamesales.crawler;

import com.example.gamesales.config.IgdbProperties;
import com.example.gamesales.model.Game;
import com.example.gamesales.repository.GameRepository;
import com.example.gamesales.service.IGDBPlatformService;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "igdb.client.id=test-client-id",
                "igdb.client.token=test-token",
                // wiremock.port는 static 블록에서 설정되므로, 여기선 placeholder가 올바르게 바인딩됩니다.
                "igdb.client.base-url=http://localhost:${wiremock.port}"
        }
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class FullIntegrationTest {

    @Autowired SteamCrawlerService steamCrawlerService;
    @Autowired
    IGDBPlatformService igdbPlatformService;
    @Autowired GameRepository gameRepo;

    // static 초기화 블록: 클래스 로딩 시점에 WireMock 서버 기동 및 시스템 프로퍼티 설정
    static WireMockServer wireMock;
    static {
        wireMock = new WireMockServer(
                WireMockConfiguration.options().dynamicPort()
        );
        wireMock.start();
        System.setProperty("wiremock.port", String.valueOf(wireMock.port()));
        configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setupStubs() {
        // 1) Steam topsellers 페이지 스텁
        wireMock.stubFor(get(urlPathMatching("/search.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","text/html; charset=UTF-8")
                        .withBodyFile("topsellers-page-0.html"))); // 파일이 __files/topsellers-page-0.html 에 있어야 함


        // 2) IGDB external_games
        wireMock.stubFor(post(urlPathEqualTo("/external_games"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBodyFile("igdb-external_games.json")));

        // 3) IGDB games
        wireMock.stubFor(post(urlPathEqualTo("/games"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBodyFile("igdb-games.json")));

        // 4) IGDB platforms
        wireMock.stubFor(post(urlPathEqualTo("/platforms"))
                .willReturn(aResponse()
                        .withHeader("Content-Type","application/json")
                        .withBodyFile("igdb-platforms.json")));
    }


    @Test
    void fullCycleIntegrationTest() {
        steamCrawlerService.fetchAndSaveTop500PaidGamesFromStorePage();
        List<Game> saved = gameRepo.findAll();
        assert !saved.isEmpty();

        igdbPlatformService.updatePlatformsForSteamGames(
                saved.stream().map(Game::getAppId).toList()
        );

        gameRepo.findAll().forEach(g ->
                // JUnit Assertions를 사용
                Assertions.assertFalse(
                        g.getSupportedPlatforms().isEmpty(),
                        "appId=" + g.getAppId() + " 플랫폼 미조회"
                )
        );
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public WebClient steamWebClient() {
            return WebClient.builder()
                    .baseUrl("http://localhost:" + wireMock.port())
                    .build();
        }
    }
}
