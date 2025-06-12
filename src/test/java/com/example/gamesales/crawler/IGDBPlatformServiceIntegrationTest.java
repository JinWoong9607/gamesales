package com.example.gamesales.crawler;

import com.example.gamesales.config.IgdbProperties;
import com.example.gamesales.service.IGDBPlatformService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
@TestPropertySource(locations = "file:.env")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("IGDB 실제 서버 연동 통합 테스트 (헤더 디버그 포함)")
class IGDBPlatformServiceIntegrationTest {

    @Autowired
    private IgdbProperties igdbProperties;

    @Autowired
    private IGDBPlatformService igdbService;

    @BeforeAll
    void beforeAll() {
        // 실제 전송될 Authorization 헤더를 찍어 봅니다
        System.out.println("=== 디버그: IGDB 클라이언트 설정 ===");
        System.out.printf("Client-ID: %s%n", igdbProperties.getId());
        System.out.printf("Client-Secret: %s%n", igdbProperties.getSecret());
        System.out.printf("Base-URL: %s%n", igdbProperties.getBaseUrl());
        System.out.println("→ Authorization 헤더 값은 서버에서 발급받은 App Access Token 을 사용합니다.");
    }

    @Test
    @DisplayName("Stellar Blade 지원 플랫폼 실제 조회")
    void testGetSupportedPlatformsByGameName() {
        List<String> platforms =
                igdbService.getSupportedPlatformsByGameName("Stellar Blade");

        System.out.println("=== 실제 IGDB 호출 결과 ===");
        platforms.forEach(System.out::println);

        assertNotNull(platforms, "플랫폼 리스트가 null이면 안 됩니다");
        assertFalse(platforms.isEmpty(), "최소 하나 이상의 플랫폼이 조회되어야 합니다");
    }
}
