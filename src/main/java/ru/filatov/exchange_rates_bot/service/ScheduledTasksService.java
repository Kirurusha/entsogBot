package ru.filatov.exchange_rates_bot.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.filatov.exchange_rates_bot.bot.ExchangeRatesBot;

@Component
public class ScheduledTasksService {

    private final ExchangeRatesBot exchangeRatesBot;
    private static final long myChatId = 598389393;

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
        exchangeRatesBot.fetchAndProcessDataAGSI();
    }

    @Scheduled(cron = "0 28 14 * * *", zone = "Europe/Moscow")
    public void performTaskExportTest() {
        exchangeRatesBot.sendMessage(myChatId,"Началась тестовая загрузка AGSI");

        exchangeRatesBot.fetchAndProcessDataAGSITest();
        exchangeRatesBot.sendMessage(myChatId,"Закончилась тестовая загрузка AGSI");
    }

}
