package ru.filatov.exchange_rates_bot.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
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
    private static final String STARTGROUP= "/start@newWathesForPronBot_bot";
    private static final String USD = "/usd";
    private static final String EUR = "/eur";
    private static final String HELP = "/help";
    private static final String HELPGROUP = "/help@newWathesForPronBot_bot";
    private static final String LOVE = "/love";
    private static final String EXCEL = "/excel";
    private static final String CHECK = "/check";
    private static final String KZD = "/fileforkzd";
    private static final String OST = "/fileforostrovskogo";
    private static final String AGSI = "/agsi";
    private static final String AGSITEST = "/agsitest";

    private static final String physicalflow = "Physical%20Flow";
    private static final String renomination = "Renomination";
    private static final String nomination = "Nomination";
    private static final String allocation = "Allocation";
    private static final String gcv = "GCV";

    private static final String allTypesNominations = "Nomination,Renomination,Allocation,Physical%20Flow,GCV";






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


    private static final List<ExcelFile> excelFilesDays = new ArrayList<>();
    private static final List<ExcelFile> excelFilesHours = new ArrayList<>();
    private static final List<ExcelFile> excelFilesAGSI = new ArrayList<>();
    private static List<String> recipients = Arrays.asList("kirillfilatoww@mail.ru", "operatorsouth@gazpromexport.gazprom.ru"
           , "operator@gazpromexport.gazprom.ru", "cpdd-export@adm.gazprom.ru", "BoetzOperator@yandex.ru");
    private static List<String> recipientsExport = Arrays.asList("kirillfilatoww@mail.ru", "cpdd-export@adm.gazprom.ru");
    private static List<String> recipientsTest = Arrays.asList("kirillfilatoww@mail.ru");


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
    private InlineKeyboardMarkup createInlineKeyboard() {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        // Создание первой строки кнопок
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("Загрузить файлы для Островского");
        button1.setCallbackData("fileforostrovskogo");
        row1.add(button1);
        buttons.add(row1);

        // Создание второй строки кнопок
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("Загрузить файлы для КЗС");
        button2.setCallbackData("fileforkzd");
        row2.add(button2);
        buttons.add(row2);

        // Создание второй строки кнопок
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton button3 = new InlineKeyboardButton();
        button3.setText("Загрузить AGSI");
        button3.setCallbackData("agsi");
        row3.add(button3);
        buttons.add(row3);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(buttons);
        return inlineKeyboardMarkup;
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
            excelFileArchiver.addFilesToArchive("nominations", null);



            emailService.sendEmailWithAttachment(recipients, "Данные для сводок Транзит Болгария Словакия", "Коллеги, такой файл, будет выгружаться" +
                    " в 4 35 часов утра по Москве и приходить к вам на почту на ежедневной основе. Если перестанет работать, то пишите", null, excelFileArchiver.closeArchive());


            excelFilesHours.clear();
            excelFilesDays.clear();


        } catch (InterruptedException | MessagingException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void fetchAndProcessDataAGSI() {
        System.out.println("Загрузка и обработка данных. Текущее время: " + LocalDateTime.now());
        // Логика для загрузки и обработки данных
        long delay = 1000;


        try {
            handleAGSI();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            String formatDateTime = now.format(formatter);
            String archiveName = "AGSI-" + formatDateTime + ".zip";

            excelFileArchiver.createArchive(archiveName);

            excelFileArchiver.addFilesToArchive("AGSI", excelFilesAGSI);

            emailService.sendEmailWithAttachment(recipientsExport, "Данные для сводки Динамика использования ПХГ Украины и реверсных поставок газа", "Коллеги, такой файл, будет выгружаться" +
                    " в 21:41  по Москве . Если перестанет работать, то пишите", null, excelFileArchiver.closeArchive());
            excelFilesAGSI.clear();

        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }

    }
    public void fetchAndProcessDataAGSITest() {
        System.out.println("Загрузка и обработка данных для тестовой загрузки. Текущее время: " + LocalDateTime.now());
        // Логика для загрузки и обработки данных

        try {
            handleAGSI();
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            String formatDateTime = now.format(formatter);
            String archiveName = "AGSI-" + formatDateTime + ".zip";

            excelFileArchiver.createArchive(archiveName);

            excelFileArchiver.addFilesToArchive("AGSI", excelFilesAGSI);

            emailService.sendEmailWithAttachment(recipientsTest, "Тест работоспособности загрузчика", "Тест работоспособности загрузчика", null, excelFileArchiver.closeArchive());
            excelFilesAGSI.clear();

        } catch (MessagingException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void editMessage(Long chatId, Integer messageId, String newText) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка редактирования сообщения", e);
        }
    }

    public String formattedTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId.toString());
        deleteMessage.setMessageId(messageId);
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка удаления сообщения", e);
        }
    }



    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            // Обработка текстовых сообщений
            var message = update.getMessage();
            var messageText = message.getText();
            var chatId = message.getChatId();
            var userName = message.getChat().getUserName();
            System.out.println(chatId);
            if (chatId != 598389393 && !userName.equals("kirillfilatoww")) {

                sendMessage(598389393L, "Кто-то другой отправляет сообщения в Entsog bot " + userName);
            }

            switch (messageText) {
                case START, STARTGROUP -> {
                    startCommand(chatId, userName);
                }
                case HELP ,HELPGROUP-> {
                    helpCommand(chatId);
                }
                case CHECK -> {
                    unknownCommand(chatId);
                }
                case KZD -> {
                    sendMessage(chatId, "Началась загрузка файлов для КЗС");
                    fetchAndProcessDataForExport();
                    fetchAndProcessDataForExportTSO();
                    fetchAndProcessDataAGSI();
                    sendMessage(chatId, "Файлы для КЗС успешно направлены");
                }
                case OST -> {
                    sendMessage(chatId, "Началась загрузка файлов для пл.Островского");
                    fetchAndProcessData();
                    sendMessage(chatId, "Файлы для пл.Островского успешно отправлены");
                }
                case AGSI -> {
                    sendMessage(chatId, "Началась загрузка файлов из AGSI");
                    fetchAndProcessDataAGSI();
                    sendMessage(chatId, "Файлы из AGSI успешно загружены и направлены");
                }
                case AGSITEST -> {
                    sendMessage(chatId, "Началась тестовая загрузка файлов из AGSI");
                    fetchAndProcessDataAGSITest();
                    sendMessage(chatId, "Тестовые файлы из AGSI успешно загружены и направлены");
                }
                default -> unknownCommand(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            // Обработка callback query
            var query = update.getCallbackQuery();
            var chatId = query.getMessage().getChatId();
            var data = query.getData();
            var callbackQueryId = query.getId();
            String userName = query.getFrom().getUserName();

            if (chatId != 598389393) {
                sendMessage(598389393L, "Кто-то другой отправляет сообщения в Entsog bot " + userName);
            }

            // Уведомляем пользователя, что процесс начался
            SendMessage loadingMessage = new SendMessage();
            loadingMessage.setChatId(chatId.toString());
            loadingMessage.setText("Процесс загрузки начался в " + formattedTime());
            answerCallbackQuery(callbackQueryId, "Процесс загрузки начался", false);

            try {
                Message message = execute(loadingMessage);

                switch (data) {
                    case "fileforostrovskogo" -> {
                        fetchAndProcessData();
                        editMessage(chatId, message.getMessageId(), "Процесс загрузки завершен в " + formattedTime() + " сообщение удалится через 2 секунды");
                        Thread.sleep(2000);
                        deleteMessage(chatId, message.getMessageId());
                    }
                    case "fileforkzd" -> {
                        fetchAndProcessDataForExport();
                        fetchAndProcessDataForExportTSO();
                        fetchAndProcessDataAGSI();
                        editMessage(chatId, message.getMessageId(), "Процесс загрузки завершен в " + formattedTime() + " сообщение удалится через 2 секунды");
                        Thread.sleep(2000);
                        deleteMessage(chatId, message.getMessageId());
                    }
                    case "agsi" -> {
                        fetchAndProcessDataAGSI();
                        editMessage(chatId, message.getMessageId(), "Процесс загрузки завершен в " + formattedTime() + " сообщение удалится через 2 секунды");
                        Thread.sleep(2000);
                        deleteMessage(chatId, message.getMessageId());
                    }
                }
            } catch (TelegramApiException | InterruptedException e) {
                LOG.error("Ошибка обработки процесса", e);
            }
        } else {
            LOG.warn("Получено обновление без сообщения или callback query");
        }
    }



    @Override
    public String getBotUsername() {
        return "filatov_bot";
    }

    private void startCommand(Long chatId, String username) {
        ClassPathResource imageResource = new ClassPathResource("static/images/image.jpg");
        try {
            // Создаем объект SendPhoto для отправки изображения
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(chatId.toString());
            sendPhoto.setPhoto(new InputFile(imageResource.getInputStream(), "image.png"));

            // Добавляем подпись к изображению
            String caption = String.format(
                    "Добро пожаловать в бот, %s!\n\n" +
                            "Здесь вы сможете скачать данные Entsog и AGSI!\n" +
                            "Для загрузки данных нажмите на одну из кнопок ниже.\n\n" +
                            "/help - получение справки", username);
            sendPhoto.setCaption(caption);

            // Добавляем кнопки
            sendPhoto.setReplyMarkup(createInlineKeyboard());

            // Отправляем изображение
            execute(sendPhoto);
        } catch (TelegramApiException | IOException e) {
            LOG.error("Ошибка отправки изображения", e);
        }}


    public void sendMessage(Long chatId, String text) {
        var chatIdStr = String.valueOf(chatId);
        var sendMessage = new SendMessage(chatIdStr, text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка отправки сообщения", e);
        }

    }
    public void sendMessage(Long chatId, String text, InlineKeyboardMarkup replyMarkup) {
        var chatIdStr = String.valueOf(chatId);
        var sendMessage = new SendMessage(chatIdStr, text);
        sendMessage.setReplyMarkup(replyMarkup);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка отправки сообщения", e);
        }
    }
    public Message sendMessageAndGetId(Long chatId, String text) {
        var chatIdStr = String.valueOf(chatId);
        var sendMessage = new SendMessage(chatIdStr, text);
        try {
            return execute(sendMessage);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка отправки сообщения", e);
            return null;
        }
    }
    private void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        answer.setText(text);
        answer.setShowAlert(showAlert); // true для показа алерта, false для уведомления
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            LOG.error("Ошибка отправки уведомления", e);
        }
    }


