package com.example.gamesales.repository;

import com.example.gamesales.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByAppId(Integer appId);
}
