package ru.filatov.exchange_rates_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExchangeRatesBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExchangeRatesBotApplication.class, args);
	}

}
