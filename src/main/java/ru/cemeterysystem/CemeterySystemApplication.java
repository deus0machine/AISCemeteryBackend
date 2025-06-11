package ru.cemeterysystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CemeterySystemApplication {

    public static void main(String[] args) {
        // Настройки для лучшей работы с IP адресами
        // Предпочитаем IPv4 над IPv6 для локальной разработки
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");
        
        SpringApplication.run(CemeterySystemApplication.class, args);
    }

}
