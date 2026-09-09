package com.workoutsmart.feed.service;

import com.workoutsmart.auth.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu/đọc media bài đăng cộng đồng trên SeaweedFS filer (self-hosted, không phải external API).
 * Key chuẩn: workoutsmart-media/{uuid}.{ext} — UUID ngẫu nhiên nên không thể đoán.
 * FR-014: kiểm tra magic bytes (không chỉ extension) — R6.
 */
@Service
public class SeaweedStorageService {

    static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    static final String KEY_PREFIX = "workoutsmart-media/";
    static final int MAGIC_BYTES_CHECK = 512;

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
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ảnh không được vượt quá 10MB");
        }
        // FR-014 / E6.5: extension suy từ magic bytes — không tin vào tên file
        // (chặn GIF/video đổi đuôi .jpg; file không có extension vẫn nhận).
        String ext = detectImageExt(file);
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
            return new StoredMedia(key, "image");
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
    
    /** Đọc media qua backend để trình duyệt không phải phân giải hostname nội bộ Docker. */
    public MediaContent fetch(String key) {
        if (key == null || !key.startsWith(KEY_PREFIX) || key.contains("..")) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy media");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mediaUrl(key)))
                    .timeout(CONNECT_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            String contentType = response.headers().firstValue("Content-Type")
                    .orElse("application/octet-stream");
            return new MediaContent(response.statusCode(), contentType, response.body());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Không thể kết nối kho lưu trữ media");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Đọc media bị gián đoạn");
        }
    }

    /** FR-014 / R6 / E6.5: đọc ≤512 bytes đầu, định type theo magic bytes (không theo tên file). */
    private String detectImageExt(MultipartFile file) {
        byte[] header = new byte[MAGIC_BYTES_CHECK];
        int read;
        try (InputStream is = file.getInputStream()) {
            read = is.read(header);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Không thể đọc file upload");
        }
        if (read < 16) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File quá nhỏ, không phải ảnh hợp lệ");
        }
        // PNG: 89 50 4E 47
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return "png";
        }
        // JPEG: FF D8 FF
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return "jpg";
        }
        // WEBP: RIFF (0-3) + WEBP (8-11)
        if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
            return "webp";
        }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "Chỉ chấp nhận ảnh PNG/JPG/JPEG/WEBP. File có nội dung GIF/video hoặc không hợp lệ");
    }

    public record StoredMedia(String key, String mediaType) {
    }
    
    public record MediaContent(int statusCode, String contentType, byte[] body) {
    }
}
