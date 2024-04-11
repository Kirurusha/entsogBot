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

    @Scheduled(cron = "0 36 4 * * *", zone = "Europe/Moscow")
    public void performTask() {
        exchangeRatesBot.fetchAndProcessData();
    }


    @Scheduled(cron = "0 40 21 * * *", zone = "Europe/Moscow")
    public void performTaskExport() {
        exchangeRatesBot.fetchAndProcessDataForExport();
        exchangeRatesBot.fetchAndProcessDataForExportTSO();
    }

}
