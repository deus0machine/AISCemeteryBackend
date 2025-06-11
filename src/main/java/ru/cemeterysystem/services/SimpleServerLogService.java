package ru.cemeterysystem.services;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SimpleServerLogService {
    
    private List<LogEntry> mockLogs = new ArrayList<>();
    
    public SimpleServerLogService() {
        // Добавляем несколько тестовых логов
        addMockLog("INFO", "Application started successfully");
        addMockLog("DEBUG", "Database connection established");
        addMockLog("WARN", "Low memory warning");
        addMockLog("ERROR", "Failed to process request");
        addMockLog("INFO", "User authentication successful");
    }
    
    private void addMockLog(String level, String message) {
        LogEntry entry = new LogEntry();
        entry.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        entry.level = level;
        entry.logger = "ru.cemeterysystem";
        entry.message = message;
        entry.thread = "main";
        mockLogs.add(entry);
    }
    
    public List<LogEntry> getRecentLogs(int limit) {
        return mockLogs.size() > limit ? 
            mockLogs.subList(Math.max(0, mockLogs.size() - limit), mockLogs.size()) : 
            mockLogs;
    }
    
    public static class LogEntry {
        public String timestamp;
        public String level;
        public String logger;
        public String message;
        public String thread;
    }
} 