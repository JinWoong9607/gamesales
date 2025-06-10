package com.example.gamesales.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "game_prices")
@Access(AccessType.FIELD)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GamePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private Integer initialPrice;

    @Column(nullable = false)
    private Integer finalPrice;

    @Column(nullable = false)
    private Integer discountPercent;

    @Column(nullable = false)
    private LocalDateTime fetchedAt;

    // custom builder logic: if initialPrice not set, default to finalPrice
    public static class GamePriceBuilder {
        public GamePriceBuilder finalPrice(Integer finalPrice) {
            this.finalPrice = finalPrice;
            if (this.initialPrice == null) {
                this.initialPrice = finalPrice;
            }
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GamePrice that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
