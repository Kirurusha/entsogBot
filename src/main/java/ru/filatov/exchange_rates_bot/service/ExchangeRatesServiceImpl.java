package ru.filatov.exchange_rates_bot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import ru.filatov.exchange_rates_bot.Exception.ServiceException;
import ru.filatov.exchange_rates_bot.client.CbrClient;
import ru.filatov.exchange_rates_bot.entity.ExcelFile;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ExchangeRatesServiceImpl implements ExchangeRatesService {

    private static final String USD_XPATH = "ValCurs//Valute[@ID='R01235']/Value";
    private static final String EUR_XPATH = "ValCurs//Valute[@ID='R01239']/Value";

    @Autowired
    private CbrClient client;

    @Override
    public String getUSDExchangeRate() throws  ServiceException {
        var xml = client.getCurrencyRatesXML();
        return exctractCurrencyValueFromXML(xml, USD_XPATH);





    }

    @Override
    public String getEURExchangeRate() throws ServiceException {
        var xml = client.getCurrencyRatesXML();
        return exctractCurrencyValueFromXML(xml, EUR_XPATH);
    }

    @Override
    public ExcelFile  getExcelFile(String periodType, List<String> pointDirections, int daysBefore, int daysAfter, String reqType) throws ServiceException {

        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = currentDate.minusDays(daysBefore);
        LocalDate endDate = currentDate.plusDays(daysAfter);

        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String formattedEndDate = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String baseUrl = "https://transparency.entsog.eu/api/v1/operationalData.xlsx";


        String pointDirectionsParam = String.join(",", pointDirections);

        String queryParams = String.format(
                "?forceDownload=true&isTransportData=true&dataset=1&from=%s&to=%s&indicators=%s&periodType=%s&timezone=CET&periodize=0&limit=-1&pointDirection=%s",
                formattedStartDate, formattedEndDate,reqType, periodType, URLEncoder.encode(pointDirectionsParam, StandardCharsets.UTF_8)
        );
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatDateTime = now.format(formatter);

        String fullUrl = baseUrl + queryParams;
        System.out.println(fullUrl +" " + formatDateTime  );


        int maxAttempts = 60;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    InputStream inputStream = response.body();
                    Optional<String> contentDisposition = response.headers().firstValue("Content-Disposition");
                    String filename = contentDisposition.map(cd -> cd.split("filename=")[1].replaceAll("\"", "")).orElse("default_filename.xlsx");
                    return new ExcelFile(inputStream, filename);
                } else if (response.statusCode() >= 500) {
                    if (attempt < maxAttempts) {
                        System.out.println("Получена ошибка 500, попытка номер " + attempt);
                        Thread.sleep(1000); // Задержка в 1 секунду перед следующей попыткой
                        continue;
                    } else {
                        throw new ServiceException("Ошибка при получении файла Excel: HTTP статус " + response.statusCode(), new IOException());
                    }
                } else {
                    throw new ServiceException("Ошибка при получении файла Excel: HTTP статус " + response.statusCode(), new IOException());
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("Ошибка при скачивании файла Excel", e);
            }
        }
        return null;

    }

    @Override
    public ExcelFile getExcelFileForTSO(String periodType, List<String> pointDirections, int daysBefore, int daysAfter, String reqType) throws SecurityException, ServiceException {

        LocalDate currentDate = LocalDate.now();
        LocalDate startDate = currentDate.minusDays(daysBefore);
        LocalDate endDate = currentDate.plusDays(daysAfter);

        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String formattedEndDate = endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String baseUrl = "https://transparency.entsog.eu/api/v1/operationalData.xlsx";


        String pointDirectionsParam = String.join(",", pointDirections);

        //https://transparency.entsog.eu/api/v1/operationalData.xlsx
        // from=2024-03-04&to=2024-03-10&indicator=Nomination,Renomination,Allocation,Physical%20Flow,GCV&periodType=day&timezone=CET&periodize=0&limit=-1&isTransportData=true&dataset=1&operatorLabel=eustream,GAZ-SYSTEM,FGSZ,Transgaz,Gas TSO UA

        String queryParams = String.format(
                "?forceDownload=true&from=%s&to=%s&indicator=%s&periodType=%s&timezone=CET&periodize=0&limit=-1&isTransportData=true&dataset=1&operatorLabel=%s",
                formattedStartDate, formattedEndDate,reqType, periodType, URLEncoder.encode(pointDirectionsParam, StandardCharsets.UTF_8)
        );
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formatDateTime = now.format(formatter);

        String fullUrl = baseUrl + queryParams;
        System.out.println(fullUrl +" " + formatDateTime  );


        int maxAttempts = 60;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(fullUrl))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    InputStream inputStream = response.body();
                    Optional<String> contentDisposition = response.headers().firstValue("Content-Disposition");
                    String filename = contentDisposition.map(cd -> cd.split("filename=")[1].replaceAll("\"", "")).orElse("default_filename.xlsx");
                    return new ExcelFile(inputStream, filename);
                } else if (response.statusCode() >= 500) {
                    if (attempt < maxAttempts) {
                        System.out.println("Получена ошибка 500, попытка номер " + attempt);
                        Thread.sleep(1000); // Задержка в 1 секунду перед следующей попыткой
                        continue;
                    } else {
                        throw new ServiceException("Ошибка при получении файла Excel: HTTP статус " + response.statusCode(), new IOException());
                    }
                } else {
                    throw new ServiceException("Ошибка при получении файла Excel: HTTP статус " + response.statusCode(), new IOException());
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("Ошибка при скачивании файла Excel", e);
            }
        }
        return null;
    }


    private static String exctractCurrencyValueFromXML(String xml, String xpathExpression) throws ServiceException {
        var source = new InputSource(new StringReader(xml));

        try {
            var xpath = XPathFactory.newInstance().newXPath();
            var document = (Document) xpath.evaluate("/", source, XPathConstants.NODE);
            return xpath.evaluate(xpathExpression, document);
        } catch (XPathExpressionException e) {
            throw new ServiceException("Не удалось распарсить XML", e);
        }
    }
}
