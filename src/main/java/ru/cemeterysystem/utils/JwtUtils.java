package ru.cemeterysystem.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import ru.cemeterysystem.Models.Guest;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

public class JwtUtils {
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256); // Генерирует безопасный ключ

    public static String generateToken(Guest guest) {
        return Jwts.builder()
                .setSubject(guest.getLogin())
                .claim("role", guest.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 часа
                .signWith(SECRET_KEY) // Используй сгенерированный ключ
                .compact();
    }
    public static Key getSecretKey() {
        return SECRET_KEY; // Метод для получения ключа
    }
}