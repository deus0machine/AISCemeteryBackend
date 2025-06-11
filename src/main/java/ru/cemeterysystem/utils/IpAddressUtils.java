package ru.cemeterysystem.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Утилита для работы с IP адресами
 */
public class IpAddressUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(IpAddressUtils.class);
    
    /**
     * Получение реального IP адреса клиента с улучшенной обработкой
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            logger.debug("Request is null, returning 'unknown'");
            return "unknown";
        }
        
        logger.debug("=== IP Address Detection Started ===");
        
        // Проверяем заголовки прокси
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            logger.debug("Header {}: {}", header, ip);
            
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // Если в заголовке несколько IP (через запятую), берем первый
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                    logger.debug("Multiple IPs found, taking first: {}", ip);
                }
                
                // Нормализуем IP адрес
                String normalizedIp = normalizeIpAddress(ip);
                logger.debug("Normalized IP: {}", normalizedIp);
                
                if (isValidIpAddress(normalizedIp)) {
                    logger.debug("Valid IP found in header {}: {}", header, normalizedIp);
                    return normalizedIp;
                }
            }
        }
        
        // Если не нашли в заголовках, берем из request
        String remoteAddr = request.getRemoteAddr();
        logger.debug("RemoteAddr: {}", remoteAddr);
        
        String result = normalizeIpAddress(remoteAddr);
        logger.debug("Final result: {}", result);
        logger.debug("=== IP Address Detection Finished ===");
        
        return result;
    }
    
    /**
     * Нормализация IP адреса для лучшего отображения
     */
    public static String normalizeIpAddress(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return "unknown";
        }
        
        logger.debug("Normalizing IP: {}", ipAddress);
        
        // Обрабатываем IPv6 localhost
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
            logger.debug("IPv6 localhost detected, converting to IPv4");
            return "127.0.0.1 (localhost)";
        }
        
        // Обрабатываем IPv4 localhost
        if ("127.0.0.1".equals(ipAddress)) {
            logger.debug("IPv4 localhost detected");
            return "127.0.0.1 (localhost)";
        }
        
        // Сокращаем длинные IPv6 адреса
        if (ipAddress.contains(":") && ipAddress.length() > 20) {
            logger.debug("Long IPv6 address detected, attempting compression");
            // Преобразуем полную запись IPv6 в сокращенную
            try {
                // Простая оптимизация для отображения
                String compressed = compressIpv6(ipAddress);
                if (compressed.length() < ipAddress.length()) {
                    logger.debug("Compressed IPv6: {} -> {}", ipAddress, compressed);
                    return compressed;
                }
            } catch (Exception e) {
                logger.debug("Failed to compress IPv6: {}", e.getMessage());
                // Если не удалось сжать, возвращаем как есть
            }
        }
        
        logger.debug("Returning normalized IP: {}", ipAddress);
        return ipAddress;
    }
    
    /**
     * Проверка валидности IP адреса
     */
    public static boolean isValidIpAddress(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return false;
        }
        
        // Исключаем специальные значения
        String lower = ipAddress.toLowerCase();
        if (lower.contains("unknown") || lower.contains("localhost") || 
            "0.0.0.0".equals(ipAddress) || "null".equals(lower)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Простое сжатие IPv6 адреса для отображения
     */
    private static String compressIpv6(String ipv6) {
        if (!ipv6.contains(":")) {
            return ipv6;
        }
        
        // Убираем ведущие нули в группах
        String[] groups = ipv6.split(":");
        StringBuilder compressed = new StringBuilder();
        
        for (int i = 0; i < groups.length; i++) {
            if (i > 0) compressed.append(":");
            
            // Убираем ведущие нули
            String group = groups[i];
            if (group.length() > 1) {
                group = group.replaceFirst("^0+", "");
                if (group.isEmpty()) group = "0";
            }
            compressed.append(group);
        }
        
        String result = compressed.toString();
        
        // Заменяем последовательности :0:0: на ::
        result = result.replaceAll(":0:0:0:0:0:0:0:", "::");
        result = result.replaceAll(":0:0:0:0:0:0:", "::");
        result = result.replaceAll(":0:0:0:0:0:", "::");
        result = result.replaceAll(":0:0:0:0:", "::");
        result = result.replaceAll(":0:0:0:", "::");
        result = result.replaceAll(":0:0:", "::");
        
        return result.length() < ipv6.length() ? result : ipv6;
    }
    
    /**
     * Получение читаемого представления IP адреса
     */
    public static String getDisplayIpAddress(String ipAddress) {
        String normalized = normalizeIpAddress(ipAddress);
        
        // Добавляем подсказки для специальных адресов
        if (normalized.startsWith("192.168.")) {
            return normalized + " (локальная сеть)";
        } else if (normalized.startsWith("10.")) {
            return normalized + " (приватная сеть)";
        } else if (normalized.startsWith("172.")) {
            return normalized + " (приватная сеть)";
        } else if (normalized.contains("localhost")) {
            return normalized;
        }
        
        return normalized;
    }
} 