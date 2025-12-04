package com.jayaa.ecommerce.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
public class FileStorageUtil {

    private final Path uploadPath;

    public FileStorageUtil(@Value("${file.upload-dir}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create upload directory", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            if (originalFilename.contains("..")) {
                throw new RuntimeException("Invalid file path: " + originalFilename);
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Only image files are allowed");
            }

            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex);
            }
            String storedFilename = UUID.randomUUID().toString() + extension;

            Path targetLocation = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return storedFilename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file: " + originalFilename, ex);
        }
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println("Could not delete file: " + filename);
        }
    }
}