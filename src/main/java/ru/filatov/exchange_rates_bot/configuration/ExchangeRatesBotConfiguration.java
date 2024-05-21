package ru.filatov.exchange_rates_bot.configuration;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.filatov.exchange_rates_bot.bot.ExchangeRatesBot;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class ExchangeRatesBotConfiguration {
   @Bean
    public TelegramBotsApi telegramBotsApi(ExchangeRatesBot exchangeRatesBot) throws TelegramApiException {
        var api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(exchangeRatesBot);
        return api;

    }


    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .retryOnConnectionFailure(true) // Включение повторных попыток при сбое соединения
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    Response response = null;
                    boolean responseOk = false;
                    int tryCount = 0;
                    while (tryCount < 3 && !responseOk) {
                        try {
                            response = chain.proceed(request);
                            responseOk = response.isSuccessful();
                        } catch (IOException e) {
                            tryCount++;
                            if (tryCount >= 3) {
                                throw e;
                            }
                        }
                    }
                    return response;
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }




}
