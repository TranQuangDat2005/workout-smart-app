package com.workoutsmart.feed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void storeRejectsOversizedVideo() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("big.mp4");
        when(file.getSize()).thenReturn(SeaweedStorageService.MAX_VIDEO_BYTES + 1);

        ApiException ex = assertThrows(ApiException.class, () -> service.store(file));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Video không được vượt quá 100MB", ex.getMessage());
    }
}
