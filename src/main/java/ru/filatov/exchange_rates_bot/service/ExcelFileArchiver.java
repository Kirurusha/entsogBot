package ru.filatov.exchange_rates_bot.service;

import org.springframework.stereotype.Service;
import ru.filatov.exchange_rates_bot.entity.ExcelFile;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.*;

@Service
public class ExcelFileArchiver {

    private ZipOutputStream zos;  // Объявление переменной на уровне класса
    private String archivePath;
    private Set<String> existingEntries = new HashSet<>();

    // Инициализация ZipOutputStream перенесена в отдельный метод
    public void createArchive(String archiveName) throws IOException {
        this.archivePath = archiveName;

        FileOutputStream fos = new FileOutputStream(archivePath);
        this.zos = new ZipOutputStream(fos, StandardCharsets.UTF_8);  // Инициализация zos с указанием кодировки
        ZipEntry folderEntry = new ZipEntry("entsog_2/");
        zos.putNextEntry(folderEntry);
        zos.closeEntry();
    }

//    public void addFilesToArchive(String folderName, List<ExcelFile> files) throws IOException {
//        if (zos == null) {
//            throw new IllegalStateException("Archive is not initialized. Call createArchive first.");
//        }
//
//        if (files == null || files.isEmpty()) {
//            ZipEntry folderEntry = new ZipEntry("entsog_2/" + folderName + "/");
//            zos.putNextEntry(folderEntry);
//            zos.closeEntry();
//            return; // Завершаем выполнение метода после создания папки
//        }
//
//
//
//        for (ExcelFile file : files) {
//
//
//
//
//
//
//            String zipEntryName = "entsog_2/" + folderName +"/"+ file.getFilename();
//            zipEntryName = getUniqueEntryName(zipEntryName);
//            ZipEntry zipEntry = new ZipEntry(zipEntryName);
//            zos.putNextEntry(zipEntry);
//
//                try (InputStream in = file.getInputStream()) {
//                byte[] buffer = new byte[1024];
//                int length;
//                while ((length = in.read(buffer)) > 0) {
//                    zos.write(buffer, 0, length);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                throw new RuntimeException("Error adding file to archive: " + file.getFilename(), e);
//            }finally {
//                    zos.closeEntry();
//                }
//
//        }
//    }

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
            byte[] fileData;

            // Читаем данные файла в массив байтов
            try (InputStream in = file.getInputStream()) {
                fileData = in.readAllBytes();
            }

            // Проверяем размер файла
            if (fileData.length <= 2048) {
                System.out.println("File " + file.getFilename() + " is smaller than 2 KB and will be skipped.");
                continue;
            }

            // Создаем запись в архиве
            String zipEntryName = "entsog_2/" + folderName + "/" + file.getFilename();
            zipEntryName = getUniqueEntryName(zipEntryName);
            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipEntry);

            // Записываем данные файла в архив
            try {
                zos.write(fileData);
            } finally {
                zos.closeEntry();
            }
        }
    }



    private String getUniqueEntryName(String zipEntryName) {
        String uniqueName = zipEntryName;
        int count = 1;
        while(existingEntries.contains(uniqueName)) {
            int dotIndex =zipEntryName.lastIndexOf(".");
            if (dotIndex != -1) {
                String baseName = zipEntryName.substring(0, dotIndex);
                String extension = zipEntryName.substring(dotIndex);
                uniqueName = baseName + "_" + count  + extension;
            } else {
                uniqueName = zipEntryName + "_" + count;
            }
            count++;
        }
        existingEntries.add(uniqueName);
        return uniqueName;
    }

    public String closeArchive() throws IOException {
        if (zos != null) {
            zos.close();  // Закрытие ZipOutputStream
            zos = null;  // Обнуляем переменную для предотвращения повторного закрытия
        }
        return archivePath;
    }
}
