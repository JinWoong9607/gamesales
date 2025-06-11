package com.example.gamesales.crawler;

import com.example.gamesales.dto.PriceDto;
import com.example.gamesales.model.Game;
import com.example.gamesales.model.GamePrice;
import com.example.gamesales.repository.GamePriceRepository;
import com.example.gamesales.repository.GameRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SteamCrawlerService {

    private final WebClient webClient;
    private final GameRepository gameRepo;
    private final GamePriceRepository priceRepo;
    private final ObjectMapper objectMapper;

    public SteamCrawlerService(
            WebClient steamWebClient,
            GameRepository gameRepo,
            GamePriceRepository priceRepo,
            ObjectMapper objectMapper
    ) {
        this.webClient = steamWebClient;
        this.gameRepo = gameRepo;
        this.priceRepo = priceRepo;
        this.objectMapper = objectMapper;
    }

    public void fetchAndSaveTop500PaidGamesFromStorePage() {
        List<PriceDto> results = new ArrayList<>();
        int ranking = 1;

        for (int i = 0; i < 500; i += 50) {
            int start = i;
            try {
                String html = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                // • scheme()와 host() 호출 제거
                                .path("/search/")
                                .queryParam("filter", "topsellers")
                                .queryParam("cc", "kr")
                                .queryParam("category1", "998")
                                .queryParam("start", start)
                                .queryParam("count", 50)
                                .build()
                        )
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                Document doc = Jsoup.parse(html);
                Elements elements = doc.select(".search_result_row");

                for (Element el : elements) {
                    String href = el.attr("href");
                    Matcher matcher = Pattern.compile("/app/(\\d+)/").matcher(href);
                    if (!matcher.find()) {
                        System.out.println("❌ appId 추출 실패: " + href);
                        continue;
                    }

                    int appId = Integer.parseInt(matcher.group(1));
                    String title = el.select(".title").text();

                    Element discountBlock = el.selectFirst(".search_discount_block");
                    if (discountBlock == null) {
                        System.out.printf("⚠️ %s (appId=%d)는 discount block 없음, 스킵%n", title, appId);
                        continue;
                    }

                    Element finalPriceEl = discountBlock.selectFirst(".discount_final_price");
                    Element originalPriceEl = discountBlock.selectFirst(".discount_original_price");
                    Element discountPercentEl = discountBlock.selectFirst(".discount_pct");

                    if (finalPriceEl == null) {
                        System.out.printf("⚠️ %s (appId=%d)는 priceEl null, 원본 HTML:\n%s%n", title, appId, el.html());
                        continue;
                    }

                    String finalPriceText = finalPriceEl.text().replaceAll("[^\\d]", "");
                    int finalPrice = finalPriceText.isBlank() ? 0 : Integer.parseInt(finalPriceText);

                    int initialPrice = originalPriceEl != null
                            ? Integer.parseInt(originalPriceEl.text().replaceAll("[^\\d]", ""))
                            : finalPrice;

                    int discountPercent = discountPercentEl != null
                            ? Integer.parseInt(discountPercentEl.text().replaceAll("[^\\d]", ""))
                            : 0;

                    if (finalPrice == 0) {
                        System.out.printf("⚠️ %s (appId=%d)는 무료, 스킵%n", title, appId);
                        continue;
                    }

                    System.out.printf("✅ 추출 성공: %s (appId=%d, finalPrice=%d, initialPrice=%d, discount=%d%%, rank=%d)%n",
                            title, appId, finalPrice, initialPrice, discountPercent, ranking);

                    results.add(new PriceDto(appId, title, title, initialPrice, finalPrice, discountPercent, ranking++));
                    if (results.size() >= 500) break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (results.size() >= 500) break;
        }

        savePricesWithRanking(results);
    }




    @Transactional
    public void savePricesWithRanking(List<PriceDto> dtos) {
        for (PriceDto dto : dtos) {
            Game game = gameRepo.findByAppId(dto.appId())
                    .orElseGet(() -> gameRepo.save(Game.builder()
                            .appId(dto.appId())
                            .englishName(dto.englishName())
                            .koreanName(dto.koreanName())
                            .normalizedKey(normalizeKey(dto.englishName(), dto.appId()))
                            .ranking(dto.ranking())
                            .build()));

            GamePrice gp = GamePrice.builder()
                    .game(game)
                    .serviceName("Steam")
                    .finalPrice(dto.finalPrice())
                    .discountPercent(dto.discountPercent())
                    .fetchedAt(LocalDateTime.now())
                    .build();

            priceRepo.save(gp);
        }
    }

    private String normalizeKey(String input, int appId) {
        String slug = Optional.ofNullable(input)
                .map(s -> s.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", "")
                        .trim()
                        .replaceAll("\\s+", "-"))
                .filter(s -> !s.isBlank())
                .orElse("game");
        return slug + "-" + appId;
    }
}
