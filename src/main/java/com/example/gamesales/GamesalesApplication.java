package com.example.gamesales;

import com.example.gamesales.config.IgdbProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class GamesalesApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamesalesApplication.class, args);
	}

}
