package ru.cemeterysystem.Services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.Models.Order;
import ru.cemeterysystem.Repositories.OrderRepository;
import ru.cemeterysystem.utils.PdfGenerator;
import java.util.*;

@Service
public class AdminService {
    private final OrderRepository orderRepository;
    private final JavaMailSender mailSender;
    private final PdfGenerator pdfGenerator;
    @Autowired
    public AdminService(JavaMailSender mailSender, PdfGenerator pdfGenerator, OrderRepository orderRepository) {
        this.mailSender = mailSender;
        this.pdfGenerator = pdfGenerator;
        this.orderRepository = orderRepository;
    }

    public void sendPdfToEmail(String email) throws MessagingException {
        List<Order> orders = (List<Order>) orderRepository.findAll();
        // Генерация PDF
        byte[] pdfBytes = pdfGenerator.generatePdf(orders); // Возвращает PDF в виде массива байтов
        email = email.replace("\"", "");
        if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
            throw new IllegalArgumentException("Некорректный email: " + email);
        }
        // Создание письма
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("sergdinamit@mail.ru");
        helper.setTo(email);
        helper.setSubject("Ваш документ");
        helper.setText("Добрый день, в приложении отправляем ваш документ.");
        helper.addAttachment("document.pdf", new ByteArrayResource(pdfBytes));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            System.err.println("Ошибка при отправке письма: " + e.getMessage());
            throw e;
        }
    }
}
