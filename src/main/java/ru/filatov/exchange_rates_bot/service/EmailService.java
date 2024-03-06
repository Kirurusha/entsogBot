package ru.filatov.exchange_rates_bot.service;

import org.springframework.stereotype.Service;
import ru.filatov.exchange_rates_bot.entity.ExcelFile;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import java.io.File;
@Service
public class EmailService {
    private String fromEmail = "entsog4@gmail.com";
    private String password="ozygbkhtzbaqiwio";

    public void sendEmailWithAttachment(List<String> toEmails, String subject, String body, List<ExcelFile> excelFiles) throws IOException, MessagingException {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        InternetAddress[] recipientAddresses = toEmails.stream().map(email -> {
            try {
                return new InternetAddress(email);
            } catch (AddressException e) {
                e.printStackTrace();
                return null;
            }
        }).toArray(InternetAddress[]::new);

        message.setRecipients(Message.RecipientType.TO, recipientAddresses);


        message.setSubject(subject);

        Multipart multipart = new MimeMultipart();

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);
        multipart.addBodyPart(messageBodyPart);

        if (excelFiles != null && !excelFiles.isEmpty()) {
            for (ExcelFile excelFile : excelFiles) {
                File tempFile = File.createTempFile(excelFile.getFilename(), ".xlsx");
                tempFile.deleteOnExit();
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    InputStream inputStream = excelFile.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                BodyPart attachmentBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(tempFile);
                attachmentBodyPart.setDataHandler(new DataHandler(source));
                attachmentBodyPart.setFileName(excelFile.getFilename());
                multipart.addBodyPart(attachmentBodyPart);
            }
        }

        message.setContent(multipart);
        Transport.send(message);

        System.out.println("Email sent successfully with " + (excelFiles != null ? excelFiles.size() + " attachments." : "no attachments."));
    }
}
