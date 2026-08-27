package com.workoutsmart.feed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.workoutsmart.auth.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class SeaweedStorageServiceTest {

    private final SeaweedStorageService service = new SeaweedStorageService("http://localhost:8888/");

    @Test
    void storeReturnsNullForMissingOrEmptyFile() {
        assertNull(service.store(null));
        assertNull(service.store(new MockMultipartFile("media", new byte[0])));
    }

    @Test
    void storeRejectsGifExtension() {
        byte[] gifData = new byte[20];
        gifData[0] = 0x47; gifData[1] = 0x49; gifData[2] = 0x46; gifData[3] = 0x38;
        gifData[4] = 0x39; gifData[5] = 0x61; // GIF89a
        MockMultipartFile file = new MockMultipartFile("media", "anim.gif", "image/gif", gifData);
        ApiException ex = assertThrows(ApiException.class, () -> service.store(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("GIF"));
    }

    @Test
    void storeRejectsUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile("media", "doc.txt", "text/plain", "x".getBytes());
        ApiException ex = assertThrows(ApiException.class, () -> service.store(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void storeRejectsOversizedImage() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("big.png");
        when(file.getSize()).thenReturn(SeaweedStorageService.MAX_IMAGE_BYTES + 1);

        ApiException ex = assertThrows(ApiException.class, () -> service.store(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Ảnh không được vượt quá 10MB", ex.getMessage());
    }

    @Test
    void storeAcceptsPngMagicBytes() {
        byte[] png = new byte[20];
        png[0] = (byte) 0x89; png[1] = 0x50; png[2] = 0x4E; png[3] = 0x47;
        MockMultipartFile file = new MockMultipartFile("media", "photo.png", "image/png", png);
        try {
            SeaweedStorageService.StoredMedia result = service.store(file);
            assertEquals("png", result.key().substring(result.key().lastIndexOf('.') + 1));
        } catch (ApiException e) {
            assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        }
    }

    @Test
    void storeAcceptsJpegMagicBytes() {
        byte[] jpeg = new byte[20];
        jpeg[0] = (byte) 0xFF; jpeg[1] = (byte) 0xD8; jpeg[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile("media", "photo.jpg", "image/jpeg", jpeg);
        try {
            SeaweedStorageService.StoredMedia result = service.store(file);
            assertEquals("jpg", result.key().substring(result.key().lastIndexOf('.') + 1));
        } catch (ApiException e) {
            assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        }
    }

    @Test
    void storeAcceptsWebpMagicBytes() {
        byte[] webp = new byte[20];
        webp[0] = 0x52; webp[1] = 0x49; webp[2] = 0x46; webp[3] = 0x46;
        webp[8] = 0x57; webp[9] = 0x45; webp[10] = 0x42; webp[11] = 0x50;
        MockMultipartFile file = new MockMultipartFile("media", "photo.webp", "image/webp", webp);
        try {
            SeaweedStorageService.StoredMedia result = service.store(file);
            assertEquals("webp", result.key().substring(result.key().lastIndexOf('.') + 1));
        } catch (ApiException e) {
            assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        }
    }

    @Test
    void storeDerivesExtensionWhenFileHasNone() {
        byte[] png = new byte[20];
        png[0] = (byte) 0x89; png[1] = 0x50; png[2] = 0x4E; png[3] = 0x47;
        // Không có extension — extension phải suy từ magic bytes
        MockMultipartFile file = new MockMultipartFile("media", "photo", "image/png", png);
        try {
            SeaweedStorageService.StoredMedia result = service.store(file);
            assertEquals("png", result.key().substring(result.key().lastIndexOf('.') + 1));
        } catch (ApiException e) {
            assertEquals(HttpStatus.BAD_GATEWAY, e.getStatus());
        }
    }

    @Test
    void storeRejectsGifMagicBytes() {
        byte[] gif = new byte[20];
        gif[0] = 0x47; gif[1] = 0x49; gif[2] = 0x46; gif[3] = 0x38;
        gif[4] = 0x39; gif[5] = 0x61; // GIF89a
        MockMultipartFile file = new MockMultipartFile("media", "photo.jpg", "image/jpeg", gif);
        ApiException ex = assertThrows(ApiException.class, () -> service.store(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("GIF/video"));
    }

    @Test
    void storeRejectsVideoMagicBytes() {
        byte[] mp4 = new byte[20];
        mp4[4] = 0x66; mp4[5] = 0x74; mp4[6] = 0x79; mp4[7] = 0x70; // ftyp
        MockMultipartFile file = new MockMultipartFile("media", "video.jpg", "image/jpeg", mp4);
        ApiException ex = assertThrows(ApiException.class, () -> service.store(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("GIF/video"));
    }

    @Test
    void storeRejectsDisguisedVideoAsJpg() {
        byte[] avi = new byte[20];
        avi[0] = 0x52; avi[1] = 0x49; avi[2] = 0x46; avi[3] = 0x46; // RIFF (not WEBP)
        MockMultipartFile file = new MockMultipartFile("media", "trick.jpg", "image/jpeg", avi);
        ApiException ex = assertThrows(ApiException.class, () -> service.store(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("GIF/video"));
    }

    @Test
    void storeRejectsTooSmallFile() {
        byte[] tiny = new byte[]{0x00, 0x01};
        MockMultipartFile file = new MockMultipartFile("media", "tiny.png", "image/png", tiny);
        ApiException ex = assertThrows(ApiException.class, () -> service.store(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("quá nhỏ"));
    }
}
