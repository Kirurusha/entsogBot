package ru.filatov.exchange_rates_bot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;


import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.filatov.exchange_rates_bot.Exception.ServiceException;
import ru.filatov.exchange_rates_bot.entity.ExcelFile;
import ru.filatov.exchange_rates_bot.service.EmailService;
import ru.filatov.exchange_rates_bot.service.ExcelFileArchiver;
import ru.filatov.exchange_rates_bot.service.ExchangeRatesService;

import javax.mail.MessagingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class ExchangeRatesBot extends TelegramLongPollingBot {
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRatesBot.class);
    private static final String START = "/start";
    private static final String USD = "/usd";
    private static final String EUR = "/eur";
    private static final String HELP = "/help";
    private static final String LOVE = "/love";
    private static final String EXCEL = "/excel";
    private static final String CHECK = "/check";
    private static final String KZD = "/fileForKZD";
    private static final String OST = "/fileForOstrovskogo";

    private static final String physicalflow = "Physical%20Flow";
    private static final String renomination = "Renomination";
    private static final String nomination = "Nomination";
    private static final String allocation = "Allocation";
    private static final String gcv = "GCV";
    private static final String allTypesNominations = "Nomination,Renomination,Allocation,Physical%20Flow,GCV";



    //Nomination,Renomination,Allocation,Physical%20Flow,GCV&periodType=day&timezone=CET&periodize=0&limit=-1&isTransportData=true&dataset=1&operatorLabel=eustream,GAZ-SYSTEM,FGSZ,Transgaz,Gas TSO UA
    private static final int[][] period1 = {{11, -8}, {7, -4}, {3, 1}};
    private static final int[][] period2 = {{6, -1}};
    private static final int[][] period3= {{2, 1}};

    private static final List<String> DAY_POINTS_SET_1 = Arrays.asList(



            "ua-tso-0001itp-00117exit",
            "at-tso-0001itp-00162entry",
            "at-tso-0001itp-00062entry",
            "at-tso-0003itp-00037entry"


    );
    private static final List<String> DAY_POINTS_SET_2 = Arrays.asList(



            "bg-tso-0001itp-00041entry",
            "bg-tso-0001itp-00549entry",
            "bg-tso-0001itp-00529exit",
            "bg-tso-0001itp-00128exit",
            "bg-tso-0001itp-00128entry",
            "bg-tso-0001itp-00036exit",
            "sk-tso-0001itp-00117entry",
            "cz-tso-0001itp-00051exit",
            "sk-tso-0001itp-00051exit",
            "sk-tso-0001itp-00168exit",
            "de-tso-0001itp-00096entry",
            "de-tso-0001itp-00096exit",
            "hu-tso-0001itp-10013entry",
            "bg-tso-0001itp-00058exit"


    );
    private static final List<String> DAY_POINTS_SET_3 = Arrays.asList(

            "ua-tso-0001itp-10008exit",
            "pl-tso-0002itp-10008entry",
            "ua-tso-0001itp-10008entry",
            "pl-tso-0002itp-10008exit",
            "sk-tso-0001itp-00117exit",
            "ua-tso-0001itp-00117entry",
            "sk-tso-0001itp-00117entry",
            "ua-tso-0001itp-00117exit",
            "sk-tso-0001itp-00421exit",
            "ua-tso-0001itp-00421entry",
            "ua-tso-0001itp-00421exit",
            "sk-tso-0001itp-00421entry",
            "hu-tso-0001itp-10006exit",
            "ua-tso-0001itp-10006entry",
            "ua-tso-0001itp-10006exit",
            "hu-tso-0001itp-10006entry",
            "ua-tso-0001itp-00429exit",
            "ua-tso-0001itp-00429entry",
            "ua-tso-0001itp-00428exit",
            "ua-tso-0001itp-00428entry",
            "ua-tso-0001itp-00440exit",
            "ua-tso-0001itp-00440entry",
            "ua-tso-0001itp-00087exit",
            "ro-tso-0001itp-00087entry",
            "ro-tso-0001itp-00087exit",
            "ua-tso-0001itp-00087entry",
            "ua-tso-0001itp-00444exit",
            "ua-tso-0001itp-00444entry"


    );

    private static final List<String> DAY_TSO_SET_4 = Arrays.asList(



            "eustream",
            "GAZ-SYSTEM",
            "FGSZ",
            "Transgaz",
            "Gas TSO UA"

    );



    // https://transparency.entsog.eu/api/v1/operationalData.xlsx?forceDownload=true&from=2024-03-04&to=2024-03-10&indicator=Nomination,Renomination,Allocation,Physical%20Flow,GCV&periodType=day&timezone=CET&periodize=0&limit=-1&isTransportData=true&dataset=1&operatorLabel=eustream,GAZ-SYSTEM,FGSZ,Transgaz,Gas TSO UA


    private static final List<ExcelFile> excelFilesDays = new ArrayList<>();
    private static final List<ExcelFile> excelFilesHours = new ArrayList<>();
    private static List<String> recipients = Arrays.asList("kirillfilatoww@mail.ru", "operatorsouth@gazpromexport.gazprom.ru"
           , "operator@gazpromexport.gazprom.ru");
    private static List<String> recipientsExport = Arrays.asList("kirillfilatoww@mail.ru", "cpdd-export@adm.gazprom.ru");
    //private static final List<String> recipients = List.of("kirillfilatoww@mail.ru");
    String gifUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExODFxM2E4b3l2djZoNDd0bXRldGkwOXh4cjFjY3djbzEyd3B6MmFpMSZlcD12MV9naWZzX3NlYXJjaCZjdD1n/5kpRuktAKcNXy4p1is/giphy.gif";
    @Autowired
    private ExcelFileArchiver excelFileArchiver;
    @Autowired
    private ExchangeRatesService exchangeRatesService;
    @Autowired
    private EmailService emailService;

    public ExchangeRatesBot(@Value("${bot.token}") String botToken) {
        super(botToken);

    }

    public void fetchAndProcessDataForExport() {
        System.out.println("Загрузка и обработка данных. Текущее время: " + LocalDateTime.now());
        // Логика для загрузки и обработки данных
        long delay = 1000;


        try {

            handleDayFiles(null, DAY_POINTS_SET_3, renomination,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_3, allocation,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_3, physicalflow,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_3, gcv,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_3, nomination,period1);
            Thread.sleep(delay);


            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            String formatDateTime = now.format(formatter);
            String archiveName = "entsog-" + formatDateTime + ".zip";
            excelFileArchiver.createArchive(archiveName);
            excelFileArchiver.addFilesToArchive("days", excelFilesDays);
            emailService.sendEmailWithAttachment(recipientsExport, "Данные для сводки показатели транспорта черех ПСП на границах Украины", "Коллеги, такой файл, по идее, будет выгружаться" +
                    " в 21:31  по Москве и приходить к вам на почту на ежедневной основе. Если перестанет работать, то пишите", null, excelFileArchiver.closeArchive());
            excelFilesDays.clear();


        } catch (InterruptedException | MessagingException | IOException e) {
            throw new RuntimeException(e);
        }



    }

    public void fetchAndProcessDataForExportTSO() {
        System.out.println("Загрузка и обработка данных. Текущее время: " + LocalDateTime.now());
        // Логика для загрузки и обработки данных
        long delay = 1000;


        try {

            handleDayFilesForTSO(null, DAY_TSO_SET_4, allTypesNominations,period2);

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            String formatDateTime = now.format(formatter);



            try {
                String archiveName = "entsog_" + formatDateTime + ".zip";
                excelFileArchiver.createArchive(archiveName);
                excelFileArchiver.addFilesToArchive("days", excelFilesDays);
                emailService.sendEmailWithAttachment(recipientsExport, "Данные для сводки Динамика использования ПХГ Украины и реверсных поставок газа", "Коллеги, такой файл, по идее, будет выгружаться" +
                        " в 21:31  по Москве и приходить к вам на почту на ежедневной основе. Если перестанет работать, то пишите", null, excelFileArchiver.closeArchive());
                excelFilesDays.clear();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                // Обработайте ошибку
            }






        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }



    }


    public void fetchAndProcessData() {
        System.out.println("Загрузка и обработка данных. Текущее время: " + LocalDateTime.now());
        // Логика для загрузки и обработки данных
        long delay = 1000;


        try {

            handleDayFiles(null, DAY_POINTS_SET_2, renomination,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_2, allocation,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_1, physicalflow,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_2, physicalflow,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_1, gcv,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_2, gcv,period1);
            Thread.sleep(delay);
            handleDayFiles(null, DAY_POINTS_SET_2, nomination,period1);
            Thread.sleep(delay);
            handleHourFile(null, DAY_POINTS_SET_2, physicalflow,period3);
            Thread.sleep(delay);
            handleHourFile(null, DAY_POINTS_SET_1, physicalflow,period3);


            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            String formatDateTime = now.format(formatter);
            String archiveName = "entsog-" + formatDateTime + ".zip";
            excelFileArchiver.createArchive(archiveName);

            excelFileArchiver.addFilesToArchive("hours", excelFilesHours);
            excelFileArchiver.addFilesToArchive("days", excelFilesDays);


            emailService.sendEmailWithAttachment(recipients, "Данные для сводок Транзит Болгария Словакия", "Коллеги, такой файл, по идее, будет выгружаться" +
                    " в 5 30 часов утра по Москве и приходить к вам на почту на ежедневной основе. Если перестанет работать, то пишите", null, excelFileArchiver.closeArchive());


            excelFilesHours.clear();
            excelFilesDays.clear();


        } catch (InterruptedException | MessagingException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void onUpdateReceived(Update update) {
        long delay = 5000;
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        var message = update.getMessage().getText();
        var chatId = update.getMessage().getChatId();
        switch (message) {
            case START -> {

                String userName = update.getMessage().getChat().getUserName();
                startCommand(chatId, userName);
            }
            case USD -> {
                // this.fetchAndProcessData();
                usdCommand(chatId);

            }
            case EUR -> {
                eurCommand(chatId);
            }
            case HELP -> {
                helpCommand(chatId);
            }
            case LOVE -> {
                sendGif(chatId);
            }
            case CHECK -> {
                unknownCommand(chatId);
            }
            case KZD -> {
                fetchAndProcessDataForExport();
                fetchAndProcessDataForExportTSO();
            }
            case OST -> {
                fetchAndProcessData();
            }
            case EXCEL -> {



            }
            default -> unknownCommand(chatId);

        }


    }

    private void sendApiButton(Long chatId) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Получить Excel");
        button.setCallbackData("get_excel");
        rowInline.add(button);
        markupInline.setKeyboard(rowsInline);

        var sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText("Нажмите кнопку ниже, чтобы получить файл Excel:");
        sendMessage.setReplyMarkup(markupInline);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка при отправке сообщения с кнопкой", e);
        }

    }

    private void sendExcelFile(Long chatId, ExcelFile excelFileStream) {
        // Логика для вызова API и получения файла Excel
        // Предположим, что у вас есть метод в сервисе, который возвращает InputStream файла
        try {

            String fileName = "rates.xlsx";
            SendDocument sendDocumentRequest = new SendDocument();
            sendDocumentRequest.setChatId(String.valueOf(chatId));
            sendDocumentRequest.setDocument(new InputFile(excelFileStream.getInputStream(), excelFileStream.getFilename()));
            execute(sendDocumentRequest);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка при отправке файла Excel", e);
        }
    }


    @Override
    public String getBotUsername() {
        return "filatov_bot";
    }

    private void startCommand(Long chatId, String username) {
        var text = """
                Добро пожаловать в бот, %s!
                                
                Здесь вы сможете узнать официальные курсы валют на сегодняб установаленные ЦБ РФ!
                                
                Для этого воспользуйтесь командами:

                
                /check - проверка работы бота, если отвечает, то ОК
                
                             
                /fileForKZD - скачать файлы для диспетчеров в КЗС
                
                
                /fileForOstrovskogo - островского
               
                                
                 
                Дополнительные команды:
                /help - получение справки
                """;

        var fomattedtext = String.format(text, username);
        sendMessage(chatId, fomattedtext);
    }


    private void sendMessage(Long chatId, String text) {
        var chatIdStr = String.valueOf(chatId);
        var sendMessage = new SendMessage(chatIdStr, text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка отправки сообщения", e);
        }

    }

    private void usdCommand(Long chatId) {
        String formattedText;
        try {
            var usd = exchangeRatesService.getUSDExchangeRate();
            var text = "Курс доллара на %s составляет %s рублей";
            emailService.sendEmailWithAttachment(recipients, "Test", "Test", null, "");
            formattedText = String.format(text, LocalDate.now(), usd);
        } catch (ServiceException e) {
            LOG.error("Ошибка при получении доллара", e);
            formattedText = "Не удалось получить текущий курс доллара. Попробуйте позже";

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        sendMessage(chatId, formattedText);
    }

    private void eurCommand(Long chatId) {
        String formattedText;
        try {
            var eur = exchangeRatesService.getEURExchangeRate();
            var text = "Курс евро на %s составляет %s рублей";
            formattedText = String.format(text, LocalDate.now(), eur);
        } catch (ServiceException e) {
            LOG.error("Ошибка при получении доллара", e);
            formattedText = "Не удалось получить текущий курс доллара. Попробуйте позже";

        }
        sendMessage(chatId, formattedText);
    }

    private void handleDayFiles(Long chatId, List<String> dayPointDirections, String fileType,int[][] periodProcessing) {
        try {






            for (int[] period : periodProcessing) {
                ExcelFile file = exchangeRatesService.getExcelFile("day", dayPointDirections, period[0], period[1], fileType);

                excelFilesDays.add(file);

            }


            // sendExcelFile(chatId, dayFile1);


        } catch (ServiceException e) {
            LOG.error("Ошибка при отправке файла Excel", e);
        }
    }

    private void handleDayFilesForTSO(Long chatId, List<String> dayPointDirections, String fileType,int[][] periodProcessing) {
        try {

            for (int[] period : periodProcessing) {
                ExcelFile file = exchangeRatesService.getExcelFileForTSO("day", dayPointDirections, period[0], period[1], fileType);

                excelFilesDays.add(file);

            }

        } catch (ServiceException e) {
            LOG.error("Ошибка при отправке файла Excel", e);
        }
    }

    private void handleHourFile(Long chatId, List<String> hourPointDirections, String fileType,int[][] periodProcessing) {
        try {




            for (int[] period : periodProcessing) {
                ExcelFile file = exchangeRatesService.getExcelFile("hour", hourPointDirections, period[0], period[1], fileType);
                excelFilesHours.add(file);
            }


        } catch (ServiceException e) {
            LOG.error("Ошибка при отправке файла Excel", e);
        }
    }


    private void helpCommand(Long chatId) {
        var text = """
                Справочная информация по боту
                                
                Для получения текущих курсов валют воспользуйтесь командами:
                /usd
                /eur
                /love
                                
                """;
        sendMessage(chatId, text);

    }

    private void unknownCommand(Long chatId) {
        var text = "Неизвестная команда";
        sendMessage(chatId, text);
    }


    public void sendGif(Long chatId) {
        SendAnimation sendAnimation = new SendAnimation();
        sendAnimation.setChatId(String.valueOf(chatId));
        sendAnimation.setAnimation(new InputFile(gifUrl)); // URL GIF файла
        try {
            execute(sendAnimation);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка при отправке GIF", e);
        }
    }
}
