package com.example.gamesales.repository;

import com.example.gamesales.model.GamePrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamePriceRepository extends JpaRepository<GamePrice, Long> {
}
