package ru.cemeterysystem.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.cemeterysystem.models.Memorial;
import ru.cemeterysystem.models.User;
import ru.cemeterysystem.repositories.FamilyTreeRepository;
import ru.cemeterysystem.repositories.MemorialRepository;
import ru.cemeterysystem.repositories.NotificationRepository;
import ru.cemeterysystem.repositories.UserRepository;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    
    private final MemorialRepository memorialRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final FamilyTreeRepository familyTreeRepository;

    /**
     * Создает шрифт с поддержкой кириллицы
     */
    private PdfFont createCyrillicFont() throws Exception {
        try {
            // Пытаемся использовать системные шрифты Windows
            try {
                return PdfFontFactory.createFont("c:/windows/fonts/arial.ttf", "Identity-H");
            } catch (Exception e) {
                log.debug("Arial не найден, пробуем другие варианты");
            }
            
            try {
                return PdfFontFactory.createFont("c:/windows/fonts/times.ttf", "Identity-H");
            } catch (Exception e) {
                log.debug("Times не найден, пробуем другие варианты");
            }
            
            try {
                return PdfFontFactory.createFont("c:/windows/fonts/calibri.ttf", "Identity-H");
            } catch (Exception e) {
                log.debug("Calibri не найден, пробуем другие варианты");
            }
            
            // Если системные шрифты не найдены, используем встроенные с правильной кодировкой
            try {
                return PdfFontFactory.createFont(StandardFonts.HELVETICA, "Cp1251");
            } catch (Exception e) {
                // В крайнем случае используем стандартный
                return PdfFontFactory.createFont(StandardFonts.HELVETICA);
            }
        } catch (Exception e) {
            log.error("Ошибка создания шрифта: {}", e.getMessage());
            return PdfFontFactory.createFont(StandardFonts.HELVETICA);
        }
    }
    
    /**
     * Добавляет параграф с безопасной обработкой текста
     */
    private void addSafeParagraph(Document document, String text, float fontSize, boolean bold) {
        try {
            Paragraph p = new Paragraph(text != null ? text : "")
                    .setFontSize(fontSize);
            if (bold) {
                p.setBold();
            }
            document.add(p);
        } catch (Exception e) {
            // В случае ошибки добавляем простой текст
            document.add(new Paragraph(text != null ? text : "").setFontSize(fontSize));
        }
    }
    
    /**
     * Добавляет ячейку в таблицу с безопасной обработкой текста
     */
    private void addSafeCell(Table table, String text, boolean bold) {
        try {
            Cell cell = new Cell().add(new Paragraph(text != null ? text : ""));
            if (bold) {
                cell.add(new Paragraph(text != null ? text : "").setBold());
            }
            table.addCell(cell);
        } catch (Exception e) {
            // В случае ошибки добавляем простую ячейку
            table.addCell(new Cell().add(new Paragraph(text != null ? text : "")));
        }
    }

    /**
     * Генерация PDF отчёта по мемориалам
     */
    public byte[] generateMemorialsPdfReport(LocalDate startDate, LocalDate endDate, String status) {
        log.info("Генерация PDF отчёта по мемориалам");
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            
            // Используем шрифт с поддержкой кириллицы
            PdfFont font = createCyrillicFont();
            document.setFont(font);
            
            // Заголовок на русском
            addSafeParagraph(document, "ОТЧЁТ ПО МЕМОРИАЛАМ", 18, true);
            document.add(new Paragraph("\n"));
            
            // Информация о периоде
            String periodText = "Период: ";
            if (startDate != null && endDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                periodText += startDate.format(formatter) + " - " + endDate.format(formatter);
            } else {
                periodText += "За всё время";
            }
            addSafeParagraph(document, periodText, 12, false);
            
            // Статус фильтр
            if (status != null && !status.isEmpty()) {
                addSafeParagraph(document, "Статус: " + getStatusDisplayName(status), 12, false);
            }
            
            document.add(new Paragraph("\n"));
            
            // Создаем таблицу с русскими заголовками
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Заголовки таблицы на русском
            table.addHeaderCell(new Cell().add(new Paragraph("Показатель").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Значение").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            // Данные на русском
            table.addCell("Всего мемориалов");
            table.addCell(String.valueOf(memorialRepository.count()));
            
            table.addCell("Опубликованных");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PUBLISHED)));
            
            table.addCell("На модерации");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION)));
            
            table.addCell("Отклонённых");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.REJECTED)));
            
            table.addCell("Черновиков");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.DRAFT)));
            
            document.add(table);
            
            // Дата генерации на русском
            document.add(new Paragraph("\n"));
            addSafeParagraph(document, "Дата генерации: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), 10, false);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации PDF отчёта по мемориалам: {}", e.getMessage(), e);
            // Возвращаем простой текст в случае ошибки
            try {
                return createSimpleErrorPdf("Ошибка генерации PDF: " + e.getMessage());
            } catch (Exception ex) {
                return "Ошибка генерации PDF".getBytes();
            }
        }
    }
    
    private String getStatusDisplayName(String status) {
        switch (status) {
            case "PUBLISHED": return "Опубликованные";
            case "PENDING_MODERATION": return "На модерации";
            case "REJECTED": return "Отклонённые";
            case "DRAFT": return "Приватные";
            case "PRIVATE": return "Приватные";
            default: return status;
        }
    }

    /**
     * Генерация Excel отчёта по мемориалам
     */
    public byte[] generateMemorialsExcelReport(LocalDate startDate, LocalDate endDate, String status) throws UnsupportedEncodingException {
        log.info("Генерация Excel отчёта по мемориалам");
        
        // Создаем настоящий CSV с BOM для правильного отображения русского текста
        StringBuilder content = new StringBuilder();
        
        // Добавляем BOM для UTF-8
        content.append('\ufeff');
        
        // Заголовок
        content.append("ID;ФИО;Статус;Дата создания;Биография\n");
        
        // Получаем данные
        var memorials = memorialRepository.findAll();
        for (var memorial : memorials) {
            content.append(memorial.getId()).append(";");
            content.append('"').append(memorial.getFio() != null ? memorial.getFio() : "").append('"').append(";");
            content.append('"').append(getStatusDisplayName(memorial.getPublicationStatus().name())).append('"').append(";");
            content.append(memorial.getCreatedAt() != null ? memorial.getCreatedAt().toString() : "").append(";");
            content.append('"').append(memorial.getBiography() != null ? memorial.getBiography().replaceAll("\"", "\"\"") : "").append('"');
            content.append("\n");
        }
        
        return content.toString().getBytes("UTF-8");
    }

    /**
     * Генерация PDF отчёта по пользователям
     */
    public byte[] generateUsersPdfReport(LocalDate startDate, LocalDate endDate, String role) {
        log.info("Генерация PDF отчёта по пользователям");
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            
            // Используем шрифт с поддержкой кириллицы
            PdfFont font = createCyrillicFont();
            document.setFont(font);
            
            // Заголовок на русском
            addSafeParagraph(document, "ОТЧЁТ ПО ПОЛЬЗОВАТЕЛЯМ", 18, true);
            document.add(new Paragraph("\n"));
            
            // Информация о периоде
            String periodText = "Период: ";
            if (startDate != null && endDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                periodText += startDate.format(formatter) + " - " + endDate.format(formatter);
            } else {
                periodText += "За всё время";
            }
            addSafeParagraph(document, periodText, 12, false);
            
            // Роль фильтр
            if (role != null && !role.isEmpty()) {
                String roleText = role.equals("ADMIN") ? "Администраторы" : 
                                 role.equals("USER") ? "Пользователи" : "Все роли";
                addSafeParagraph(document, "Роль: " + roleText, 12, false);
            }
            
            document.add(new Paragraph("\n"));
            
            // Создаем таблицу статистики
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Заголовок таблицы на русском
            table.addHeaderCell(new Cell().add(new Paragraph("Показатель").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Значение").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            // Данные на русском
            table.addCell("Всего пользователей");
            table.addCell(String.valueOf(userRepository.count()));
            
            table.addCell("Администраторов");
            table.addCell(String.valueOf(userRepository.countByRole(User.Role.ADMIN)));
            
            table.addCell("Обычных пользователей");
            table.addCell(String.valueOf(userRepository.countByRole(User.Role.USER)));
            
            table.addCell("С подпиской");
            table.addCell(String.valueOf(userRepository.countByHasSubscription(true)));
            
            document.add(table);
            
            // Дата генерации на русском
            document.add(new Paragraph("\n"));
            addSafeParagraph(document, "Дата генерации: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), 10, false);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации PDF отчёта по пользователям: {}", e.getMessage(), e);
            try {
                return createSimpleErrorPdf("Ошибка генерации PDF: " + e.getMessage());
            } catch (Exception ex) {
                return "Ошибка генерации PDF".getBytes();
            }
        }
    }
    
    /**
     * Генерация Excel отчёта по пользователям
     */
    public byte[] generateUsersExcelReport(LocalDate startDate, LocalDate endDate, String role) throws UnsupportedEncodingException {
        log.info("Генерация Excel отчёта по пользователям");
        
        StringBuilder content = new StringBuilder();
        
        // Добавляем BOM для UTF-8
        content.append('\ufeff');
        
        content.append("ID;ФИО;Логин;Роль;Дата регистрации;Имеет подписку\n");
        
        var users = userRepository.findAll();
        for (var user : users) {
            content.append(user.getId()).append(";");
            content.append('"').append(user.getFio() != null ? user.getFio() : "").append('"').append(";");
            content.append('"').append(user.getLogin()).append('"').append(";");
            content.append('"').append(user.getRole().name()).append('"').append(";");
            content.append(user.getDateOfRegistration() != null ? user.getDateOfRegistration().toString() : "").append(";");
            content.append(user.getHasSubscription() ? "Да" : "Нет");
            content.append("\n");
        }
        
        return content.toString().getBytes("UTF-8");
    }
    
    /**
     * Генерация сводного PDF отчёта
     */
    public byte[] generateSummaryPdfReport(LocalDate startDate, LocalDate endDate) {
        log.info("Генерация сводного PDF отчёта");
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            
            // Используем шрифт с поддержкой кириллицы
            PdfFont font = createCyrillicFont();
            document.setFont(font);
            
            // Заголовок на русском
            addSafeParagraph(document, "СВОДНЫЙ ОТЧЁТ ПО СИСТЕМЕ", 18, true);
            document.add(new Paragraph("\n"));
            
            // Информация о периоде
            String periodText = "Период: ";
            if (startDate != null && endDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                periodText += startDate.format(formatter) + " - " + endDate.format(formatter);
            } else {
                periodText += "За всё время";
            }
            addSafeParagraph(document, periodText, 12, false);
            
            document.add(new Paragraph("\n"));
            
            // Создаем таблицу статистики
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Заголовок таблицы на русском
            table.addHeaderCell(new Cell().add(new Paragraph("Показатель").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Значение").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            // Статистика мемориалов на русском
            table.addCell(new Cell().add(new Paragraph("МЕМОРИАЛЫ").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addCell(new Cell().add(new Paragraph("").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            table.addCell("Всего мемориалов");
            table.addCell(String.valueOf(memorialRepository.count()));
            
            table.addCell("Опубликованных");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PUBLISHED)));
            
            table.addCell("На модерации");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION)));
            
            // Статистика пользователей на русском
            table.addCell(new Cell().add(new Paragraph("ПОЛЬЗОВАТЕЛИ").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addCell(new Cell().add(new Paragraph("").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            table.addCell("Всего пользователей");
            table.addCell(String.valueOf(userRepository.count()));
            
            table.addCell("Администраторов");
            table.addCell(String.valueOf(userRepository.countByRole(User.Role.ADMIN)));
            
            table.addCell("С подпиской");
            table.addCell(String.valueOf(userRepository.countByHasSubscription(true)));
            
            // Уведомления на русском
            table.addCell(new Cell().add(new Paragraph("УВЕДОМЛЕНИЯ").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addCell(new Cell().add(new Paragraph("").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            table.addCell("Всего уведомлений");
            table.addCell(String.valueOf(notificationRepository.count()));
            
            document.add(table);
            
            // Дата генерации на русском
            document.add(new Paragraph("\n"));
            addSafeParagraph(document, "Дата генерации: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), 10, false);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации сводного PDF отчёта: {}", e.getMessage(), e);
            try {
                return createSimpleErrorPdf("Ошибка генерации PDF: " + e.getMessage());
            } catch (Exception ex) {
                return "Ошибка генерации PDF".getBytes();
            }
        }
    }
    
    /**
     * Генерация отчёта по модерации в PDF
     */
    public byte[] generateModerationPdfReport(LocalDate startDate, LocalDate endDate) {
        log.info("Генерация PDF отчёта по модерации");
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            
            // Используем шрифт с поддержкой кириллицы
            PdfFont font = createCyrillicFont();
            document.setFont(font);
            
            // Заголовок на русском
            addSafeParagraph(document, "ОТЧЁТ ПО МОДЕРАЦИИ", 18, true);
            document.add(new Paragraph("\n"));
            
            // Информация о периоде
            String periodText = "Период: ";
            if (startDate != null && endDate != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                periodText += startDate.format(formatter) + " - " + endDate.format(formatter);
            } else {
                periodText += "За всё время";
            }
            addSafeParagraph(document, periodText, 12, false);
            
            document.add(new Paragraph("\n"));
            
            // Создаем таблицу статистики
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Заголовок таблицы на русском
            table.addHeaderCell(new Cell().add(new Paragraph("Показатель").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Значение").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            // Модерация мемориалов на русском
            table.addCell(new Cell().add(new Paragraph("МОДЕРАЦИЯ МЕМОРИАЛОВ").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addCell(new Cell().add(new Paragraph("").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            table.addCell("На модерации");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PENDING_MODERATION)));
            
            table.addCell("Одобрено");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.PUBLISHED)));
            
            table.addCell("Отклонено");
            table.addCell(String.valueOf(memorialRepository.countByPublicationStatus(Memorial.PublicationStatus.REJECTED)));
            
            // Семейные деревья на русском
            table.addCell(new Cell().add(new Paragraph("СЕМЕЙНЫЕ ДЕРЕВЬЯ").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addCell(new Cell().add(new Paragraph("").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            table.addCell("Всего деревьев");
            table.addCell(String.valueOf(familyTreeRepository.count()));
            
            document.add(table);
            
            // Дата генерации на русском
            document.add(new Paragraph("\n"));
            addSafeParagraph(document, "Дата генерации: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), 10, false);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации PDF отчёта по модерации: {}", e.getMessage(), e);
            try {
                return createSimpleErrorPdf("Ошибка генерации PDF: " + e.getMessage());
            } catch (Exception ex) {
                return "Ошибка генерации PDF".getBytes();
            }
        }
    }
    
    /**
     * Генерация быстрого PDF отчёта
     */
    public byte[] generateQuickPdfReport(String type, String content) {
        log.info("Генерация быстрого PDF отчёта: type={}", type);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            
            // Используем шрифт с поддержкой кириллицы
            PdfFont font = createCyrillicFont();
            document.setFont(font);
            
            // Заголовок на русском
            String title = getQuickReportTitle(type);
            addSafeParagraph(document, title, 18, true);
            
            document.add(new Paragraph("\n"));
            
            // Дата генерации на русском
            addSafeParagraph(document, "Дата генерации: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), 10, false);
            
            document.add(new Paragraph("\n"));
            
            // Создаем таблицу для лучшего форматирования
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            
            // Заголовок таблицы на русском
            table.addHeaderCell(new Cell().add(new Paragraph("Показатель").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Значение").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            
            // Парсим содержимое и добавляем в таблицу
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty() || !line.contains(":")) {
                    continue;
                }
                
                String[] parts = line.split(":", 2);
                if (parts.length == 2) {
                    table.addCell(parts[0].trim());
                    table.addCell(parts[1].trim());
                }
            }
            
            document.add(table);
            
            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Ошибка генерации быстрого PDF отчёта: {}", e.getMessage(), e);
            try {
                return createSimpleErrorPdf("Ошибка генерации PDF: " + e.getMessage());
            } catch (Exception ex) {
                return "Ошибка генерации PDF".getBytes();
            }
        }
    }
    
    private String getQuickReportTitle(String type) {
        switch (type) {
            case "pending-memorials": return "МЕМОРИАЛЫ НА МОДЕРАЦИИ";
            case "new-users": return "НОВЫЕ ПОЛЬЗОВАТЕЛИ";
            case "popular-memorials": return "ПОПУЛЯРНЫЕ МЕМОРИАЛЫ";
            case "subscription-stats": return "СТАТИСТИКА ПОДПИСОК";
            default: return "БЫСТРЫЙ ОТЧЁТ";
        }
    }
    
    private byte[] createSimpleErrorPdf(String errorMessage) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            
            document.add(new Paragraph("ОШИБКА ГЕНЕРАЦИИ ОТЧЁТА")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16)
                    .setBold());
            
            document.add(new Paragraph("\n"));
            document.add(new Paragraph(errorMessage));
            
            document.close();
            return baos.toByteArray();
        }
    }
} 