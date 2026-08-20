package com.workoutsmart.feed.service;

import com.workoutsmart.auth.exception.ApiException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu/đọc media bài đăng cộng đồng trên SeaweedFS filer (self-hosted, không phải external API).
 * Key chuẩn: workoutsmart-media/{uuid}.{ext} — UUID ngẫu nhiên nên không thể đoán.
 */
@Service
public class SeaweedStorageService {

    static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    static final String KEY_PREFIX = "workoutsmart-media/";

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private final String filerUrl;
    private final HttpClient httpClient;

    public SeaweedStorageService(@Value("${app.seaweedfs-filer-url:http://localhost:8888}") String filerUrl) {
        this.filerUrl = filerUrl.endsWith("/") ? filerUrl.substring(0, filerUrl.length() - 1) : filerUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    /** Upload media → StoredMedia(key, mediaType); file rỗng/null trả về null. */
    public StoredMedia store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String ext = extension(file.getOriginalFilename());
        MediaKind kind = classify(ext);
        long maxBytes = MAX_IMAGE_BYTES;
        if (file.getSize() > maxBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ảnh không được vượt quá 10MB");
        }
        String key = KEY_PREFIX + UUID.randomUUID() + "." + ext;
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(filerUrl + "/" + key))
                    .header("Content-Type", contentType)
                    .timeout(Duration.ofMinutes(2))
                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() -> {
                        try {
                            return file.getInputStream();
                        } catch (IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    }))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 400) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Không thể lưu media lên kho lưu trữ");
            }
            return new StoredMedia(key, kind.name().toLowerCase(Locale.ROOT));
        } catch (java.io.UncheckedIOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Không thể đọc file upload");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Không thể kết nối kho lưu trữ media");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Upload media bị gián đoạn");
        }
    }

    /** Xóa media theo key — lỗi kết nối không chặn luồng xóa bài đăng. */
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(filerUrl + "/" + key))
                    .timeout(CONNECT_TIMEOUT)
                    .DELETE()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            // SeaweedFS không chạy — vẫn cho xóa record bài đăng.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** URL công khai trên filer để redirect media. */
    public String mediaUrl(String key) {
        return filerUrl + "/" + key;
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private MediaKind classify(String ext) {
        if (IMAGE_EXTENSIONS.contains(ext)) {
            return MediaKind.IMAGE;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Chỉ hỗ trợ ảnh (PNG/JPG/JPEG/GIF/WEBP)");
    }

    enum MediaKind { IMAGE }

    public record StoredMedia(String key, String mediaType) {
    }
}
