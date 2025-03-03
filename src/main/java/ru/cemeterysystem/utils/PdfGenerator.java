package ru.cemeterysystem.utils;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import org.springframework.stereotype.Component;
import ru.cemeterysystem.models.Order;

import java.io.ByteArrayOutputStream;
import java.util.List;
@Component
public class PdfGenerator {
    public byte[] generatePdf(List<Order> orders) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Создание документа PDF
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            // Заголовок
            String fontPath = "src/main/resources/fonts/ArialRegular.ttf"; // Укажите путь к файлу шрифта
            PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
            document.setFont(font);

            // Заголовок
            document.add(new Paragraph("Список заказов").setFont(font).setBold().setFontSize(16));

            // Таблица
            float[] columnWidths = {1, 4, 4, 3, 2, 3, 2};
            Table table = new Table(columnWidths);

            // Шапка таблицы
            table.addCell("ID");
            table.addCell("Заказчик");
            table.addCell("Захоронение");
            table.addCell("Услуга");
            table.addCell("Цена");
            table.addCell("Дата");
            table.addCell("Статус");

            // Заполнение данными
            for (Order order : orders) {
                table.addCell(String.valueOf(order.getId()));
                table.addCell(order.getUser().getFio());
                table.addCell(order.getMemorial().getFio());
                table.addCell(order.getOrderName());
                table.addCell(order.getOrderCost().toString());
                table.addCell(order.getOrderDate().toString());
                table.addCell(order.isCompleted() ? "Выполнен" : "В процессе");
            }

            document.add(table);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при генерации PDF", e);
        }
    }
}
