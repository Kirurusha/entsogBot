package ru.filatov.exchange_rates_bot.service;
import ru.filatov.exchange_rates_bot.service.JsonToExcelService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import ru.filatov.exchange_rates_bot.entity.ExcelFile;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
public class JsonToExcelService {
    private static final String ENDPOINT = "https://wabi-west-europe-d-primary-api.analysis.windows.net/public/reports/querydata?synchronous=true";
    private static final String RESOURCE_KEY = "f92e60f5-3a1e-499b-9361-33e9d8757529";
    public final Map<LocalDate, Boolean> processedDates = new HashMap<>();



    private void fetchData() {
        LocalDate beforeyesterday = LocalDate.now().minusDays(4);
        LocalDate yesterday = LocalDate.now().minusDays(3);
        LocalDate today = LocalDate.now().minusDays(2);

        // Запрашиваем данные за вчера и сегодня для Kaushany GMS (Moldova)
        fetchDataForDateAndPoint(beforeyesterday, "Kaushany GMS (Moldova)");
        fetchDataForDateAndPoint(yesterday, "Kaushany GMS (Moldova)");
        fetchDataForDateAndPoint(today, "Kaushany GMS (Moldova)");
        fetchDataForDateAndPoint(beforeyesterday, "Republic of Moldova");
        fetchDataForDateAndPoint(yesterday, "Republic of Moldova");
        fetchDataForDateAndPoint(today, "Republic of Moldova");


    }




    public ExcelFile downloadAndSaveData() throws IOException {
        Map<LocalDate, Map<String, String>> dataMap = new LinkedHashMap<>();

        // Запрашиваем данные
        processData(dataMap, beforeyesterday, "Kaushany GMS (Moldova)", "Каушаны_физика");
        processData(dataMap, yesterday, "Kaushany GMS (Moldova)", "Каушаны_физика");
        processData(dataMap, today, "Kaushany GMS (Moldova)", "Каушаны_физика");

        processData(dataMap, beforeyesterday, "Republic of Moldova", "Юг_Молдавии_физика");
        processData(dataMap, yesterday, "Republic of Moldova", "Юг_Молдавии_физика");
        processData(dataMap, today, "Republic of Moldova", "Юг_Молдавии_физика");

        // Создаём уникальное имя файла (например: exchange_rates_20250306.xlsx)
        String filename = "exchange_rates_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" + LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss")) + ".xlsx";

        // Генерируем Excel-файл
        ExcelFile excelFile = createExcelFile(dataMap, filename);

        return excelFile;
    }


    private void processData(Map<LocalDate, Map<String, String>> dataMap, LocalDate date, String point, String columnKey) {
        String value = fetchDataForDateAndPoint(date, point); // Вызов API и получение значения

        // Добавляем данные в структуру (сохраняем строку "н/д" или число)
        dataMap.computeIfAbsent(date, k -> new HashMap<>()).put(columnKey, value);
    }


