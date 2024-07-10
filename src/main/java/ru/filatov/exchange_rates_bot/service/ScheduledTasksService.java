package ru.filatov.exchange_rates_bot.service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.filatov.exchange_rates_bot.bot.ExchangeRatesBot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

        Message testMessage= exchangeRatesBot.sendMessageAndGetId(myChatId, "Началась  загрузка файлов в 04:36" );

        exchangeRatesBot.fetchAndProcessData();

        if (testMessage != null) {
            try {
                Thread.sleep(2000);
                exchangeRatesBot.deleteMessage(myChatId, testMessage.getMessageId());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Scheduled(cron = "0 40 21 * * *", zone = "Europe/Moscow")
    public void performTaskExport() {
        //exchangeRatesBot.sendMessage(myChatId,"Началась  загрузка файлов в 21:40");

        Message testMessage= exchangeRatesBot.sendMessageAndGetId(myChatId, "Началась  загрузка файлов в 21:40" );



        exchangeRatesBot.fetchAndProcessDataForExport();
        exchangeRatesBot.fetchAndProcessDataForExportTSO();
        exchangeRatesBot.fetchAndProcessDataAGSI();

        if (testMessage != null) {
            try {
                Thread.sleep(2000);
                exchangeRatesBot.deleteMessage(myChatId, testMessage.getMessageId());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Scheduled(cron = "0 0 9,12,15,20,3 * * *", zone = "Europe/Moscow")
    public void performTest() {



        String formatDateTime =  exchangeRatesBot.formattedTime();
        Message testMessage= exchangeRatesBot.sendMessageAndGetId(myChatId, "Тест успешно пройдет в " + formatDateTime);
        if (testMessage != null) {
            try {
                Thread.sleep(2000);
                exchangeRatesBot.deleteMessage(myChatId, testMessage.getMessageId());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }



}
