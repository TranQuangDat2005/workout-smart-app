package com.workoutsmart.exercise.service;

import com.workoutsmart.auth.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Lưu media người dùng upload (chỉ cho bài tập cá nhân) — FR-008 (011). */
@Service
public class MediaUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path uploadDir;

    public MediaUploadService(@Value("${app.user-media-path:./uploads/media/}") String userMediaPath) {
        this.uploadDir = Path.of(userMediaPath).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File rỗng");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File không được vượt quá 5MB");
        }
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận PNG/JPG/JPEG/GIF/WEBP");
        }

        try {
            Files.createDirectories(uploadDir);
            String filename = UUID.randomUUID() + "." + ext;
            Path target = uploadDir.resolve(filename).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "user/" + filename;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu file");
        }
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
