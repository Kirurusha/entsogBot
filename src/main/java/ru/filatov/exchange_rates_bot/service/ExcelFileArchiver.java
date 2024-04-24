package ru.filatov.exchange_rates_bot.service;

import org.springframework.stereotype.Service;
import ru.filatov.exchange_rates_bot.entity.ExcelFile;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.*;

@Service
public class ExcelFileArchiver {

    private ZipOutputStream zos;  // Объявление переменной на уровне класса
    private String archivePath;

    // Инициализация ZipOutputStream перенесена в отдельный метод
    public void createArchive(String archiveName) throws IOException {
        this.archivePath = archiveName;

        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder();
        FileOutputStream fos = new FileOutputStream(archivePath);
        OutputStreamWriter osw = new OutputStreamWriter(fos, encoder);
        this.zos = new ZipOutputStream(fos);  // Инициализация zos
        ZipEntry folderEntry = new ZipEntry("entsog_2/");
        zos.putNextEntry(folderEntry);
        zos.closeEntry();
    }

    public void addFilesToArchive(String folderName, List<ExcelFile> files) throws IOException {
        if (zos == null) {
            throw new IllegalStateException("Archive is not initialized. Call createArchive first.");
        }

        if (files == null || files.isEmpty()) {
            ZipEntry folderEntry = new ZipEntry("entsog_2/" + folderName + "/");
            zos.putNextEntry(folderEntry);
            zos.closeEntry();
            return; // Завершаем выполнение метода после создания папки
        }

        for (ExcelFile file : files) {
            ZipEntry zipEntry = new ZipEntry("entsog_2/" + folderName + "/" + file.getFilename());
            zos.putNextEntry(zipEntry);
            try (InputStream in = file.getInputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }
            zos.closeEntry();
        }
    }

    public String closeArchive() throws IOException {
        if (zos != null) {
            zos.close();  // Закрытие ZipOutputStream
        }
        return archivePath;
    }
}
