package com.example.gamesales.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "games")
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)    // JPA용
@AllArgsConstructor(access = AccessLevel.PRIVATE)     // 빌더 전용
@Builder
public class Game {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true)
        private Integer appId;

        @Column(nullable = false)
        private String englishName;

        @Column(nullable = false)
        private String koreanName;

        @Column(nullable = false, unique = true)
        private String normalizedKey;

        @Column(nullable = false)
        private Integer ranking; // 인기순 랭킹 필드 추가

        @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<GamePrice> prices = new ArrayList<>();

        @ElementCollection(fetch = FetchType.LAZY)
        @CollectionTable(name = "game_platforms", joinColumns = @JoinColumn(name = "game_id"))
        @Column(name = "platform", nullable = false)
        @Builder.Default
        private List<String> supportedPlatforms = new ArrayList<>();

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Game that)) return false;
                return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id);
        }

        /** 예시: 가격 목록을 추가하거나 교체할 때만 노출하는 비즈니스 메서드 */
        public void setPrices(List<GamePrice> newPrices) {
                this.prices.clear();
                this.prices.addAll(newPrices);
        }

        /** 지원 플랫폼 목록을 추가하거나 교체하는 비즈니스 메서드 */
        public void setSupportedPlatforms(List<String> newPlatforms) {
                this.supportedPlatforms.clear();
                if (newPlatforms != null) {
                        this.supportedPlatforms.addAll(newPlatforms);
                }
        }
}
