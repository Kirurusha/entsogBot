package ru.filatov.exchange_rates_bot.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.filatov.exchange_rates_bot.bot.ExchangeRatesBot;

@Component
public class ScheduledTasksService {

    private final ExchangeRatesBot exchangeRatesBot;

    public ScheduledTasksService(ExchangeRatesBot exchangeRatesBot) {
        this.exchangeRatesBot = exchangeRatesBot;
    }

    // Внедряем зависимость через конструктор

    @Scheduled(cron = "0 13 12 * * *") // Запускается каждый день в 5:00 утра
    public void performTask() {
        exchangeRatesBot.fetchAndProcessData();
    }
}
