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
        exchangeRatesBot.sendMessage(myChatId,"Началась  загрузка файлов в 04:36");
        exchangeRatesBot.fetchAndProcessData();
        exchangeRatesBot.sendMessage(myChatId,"Закончилась загрузка файлов в 04:36");
    }


    @Scheduled(cron = "0 40 21 * * *", zone = "Europe/Moscow")
    public void performTaskExport() {
        exchangeRatesBot.sendMessage(myChatId,"Началась  загрузка файлов в 21:40");
        exchangeRatesBot.fetchAndProcessDataForExport();
        exchangeRatesBot.fetchAndProcessDataForExportTSO();
        exchangeRatesBot.fetchAndProcessDataAGSI();
        exchangeRatesBot.sendMessage(myChatId,"Закончилась загрузка файлов в 21:40");
    }



}
