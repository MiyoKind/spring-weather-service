package com.miyo.weatherservice.services;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailAutoSender
{

    private static final Logger logs = LoggerFactory.getLogger(EmailAutoSender.class);

    public void sendEmail(String fileName)
    {
        try
        {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.ssl.enable","true");
            props.put("mail.smtp.host", "smtp.client");
            props.put("mail.smtp.port", 465);

            Session session = Session.getInstance(props, new javax.mail.Authenticator()
            {
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    return new PasswordAuthentication("your@e-mail.address", "yourpassword");
                }
            });
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("miyoempl@yandex.ru", false));

            msg.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("shevchenkoap@tne.transneft.ru"));
            msg.setSubject("Погода за час (test)");
            msg.setSentDate(new Date());

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Отчёт по погоде в представленных городах", "utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            MimeBodyPart attachPart = new MimeBodyPart();
            attachPart.attachFile(fileName);
            multipart.addBodyPart(attachPart);
            msg.setContent(multipart);
            Transport.send(msg);
            logs.info("Email sent successfully");
        }
        catch (Exception exe)
        {
            logs.error("Email sending failed!");
            System.out.println();
            exe.printStackTrace();
        }
    }
}