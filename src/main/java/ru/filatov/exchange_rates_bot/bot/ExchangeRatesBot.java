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
import ru.filatov.exchange_rates_bot.service.ExchangeRatesService;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static List<ExcelFile> excelFilesDays = new ArrayList<>();
    private static List<ExcelFile> excelFilesHours= new ArrayList<>();


    private static final List<String> DAY_POINTS_SET_1 = Arrays.asList(
//            "bg-tso-0001itp-00041entry",
//            "bg-tso-0001itp-00549entry",
//            "bg-tso-0001itp-00529exit",
//            "bg-tso-0001itp-00128exit",
//            "bg-tso-0001itp-00128entry",
//            "bg-tso-0001itp-00036exit",
//            "ua-tso-0001itp-00117exit",
//            "at-tso-0001itp-00162entry",
//            "at-tso-0001itp-00062entry",
//            "at-tso-0003itp-00037entry",
//            "de-tso-0001itp-00096exit",
//            "sk-tso-0001itp-00051exit"


            "ua-tso-0001itp-00117exit",
            "at-tso-0001itp-00162entry",
            "at-tso-0001itp-00062entry",
            "at-tso-0003itp-00037entry"


    );
    private static final List<String> DAY_POINTS_SET_2 = Arrays.asList(
//            "sk-tso-0001itp-00117entry",
//            "cz-tso-0001itp-00051exit",
//            "sk-tso-0001itp-00051exit",
//            "sk-tso-0001itp-00168exit",
//            "de-tso-0001itp-00096entry",
//            "bg-tso-0001itp-00058exit",
//            "sk-tso-0001itp-00168exit",
//            "de-tso-0001itp-00096entry",
//            "de-tso-0001itp-00096exit",
//            "hu-tso-0001itp-10013entry",
//            "hu-tso-0001itp-10013entry",
//            "bg-tso-0001itp-00058exit"


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











    String gifUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExODFxM2E4b3l2djZoNDd0bXRldGkwOXh4cjFjY3djbzEyd3B6MmFpMSZlcD12MV9naWZzX3NlYXJjaCZjdD1n/5kpRuktAKcNXy4p1is/giphy.gif";
    @Autowired
    private ExchangeRatesService exchangeRatesService;
    @Autowired
    private EmailService emailService;
    public ExchangeRatesBot(@Value("${bot.token}") String botToken) {
        super(botToken);

    }
    public void fetchAndProcessData() {
        System.out.println("Загрузка и обработка данных. Текущее время: " + LocalDateTime.now());
        // Логика для загрузки и обработки данных
        long delay =3000;

        try {
            handleDayFiles(null, DAY_POINTS_SET_1);

            Thread.sleep(delay );

            handleHourFile(null, DAY_POINTS_SET_1);
            Thread.sleep(delay );

            handleDayFiles(null, DAY_POINTS_SET_2);
            Thread.sleep(delay );
            handleHourFile(null, DAY_POINTS_SET_2);
            Thread.sleep(delay );


            emailService.sendEmailWithAttachment("kirillfilatoww@mail.ru", "Files from Entsog with daily data", "Test", excelFilesDays);
            emailService.sendEmailWithAttachment("kirillfilatoww@mail.ru", "Files from Entsog with hourly data", "Test", excelFilesHours);

            excelFilesHours.clear();
            excelFilesDays.clear();


        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public void onUpdateReceived(Update update) {
        long delay =5000;
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
            case EXCEL -> {
                try {
                handleDayFiles(chatId, DAY_POINTS_SET_1);

                    Thread.sleep(delay );

                handleHourFile(chatId, DAY_POINTS_SET_1);
                Thread.sleep(delay );

                handleDayFiles(chatId, DAY_POINTS_SET_2);
                Thread.sleep(delay );
                handleHourFile(chatId, DAY_POINTS_SET_2);
                Thread.sleep(delay );

                    emailService.sendEmailWithAttachment("kirillfilatoww@mail.ru", "Files from Entsog with daily data", "Test", excelFilesDays);
                    emailService.sendEmailWithAttachment("kirillfilatoww@mail.ru", "Files from Entsog with hourly data", "Test", excelFilesHours);

                    excelFilesHours.clear();
                    excelFilesDays.clear();


                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


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
    private void sendExcelFile(Long chatId,ExcelFile excelFileStream) {
        // Логика для вызова API и получения файла Excel
        // Предположим, что у вас есть метод в сервисе, который возвращает InputStream файла
        try {

            String fileName = "rates.xlsx";
            SendDocument sendDocumentRequest = new SendDocument();
            sendDocumentRequest.setChatId(String.valueOf(chatId));
            sendDocumentRequest.setDocument(new InputFile(excelFileStream.getInputStream(),excelFileStream.getFilename()));
            execute(sendDocumentRequest);
        } catch ( TelegramApiException e) {
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
                /usd - курс доллара
                /eur - курс евро
                /love - кнопка для Анастасии
                /excel - отправить файл
                
                 
                Дополнительные команды:
                /help - получение справки
                """;

        var fomattedtext = String.format(text, username);
        sendMessage(chatId,fomattedtext);
    }


    private void sendMessage(Long chatId, String text) {
        var chatIdStr = String.valueOf(chatId);
        var sendMessage = new SendMessage(chatIdStr, text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e ) {
            LOG.error("Ошибка отправки сообщения", e);
        }

    }

    private void usdCommand(Long chatId) {
        String formattedText;
        try {
            var usd = exchangeRatesService.getUSDExchangeRate();
            var text = "Курс доллара на %s составляет %s рублей";
            emailService.sendEmailWithAttachment("kirillfilatoww@mail.ru", "Test", "Test",null);
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
    private void handleDayFiles(Long chatId, List<String> dayPointDirections) {
        try {

            String[] fileTypes = { "Physical%20Flow","Renomination", "GCV", "Nomination"};
            int[][] periods = {{11, -8}, {7, -4}, {3, 1}};

            for (String fileType : fileTypes) {
                for (int[] period : periods) {
                    ExcelFile file = exchangeRatesService.getExcelFile("day", dayPointDirections, period[0], period[1], fileType);

                    excelFilesDays.add(file);

                }
            }


            // sendExcelFile(chatId, dayFile1);


        } catch (ServiceException  e) {
            LOG.error("Ошибка при отправке файла Excel", e);
        }
    }

    private void handleHourFile(Long chatId, List<String> hourPointDirections) {
        try {
            // Файл для часов\
            String[] fileTypes = { "Physical%20Flow"};
            int[][] periods = {{2, -2}, {1, -1}, {0, 0}, {-1, 1}};

            for (String fileType : fileTypes) {
                for (int[] period : periods) {
                    ExcelFile file = exchangeRatesService.getExcelFile("hour", hourPointDirections, period[0], period[1], fileType);
                    excelFilesHours.add(file);
                }
            }


        } catch (ServiceException  e) {
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
        sendMessage(chatId,text);

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
