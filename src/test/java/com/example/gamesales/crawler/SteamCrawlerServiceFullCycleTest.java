package com.example.gamesales.crawler;

import com.example.gamesales.model.Game;
import com.example.gamesales.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class SteamCrawlerServiceFullCycleTest {

    @Autowired SteamCrawlerService crawlerService;
    @Autowired GameRepository gameRepo;

    @Test
    void fullCycle_insertsGamesWithCorrectFieldsAndRanking() {
        crawlerService.fetchAndSaveTop500PaidGamesFromStorePage(); // ✅ 이걸로 교체

        List<Game> games = gameRepo.findAll();

        assertThat(games.size()).isBetween(1, 500); // 적어도 하나는 있어야 함

        games.forEach(g -> {
            System.out.printf("appId=%d, EN=\"%s\", KR=\"%s\", key=\"%s\", rank=%d%n",
                    g.getAppId(),
                    g.getEnglishName(),
                    g.getKoreanName(),
                    g.getNormalizedKey(),
                    g.getRanking()
            );

            assertThat(g.getAppId()).isNotNull();
            assertThat(g.getEnglishName()).isNotBlank();
            assertThat(g.getKoreanName()).isNotBlank();
            assertThat(g.getNormalizedKey()).matches("^[a-z0-9\\-]+$");
            assertThat(g.getRanking()).isBetween(1, 500);
        });
    }
}