//    private void usdCommand(Long chatId) {
//        String formattedText;
//        try {
//            var usd = exchangeRatesService.getUSDExchangeRate();
//            var text = "Курс доллара на %s составляет %s рублей";
//            emailService.sendEmailWithAttachment(recipients, "Test", "Test", null, "");
//            formattedText = String.format(text, LocalDate.now(), usd);
//        } catch (ServiceException e) {
//            LOG.error("Ошибка при получении доллара", e);
//            formattedText = "Не удалось получить текущий курс доллара. Попробуйте позже";
//
//        } catch (MessagingException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        sendMessage(chatId, formattedText);
//    }

    private void handleDayFiles(Long chatId, List<String> dayPointDirections, String fileType,int[][] periodProcessing) {
        try {
            for (int[] period : periodProcessing) {
                ExcelFile file = exchangeRatesService.getExcelFile("day", dayPointDirections, period[0], period[1], fileType);
                excelFilesDays.add(file);
            }
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

    private void handleAGSI() {
        try {

            ExcelFile file = exchangeRatesService.getExcelFileAGSI();
                excelFilesAGSI.add(file);


        } catch (ServiceException e) {
            LOG.error("Ошибка при отправке файла Excel", e);
        }
    }


    private void helpCommand(Long chatId) {
        var text = """
                Справочная информация по боту
                                
                Для скачивания файлов воспользуйтесь командами:
                
                /fileforkzd - скачать данные для построения сводок в КЗС
                /fileforostrovskogo - скачать данные для построения сводок на пл. Островского
                /agsi - скачать только AGSI
                /agsitest 
          
                """;
        sendMessage(chatId, text);

    }

    private void unknownCommand(Long chatId) {
        var text = "Бот работает!";
        sendMessage(chatId, text);
    }



}
