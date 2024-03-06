package ru.filatov.exchange_rates_bot.entity;

import java.io.InputStream;

public class ExcelFile {
    private final InputStream inputStream;
    private final String filename;

    public ExcelFile(InputStream inputStream, String filename) {
        this.inputStream = inputStream;
        this.filename = filename;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getFilename() {
        return filename;
    }
}

