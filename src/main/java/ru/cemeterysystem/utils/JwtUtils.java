package ru.cemeterysystem.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import ru.cemeterysystem.models.User;

import java.security.Key;
import java.util.Date;

public class JwtUtils {
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256); // Генерирует безопасный ключ

    public static String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getLogin())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(SECRET_KEY)
                .compact();
    }
    public static Key getSecretKey() {
        return SECRET_KEY; // Метод для получения ключа
    }
}