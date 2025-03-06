package ru.filatov.exchange_rates_bot.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.filatov.exchange_rates_bot.bot.ExchangeRatesBot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class ScheduledTasksService {

    private final ExchangeRatesBot exchangeRatesBot;
    private static final long myChatId = 598389393;


    @Autowired
    private JsonToExcelService jsonToExcelService;


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

    @Scheduled(cron = "0 */1 7-10 * * *", zone = "Europe/Moscow")
    public void scheduledDataCheck() {
        LocalDate targetDate = LocalDate.now().minusDays(2); // Сегодня -2 дня

        // Если на этот день уже были обработаны данные, ничего не делаем
        if (jsonToExcelService.processedDates.getOrDefault(targetDate, false)) {
            System.out.println("На " + targetDate + " уже отправлены данные. Проверка пропущена.");
            return;
        }

        // Проверяем данные
        boolean hasValidData = jsonToExcelService.checkForValidData(targetDate);
        System.out.println(hasValidData);

        // Если найдены данные, запускаем действие и отмечаем день как обработанный
        if (hasValidData) {
            exchangeRatesBot.fetchAndProcessDataTSOUA();
            System.out.println("Началась загрузка данных для ОГТСУ");
            //jsonToExcelService.downloadAndSaveData();
            //performAction(targetDate);
            jsonToExcelService.processedDates.put(targetDate, true);
            System.out.println("Изменен флаг для даты");
        }
    }




}
