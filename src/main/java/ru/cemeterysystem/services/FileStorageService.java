package ru.cemeterysystem.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    private final S3Client s3Client;

    @Value("${s3.bucket}")
    private String bucketName;

    @Value("${s3.endpoint}")
    private String endpoint;

    public String storeFile(MultipartFile file) {
        try {
            String fileName = generateFileName(file);
            String key = "diplomSS/" + fileName;
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

            s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return getFileUrl(key);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public void deleteFile(String fileUrl) {
        String key = extractFileNameFromUrl(fileUrl);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * Получает файл из S3 хранилища для просмотра или скачивания
     */
    public ResponseEntity<Resource> getFile(String fileUrl) {
        log.info("FileStorageService.getFile: начало получения файла по URL: {}", fileUrl);
        
        try {
            String key = extractKeyFromUrl(fileUrl);
            log.info("FileStorageService.getFile: извлеченный ключ S3: {}", key);
            log.info("FileStorageService.getFile: используемый bucket: {}", bucketName);
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

            log.info("FileStorageService.getFile: отправляем запрос в S3...");
            var s3Object = s3Client.getObject(getObjectRequest);
            log.info("FileStorageService.getFile: ответ от S3 получен, contentType: {}, contentLength: {}", 
                    s3Object.response().contentType(), s3Object.response().contentLength());
            
            // Определяем тип контента
            String contentType = s3Object.response().contentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
                log.info("FileStorageService.getFile: contentType не определен, используем по умолчанию: {}", contentType);
            }
            
            // Извлекаем имя файла из ключа
            String fileName = key.substring(key.lastIndexOf("/") + 1);
            log.info("FileStorageService.getFile: извлеченное имя файла: {}", fileName);
            
            Resource resource = new InputStreamResource(s3Object);
            
            ResponseEntity<Resource> response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
            
            log.info("FileStorageService.getFile: файл успешно подготовлен для отправки");
            return response;
                
        } catch (Exception e) {
            log.error("FileStorageService.getFile: ОШИБКА при получении файла {}: {}", fileUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve file: " + e.getMessage(), e);
        }
    }

    private String generateFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null ? 
            originalFileName.substring(originalFileName.lastIndexOf(".")) : "";
        return UUID.randomUUID().toString() + extension;
    }

    private String getFileUrl(String key) {
        return String.format("%s/%s/%s", endpoint, bucketName, key);
    }

    private String extractFileNameFromUrl(String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    /**
     * Извлекает ключ S3 из полного URL файла
     */
    private String extractKeyFromUrl(String fileUrl) {
        // URL имеет вид: https://s3.amazonaws.com/bucket/diplomSS/filename.ext
        // Нужно извлечь "diplomSS/filename.ext"
        String[] parts = fileUrl.split("/");
        if (parts.length >= 3) {
            // Начинаем с части после bucket name
            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 4; i < parts.length; i++) {
                if (keyBuilder.length() > 0) {
                    keyBuilder.append("/");
                }
                keyBuilder.append(parts[i]);
            }
            return keyBuilder.toString();
        }
        // Fallback - просто берем последнюю часть
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }
} 