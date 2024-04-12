package ru.filatov.exchange_rates_bot.service;

import ru.filatov.exchange_rates_bot.Exception.ServiceException;
import ru.filatov.exchange_rates_bot.entity.ExcelFile;

import java.io.InputStream;
import java.util.List;

public interface ExchangeRatesService {
    String getUSDExchangeRate() throws SecurityException, ServiceException;
    String getEURExchangeRate() throws SecurityException, ServiceException;


    ExcelFile getExcelFile(String periodType, List<String> pointDirections, int daysBefore, int daysAfter,String reqType) throws SecurityException, ServiceException;
    ExcelFile getExcelFileForTSO(String periodType, List<String> pointDirections, int daysBefore, int daysAfter,String reqType) throws SecurityException, ServiceException;


    ExcelFile getExcelFileAGSI() throws SecurityException, ServiceException;
}