    public static String  fetchDataForDateAndPoint(LocalDate date, String point) {
        LocalDate nextDay = date.plusDays(1);

        String requestBody = """
                {
                   "version": "1.0.0",
                   "modelId": 980485,
                   "queries": [
                     {
                       "QueryId": "",
                       "ApplicationContext": {
                         "DatasetId": "58a2f1f9-818a-4bf9-818b-a4cc57e3f179",
                         "Sources": [
                           {
                             "ReportId": "34cfd043-3779-4fec-9170-c39d231f20a9",
                             "VisualId": "09e5a6cf93cc41968065"
                           }
                         ]
                       },
                       "Query": {
                         "Commands": [
                           {
                             "SemanticQueryDataShapeCommand": {
                               "Query": {
                                 "Version": 2,
                                 "From": [
                                   {"Name": "p1", "Entity": "Physics", "Type": 0},
                                   {"Name": "d", "Entity": "Direction", "Type": 0},
                                   {"Name": "p", "Entity": "Points", "Type": 0},
                                   {"Name": "c", "Entity": "Calendar", "Type": 0}
                                 ],
                                 "Select": [
                                   {"Measure": {"Expression": {"SourceRef": {"Source": "p1"}}, "Property": "Фізичний потік, млн м3"}}
                                 ],
                                 "Where": [
                                   {
                                     "Condition": {
                                       "And": {
                                         "Left": {
                                           "Comparison": {
                                             "ComparisonKind": 2,
                                             "Left": {"Column": {"Expression": {"SourceRef": {"Source": "c"}}, "Property": "Період"}},
                                             "Right": {"Literal": {"Value": "datetime'%sT00:00:00'"}}
                                           }
                                         },
                                         "Right": {
                                           "Comparison": {
                                             "ComparisonKind": 3,
                                             "Left": {"Column": {"Expression": {"SourceRef": {"Source": "c"}}, "Property": "Період"}},
                                             "Right": {"Literal": {"Value": "datetime'%sT00:00:00'"}}
                                           }
                                         }
                                       }
                                     }
                                   },
                                   {
                                     "Condition": {
                                       "In": {
                                         "Expressions": [{"Column": {"Expression": {"SourceRef": {"Source": "p"}}, "Property": "[EnP] VIP name"}}],
                                         "Values": [[{"Literal": {"Value": "'%s'"}}]]
                                       }
                                     }
                                   },
                                                      {
                                                        "Condition": {
                                                          "In": {
                                                            "Expressions": [{"Column": {"Expression": {"SourceRef": {"Source": "d"}}, "Property": "Direction"}}],
                                                            "Values": [[{"Literal": {"Value": "'Exit'"}}]]
                                                          }
                                                        }
                                                      }
                                 ]
                               }
                             }
                           }
                         ]
                       }
                     }
                   ],
                   "cancelQueries": []
                 }
                """.formatted(date, nextDay, point);
        //System.out.println(requestBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("accept", "application/json, text/plain, */*")
                .header("accept-encoding", "gzip, deflate, br, zstd")
                .header("content-type", "application/json;charset=UTF-8")
                .header("origin", "https://app.powerbi.com")
                .header("referer", "https://app.powerbi.com/")
                .header("x-powerbi-resourcekey", RESOURCE_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            InputStream responseBodyStream = response.body();

            String encoding = response.headers().firstValue("content-encoding").orElse("");
            if ("gzip".equalsIgnoreCase(encoding)) {
                responseBodyStream = new GZIPInputStream(response.body());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(responseBodyStream, StandardCharsets.UTF_8));
            StringBuilder responseContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }

            JSONObject jsonResponse = new JSONObject(responseContent.toString());
            System.out.println(jsonResponse);
            return extractM0Value(jsonResponse);

        } catch (Exception e) {
            System.err.println("Ошибка при отправке запроса: " + e.getMessage());
            return "0.0";
        }
    }

    public boolean checkForValidData(LocalDate date) {
        String moldovaValue = fetchDataForDateAndPoint(date, "Republic of Moldova");
        String kaushanyValue = fetchDataForDateAndPoint(date, "Uzhgorod/Velke Kapushany GMS (Slovakia)");
        System.out.println("Republic of Moldova" + " " + moldovaValue);

        System.out.println("Uzhgorod/Velke Kapushany GMS (Slovakia)"+ " " +kaushanyValue);
        // Если хотя бы одно значение не "н/д", значит, данные есть
        return !moldovaValue.equals("н/д") || !kaushanyValue.equals("н/д");
    }


    private static String extractM0Value(JSONObject jsonObject) {
        try {
            JSONArray results = jsonObject.optJSONArray("results");
            if (results != null && results.length() > 0) {
                JSONObject resultObject = results.getJSONObject(0).optJSONObject("result");
                if (resultObject != null) {
                    JSONObject data = resultObject.optJSONObject("data");
                    if (data != null) {
                        JSONObject dsr = data.optJSONObject("dsr");
                        if (dsr != null) {
                            JSONArray dsArray = dsr.optJSONArray("DS");
                            if (dsArray != null && dsArray.length() > 0) {
                                JSONObject ds0 = dsArray.getJSONObject(0);
                                JSONArray phArray = ds0.optJSONArray("PH");
                                if (phArray != null && phArray.length() > 0) {
                                    JSONObject ph0 = phArray.getJSONObject(0);
                                    JSONArray dm0Array = ph0.optJSONArray("DM0");
                                    if (dm0Array != null && dm0Array.length() > 0) {
                                        JSONObject dm0Object = dm0Array.getJSONObject(0);

                                        // Проверяем наличие ключа "M0"
                                        if (dm0Object.has("M0")) {
                                            return String.valueOf(dm0Object.optDouble("M0", 0.0));
                                        } else {
                                            return "н/д"; // Возвращаем "н/д", если "M0" отсутствует
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка разбора JSON: " + e.getMessage());
        }
        return "н/д"; // Если произошла ошибка или нет данных
    }



    public File processJsonToExcel(InputStream jsonStream) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonStream);

        List<Map<String, Object>> records = parseJson(rootNode);

        return writeToExcel(records);
    }

    private List<Map<String, Object>> parseJson(JsonNode rootNode) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (rootNode == null || !rootNode.has("results")) {
            System.out.println("Ошибка: JSON не содержит results.");
            return result;
        }

        for (JsonNode resultNode : rootNode.get("results")) {
            if (!resultNode.has("result") || !resultNode.get("result").has("data")) {
                System.out.println("Ошибка: JSON не содержит result → data.");
                continue;
            }

            JsonNode dataNode = resultNode.get("result").get("data");
            JsonNode dsrNode = dataNode.path("dsr");

            if (!dsrNode.has("DS")) {
                System.out.println("Ошибка: JSON не содержит DS.");
                continue;
            }

            for (JsonNode dsNode : dsrNode.get("DS")) {
                if (!dsNode.has("PH")) continue;

                for (JsonNode phNode : dsNode.get("PH")) {
                    for (JsonNode dmNode : phNode) {
                        Map<String, Object> entry = new HashMap<>();

                        // Дата (из полей G3 - год, G4 - месяц, G5 - день)
                        int year = dmNode.has("G3") ? dmNode.get("G3").asInt() : 0;
                        int month = dmNode.has("G4") ? dmNode.get("G4").asInt() : 0;
                        int day = dmNode.has("G5") ? dmNode.get("G5").asInt() : 0;
                        String date = (year > 0 && month > 0 && day > 0) ? String.format("%04d-%02d-%02d", year, month, day) : "Неизвестно";

                        // Пункт (из полей G0, G1, G2)
                        String g0 = dmNode.has("G0") ? dmNode.get("G0").asText() : "Неизвестно";
                        String g1 = dmNode.has("G1") ? dmNode.get("G1").asText() : "Неизвестно";
                        String g2 = dmNode.has("G2") ? dmNode.get("G2").asText() : "Неизвестно";
                        String point = g0 + " / " + g1 + " / " + g2;

                        // Значение (M0)
                        double value = dmNode.has("M0") ? dmNode.get("M0").asDouble() : 0.0;

                        entry.put("Дата", date);
                        entry.put("Пункт", point);
                        entry.put("Значение", value);

                        result.add(entry);
                    }
                }
            }
        }

        return result;
    }






    public ExcelFile createExcelFile(Map<LocalDate, Map<String, String>> data, String filename) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Данные");

        // Заголовки
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Дата", "Каушаны_физика", "Юг_Молдавии_физика"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Создаём стиль для даты
        CellStyle dateCellStyle = workbook.createCellStyle();
        CreationHelper creationHelper = workbook.getCreationHelper();
        dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("dd.MM.yyyy"));

        // Создаём стиль для чисел с запятой
        CellStyle numberCellStyle = workbook.createCellStyle();
        numberCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("#,##0.00"));

        // Данные
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (Map.Entry<LocalDate, Map<String, String>> entry : data.entrySet()) {
            Row row = sheet.createRow(rowNum++);

            // Устанавливаем дату в нужном формате
            Cell dateCell = row.createCell(0);
            dateCell.setCellValue(entry.getKey().format(formatter)); // Форматируем дату как строку
            dateCell.setCellStyle(dateCellStyle); // Применяем стиль

            // Данные
            String kaushanyValue = entry.getValue().getOrDefault("Каушаны_физика", "н/д");
            String moldovaValue = entry.getValue().getOrDefault("Юг_Молдавии_физика", "н/д");

            // Создаём ячейки
            Cell kaushanyCell = row.createCell(1);
            Cell moldovaCell = row.createCell(2);

            // Если значение "н/д" — вставляем как текст
            if ("н/д".equals(kaushanyValue)) {
                kaushanyCell.setCellValue("н/д");
            } else {
                try {
                    double number = Double.parseDouble(kaushanyValue.replace(",", ".")); // Поддержка чисел с запятой
                    kaushanyCell.setCellValue(number);
                    kaushanyCell.setCellStyle(numberCellStyle); // Применяем числовой стиль
                } catch (NumberFormatException e) {
                    kaushanyCell.setCellValue(kaushanyValue); // Если ошибка, записываем как текст
                }
            }

            if ("н/д".equals(moldovaValue)) {
                moldovaCell.setCellValue("н/д");
            } else {
                try {
                    double number = Double.parseDouble(moldovaValue.replace(",", ".")); // Поддержка чисел с запятой
                    moldovaCell.setCellValue(number);
                    moldovaCell.setCellStyle(numberCellStyle); // Применяем числовой стиль
                } catch (NumberFormatException e) {
                    moldovaCell.setCellValue(moldovaValue);
                }
            }
        }

        // Автоширина колонок
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return new ExcelFile(new ByteArrayInputStream(outputStream.toByteArray()), filename);
    }




    private File writeToExcel(List<Map<String, Object>> records) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        // Создаем заголовки
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Дата", "Пункт", "Значение"};
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Заполняем данными
        int rowNum = 1;
        for (Map<String, Object> record : records) {
            Row row = sheet.createRow(rowNum++);

            // Достаем данные и превращаем в строковые значения
            String date = record.getOrDefault("Дата", "Неизвестно").toString();
            String point = record.getOrDefault("Пункт", "Неизвестно").toString();
            double value = (double) record.getOrDefault("Значение", 0.0);

            row.createCell(0).setCellValue(date);
            row.createCell(1).setCellValue(point);
            row.createCell(2).setCellValue(value);
        }

        // Сохраняем в файл
        File outputFile = new File("output.xlsx");
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }
        workbook.close();

        return outputFile;
    }
}
